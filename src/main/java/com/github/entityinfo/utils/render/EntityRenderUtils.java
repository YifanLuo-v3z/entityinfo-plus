package com.github.entityinfo.utils.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class EntityRenderUtils {

    private EntityRenderUtils() {
    }

    public static boolean isInsideFrustum(Minecraft mc, Entity entity) {
        Frustum frustum = mc.gameRenderer.getMainCamera().getCullFrustum();

        if (frustum == null) {
            return true;
        }

        Vec3 cameraPosition = mc.gameRenderer.getMainCamera().position();
        return mc.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraPosition.x, cameraPosition.y, cameraPosition.z);
    }

    public static List<DroppedItemEntry> collectDroppedItemEntries(Minecraft mc, double maxDistance, Comparator<DroppedItemEntry> comparator) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) {
            return List.of();
        }

        double maxDistanceSq = Mth.square(maxDistance);
        AABB searchBox = player.getBoundingBox().inflate(maxDistance);
        List<ItemEntity> itemEntities = level.getEntitiesOfClass(
            ItemEntity.class,
            searchBox,
            itemEntity -> isValidDroppedItemEntity(itemEntity, player, maxDistanceSq)
        );
        List<DroppedItemEntry> entries = new ArrayList<>(itemEntities.size());
        String typeName = null;

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            if (typeName == null) {
                typeName = itemEntity.getType().getDescription().getString();
            }

            entries.add(new DroppedItemEntry(
                itemEntity,
                typeName,
                Math.sqrt(player.distanceToSqr(itemEntity))
            ));
        }

        entries.sort(comparator);
        return entries;
    }

    private static boolean isValidDroppedItemEntity(ItemEntity itemEntity, LocalPlayer player, double maxDistanceSq) {
        return itemEntity != null
            && itemEntity.isAlive()
            && !itemEntity.isRemoved()
            && !itemEntity.isInvisible()
            && player.distanceToSqr(itemEntity) <= maxDistanceSq;
    }

    public static List<AggregatedDroppedItemEntry> aggregateDroppedItemEntries(
        List<DroppedItemEntry> entries,
        Comparator<AggregatedDroppedItemEntry> comparator
    ) {
        Map<DroppedItemKey, AggregatedDroppedItemEntry> aggregatedEntries = new LinkedHashMap<>();

        for (DroppedItemEntry entry : entries) {
            ItemStack stack = entry.itemEntity().getItem();
            if (stack.isEmpty()) {
                continue;
            }

            DroppedItemKey mergeKey = new DroppedItemKey(stack);
            AggregatedDroppedItemEntry existingEntry = aggregatedEntries.get(mergeKey);
            if (existingEntry == null) {
                aggregatedEntries.put(mergeKey, new AggregatedDroppedItemEntry(
                    mergeKey.hashCode(),
                    getDroppedItemBaseName(stack),
                    entry.typeName(),
                    stack.getCount(),
                    entry.distance()
                ));
                continue;
            }

            aggregatedEntries.put(mergeKey, new AggregatedDroppedItemEntry(
                existingEntry.mergeKey(),
                existingEntry.displayName(),
                existingEntry.typeName(),
                existingEntry.count() + stack.getCount(),
                Math.min(existingEntry.distance(), entry.distance())
            ));
        }

        List<AggregatedDroppedItemEntry> result = new ArrayList<>(aggregatedEntries.values());
        result.sort(comparator);
        return result;
    }

    public static String buildDroppedItemHudLine(AggregatedDroppedItemEntry entry, boolean showType, boolean showDistance) {
        List<String> parts = new ArrayList<>(3);
        parts.add(entry.count() > 1 ? entry.displayName() + " x" + entry.count() : entry.displayName());

        if (showType) {
            parts.add(entry.typeName());
        }

        if (showDistance) {
            parts.add(String.format(Locale.ROOT, "%.1fm", entry.distance()));
        }

        return String.join(" | ", parts);
    }

    private static String getDroppedItemBaseName(ItemStack stack) {
        return stack.getHoverName().getString();
    }

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

    @Environment(EnvType.CLIENT)
    public record DroppedItemEntry(ItemEntity itemEntity, String typeName, double distance) {
    }

    @Environment(EnvType.CLIENT)
    public record AggregatedDroppedItemEntry(
        int mergeKey,
        String displayName,
        String typeName,
        int count,
        double distance
    ) {
    }
}
