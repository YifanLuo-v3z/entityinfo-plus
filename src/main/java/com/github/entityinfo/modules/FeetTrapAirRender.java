package com.github.entityinfo.modules;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.Render3DEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.world.BlockUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

@Environment(EnvType.CLIENT)
public final class FeetTrapAirRender extends Module {

    public static final FeetTrapAirRender INSTANCE = new FeetTrapAirRender();
    private static final double FACE_THICKNESS = 0.002D;

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgRender = settingGroup("Render");
    private final SettingGroup sgPerformance = settingGroup("Performance");

    private final BoolSetting onlyBelowAir = boolSetting("Only Below Air", true).group(sgGeneral);
    private final BoolSetting onlyGround = boolSetting("Only Ground", false).group(sgGeneral);
    private final BoolSetting requireFeetTrapBlocks = boolSetting("Require Feet Trap Blocks", false).group(sgGeneral);
    private final IntSetting minFeetTrapBlocks = intSetting(
        "Min Feet Trap Blocks",
        1,
        1,
        16,
        1,
        requireFeetTrapBlocks::getValue
    ).group(sgGeneral);
    private final BoolSetting filled = boolSetting("Filled", true).group(sgRender);
    private final BoolSetting outline = boolSetting("Outline", true).group(sgRender);
    private final DoubleSetting lineWidth = doubleSetting("Line Width", 1.5D, 0.5D, 6.0D, 0.5D, outline::getValue).group(sgRender);
    private final ColorSetting fillColor = colorSetting("Fill Color", new Color(75, 165, 255, 72), true, filled::getValue).group(sgRender);
    private final ColorSetting lineColor = colorSetting("Line Color", new Color(80, 210, 255, 220), true, outline::getValue).group(sgRender);
    private final IntSetting refreshDelay = intSetting("Refresh Delay", 50, 0, 1000, 10).group(sgPerformance);

    private final List<BlockPos> cachedTargets = new ArrayList<>();
    private long lastRefreshTime;

    private FeetTrapAirRender() {
        super("Feet Trap Air Render", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        cachedTargets.clear();
        lastRefreshTime = 0L;
    }

    @Override
    protected void onDisable() {
        cachedTargets.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (nullCheck() || (!filled.getValue() && !outline.getValue())) {
            return;
        }

        if (onlyGround.getValue() && !mc.player.onGround()) {
            cachedTargets.clear();
            lastRefreshTime = 0L;
            return;
        }

        if (mc.player.onGround() && !hasOnlyFullBlockSupportUnderPlayer()) {
            cachedTargets.clear();
            lastRefreshTime = 0L;
            return;
        }

        refreshTargetsIfNeeded();
        if (cachedTargets.isEmpty()) {
            return;
        }

        float thickness = lineWidth.getValue().floatValue();
        for (BlockPos target : cachedTargets) {
            AABB targetBox = createBottomFaceBox(target);
            if (filled.getValue()) {
                Render3DUtils.drawFilledBox(targetBox, fillColor.getValue());
            }
            if (outline.getValue()) {
                Render3DUtils.drawOutlineBox(event.getPoseStack(), targetBox, lineColor.getValue(), thickness);
            }
        }
    }

    private static AABB createBottomFaceBox(BlockPos target) {
        return new AABB(
            target.getX(),
            target.getY(),
            target.getZ(),
            target.getX() + 1.0D,
            target.getY() + FACE_THICKNESS,
            target.getZ() + 1.0D
        );
    }

    private void refreshTargetsIfNeeded() {
        long now = System.currentTimeMillis();
        if (refreshDelay.getValue() > 0 && now - lastRefreshTime < refreshDelay.getValue()) {
            return;
        }

        lastRefreshTime = now;
        cachedTargets.clear();
        cachedTargets.addAll(getFeetTrapTargets());
    }

    private boolean shouldRenderBottomFace(BlockPos target, Set<BlockPos> targets) {
        if (!onlyBelowAir.getValue()) {
            return true;
        }

        BlockPos below = target.below();
        return !targets.contains(below)
            && mc.level.hasChunkAt(below)
            && mc.level.getBlockState(below).canBeReplaced();
    }

    private Set<BlockPos> getFeetTrapTargets() {
        Set<BlockPos> feetPositions = getFeetPositions();
        Set<BlockPos> surroundPositions = getSurroundPositions(feetPositions);

        if (requireFeetTrapBlocks.getValue()) {
            return getExistingFeetTrapTargets(surroundPositions);
        }

        return getPlannedFeetTrapTargets(surroundPositions);
    }

    private Set<BlockPos> getExistingFeetTrapTargets(Set<BlockPos> surroundPositions) {
        Set<BlockPos> targets = new LinkedHashSet<>();

        if (!hasEnoughFeetTrapBlocks(surroundPositions)) {
            return targets;
        }

        for (BlockPos feetTrapBlock : surroundPositions) {
            if (shouldRenderExistingFeetTrapBlock(feetTrapBlock)) {
                targets.add(feetTrapBlock);
            }
        }

        return targets;
    }

    private Set<BlockPos> getPlannedFeetTrapTargets(Set<BlockPos> surroundPositions) {
        Set<BlockPos> targets = new LinkedHashSet<>();

        for (BlockPos target : surroundPositions) {
            if (mc.level.hasChunkAt(target)
                && BlockUtils.canPlaceAt(target)
                && shouldRenderBottomFace(target, targets)) {
                targets.add(target);
            }
        }

        return targets;
    }

    private boolean shouldRenderExistingFeetTrapBlock(BlockPos feetTrapBlock) {
        if (!isExistingFeetTrapBlock(feetTrapBlock)) {
            return false;
        }

        BlockPos airBelow = feetTrapBlock.below();
        if (!mc.level.hasChunkAt(airBelow)) {
            return false;
        }

        BlockState belowState = mc.level.getBlockState(airBelow);
        if (!belowState.isAir()) {
            return false;
        }

        // The marked air block must have the actual trap block above it.
        return !mc.level.getBlockState(airBelow.above()).isAir();
    }

    private boolean hasEnoughFeetTrapBlocks(Set<BlockPos> surroundPositions) {
        if (!requireFeetTrapBlocks.getValue()) {
            return true;
        }

        int foundBlocks = 0;
        int requiredBlocks = minFeetTrapBlocks.getValue();

        for (BlockPos surroundPos : surroundPositions) {
            if (!mc.level.hasChunkAt(surroundPos)) {
                continue;
            }

            if (isExistingFeetTrapBlock(surroundPos) && ++foundBlocks >= requiredBlocks) {
                return true;
            }
        }

        return false;
    }

    private boolean isExistingFeetTrapBlock(BlockPos pos) {
        return isFullCollisionBlock(pos);
    }

    private boolean isFullCollisionBlock(BlockPos pos) {
        if (!mc.level.hasChunkAt(pos)) {
            return false;
        }

        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced() && state.isCollisionShapeFullBlock(mc.level, pos);
    }

    private boolean hasOnlyFullBlockSupportUnderPlayer() {
        AABB playerBox = mc.player.getBoundingBox();
        double feetY = playerBox.minY;
        int minX = Mth.floor(playerBox.minX + 1.0E-4D);
        int maxX = Mth.floor(playerBox.maxX - 1.0E-4D);
        int minZ = Mth.floor(playerBox.minZ + 1.0E-4D);
        int maxZ = Mth.floor(playerBox.maxZ - 1.0E-4D);
        int supportY = Mth.floor(feetY - 1.0E-4D);
        boolean foundSupport = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, supportY, z);
                if (!mc.level.hasChunkAt(pos)) {
                    return false;
                }

                BlockState state = mc.level.getBlockState(pos);
                for (AABB localBox : state.getCollisionShape(mc.level, pos).toAabbs()) {
                    AABB worldBox = localBox.move(pos);
                    if (!horizontallyOverlaps(playerBox, worldBox) || Math.abs(worldBox.maxY - feetY) > 1.0E-3D) {
                        continue;
                    }

                    foundSupport = true;
                    if (!isFullCollisionBlock(pos)) {
                        return false;
                    }

                    break;
                }
            }
        }

        return foundSupport;
    }

    private static boolean horizontallyOverlaps(AABB first, AABB second) {
        return first.maxX > second.minX
            && first.minX < second.maxX
            && first.maxZ > second.minZ
            && first.minZ < second.maxZ;
    }

    private Set<BlockPos> getSurroundPositions(Set<BlockPos> feetPositions) {
        Set<BlockPos> surroundPositions = new LinkedHashSet<>();

        for (BlockPos feetPos : feetPositions) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos surroundPos = feetPos.relative(direction);
                if (!isInsideFeet(surroundPos, feetPositions)) {
                    surroundPositions.add(surroundPos);
                }
            }
        }

        return surroundPositions;
    }

    private Set<BlockPos> getFeetPositions() {
        Set<BlockPos> feetPositions = new LinkedHashSet<>();
        AABB box = mc.player.getBoundingBox().deflate(0.001D);
        int minX = BlockPos.containing(box.minX, mc.player.getY(), box.minZ).getX();
        int maxX = BlockPos.containing(box.maxX, mc.player.getY(), box.maxZ).getX();
        int minZ = BlockPos.containing(box.minX, mc.player.getY(), box.minZ).getZ();
        int maxZ = BlockPos.containing(box.maxX, mc.player.getY(), box.maxZ).getZ();

        Set<Integer> yLevels = new LinkedHashSet<>();
        yLevels.add(BlockPos.containing(mc.player.position()).getY());
        yLevels.add(BlockPos.containing(mc.player.getX(), mc.player.getY() + 0.8D, mc.player.getZ()).getY());

        for (int y : yLevels) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    feetPositions.add(new BlockPos(x, y, z));
                }
            }
        }

        return feetPositions;
    }

    private static boolean isInsideFeet(BlockPos pos, Set<BlockPos> feetPositions) {
        for (BlockPos feetPos : feetPositions) {
            if (pos.getX() == feetPos.getX() && pos.getZ() == feetPos.getZ()) {
                return true;
            }
        }

        return false;
    }
}
