package com.github.entityinfo.modules;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.Render3DEvent;
import com.github.epsilon.graphics.shaders.BlurShader;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.render.Render3DUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class BedRender extends Module {

    public static final BedRender INSTANCE = new BedRender();

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgRender = settingGroup("Render");
    private final SettingGroup sgPerformance = settingGroup("Performance");

    private final DoubleSetting range = doubleSetting("Range", 48.0D, 4.0D, 128.0D, 1.0D).group(sgGeneral);
    private final IntSetting verticalRange = intSetting("Vertical Range", 24, 4, 96, 1).group(sgGeneral);
    private final BoolSetting filled = boolSetting("Filled", true).group(sgRender);
    private final BoolSetting outline = boolSetting("Outline", true).group(sgRender);
    private final BoolSetting blur = boolSetting("Blur", false, filled::getValue).group(sgRender);
    private final DoubleSetting blurStrength = doubleSetting("Blur Strength", 4.0D, 0.0D, 16.0D, 0.5D, () -> filled.getValue() && blur.getValue()).group(sgRender);
    private final DoubleSetting lineWidth = doubleSetting("Line Width", 1.5D, 0.5D, 6.0D, 0.5D, outline::getValue).group(sgRender);
    private final ColorSetting fillColor = colorSetting("Fill Color", new Color(255, 85, 85, 45), true, filled::getValue).group(sgRender);
    private final ColorSetting outlineColor = colorSetting("Line Color", new Color(255, 110, 110, 220), true, outline::getValue).group(sgRender);
    private final IntSetting scanDelay = intSetting("Scan Delay", 500, 100, 3000, 50).group(sgPerformance);
    private final IntSetting maxBeds = intSetting("Max Beds", 96, 1, 512, 1).group(sgPerformance);

    private final List<BedEntry> cachedBeds = new ArrayList<>();
    private long lastScanTime;

    private BedRender() {
        super("Bed Render", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        cachedBeds.clear();
        lastScanTime = 0L;
    }

    @Override
    protected void onDisable() {
        cachedBeds.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (nullCheck() || (!filled.getValue() && !outline.getValue())) {
            return;
        }

        refreshBedsIfNeeded();

        float thickness = lineWidth.getValue().floatValue();
        for (BedEntry entry : cachedBeds) {
            if (!isWithinRenderRange(entry)) {
                continue;
            }

            AABB box = entry.box();
            if (blur.getValue() && filled.getValue()) {
                BlurShader.INSTANCE.render3DBox(box, blurStrength.getValue());
            }
            if (filled.getValue()) {
                Render3DUtils.drawFilledBox(box, fillColor.getValue());
            }
            if (outline.getValue()) {
                Render3DUtils.drawOutlineBox(event.getPoseStack(), box, outlineColor.getValue(), thickness);
            }
        }
    }

    private void refreshBedsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastScanTime < scanDelay.getValue()) {
            return;
        }

        lastScanTime = now;
        cachedBeds.clear();

        BlockPos center = mc.player.blockPosition();
        int horizontal = Mth.ceil(range.getValue());
        int vertical = verticalRange.getValue();
        double maxRangeSq = range.getValue() * range.getValue();
        int minY = Math.max(mc.level.getMinY(), center.getY() - vertical);
        int maxY = Math.min(mc.level.getMaxY() - 1, center.getY() + vertical);
        int limit = maxBeds.getValue();

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - horizontal; x <= center.getX() + horizontal; x++) {
            for (int z = center.getZ() - horizontal; z <= center.getZ() + horizontal; z++) {
                double horizontalDistanceSq = Mth.square(x + 0.5D - mc.player.getX()) + Mth.square(z + 0.5D - mc.player.getZ());
                if (horizontalDistanceSq > maxRangeSq) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    if (!mc.level.hasChunkAt(mutable)) {
                        continue;
                    }

                    BlockState state = mc.level.getBlockState(mutable);
                    if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD) {
                        continue;
                    }

                    BlockPos headPos = mutable.immutable();
                    if (headPos.distToCenterSqr(mc.player.position()) > maxRangeSq) {
                        continue;
                    }

                    cachedBeds.add(createBedEntry(headPos, state));
                    if (cachedBeds.size() >= limit) {
                        return;
                    }
                }
            }
        }
    }

    private BedEntry createBedEntry(BlockPos headPos, BlockState headState) {
        Direction facing = headState.getValue(BedBlock.FACING);
        BlockPos footPos = headPos.relative(facing.getOpposite());
        AABB box = getShapeBox(headPos, headState);

        if (mc.level.hasChunkAt(footPos)) {
            BlockState footState = mc.level.getBlockState(footPos);
            if (footState.getBlock() instanceof BedBlock && footState.getValue(BedBlock.PART) == BedPart.FOOT) {
                box = union(box, getShapeBox(footPos, footState));
            }
        }

        return new BedEntry(headPos, footPos, box);
    }

    private AABB getShapeBox(BlockPos pos, BlockState state) {
        AABB shapeBox = state.getShape(mc.level, pos).bounds();
        return shapeBox.move(pos);
    }

    private boolean isWithinRenderRange(BedEntry entry) {
        Vec3 playerPos = mc.player.position();
        return entry.box().getCenter().distanceToSqr(playerPos) <= range.getValue() * range.getValue();
    }

    private static AABB union(AABB first, AABB second) {
        return new AABB(
            Math.min(first.minX, second.minX),
            Math.min(first.minY, second.minY),
            Math.min(first.minZ, second.minZ),
            Math.max(first.maxX, second.maxX),
            Math.max(first.maxY, second.maxY),
            Math.max(first.maxZ, second.maxZ)
        );
    }

    private record BedEntry(BlockPos headPos, BlockPos footPos, AABB box) {
    }
}
