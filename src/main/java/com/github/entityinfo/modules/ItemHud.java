package com.github.entityinfo.modules;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.shaders.BlurShader;
import com.github.epsilon.gui.hudeditor.HudEditorScreen;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ButtonSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.settings.impl.StringSetting;
import com.google.common.base.Suppliers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public final class ItemHud extends HudModule {

    public static final ItemHud INSTANCE = new ItemHud();

    private static final float ICON_SIZE = 16.0F;
    private static final float SLOT_SIZE = 18.0F;
    private static final float ENTRY_GAP = 8.0F;
    private static final float TEXT_GAP = 2.0F;
    private static final float TEXT_RIGHT_INSET = 1.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_Y = 5.0F;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgSources = settingGroup("Sources");
    private final SettingGroup sgAppearance = settingGroup("Appearance");
    private final SettingGroup sgEffect = settingGroup("Effect");

    private final StringSetting selectedItemId = stringSetting("Selected Item", "minecraft:totem_of_undying").group(sgGeneral);
    private final StringSetting trackedItems = stringSetting("Tracked Items", "minecraft:totem_of_undying").group(sgGeneral);
    private final ButtonSetting useHeldItem = buttonSetting("Use Held Item", this::copyHeldItemId).group(sgGeneral);
    private final ButtonSetting addSelectedItem = buttonSetting("Add Selected", this::addSelectedItem).group(sgGeneral);
    private final ButtonSetting removeSelectedItem = buttonSetting("Remove Selected", this::removeSelectedItem).group(sgGeneral);
    private final ButtonSetting clearItems = buttonSetting("Clear Items", this::clearItems).group(sgGeneral);
    private final BoolSetting ignoreInvalid = boolSetting("Ignore Invalid", true).group(sgGeneral);
    private final BoolSetting hideEmpty = boolSetting("Hide Empty", false).group(sgGeneral);
    private final BoolSetting renderInScreens = boolSetting("Render In Screens", false).group(sgGeneral);
    private final EnumSetting<SortMode> sortMode = enumSetting("Sort Mode", SortMode.Original).group(sgGeneral);
    private final BoolSetting includeMainInventory = boolSetting("Include Inventory", true).group(sgSources);
    private final BoolSetting includeHotbar = boolSetting("Include Hotbar", true).group(sgSources);
    private final BoolSetting includeOffhand = boolSetting("Include Offhand", true).group(sgSources);
    private final BoolSetting includeArmor = boolSetting("Include Armor", false).group(sgSources);
    private final BoolSetting includeCraftingSlots = boolSetting("Include Crafting Slots", true).group(sgSources);
    private final DoubleSetting scale = doubleSetting("Scale", 1.0D, 0.5D, 2.0D, 0.1D).group(sgAppearance);
    private final EnumSetting<LayoutMode> layoutMode = enumSetting("Layout", LayoutMode.Horizontal).group(sgAppearance);
    private final BoolSetting textShadow = boolSetting("Text Shadow", true).group(sgAppearance);
    private final BoolSetting showBackground = boolSetting("Background", false).group(sgAppearance);
    private final BoolSetting showSlots = boolSetting("Slots", false).group(sgAppearance);
    private final IntSetting minWidth = intSetting("Min Width", 0, 0, 320, 2, () -> layoutMode.getValue() == LayoutMode.Horizontal).group(sgAppearance);
    private final IntSetting columns = intSetting("Columns", 4, 1, 12, 1, () -> layoutMode.getValue() == LayoutMode.Grid).group(sgAppearance);
    private final ColorSetting textColor = colorSetting("Text Color", Color.WHITE, true).group(sgAppearance);
    private final ColorSetting backgroundColor = colorSetting("Background Color", new Color(15, 15, 15, 135), showBackground::getValue).group(sgAppearance);
    private final ColorSetting slotColor = colorSetting("Slot Color", new Color(0, 0, 0, 70), showSlots::getValue).group(sgAppearance);
    private final BoolSetting drawShadow = boolSetting("Drop Shadow", false).group(sgEffect);
    private final DoubleSetting shadowBlur = doubleSetting("Shadow Blur", 2.2D, 0.1D, 32.0D, 0.5D, drawShadow::getValue).group(sgEffect);
    private final ColorSetting shadowColor = colorSetting("Shadow Color", new Color(0, 0, 0, 70), drawShadow::getValue).group(sgEffect);
    private final BoolSetting backgroundBlur = boolSetting("Background Blur", false, showBackground::getValue).group(sgEffect);
    private final IntSetting blurStrength = intSetting("Blur Strength", 5, 1, 16, 1, backgroundBlur::getValue).group(sgEffect);

    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::create);
    private final Supplier<ShadowRenderer> shadowRendererSupplier = Suppliers.memoize(ShadowRenderer::create);
    private final List<ResolvedTrackedItem> cachedTrackedItems = new ArrayList<>();
    private String cachedTrackedItemsValue = "";
    private boolean cachedIgnoreInvalid = true;

    private ItemHud() {
        super("Item HUD", Category.HUD, 8.0F, 116.0F, 18.0F, 18.0F);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (nullCheck() || shouldSkipScreenRender()) {
            return;
        }

        List<TrackedItemEntry> entries = collectTrackedEntries();
        if (entries.isEmpty()) {
            setBounds(0.0F, 0.0F);
            return;
        }

        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();
        ShadowRenderer shadowRenderer = shadowRendererSupplier.get();

        float renderScale = scale.getValue().floatValue();
        float slotSize = (showSlots.getValue() ? SLOT_SIZE : ICON_SIZE) * renderScale;
        float itemInset = showSlots.getValue() ? renderScale : 0.0F;
        float entryGap = ENTRY_GAP * renderScale;
        float textGap = TEXT_GAP * renderScale;
        float paddingX = visualPaddingX(renderScale);
        float paddingY = visualPaddingY(renderScale);
        float backgroundRadius = 4.0F * renderScale;
        float slotRadius = 2.0F * renderScale;

        List<RenderEntry> renderEntries = buildRenderEntries(entries, renderScale, slotSize, textGap, paddingX, paddingY);
        LayoutMetrics layout = computeLayout(renderEntries, renderScale, slotSize, entryGap, paddingX, paddingY);
        float totalWidth = layout.totalWidth();
        float totalHeight = layout.totalHeight();

        if (showBackground.getValue() && backgroundBlur.getValue()) {
            BlurShader.INSTANCE.render(this.x, this.y, totalWidth, totalHeight, backgroundRadius, blurStrength.getValue());
        }

        if (showBackground.getValue() && drawShadow.getValue()) {
            shadowRenderer.addShadow(
                this.x,
                this.y,
                totalWidth,
                totalHeight,
                backgroundRadius,
                shadowBlur.getValue().floatValue(),
                shadowColor.getValue()
            );
        }

        if (showBackground.getValue()) {
            roundRectRenderer.addRoundRect(this.x, this.y, totalWidth, totalHeight, backgroundRadius, backgroundColor.getValue());
        }

        if (showSlots.getValue()) {
            for (int index = 0; index < renderEntries.size(); index++) {
                RenderEntry renderEntry = renderEntries.get(index);
                roundRectRenderer.addRoundRect(
                    this.x + renderEntry.slotX(),
                    this.y + renderEntry.slotY(),
                    slotSize,
                    slotSize,
                    slotRadius,
                    slotColor.getValue()
                );
            }
        }

        if (showBackground.getValue() && drawShadow.getValue()) {
            shadowRenderer.draw();
            shadowRenderer.clear();
        }

        if (showBackground.getValue() || showSlots.getValue()) {
            roundRectRenderer.draw();
            roundRectRenderer.clear();
        }

        for (RenderEntry renderEntry : renderEntries) {
            TrackedItemEntry entry = renderEntry.entry();

            graphics.pose().pushMatrix();
            graphics.pose().translate(this.x + renderEntry.slotX() + itemInset, this.y + renderEntry.slotY() + itemInset);
            graphics.pose().scale(renderScale, renderScale);
            graphics.item(entry.previewStack(), 0, 0);
            graphics.pose().popMatrix();

            graphics.pose().pushMatrix();
            graphics.pose().translate(this.x + renderEntry.textX(), this.y + renderEntry.textY());
            graphics.pose().scale(renderScale, renderScale);
            graphics.text(mc.font, entry.countLabel(), 0, 0, textColor.getValue().getRGB(), textShadow.getValue());
            graphics.pose().popMatrix();
        }

        setBounds(totalWidth, totalHeight);
    }

    private List<TrackedItemEntry> collectTrackedEntries() {
        List<ResolvedTrackedItem> resolvedItems = getResolvedTrackedItems();
        if (resolvedItems.isEmpty()) {
            return List.of();
        }

        Map<Item, Integer> itemCounts = countTrackedItems(resolvedItems);
        List<TrackedItemEntry> entries = new ArrayList<>(resolvedItems.size());

        for (ResolvedTrackedItem resolvedItem : resolvedItems) {
            Item item = resolvedItem.item();
            if (item == null) {
                entries.add(new TrackedItemEntry(
                    resolvedItem.itemId(),
                    resolvedItem.sortName(),
                    resolvedItem.previewStack(),
                    0,
                    resolvedItem.originalIndex()
                ));
                continue;
            }

            int count = itemCounts.getOrDefault(item, 0);
            if (hideEmpty.getValue() && count <= 0) {
                continue;
            }
            entries.add(new TrackedItemEntry(
                resolvedItem.itemId(),
                resolvedItem.sortName(),
                resolvedItem.previewStack(),
                count,
                resolvedItem.originalIndex()
            ));
        }

        entries.sort(createComparator());
        return entries;
    }

    private List<ResolvedTrackedItem> getResolvedTrackedItems() {
        String trackedItemsValue = trackedItems.getValue();
        boolean ignoreInvalidValue = ignoreInvalid.getValue();
        if (trackedItemsValue.equals(cachedTrackedItemsValue) && ignoreInvalidValue == cachedIgnoreInvalid) {
            return cachedTrackedItems;
        }

        cachedTrackedItemsValue = trackedItemsValue;
        cachedIgnoreInvalid = ignoreInvalidValue;
        cachedTrackedItems.clear();

        Set<String> uniqueIds = parseTrackedItemIds(trackedItemsValue);
        int originalIndex = 0;

        for (String itemId : uniqueIds) {
            Optional<Item> item = resolveItem(itemId);
            if (item.isEmpty()) {
                if (!ignoreInvalidValue) {
                    cachedTrackedItems.add(new ResolvedTrackedItem(itemId, itemId, new ItemStack(Items.BARRIER), null, originalIndex++));
                }
                continue;
            }

            Item resolvedItem = item.get();
            ItemStack previewStack = new ItemStack(resolvedItem);
            cachedTrackedItems.add(new ResolvedTrackedItem(
                itemId,
                previewStack.getHoverName().getString(),
                previewStack,
                resolvedItem,
                originalIndex++
            ));
        }

        return cachedTrackedItems;
    }

    private boolean shouldSkipScreenRender() {
        return mc.screen instanceof CreativeModeInventoryScreen
            || !renderInScreens.getValue() && mc.screen != null && !(mc.screen instanceof HudEditorScreen);
    }

    private Comparator<TrackedItemEntry> createComparator() {
        if (sortMode.getValue() == SortMode.Original) {
            return Comparator.comparingInt(TrackedItemEntry::originalIndex);
        }

        Comparator<TrackedItemEntry> comparator = switch (sortMode.getValue()) {
            case Original -> Comparator.comparingInt(TrackedItemEntry::originalIndex);
            case NameAsc -> Comparator.comparing(TrackedItemEntry::sortName, String.CASE_INSENSITIVE_ORDER);
            case NameDesc -> Comparator.comparing(TrackedItemEntry::sortName, String.CASE_INSENSITIVE_ORDER).reversed();
            case CountAsc -> Comparator.comparingInt(TrackedItemEntry::count);
            case CountDesc -> Comparator.comparingInt(TrackedItemEntry::count).reversed();
        };

        return comparator
            .thenComparing(TrackedItemEntry::sortName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TrackedItemEntry::itemId, String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(TrackedItemEntry::originalIndex);
    }

    private List<RenderEntry> buildRenderEntries(
        List<TrackedItemEntry> entries,
        float renderScale,
        float slotSize,
        float textGap,
        float paddingX,
        float paddingY
    ) {
        List<RenderEntry> renderEntries = new ArrayList<>(entries.size());
        LayoutMode mode = layoutMode.getValue();

        int gridColumns = Math.max(1, columns.getValue());
        float gridEntryWidth = mode == LayoutMode.Grid ? getMaxEntryWidth(entries, renderScale, slotSize) : 0.0F;
        float entryGap = ENTRY_GAP * renderScale;
        float currentX = paddingX;
        float currentY = paddingY;

        for (int index = 0; index < entries.size(); index++) {
            TrackedItemEntry entry = entries.get(index);
            float textWidth = mc.font.width(entry.countLabel()) * renderScale;
            float textYOffset = 9.0F * renderScale;
            float textHeight = mc.font.lineHeight * renderScale;
            float entryWidth = mode == LayoutMode.Grid
                ? gridEntryWidth
                : getEntryWidth(entry, renderScale, slotSize);
            float entryHeight = Math.max(slotSize, textYOffset + textHeight);
            if (mode == LayoutMode.Grid) {
                int column = index % gridColumns;
                int row = index / gridColumns;
                currentX = paddingX + column * (gridEntryWidth + entryGap);
                currentY = paddingY + row * (entryHeight + entryGap);
            }
            float slotX = currentX + entryWidth - slotSize;
            float slotY = currentY + (entryHeight - slotSize) * 0.5F;
            float textX = currentX + entryWidth - textWidth;
            float textY = currentY + textYOffset;
            renderEntries.add(new RenderEntry(entry, currentX, currentY, slotX, slotY, textX, textY, entryWidth, entryHeight));

            if (mode == LayoutMode.Horizontal) {
                currentX += entryWidth + entryGap;
            }
        }

        return renderEntries;
    }

    private float getMaxEntryWidth(List<TrackedItemEntry> entries, float renderScale, float slotSize) {
        float maxWidth = slotSize;
        for (TrackedItemEntry entry : entries) {
            maxWidth = Math.max(maxWidth, getEntryWidth(entry, renderScale, slotSize));
        }
        return maxWidth;
    }

    private float getEntryWidth(TrackedItemEntry entry, float renderScale, float slotSize) {
        float textWidth = mc.font.width(entry.countLabel()) * renderScale;
        return Math.max(slotSize, textWidth + TEXT_RIGHT_INSET * renderScale);
    }

    private LayoutMetrics computeLayout(
        List<RenderEntry> renderEntries,
        float renderScale,
        float slotSize,
        float entryGap,
        float paddingX,
        float paddingY
    ) {
        float contentWidth = 0.0F;
        float contentHeight = 0.0F;
        LayoutMode mode = layoutMode.getValue();

        if (mode == LayoutMode.Horizontal) {
            float rightMost = 0.0F;
            float tallest = slotSize;
            for (RenderEntry renderEntry : renderEntries) {
                rightMost = Math.max(rightMost, renderEntry.entryX() + renderEntry.width());
                tallest = Math.max(tallest, renderEntry.height());
            }
            contentWidth = rightMost - paddingX;
            contentHeight = tallest;
            float totalWidth = Math.max(minWidth.getValue().floatValue() * renderScale, paddingX * 2.0F + contentWidth);
            float totalHeight = paddingY * 2.0F + contentHeight;
            return new LayoutMetrics(totalWidth, totalHeight);
        }

        float rightMost = 0.0F;
        float bottomMost = 0.0F;
        for (RenderEntry renderEntry : renderEntries) {
            rightMost = Math.max(rightMost, renderEntry.entryX() + renderEntry.width());
            bottomMost = Math.max(bottomMost, renderEntry.entryY() + renderEntry.height());
        }
        contentWidth = Math.max(0.0F, rightMost - paddingX);
        contentHeight = Math.max(slotSize, bottomMost - paddingY);
        return new LayoutMetrics(paddingX * 2.0F + contentWidth, paddingY * 2.0F + contentHeight);
    }

    private float visualPaddingX(float renderScale) {
        return (showBackground.getValue() || showSlots.getValue()) ? PADDING_X * renderScale : 0.0F;
    }

    private float visualPaddingY(float renderScale) {
        return (showBackground.getValue() || showSlots.getValue()) ? PADDING_Y * renderScale : 0.0F;
    }

    private Set<String> parseTrackedItemIds(String trackedItemsValue) {
        Set<String> itemIdsSet = new LinkedHashSet<>();
        for (String part : trackedItemsValue.split("[,;\\n\\r\\t]+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                String normalized = normalizeItemId(trimmed);
                itemIdsSet.add(normalized != null ? normalized : trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return itemIdsSet;
    }

    private Optional<Item> resolveItem(String rawId) {
        Identifier id = parseItemIdentifier(rawId);
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(id);
    }

    private Map<Item, Integer> countTrackedItems(List<ResolvedTrackedItem> resolvedItems) {
        if (mc.player == null) {
            return Map.of();
        }

        Set<Item> trackedItemSet = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ResolvedTrackedItem resolvedItem : resolvedItems) {
            if (resolvedItem.item() != null) {
                trackedItemSet.add(resolvedItem.item());
            }
        }

        if (trackedItemSet.isEmpty()) {
            return Map.of();
        }

        Map<Item, Integer> counts = new IdentityHashMap<>();
        Inventory inventory = mc.player.getInventory();
        if (includeHotbar.getValue() || includeMainInventory.getValue()) {
            NonNullList<ItemStack> nonEquipmentItems = inventory.getNonEquipmentItems();

            for (int index = 0; index < nonEquipmentItems.size(); index++) {
                ItemStack stack = nonEquipmentItems.get(index);
                if (stack.isEmpty()) {
                    continue;
                }

                boolean hotbarSlot = Inventory.isHotbarSlot(index);
                if (hotbarSlot && !includeHotbar.getValue()) {
                    continue;
                }
                if (!hotbarSlot && !includeMainInventory.getValue()) {
                    continue;
                }

                addTrackedStackCount(counts, trackedItemSet, stack);
            }
        }

        if (includeOffhand.getValue()) {
            addTrackedStackCount(counts, trackedItemSet, mc.player.getOffhandItem());
        }

        if (includeArmor.getValue()) {
            for (EquipmentSlot armorSlot : ARMOR_SLOTS) {
                addTrackedStackCount(counts, trackedItemSet, mc.player.getItemBySlot(armorSlot));
            }
        }

        if (includeCraftingSlots.getValue() && mc.player.inventoryMenu != null) {
            Container craftSlots = mc.player.inventoryMenu.getCraftSlots();
            for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
                addTrackedStackCount(counts, trackedItemSet, craftSlots.getItem(slot));
            }
        }

        return counts;
    }

    private void addTrackedStackCount(Map<Item, Integer> counts, Set<Item> trackedItems, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        Item item = stack.getItem();
        if (trackedItems.contains(item)) {
            counts.merge(item, stack.getCount(), Integer::sum);
        }
    }

    private void copyHeldItemId() {
        if (mc.player == null) {
            return;
        }

        ItemStack heldStack = mc.player.getMainHandItem();
        if (heldStack.isEmpty()) {
            heldStack = mc.player.getOffhandItem();
        }

        if (heldStack.isEmpty()) {
            return;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(heldStack.getItem());
        if (itemId == null) {
            return;
        }

        selectedItemId.setValue(itemId.toString());
    }

    private void addSelectedItem() {
        String normalized = normalizeItemId(selectedItemId.getValue());
        if (normalized == null) {
            return;
        }

        Set<String> ids = parseTrackedItemIds(trackedItems.getValue());
        ids.add(normalized);
        trackedItems.setValue(String.join(", ", ids));
    }

    private void removeSelectedItem() {
        String normalized = normalizeItemId(selectedItemId.getValue());
        if (normalized == null) {
            return;
        }

        Set<String> ids = parseTrackedItemIds(trackedItems.getValue());
        if (!ids.remove(normalized)) {
            return;
        }
        trackedItems.setValue(String.join(", ", ids));
    }

    private void clearItems() {
        trackedItems.setValue("");
    }

    private String normalizeItemId(String rawId) {
        String trimmed = rawId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }

        Identifier id = parseItemIdentifier(trimmed);
        if (id == null || BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
            return null;
        }

        return id.toString();
    }

    private Identifier parseItemIdentifier(String rawId) {
        String trimmed = rawId.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (trimmed.isEmpty()) {
            return null;
        }

        return Identifier.tryParse(trimmed.contains(":") ? trimmed : "minecraft:" + trimmed);
    }

    @Environment(EnvType.CLIENT)
    private record ResolvedTrackedItem(String itemId, String sortName, ItemStack previewStack, Item item, int originalIndex) {}

    @Environment(EnvType.CLIENT)
    private record TrackedItemEntry(String itemId, String sortName, ItemStack previewStack, int count, int originalIndex) {

        private String countLabel() {
            return Integer.toString(count);
        }
    }

    @Environment(EnvType.CLIENT)
    private record RenderEntry(
        TrackedItemEntry entry,
        float entryX,
        float entryY,
        float slotX,
        float slotY,
        float textX,
        float textY,
        float width,
        float height
    ) {}

    @Environment(EnvType.CLIENT)
    private record LayoutMetrics(float totalWidth, float totalHeight) {}

    @Environment(EnvType.CLIENT)
    private enum LayoutMode {
        Horizontal,
        Grid
    }

    @Environment(EnvType.CLIENT)
    private enum SortMode {
        Original,
        NameAsc,
        NameDesc,
        CountAsc,
        CountDesc
    }
}
