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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class BedRender extends Module {

    public static final BedRender INSTANCE = new BedRender();
    private static final int INITIAL_SCAN_CHUNK_BUDGET = 8;
    private static final int REFRESH_SCAN_CHUNK_BUDGET = 2;

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
    private final Map<Long, List<BedEntry>> cachedBedsByChunk = new HashMap<>();
    private final List<ChunkPos> pendingChunkScans = new ArrayList<>();
    private long lastScanTime;
    private ScanState lastScanState;

    private BedRender() {
        super("Bed Render", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        resetCache();
    }

    @Override
    protected void onDisable() {
        resetCache();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (nullCheck() || (!filled.getValue() && !outline.getValue())) {
            return;
        }

        refreshBedsIfNeeded();

        float thickness = lineWidth.getValue().floatValue();
        Frustum frustum = mc.gameRenderer.getMainCamera().getCullFrustum();
        for (BedEntry entry : cachedBeds) {
            if (!shouldRenderEntry(entry, frustum)) {
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
        ScanState state = captureScanState();
        boolean scanStateChanged = !state.equals(lastScanState);
        long now = System.currentTimeMillis();
        boolean needsImmediateRefresh = scanStateChanged || cachedBeds.isEmpty();
        int delay = scanDelay.getValue();

        if (!needsImmediateRefresh && delay > 0 && now - lastScanTime < delay) {
            return;
        }

        if (scanStateChanged || pendingChunkScans.isEmpty()) {
            rebuildScanPlan(state);
            needsImmediateRefresh = true;
        }

        lastScanTime = now;
        processPendingChunkScans(needsImmediateRefresh ? INITIAL_SCAN_CHUNK_BUDGET : REFRESH_SCAN_CHUNK_BUDGET);
        rebuildVisibleCache();
    }

    private void rebuildScanPlan(ScanState state) {
        pendingChunkScans.clear();

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        Set<Long> activeChunkKeys = new HashSet<>();

        for (int chunkX = state.centerChunkX() - state.horizontalChunkRadius(); chunkX <= state.centerChunkX() + state.horizontalChunkRadius(); chunkX++) {
            for (int chunkZ = state.centerChunkZ() - state.horizontalChunkRadius(); chunkZ <= state.centerChunkZ() + state.horizontalChunkRadius(); chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                if (!intersectsHorizontalRange(chunkPos, playerX, playerZ, range.getValue())) {
                    continue;
                }

                pendingChunkScans.add(chunkPos);
                activeChunkKeys.add(chunkKey(chunkPos));
            }
        }

        pendingChunkScans.sort(Comparator.comparingDouble(chunkPos -> chunkDistanceSq(chunkPos, playerX, playerZ)));
        cachedBedsByChunk.keySet().retainAll(activeChunkKeys);
        lastScanState = state;
    }

    private void processPendingChunkScans(int chunkBudget) {
        if (pendingChunkScans.isEmpty()) {
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        int minY = Math.max(mc.level.getMinY(), playerPos.getY() - verticalRange.getValue());
        int maxY = Math.min(mc.level.getMaxY() - 1, playerPos.getY() + verticalRange.getValue());
        double maxRangeSq = range.getValue() * range.getValue();
        int processed = 0;

        while (processed < chunkBudget && !pendingChunkScans.isEmpty()) {
            ChunkPos chunkPos = pendingChunkScans.removeFirst();
            cachedBedsByChunk.put(chunkKey(chunkPos), scanChunkBeds(chunkPos, minY, maxY, maxRangeSq));
            processed++;
        }
    }

    private List<BedEntry> scanChunkBeds(ChunkPos chunkPos, int minY, int maxY, double maxRangeSq) {
        BlockPos chunkOrigin = new BlockPos(chunkPos.getMinBlockX(), mc.player.blockPosition().getY(), chunkPos.getMinBlockZ());
        if (!mc.level.hasChunkAt(chunkOrigin)) {
            return List.of();
        }

        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        List<BedEntry> chunkBeds = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        var chunk = mc.level.getChunk(chunkPos.x(), chunkPos.z());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double horizontalDistanceSq = Mth.square(x + 0.5D - playerX) + Mth.square(z + 0.5D - playerZ);
                if (horizontalDistanceSq > maxRangeSq) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    BlockState state = chunk.getBlockState(mutable);
                    if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD) {
                        continue;
                    }

                    BlockPos headPos = mutable.immutable();
                    if (headPos.distToCenterSqr(mc.player.position()) > maxRangeSq) {
                        continue;
                    }

                    chunkBeds.add(createBedEntry(headPos, state));
                }
            }
        }

        return chunkBeds;
    }

    private void rebuildVisibleCache() {
        cachedBeds.clear();
        if (cachedBedsByChunk.isEmpty()) {
            return;
        }

        Vec3 playerPos = mc.player.position();
        int limit = maxBeds.getValue();

        List<BedEntry> mergedBeds = new ArrayList<>();
        for (List<BedEntry> chunkBeds : cachedBedsByChunk.values()) {
            for (BedEntry entry : chunkBeds) {
                if (entry.box().getCenter().distanceToSqr(playerPos) <= range.getValue() * range.getValue()) {
                    mergedBeds.add(entry);
                }
            }
        }

        mergedBeds.sort(Comparator.comparingDouble(entry -> entry.box().getCenter().distanceToSqr(playerPos)));
        if (mergedBeds.size() > limit) {
            mergedBeds = mergedBeds.subList(0, limit);
        }

        cachedBeds.addAll(mergedBeds);
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

    private boolean shouldRenderEntry(BedEntry entry, Frustum frustum) {
        return isWithinRenderRange(entry) && (frustum == null || frustum.isVisible(entry.box()));
    }

    private boolean intersectsHorizontalRange(ChunkPos chunkPos, double playerX, double playerZ, double renderRange) {
        double nearestX = Mth.clamp(playerX, chunkPos.getMinBlockX(), chunkPos.getMaxBlockX() + 1.0D);
        double nearestZ = Mth.clamp(playerZ, chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ() + 1.0D);
        return Mth.square(nearestX - playerX) + Mth.square(nearestZ - playerZ) <= renderRange * renderRange;
    }

    private double chunkDistanceSq(ChunkPos chunkPos, double playerX, double playerZ) {
        double centerX = chunkPos.getMiddleBlockX() + 0.5D;
        double centerZ = chunkPos.getMiddleBlockZ() + 0.5D;
        return Mth.square(centerX - playerX) + Mth.square(centerZ - playerZ);
    }

    private long chunkKey(ChunkPos chunkPos) {
        return ((long) chunkPos.x() & 4294967295L) | (((long) chunkPos.z() & 4294967295L) << 32);
    }

    private ScanState captureScanState() {
        ChunkPos chunkPos = mc.player.chunkPosition();
        return new ScanState(
            chunkPos.x(),
            chunkPos.z(),
            Mth.ceil(range.getValue() / 16.0D),
            verticalRange.getValue(),
            mc.level.dimension().toString()
        );
    }

    private void resetCache() {
        cachedBeds.clear();
        cachedBedsByChunk.clear();
        pendingChunkScans.clear();
        lastScanTime = 0L;
        lastScanState = null;
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

    private record ScanState(int centerChunkX, int centerChunkZ, int horizontalChunkRadius, int verticalRange, String dimensionId) {
    }
}
