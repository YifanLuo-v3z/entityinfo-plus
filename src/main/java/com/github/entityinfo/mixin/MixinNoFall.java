package com.github.entityinfo.mixin;

import com.github.epsilon.events.impl.SendPositionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.movement.NoFall")
public abstract class MixinNoFall extends Module {

    @Unique
    private static final double ENTITYINFO_AUTO_SCAN_DISTANCE = 128.0D;

    @Unique
    private static final double ENTITYINFO_TRIGGER_MARGIN = 1.25D;

    @Shadow
    @Final
    private EnumSetting<?> mode;

    @Shadow
    @Final
    private DoubleSetting fallDistance;

    @Shadow
    private boolean flag;

    @Shadow
    private boolean jump;

    @Unique
    private BoolSetting entityinfo$autoFallDistance;

    protected MixinNoFall(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectAutoFallDistanceSetting(CallbackInfo ci) {
        entityinfo$autoFallDistance = this.boolSetting("Auto Fall Distance", false, this::entityinfo$isGrimSimulation);
        entityinfo$moveSettingAfter(entityinfo$autoFallDistance, fallDistance);
    }

    @Inject(method = "onMotion", at = @At("HEAD"), cancellable = true)
    private void entityinfo$autoSelectGrimFallDistance(SendPositionEvent event, CallbackInfo ci) {
        if (!entityinfo$shouldUseAutoFallDistance()) {
            return;
        }

        ci.cancel();
        if (nullCheck()) {
            return;
        }

        if (mc.player.fallDistance > entityinfo$getAutoFallDistanceThreshold()) {
            flag = true;
        }

        if (flag && mc.player.onGround()) {
            event.setY(event.getY() + 0.1D);
            jump = true;
            flag = false;
        }
    }

    @Unique
    private boolean entityinfo$shouldUseAutoFallDistance() {
        return entityinfo$autoFallDistance != null
            && entityinfo$autoFallDistance.getValue()
            && entityinfo$isGrimSimulation();
    }

    @Unique
    private boolean entityinfo$isGrimSimulation() {
        return mode != null && mode.is("GrimSimulation");
    }

    @Unique
    private double entityinfo$getAutoFallDistanceThreshold() {
        double remainingDistance = entityinfo$getRemainingFallDistance();
        if (!Double.isFinite(remainingDistance)) {
            return fallDistance.getMin();
        }

        double predictedTotalFall = mc.player.fallDistance + remainingDistance;
        double steppedThreshold = Math.floor((predictedTotalFall - ENTITYINFO_TRIGGER_MARGIN) / fallDistance.getStep()) * fallDistance.getStep();
        return Mth.clamp(steppedThreshold, fallDistance.getMin(), fallDistance.getMax());
    }

    @Unique
    private double entityinfo$getRemainingFallDistance() {
        if (mc.player.onGround()) {
            return 0.0D;
        }

        AABB playerBox = mc.player.getBoundingBox();
        double playerFeetY = playerBox.minY;
        int minX = Mth.floor(playerBox.minX + 1.0E-4D);
        int maxX = Mth.floor(playerBox.maxX - 1.0E-4D);
        int minZ = Mth.floor(playerBox.minZ + 1.0E-4D);
        int maxZ = Mth.floor(playerBox.maxZ - 1.0E-4D);
        int startY = Math.min(Mth.floor(playerFeetY), mc.level.getMaxY() - 1);
        int endY = Math.max(mc.level.getMinY(), Mth.floor(playerFeetY - ENTITYINFO_AUTO_SCAN_DISTANCE));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= endY; y--) {
            double closestDistance = Double.POSITIVE_INFINITY;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!mc.level.hasChunkAt(pos)) {
                        continue;
                    }

                    BlockState state = mc.level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(mc.level, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (AABB localBox : shape.toAabbs()) {
                        AABB worldBox = localBox.move(pos);
                        if (!entityinfo$horizontallyOverlaps(playerBox, worldBox) || worldBox.maxY > playerFeetY + 1.0E-4D) {
                            continue;
                        }

                        closestDistance = Math.min(closestDistance, Math.max(0.0D, playerFeetY - worldBox.maxY));
                    }
                }
            }

            if (Double.isFinite(closestDistance)) {
                return closestDistance;
            }
        }

        return Double.NaN;
    }

    @Unique
    private static boolean entityinfo$horizontallyOverlaps(AABB playerBox, AABB worldBox) {
        return playerBox.maxX > worldBox.minX
            && playerBox.minX < worldBox.maxX
            && playerBox.maxZ > worldBox.minZ
            && playerBox.minZ < worldBox.maxZ;
    }

    @Unique
    private void entityinfo$moveSettingAfter(Setting<?> setting, Setting<?> anchor) {
        if (setting == null || anchor == null || setting == anchor || !settings.remove(setting)) {
            return;
        }

        int anchorIndex = settings.indexOf(anchor);
        settings.add(anchorIndex < 0 ? settings.size() : anchorIndex + 1, setting);
    }
}
