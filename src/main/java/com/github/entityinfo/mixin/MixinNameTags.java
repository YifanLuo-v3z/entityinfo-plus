package com.github.entityinfo.mixin;

import com.github.epsilon.events.impl.Render2DEvent;
import com.github.epsilon.events.impl.Render3DEvent;
import com.github.epsilon.graphics.LuminRenderSystem;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.render.WorldToScreen;
import com.google.common.base.Suppliers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.render.NameTags")
public abstract class MixinNameTags extends Module {

    @Unique
    private static final Color entityinfo$TAG_BACKGROUND = new Color(0, 0, 0, 130);

    @Unique
    private static final Color entityinfo$NAME_COLOR = new Color(255, 255, 255, 235);

    @Unique
    private final Supplier<TextRenderer> entityinfo$textRendererSupplier = Suppliers.memoize(TextRenderer::create);

    @Unique
    private final Supplier<RectRenderer> entityinfo$rectRendererSupplier = Suppliers.memoize(RectRenderer::create);

    @Unique
    private DoubleSetting entityinfo$rangeSetting;

    @Unique
    private DoubleSetting entityinfo$scaleSetting;

    @Unique
    private DoubleSetting entityinfo$heightOffsetSetting;

    @Unique
    private ColorSetting entityinfo$backgroundColorSetting;

    @Unique
    private BoolSetting entityinfo$showDroppedItems;

    @Unique
    private BoolSetting entityinfo$mergeDroppedItems;

    @Unique
    private BoolSetting entityinfo$droppedItemsMultiline;

    @Unique
    private DoubleSetting entityinfo$droppedItemsMergeRadius;

    @Unique
    private BoolSetting entityinfo$droppedItemBackground;

    @Unique
    private EnumSetting<SortMode> entityinfo$droppedItemsSortMode;

    @Unique
    private final List<DroppedItemTextDrawData> entityinfo$droppedItemDraws = new ArrayList<>();

    protected MixinNameTags(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectSettings(CallbackInfo ci) {
        entityinfo$rangeSetting = entityinfo$findSetting("Range", DoubleSetting.class);
        entityinfo$scaleSetting = entityinfo$findSetting("Scale", DoubleSetting.class);
        entityinfo$heightOffsetSetting = entityinfo$findSetting("Height Offset", DoubleSetting.class);
        entityinfo$backgroundColorSetting = entityinfo$findSetting("Background Color", ColorSetting.class);

        entityinfo$showDroppedItems = this.boolSetting("Show Dropped Items", true);
        entityinfo$mergeDroppedItems = this.boolSetting("Merge Dropped Items", true, entityinfo$showDroppedItems::getValue);
        entityinfo$droppedItemsMultiline = this.boolSetting(
            "Dropped Items Multiline",
            true,
            this::entityinfo$shouldShowDroppedItemMergeSettings
        );
        entityinfo$droppedItemBackground = this.boolSetting("Dropped Item Background", true, entityinfo$showDroppedItems::getValue);
        entityinfo$droppedItemsMergeRadius = this.doubleSetting(
            "Dropped Items Merge Radius",
            1.0D,
            0.1D,
            4.0D,
            0.1D,
            this::entityinfo$shouldShowDroppedItemMergeSettings
        );
        entityinfo$droppedItemsSortMode = this.enumSetting(
            "Dropped Items Sort Mode",
            SortMode.NameAsc,
            this::entityinfo$shouldShowDroppedItemMergeSettings
        );
    }

    @Inject(method = "onRender3D", at = @At("TAIL"))
    private void entityinfo$appendDroppedItems(Render3DEvent event, CallbackInfo ci) {
        entityinfo$droppedItemDraws.clear();

        if (nullCheck() || entityinfo$showDroppedItems == null || !entityinfo$showDroppedItems.getValue()) {
            return;
        }

        double maxDistance = entityinfo$getRange();
        double maxDistanceSq = Mth.square(maxDistance);
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        TextRenderer textRenderer = entityinfo$textRendererSupplier.get();
        float scaledWidth = LuminRenderSystem.getScaledWidth();
        float scaledHeight = LuminRenderSystem.getScaledHeight();
        float guiScale = (float) LuminRenderSystem.getGuiScale();
        float baseScale = entityinfo$getScale();

        // Keep dropped-item labels independent from Epsilon's private TagDrawData record.
        for (DroppedItemCluster cluster : entityinfo$collectDroppedItemClusters(maxDistance, maxDistanceSq)) {
            Vector4d projectedBounds = entityinfo$projectInterpolatedClusterBounds(cluster, partialTick);
            if (projectedBounds == null) {
                continue;
            }

            if (projectedBounds.z < 0.0D || projectedBounds.w < 0.0D || projectedBounds.x > scaledWidth || projectedBounds.y > scaledHeight) {
                continue;
            }

            float projectedHeight = (float) Math.max(1.0D, projectedBounds.w - projectedBounds.y);
            float perspectiveScale = entityinfo$getPerspectiveScale(baseScale, projectedHeight);
            float padding = 3.0F * perspectiveScale;
            float lineGap = 2.0F * perspectiveScale;
            float textHeight = textRenderer.getHeight(perspectiveScale);
            List<String> lineTexts = cluster.lines();
            if (lineTexts.isEmpty()) {
                continue;
            }

            float maxTextWidth = 0.0F;
            for (String lineText : lineTexts) {
                maxTextWidth = Math.max(maxTextWidth, textRenderer.getWidth(lineText, perspectiveScale));
            }

            float boxWidth = maxTextWidth + padding * 2.0F;
            float boxHeight = padding * 2.0F + textHeight;
            float totalHeight = lineTexts.size() * boxHeight + Math.max(0, lineTexts.size() - 1) * lineGap;

            Vector3f projectedTop = WorldToScreen.getWorldPositionToScreen(entityinfo$getClusterLabelPosition(cluster, partialTick));
            if (projectedTop.z > 1.0F || projectedTop.z < 0.0F) {
                continue;
            }

            float screenCenterX = projectedTop.x / guiScale;
            float screenX = screenCenterX - boxWidth / 2.0F;
            float screenY = projectedTop.y / guiScale - totalHeight - 4.0F * perspectiveScale;

            if (screenX + boxWidth < 0.0F || screenY + totalHeight < 0.0F || screenX > scaledWidth || screenY > scaledHeight) {
                continue;
            }

            for (int index = 0; index < lineTexts.size(); index++) {
                String lineText = lineTexts.get(index);
                float lineY = screenY + index * (boxHeight + lineGap);
                entityinfo$droppedItemDraws.add(new DroppedItemTextDrawData(
                    lineText,
                    screenX,
                    lineY,
                    boxWidth,
                    boxHeight,
                    perspectiveScale,
                    padding
                ));
            }
        }
    }

    @Inject(method = "renderTagList", at = @At("TAIL"))
    private void entityinfo$renderDroppedItems(Render2DEvent.Level event, CallbackInfo ci) {
        if (entityinfo$droppedItemDraws.isEmpty()) {
            return;
        }

        TextRenderer textRenderer = entityinfo$textRendererSupplier.get();
        RectRenderer rectRenderer = entityinfo$rectRendererSupplier.get();
        boolean drawBackground = entityinfo$droppedItemBackground == null || entityinfo$droppedItemBackground.getValue();

        if (drawBackground) {
            for (DroppedItemTextDrawData drawData : entityinfo$droppedItemDraws) {
                rectRenderer.addRect(drawData.x(), drawData.y(), drawData.width(), drawData.height(), entityinfo$getBackgroundColor());
            }
            rectRenderer.drawAndClear();
        }

        for (DroppedItemTextDrawData drawData : entityinfo$droppedItemDraws) {
            float textWidth = textRenderer.getWidth(drawData.text(), drawData.scale());
            float textX = drawData.x() + (drawData.width() - textWidth) * 0.5F;
            float textY = drawData.y() + drawData.padding();
            textRenderer.addText(drawData.text(), textX, textY, drawData.scale(), entityinfo$NAME_COLOR);
        }

        textRenderer.drawAndClear();
        entityinfo$droppedItemDraws.clear();
    }

    @Unique
    private List<DroppedItemCluster> entityinfo$collectDroppedItemClusters(double maxDistance, double maxDistanceSq) {
        List<ItemEntity> itemEntities = new ArrayList<>(mc.level.getEntitiesOfClass(
            ItemEntity.class,
            mc.player.getBoundingBox().inflate(maxDistance),
            itemEntity -> entityinfo$isValidDroppedItem(itemEntity, maxDistanceSq)
        ));
        itemEntities.sort(Comparator.comparingDouble(mc.player::distanceToSqr));

        List<DroppedItemClusterBuilder> clusters = new ArrayList<>();
        boolean mergeDroppedItems = entityinfo$mergeDroppedItems != null && entityinfo$mergeDroppedItems.getValue();
        double mergeRadiusSq = entityinfo$droppedItemsMergeRadius == null
            ? 1.0D
            : Mth.square(entityinfo$droppedItemsMergeRadius.getValue());

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            if (!mergeDroppedItems) {
                DroppedItemClusterBuilder builder = new DroppedItemClusterBuilder(itemEntity);
                builder.addStack(itemEntity);
                clusters.add(builder);
                continue;
            }

            DroppedItemClusterBuilder matchedCluster = null;
            Vec3 itemPosition = itemEntity.position();

            for (DroppedItemClusterBuilder cluster : clusters) {
                if (cluster.canMerge(itemPosition, mergeRadiusSq)) {
                    matchedCluster = cluster;
                    break;
                }
            }

            if (matchedCluster == null) {
                matchedCluster = new DroppedItemClusterBuilder(itemEntity);
                clusters.add(matchedCluster);
            }

            matchedCluster.addStack(itemEntity);
        }

        boolean multiline = mergeDroppedItems && entityinfo$droppedItemsMultiline != null && entityinfo$droppedItemsMultiline.getValue();
        List<DroppedItemCluster> result = new ArrayList<>(clusters.size());

        for (DroppedItemClusterBuilder cluster : clusters) {
            DroppedItemCluster builtCluster = cluster.build(multiline, entityinfo$getDroppedItemsComparator());
            if (builtCluster != null) {
                result.add(builtCluster);
            }
        }

        return result;
    }

    @Unique
    private boolean entityinfo$isValidDroppedItem(ItemEntity itemEntity, double maxDistanceSq) {
        return itemEntity.isAlive()
            && !itemEntity.isRemoved()
            && !itemEntity.isInvisible()
            && mc.player.distanceToSqr(itemEntity) <= maxDistanceSq;
    }

    @Unique
    private boolean entityinfo$shouldShowDroppedItemMergeSettings() {
        return entityinfo$showDroppedItems != null
            && entityinfo$showDroppedItems.getValue()
            && entityinfo$mergeDroppedItems != null
            && entityinfo$mergeDroppedItems.getValue();
    }

    @Unique
    private Vector4d entityinfo$projectInterpolatedClusterBounds(DroppedItemCluster cluster, float partialTick) {
        if (cluster.members().isEmpty()) {
            return null;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        boolean hasBounds = false;

        for (ItemEntity member : cluster.members()) {
            if (member == null || member.isRemoved()) {
                continue;
            }

            Vec3 interpolated = entityinfo$interpolate(member, partialTick);
            double halfWidth = member.getBbWidth() * 0.5F;
            minX = Math.min(minX, interpolated.x - halfWidth);
            minY = Math.min(minY, interpolated.y);
            minZ = Math.min(minZ, interpolated.z - halfWidth);
            maxX = Math.max(maxX, interpolated.x + halfWidth);
            maxY = Math.max(maxY, interpolated.y + member.getBbHeight());
            maxZ = Math.max(maxZ, interpolated.z + halfWidth);
            hasBounds = true;
        }

        if (!hasBounds) {
            return null;
        }

        return WorldToScreen.projectAbsoluteAABBOn2D(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Unique
    private Vec3 entityinfo$getLabelPosition(ItemEntity itemEntity, float partialTick) {
        Vec3 interpolated = entityinfo$interpolate(itemEntity, partialTick);
        return interpolated.add(0.0D, entityinfo$getHeightOffset() + itemEntity.getBbHeight(), 0.0D);
    }

    @Unique
    private Vec3 entityinfo$getClusterLabelPosition(DroppedItemCluster cluster, float partialTick) {
        if (cluster.members().isEmpty()) {
            return entityinfo$getLabelPosition(cluster.representative(), partialTick);
        }

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;
        double maxHeight = cluster.representative().getBbHeight();

        for (ItemEntity member : cluster.members()) {
            if (member == null || member.isRemoved()) {
                continue;
            }

            Vec3 interpolated = entityinfo$interpolate(member, partialTick);
            x += interpolated.x;
            y += interpolated.y;
            z += interpolated.z;
            maxHeight = Math.max(maxHeight, member.getBbHeight());
            count++;
        }

        if (count == 0) {
            return entityinfo$getLabelPosition(cluster.representative(), partialTick);
        }

        return new Vec3(
            x / count,
            y / count + entityinfo$getHeightOffset() + maxHeight,
            z / count
        );
    }

    @Unique
    private Vec3 entityinfo$interpolate(ItemEntity itemEntity, float partialTick) {
        return new Vec3(
            Mth.lerp(partialTick, itemEntity.xOld, itemEntity.getX()),
            Mth.lerp(partialTick, itemEntity.yOld, itemEntity.getY()),
            Mth.lerp(partialTick, itemEntity.zOld, itemEntity.getZ())
        );
    }

    @Unique
    private Comparator<MergedEntry> entityinfo$getDroppedItemsComparator() {
        if (entityinfo$droppedItemsSortMode == null) {
            return entityinfo$withDroppedItemsTieBreakers(
                Comparator.comparing(MergedEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            );
        }

        Comparator<MergedEntry> comparator = switch (entityinfo$droppedItemsSortMode.getValue()) {
            case NameAsc -> Comparator.comparing(MergedEntry::displayName, String.CASE_INSENSITIVE_ORDER);
            case NameDesc -> Comparator.comparing(MergedEntry::displayName, String.CASE_INSENSITIVE_ORDER).reversed();
            case CountAsc -> Comparator.comparingInt(MergedEntry::count);
            case CountDesc -> Comparator.comparingInt(MergedEntry::count).reversed();
        };

        return entityinfo$withDroppedItemsTieBreakers(comparator);
    }

    @Unique
    private static Comparator<MergedEntry> entityinfo$withDroppedItemsTieBreakers(Comparator<MergedEntry> comparator) {
        return comparator
            .thenComparing(MergedEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(MergedEntry::count)
            .thenComparingInt(MergedEntry::mergeKey);
    }

    @Unique
    private final class DroppedItemClusterBuilder {
        private final ItemEntity representative;
        private final List<ItemEntity> members = new ArrayList<>();
        private final Map<DroppedItemKey, MergedEntry> mergedEntries = new LinkedHashMap<>();

        private DroppedItemClusterBuilder(ItemEntity representative) {
            this.representative = representative;
        }

        private boolean canMerge(Vec3 itemPosition, double mergeRadiusSq) {
            for (ItemEntity member : members) {
                if (member.position().distanceToSqr(itemPosition) <= mergeRadiusSq) {
                    return true;
                }
            }

            return false;
        }

        private void addStack(ItemEntity itemEntity) {
            members.add(itemEntity);
            ItemStack stack = itemEntity.getItem();
            DroppedItemKey mergeKey = new DroppedItemKey(stack);
            MergedEntry existingEntry = mergedEntries.get(mergeKey);

            if (existingEntry == null) {
                mergedEntries.put(mergeKey, new MergedEntry(mergeKey.hashCode(), stack.getHoverName().getString(), stack.getCount()));
                return;
            }

            mergedEntries.put(
                mergeKey,
                new MergedEntry(
                    existingEntry.mergeKey(),
                    existingEntry.displayName(),
                    existingEntry.count() + stack.getCount()
                )
            );
        }

        private DroppedItemCluster build(boolean multiline, Comparator<MergedEntry> comparator) {
            List<MergedEntry> sortedEntries = new ArrayList<>(mergedEntries.values());
            sortedEntries.sort(comparator);
            List<String> lines = new ArrayList<>(sortedEntries.size());

            for (MergedEntry entry : sortedEntries) {
                lines.add(entry.formatLine());
            }

            if (lines.isEmpty()) {
                return null;
            }

            if (!multiline && lines.size() > 1) {
                lines = List.of(String.join(" | ", lines));
            }

            return new DroppedItemCluster(representative, List.copyOf(members), List.copyOf(lines));
        }
    }

    @Unique
    private record DroppedItemCluster(ItemEntity representative, List<ItemEntity> members, List<String> lines) {
    }

    @Unique
    private record DroppedItemKey(ItemStack stack) {
        private DroppedItemKey(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DroppedItemKey other && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }

    @Unique
    private record MergedEntry(int mergeKey, String displayName, int count) {
        private String formatLine() {
            return count > 1 ? displayName + " x" + count : displayName;
        }
    }

    @Unique
    private record DroppedItemTextDrawData(
        String text,
        float x,
        float y,
        float width,
        float height,
        float scale,
        float padding
    ) {
    }

    @Unique
    private float entityinfo$getPerspectiveScale(float baseScale, float projectedHeight) {
        float perspectiveFactor = Mth.clamp(projectedHeight / 36.0F, 0.55F, 2.2F);
        return baseScale * perspectiveFactor;
    }

    @Unique
    private double entityinfo$getRange() {
        return entityinfo$rangeSetting != null ? entityinfo$rangeSetting.getValue() : 64.0D;
    }

    @Unique
    private float entityinfo$getScale() {
        return entityinfo$scaleSetting != null ? entityinfo$scaleSetting.getValue().floatValue() : 0.4F;
    }

    @Unique
    private double entityinfo$getHeightOffset() {
        return entityinfo$heightOffsetSetting != null ? entityinfo$heightOffsetSetting.getValue() : 0.15D;
    }

    @Unique
    private Color entityinfo$getBackgroundColor() {
        return entityinfo$backgroundColorSetting != null
            ? entityinfo$backgroundColorSetting.getValue()
            : entityinfo$TAG_BACKGROUND;
    }

    @Unique
    private <T extends Setting<?>> T entityinfo$findSetting(String name, Class<T> type) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equals(name) && type.isInstance(setting)) {
                return type.cast(setting);
            }
        }

        return null;
    }

    @Environment(EnvType.CLIENT)
    private enum SortMode {
        NameAsc,
        NameDesc,
        CountAsc,
        CountDesc
    }
}
