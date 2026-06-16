package com.github.entityinfo.mixin;

import com.github.epsilon.events.impl.Render3DEvent;
import com.github.epsilon.graphics.shaders.BlurShader;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.render.Render3DUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.render.BlockHighlight")
public abstract class MixinBlockHighlightShape extends Module {

    @Shadow
    private EnumSetting<?> mode;

    @Shadow
    private ColorSetting sideColor;

    @Shadow
    private ColorSetting lineColor;

    @Shadow
    private DoubleSetting lineWidth;

    @Shadow
    private BoolSetting blur;

    @Shadow
    private DoubleSetting blurStrength;

    protected MixinBlockHighlightShape(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "onRender3D", at = @At("HEAD"), cancellable = true)
    private void entityinfo$renderVoxelShapeHighlight(Render3DEvent event, CallbackInfo ci) {
        if (!(mc.hitResult instanceof BlockHitResult blockHitResult) || mc.level == null) {
            return;
        }
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState blockState = mc.level.getBlockState(blockPos);
        VoxelShape shape = blockState.getShape(mc.level, blockPos);
        if (shape.isEmpty()) {
            ci.cancel();
            return;
        }

        List<AABB> localBoxes = shape.toAabbs();
        if (localBoxes.isEmpty()) {
            ci.cancel();
            return;
        }

        List<AABB> boxes = new ArrayList<>(localBoxes.size());
        for (AABB box : localBoxes) {
            boxes.add(box.move(blockPos));
        }

        AABB shapeBounds = shape.bounds().move(blockPos);
        Direction direction = blockHitResult.getDirection();
        float thickness = lineWidth.getValue().floatValue();
        Color fill = sideColor.getValue();
        Color outline = lineColor.getValue();

        switch (entityinfo$getModeName()) {
            case "Both" -> {
                entityinfo$drawBlur(boxes);
                entityinfo$drawFilledBoxes(boxes, fill);
                entityinfo$drawOutlinedBoxes(event, boxes, outline, thickness);
            }
            case "BothSide" -> {
                entityinfo$drawBlur(boxes);
                entityinfo$drawBoundarySides(event, boxes, shapeBounds, fill, outline, thickness, direction, true, true);
            }
            case "Fill" -> {
                entityinfo$drawBlur(boxes);
                entityinfo$drawFilledBoxes(boxes, fill);
            }
            case "FilledSide" -> entityinfo$drawBoundarySides(event, boxes, shapeBounds, fill, outline, thickness, direction, true, false);
            case "Outline" -> entityinfo$drawOutlinedBoxes(event, boxes, outline, thickness);
            case "OutlinedSide" -> entityinfo$drawBoundarySides(event, boxes, shapeBounds, fill, outline, thickness, direction, false, true);
            default -> {
                return;
            }
        }

        ci.cancel();
    }

    private String entityinfo$getModeName() {
        Object value = mode.getValue();
        return value instanceof Enum<?> enumValue ? enumValue.name() : "";
    }

    private void entityinfo$drawBlur(List<AABB> boxes) {
        if (!blur.getValue()) {
            return;
        }

        double strength = blurStrength.getValue();
        for (AABB box : boxes) {
            BlurShader.INSTANCE.render3DBox(box, strength);
        }
    }

    private void entityinfo$drawFilledBoxes(List<AABB> boxes, Color color) {
        for (AABB box : boxes) {
            Render3DUtils.drawFilledBox(box, color);
        }
    }

    private void entityinfo$drawOutlinedBoxes(Render3DEvent event, List<AABB> boxes, Color color, float thickness) {
        for (AABB box : boxes) {
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, color, thickness);
        }
    }

    private void entityinfo$drawBoundarySides(
        Render3DEvent event,
        List<AABB> boxes,
        AABB shapeBounds,
        Color fill,
        Color outline,
        float thickness,
        Direction direction,
        boolean renderFill,
        boolean renderOutline
    ) {
        for (AABB box : boxes) {
            if (!entityinfo$isBoundaryBox(box, shapeBounds, direction)) {
                continue;
            }

            if (renderOutline) {
                Render3DUtils.drawSideOutline(event.getPoseStack(), box, outline, thickness, direction);
            }
            if (renderFill) {
                Render3DUtils.drawFilledSide(box, fill, direction);
            }
        }
    }

    private static boolean entityinfo$isBoundaryBox(AABB box, AABB shapeBounds, Direction direction) {
        final double epsilon = 1.0E-4D;
        return switch (direction.getAxis()) {
            case X -> direction.getStepX() > 0
                ? Math.abs(box.maxX - shapeBounds.maxX) <= epsilon
                : Math.abs(box.minX - shapeBounds.minX) <= epsilon;
            case Y -> direction.getStepY() > 0
                ? Math.abs(box.maxY - shapeBounds.maxY) <= epsilon
                : Math.abs(box.minY - shapeBounds.minY) <= epsilon;
            case Z -> direction.getStepZ() > 0
                ? Math.abs(box.maxZ - shapeBounds.maxZ) <= epsilon
                : Math.abs(box.minZ - shapeBounds.minZ) <= epsilon;
        };
    }
}
