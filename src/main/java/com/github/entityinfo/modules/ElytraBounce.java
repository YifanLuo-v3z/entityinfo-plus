package com.github.entityinfo.modules;

import com.mojang.blaze3d.platform.InputConstants;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.ClientTickEvent;
import com.github.epsilon.events.impl.FallFlyingEvent;
import com.github.epsilon.events.impl.MoveEvent;
import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.events.impl.PlayerTickEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class ElytraBounce extends Module {

    public static final ElytraBounce INSTANCE = new ElytraBounce();

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgControl = settingGroup("Control");
    private final SettingGroup sgSafety = settingGroup("Safety");
    private final SettingGroup sgDebug = settingGroup("Debug");

    private final DoubleSetting divePitch = doubleSetting("Dive Pitch", 85.0D, -90.0D, 90.0D, 1.0D).group(sgGeneral);
    private final DoubleSetting liftPitch = doubleSetting("Lift Pitch", -35.0D, -90.0D, 90.0D, 1.0D).group(sgGeneral);
    private final DoubleSetting relaunchDelay = doubleSetting("Relaunch Delay", 0.5D, 0.0D, 5.0D, 0.05D).group(sgGeneral);
    private final BoolSetting instantStart = boolSetting("Instant Start", true).group(sgGeneral);
    private final BoolSetting autoJump = boolSetting("Auto Jump", true).group(sgControl);
    private final BoolSetting autoRun = boolSetting("Auto Run", true).group(sgControl);
    private final BoolSetting sprint = boolSetting("Sprint", true).group(sgControl);
    private final BoolSetting chunkStop = boolSetting("Chunk Stop", true).group(sgSafety);
    private final BoolSetting stopOnRubberband = boolSetting("Stop On Rubberband", true).group(sgSafety);
    private final BoolSetting speedCap = boolSetting("Speed Cap", false).group(sgSafety);
    private final DoubleSetting speedCapKmh = doubleSetting("Speed Cap KMH", 110.0D, 20.0D, 400.0D, 5.0D, speedCap::getValue).group(sgSafety);
    private final BoolSetting debugViewLock = boolSetting("Debug View Lock", false).group(sgDebug);
    private final BoolSetting debugYawLock = boolSetting("Debug Yaw Lock", false).group(sgDebug);
    private final DoubleSetting debugYaw = doubleSetting("Debug Yaw", 0.0D, -180.0D, 180.0D, 1.0D, debugYawLock::getValue).group(sgDebug);

    private long lastRelaunchMs;
    private boolean rubberbanded;
    private boolean forcedSprint;

    private ElytraBounce() {
        super("Elytra Bounce", Category.MOVEMENT);
    }

    @EventHandler
    private void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {
            resetTransientState();
            return;
        }

        if (!hasElytraEquipped()) {
            resetTransientState();
            return;
        }

        if (autoJump.getValue()) {
            mc.options.keyJump.setDown(true);
        } else {
            restoreKeyState(mc.options.keyJump);
        }

        applyDebugRotationLocks();
        boolean canControl = canControlElytra();

        if (instantStart.getValue()
            && !mc.player.isFallFlying()
            && canControl
            && !mc.player.onGround()
            && mc.player.getDeltaMovement().y < 0.0D
            && hasRelaunchDelayElapsed()
            && (!rubberbanded || !stopOnRubberband.getValue())) {
            sendStartFallFlying();
        }

        if (!canControl) {
            releaseControlState();
            return;
        }

        updateMovementKeys();

        if (sprint.getValue()) {
            mc.player.setSprinting(true);
            forcedSprint = true;
        } else {
            releaseSprintState();
        }
    }

    @EventHandler
    private void onFallFlying(FallFlyingEvent event) {
        if (nullCheck() || !hasElytraEquipped() || !mc.player.isFallFlying()) {
            return;
        }

        event.setPitch(getFlightPitch());
    }

    @EventHandler
    private void onMove(MoveEvent event) {
        if (nullCheck() || !chunkStop.getValue() || !mc.player.isFallFlying() || !hasElytraEquipped()) {
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        if (!mc.level.hasChunk(playerPos.getX() >> 4, playerPos.getZ() >> 4)) {
            event.setX(0.0D);
            event.setY(0.0D);
            event.setZ(0.0D);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof ClientboundPlayerPositionPacket) || mc.player == null || !hasElytraEquipped()) {
            return;
        }

        // Ignore unrelated ground corrections so they do not block the next elytra relaunch.
        if (!mc.player.onGround()) {
            rubberbanded = true;
        }
    }

    @EventHandler
    private void onClientTickPost(ClientTickEvent.Post event) {
        if (mc.player == null) {
            resetTransientState();
            return;
        }

        if (!hasElytraEquipped()) {
            resetTransientState();
            return;
        }

        applyDebugRotationLocks();

        if (mc.player.onGround() || !mc.player.isFallFlying()) {
            rubberbanded = false;
        }
    }

    @Override
    protected void onDisable() {
        resetTransientState();
    }

    private void updateMovementKeys() {
        if (!autoRun.getValue()) {
            restoreKeyState(mc.options.keyUp);
            restoreKeyState(mc.options.keyDown);
            return;
        }

        double speedKmh = mc.player.getDeltaMovement().horizontalDistance() * 20.0D * 3.6D;
        if (speedCap.getValue() && speedKmh > speedCapKmh.getValue()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(true);
        } else {
            mc.options.keyUp.setDown(true);
            mc.options.keyDown.setDown(false);
        }
    }

    private void releaseMovementKeys() {
        restoreKeyState(mc.options.keyJump);
        restoreKeyState(mc.options.keyUp);
        restoreKeyState(mc.options.keyDown);
    }

    private void releaseControlState() {
        releaseMovementKeys();
        releaseSprintState();
    }

    private void releaseSprintState() {
        if (!forcedSprint) {
            return;
        }

        forcedSprint = false;
        if (mc.player == null) {
            return;
        }

        restoreKeyState(mc.options.keySprint);
        if (!mc.options.keySprint.isDown()) {
            mc.player.setSprinting(false);
        }
    }

    private void restoreKeyState(KeyMapping key) {
        if (mc.getWindow() == null) {
            return;
        }

        if (key.isUnbound()) {
            key.setDown(false);
            return;
        }

        InputConstants.Key boundKey = InputConstants.getKey(key.saveString());
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            key.setDown(GLFW.glfwGetMouseButton(mc.getWindow().handle(), boundKey.getValue()) == GLFW.GLFW_PRESS);
            return;
        }

        key.setDown(InputConstants.isKeyDown(mc.getWindow(), boundKey.getValue()));
    }

    private void applyDebugRotationLocks() {
        if (!debugViewLock.getValue() && !debugYawLock.getValue()) {
            return;
        }

        if (debugViewLock.getValue()) {
            float lockedPitch = getFlightPitch();
            mc.player.setXRot(lockedPitch);
            mc.player.xRotO = lockedPitch;
        }

        if (debugYawLock.getValue()) {
            float lockedYaw = debugYaw.getValue().floatValue();
            mc.player.setYRot(lockedYaw);
            mc.player.yRotO = lockedYaw;
            mc.player.yHeadRot = lockedYaw;
            mc.player.yHeadRotO = lockedYaw;
            mc.player.yBodyRot = lockedYaw;
            mc.player.yBodyRotO = lockedYaw;
        }
    }

    private boolean canControlElytra() {
        return !mc.player.getAbilities().flying
            && mc.player.getVehicle() == null
            && !mc.player.onClimbable()
            && !mc.player.isInWater()
            && !mc.player.isUnderWater()
            && !mc.player.isInLava()
            && hasElytraEquipped();
    }

    private boolean hasElytraEquipped() {
        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        return LivingEntity.canGlideUsing(chestStack, EquipmentSlot.CHEST);
    }

    private float getFlightPitch() {
        return shouldUseLiftPitch()
            ? liftPitch.getValue().floatValue()
            : divePitch.getValue().floatValue();
    }

    private boolean shouldUseLiftPitch() {
        return mc.player != null
            && mc.player.isFallFlying()
            && mc.player.getDeltaMovement().y > 0.015D;
    }

    private boolean hasRelaunchDelayElapsed() {
        return System.currentTimeMillis() - lastRelaunchMs >= (long) (relaunchDelay.getValue() * 1000.0D);
    }

    private void sendStartFallFlying() {
        if (mc.player == null || mc.getConnection() == null || !canControlElytra()) {
            return;
        }

        lastRelaunchMs = System.currentTimeMillis();
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
            mc.player,
            ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }

    private void resetTransientState() {
        rubberbanded = false;
        lastRelaunchMs = 0L;
        releaseControlState();
    }
}
