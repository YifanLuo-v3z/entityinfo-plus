package com.github.entityinfo.mixin;

import com.github.epsilon.events.impl.KeyboardInputEvent;
import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.events.impl.PlayerTickEvent;
import com.github.epsilon.events.impl.SlowdownEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.BoolSetting;
import java.lang.reflect.Field;
import java.util.Queue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.movement.NoSlow")
public abstract class MixinNoSlow extends Module {

    @Unique
    private BoolSetting entityinfo$onlyGround;

    protected MixinNoSlow(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectSettings(CallbackInfo ci) {
        entityinfo$onlyGround = this.boolSetting("Only Ground", false);
        entityinfo$moveSettingAfter(entityinfo$onlyGround, entityinfo$findSetting("Crossbow"));
    }

    @Inject(method = "onSlowdown", at = @At("HEAD"), cancellable = true)
    private void entityinfo$skipAirborneSlowdown(SlowdownEvent event, CallbackInfo ci) {
        if (entityinfo$shouldSkipAirborneNoSlow()) {
            ci.cancel();
        }
    }

    @Inject(method = "onKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void entityinfo$skipAirborneKeyboardInput(KeyboardInputEvent event, CallbackInfo ci) {
        if (entityinfo$shouldSkipAirborneNoSlow()) {
            ci.cancel();
        }
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void entityinfo$skipAirborneTick(PlayerTickEvent.Pre event, CallbackInfo ci) {
        if (entityinfo$shouldSkipAirborneNoSlow()) {
            entityinfo$resetGrimC0FState();
            ci.cancel();
        }
    }

    @Inject(method = "onPacketSend", at = @At("HEAD"), cancellable = true)
    private void entityinfo$skipAirbornePacketSend(PacketEvent.Send event, CallbackInfo ci) {
        if (entityinfo$shouldSkipAirborneNoSlow()) {
            entityinfo$resetGrimC0FState();
            ci.cancel();
        }
    }

    @Inject(method = "onPacketSend", at = @At("HEAD"))
    private void entityinfo$releaseStaleGrimStateDuringLogin(PacketEvent.Send event, CallbackInfo ci) {
        if (!(event.getPacket() instanceof ServerboundPongPacket)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.getConnection() != null) {
            return;
        }

        entityinfo$resetGrimC0FState();
    }

    @Unique
    private void entityinfo$resetGrimC0FState() {
        try {
            Field stepField = entityinfo$getField("step");
            Object noneStep = entityinfo$getEnumConstant(stepField.getType(), "NONE");
            if (noneStep != null) {
                stepField.set(this, noneStep);
            }

            Field ticksField = entityinfo$getField("noUsingItemTicks");
            ticksField.setInt(this, 0);

            Field packetsField = entityinfo$getField("packets");
            Object packets = packetsField.get(this);
            if (packets instanceof Queue<?> queue) {
                queue.clear();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // If Epsilon changes these internals later, leave the original NoSlow path untouched.
        }
    }

    @Unique
    private Field entityinfo$getField(String name) throws NoSuchFieldException {
        Field field = this.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Unique
    private static Object entityinfo$getEnumConstant(Class<?> enumType, String name) {
        if (!enumType.isEnum()) {
            return null;
        }

        for (Object constant : enumType.getEnumConstants()) {
            if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(name)) {
                return constant;
            }
        }

        return null;
    }

    @Unique
    private boolean entityinfo$shouldSkipAirborneNoSlow() {
        return entityinfo$onlyGround != null
            && entityinfo$onlyGround.getValue()
            && mc.player != null
            && !mc.player.onGround();
    }

    @Unique
    private void entityinfo$moveSettingAfter(Setting<?> setting, Setting<?> anchor) {
        if (setting == null || anchor == null || setting == anchor || !settings.remove(setting)) {
            return;
        }

        int anchorIndex = settings.indexOf(anchor);
        settings.add(anchorIndex < 0 ? settings.size() : anchorIndex + 1, setting);
    }

    @Unique
    private Setting<?> entityinfo$findSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }
}
