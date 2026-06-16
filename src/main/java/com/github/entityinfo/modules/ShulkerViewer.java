package com.github.entityinfo.modules;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.entityinfo.gui.ShulkerPreviewScreen;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.server.level.ServerPlayer;

@Environment(EnvType.CLIENT)
public final class ShulkerViewer extends Module {

    private static final int PREVIEW_SLOT_COUNT = 27;
    private static final String BLOCK_ENTITY_TAG_KEY = "BlockEntityTag";
    private static final String TAG_KEY = "tag";
    private static final String SLOT_KEY = "Slot";

    public static final ShulkerViewer INSTANCE = new ShulkerViewer();

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgPreview = settingGroup("Preview");
    private final SettingGroup sgSidebar = settingGroup("Sidebar");

    private final BoolSetting tooltips = boolSetting("Tooltips", true).group(sgGeneral);
    private final BoolSetting middleClickOpen = boolSetting("Middle Click Open", true).group(sgGeneral);
    private final BoolSetting allowEnderChest = boolSetting("Allow Ender Chest", true).group(sgGeneral);
    private final BoolSetting previewBackground = boolSetting("Preview Background", true).group(sgPreview);
    private final BoolSetting previewSlotFrames = boolSetting("Preview Slot Frames", true).group(sgPreview);
    private final BoolSetting drawEmptySlots = boolSetting("Draw Empty Slots", true).group(sgPreview);
    private final BoolSetting containerSidebar = boolSetting("Container Sidebar", true).group(sgSidebar);
    private final BoolSetting sidebarBackground = boolSetting("Sidebar Background", true, containerSidebar::getValue).group(sgSidebar);
    private final BoolSetting sidebarSlotFrames = boolSetting("Sidebar Slot Frames", true, containerSidebar::getValue).group(sgSidebar);
    private final EnumSetting<SidebarSortMode> sidebarSortMode = enumSetting("Sidebar Sort", SidebarSortMode.Original, containerSidebar::getValue).group(sgSidebar);
    private final BoolSetting compactUniformPreview = boolSetting("Compact Uniform Preview", true, containerSidebar::getValue).group(sgSidebar);
    private final IntSetting sidebarX = intSetting("Sidebar X", 0, 0, 240, 1, containerSidebar::getValue).group(sgSidebar);
    private final IntSetting sidebarY = intSetting("Sidebar Y", 24, 0, 240, 1, containerSidebar::getValue).group(sgSidebar);

    private ShulkerViewer() {
        super("Shulker Viewer", Category.PLAYER);
    }

    public boolean shouldRenderTooltip() {
        return isEnabled() && tooltips.getValue();
    }

    public boolean shouldOpenOnMiddleClick() {
        return isEnabled() && middleClickOpen.getValue();
    }

    public boolean shouldDrawEmptySlots() {
        return drawEmptySlots.getValue();
    }

    public boolean shouldDrawPreviewBackground() {
        return previewBackground.getValue();
    }

    public boolean shouldDrawPreviewSlotFrames() {
        return previewSlotFrames.getValue();
    }

    public boolean shouldDrawSidebarBackground() {
        return shouldRenderContainerSidebar() && sidebarBackground.getValue();
    }

    public boolean shouldDrawSidebarSlotFrames() {
        return shouldRenderContainerSidebar() && sidebarSlotFrames.getValue();
    }

    public boolean shouldShowNestedPreviewHint(List<ItemStack> contents) {
        if (!shouldOpenOnMiddleClick() || contents == null) {
            return false;
        }

        for (ItemStack stack : contents) {
            if (!stack.isEmpty() && canPreviewNested(stack)) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldRenderContainerSidebar() {
        return isEnabled() && containerSidebar.getValue();
    }

    public boolean shouldCompactUniformPreview() {
        return shouldRenderContainerSidebar() && compactUniformPreview.getValue();
    }

    public int getPreviewAccentColor(ItemStack stack) {
        return getAccentColor(stack);
    }

    public int getPreviewHeaderTextColor(ItemStack stack) {
        return getPreviewHeaderTextColor(getAccentColor(stack));
    }

    public int getPreviewHeaderTextColor(int accentColor) {
        return isLightAccent(accentColor) ? 0xFF111111 : 0xFFFFFFFF;
    }

    public int getPreviewHeaderHintColor(ItemStack stack) {
        return getPreviewHeaderHintColor(getAccentColor(stack));
    }

    public int getPreviewHeaderHintColor(int accentColor) {
        return isLightAccent(accentColor) ? 0xDD202020 : 0xFFF0E6FF;
    }

    public int getSidebarOffsetX() {
        return Math.max(0, sidebarX.getValue());
    }

    public int getSidebarOffsetY() {
        return Math.max(0, sidebarY.getValue());
    }

    public boolean canPreview(ItemStack stack) {
        if (!isPotentialPreviewContainer(stack)) {
            return false;
        }

        return hasAnyPreviewContents(getPreviewContents(stack));
    }

    public boolean canPreviewNested(ItemStack stack) {
        return !isNestedPreviewExcluded(stack) && canPreview(stack);
    }

    private boolean isPotentialPreviewContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.is(Items.ENDER_CHEST)) {
            return allowEnderChest.getValue();
        }

        return isShulkerBox(stack) || hasContainerData(stack);
    }

    public List<ItemStack> getPreviewContents(ItemStack stack) {
        List<ItemStack> contents = createEmptyPreviewContents();
        if (stack == null || stack.isEmpty()) {
            return contents;
        }

        if (allowEnderChest.getValue() && stack.is(Items.ENDER_CHEST)) {
            collectEnderChestContents(contents);
            return contents;
        }

        if (copyContainerComponent(stack, contents)) {
            return contents;
        }

        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (copyItemsListFromTag(blockEntityData != null ? blockEntityData.copyTagWithoutId() : null, contents)) {
            return contents;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag customTag = customData.copyTag();
            if (copyItemsListFromTag(customTag, contents)) {
                return contents;
            }

            CompoundTag blockEntityTag = customTag.getCompound(BLOCK_ENTITY_TAG_KEY).orElse(null);
            if (copyItemsListFromTag(blockEntityTag, contents)) {
                return contents;
            }

            CompoundTag nestedTag = customTag.getCompound(TAG_KEY).orElse(null);
            if (copyItemsListFromTag(nestedTag, contents)) {
                return contents;
            }
        }

        return contents;
    }

    public void openPreview(ItemStack stack) {
        if (nullCheck() || !canPreview(stack)) {
            return;
        }

        openPreview(stack, mc.screen);
    }

    public void openPreview(ItemStack stack, Screen parentScreen) {
        if (nullCheck() || !isPotentialPreviewContainer(stack)) {
            return;
        }

        List<ItemStack> contents = getPreviewContents(stack);
        if (!hasAnyPreviewContents(contents)) {
            return;
        }

        mc.setScreen(new ShulkerPreviewScreen(
            parentScreen,
            stack.copy(),
            contents,
            shouldDrawEmptySlots(),
            shouldShowNestedPreviewHint(contents)
        ));
    }

    public void renderTooltipPreview(GuiGraphicsExtractor graphics, ItemStack stack, int mouseX, int mouseY) {
        if (!shouldRenderTooltip() || !isPotentialPreviewContainer(stack)) {
            return;
        }

        List<ItemStack> contents = getPreviewContents(stack);
        if (!hasAnyPreviewContents(contents)) {
            return;
        }

        ShulkerPreviewScreen.renderPreviewTooltip(
            graphics,
            mc.font,
            stack,
            contents,
            mouseX,
            mouseY,
            shouldDrawEmptySlots(),
            shouldShowNestedPreviewHint(contents)
        );
    }

    public List<ContainerPreviewEntry> collectContainerPreviewEntries(AbstractContainerMenu menu) {
        if (menu == null) {
            return List.of();
        }

        List<ContainerPreviewEntry> entries = new ArrayList<>();
        Set<ContainerPreviewKey> seenEntries = new HashSet<>();
        for (Slot slot : menu.slots) {
            if (slot == null || !slot.isActive() || !isContainerSlot(slot)) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (!shouldIncludeSidebarItem(stack) || !isPotentialPreviewContainer(stack)) {
                continue;
            }

            List<ItemStack> contents = getPreviewContents(stack);
            if (!hasAnyPreviewContents(contents)) {
                continue;
            }
            List<ItemStack> frozenContents = copyContents(contents);
            List<ItemStack> frozenSidebarContents = buildSidebarContents(frozenContents, createSidebarComparator());
            if (!hasAnyPreviewContents(frozenSidebarContents)) {
                continue;
            }
            boolean compactSidebar = shouldCompactUniformPreview() && shouldRenderCompactSidebar(frozenSidebarContents);
            int visibleRows = getSidebarVisibleRows(frozenSidebarContents, compactSidebar);
            ContainerPreviewKey previewKey = new ContainerPreviewKey(stack.copy(), frozenContents, compactSidebar);
            if (!seenEntries.add(previewKey)) {
                continue;
            }

            entries.add(new ContainerPreviewEntry(
                slot.x,
                slot.y,
                stack.copy(),
                frozenContents,
                frozenSidebarContents,
                getAccentColor(stack),
                compactSidebar,
                visibleRows
            ));
        }

        entries.sort(Comparator.comparingInt(ContainerPreviewEntry::slotY).thenComparingInt(ContainerPreviewEntry::slotX));
        return entries;
    }

    private void collectEnderChestContents(List<ItemStack> contents) {
        if (mc.player == null) {
            return;
        }

        if (copyEnderChestContents(mc.player.getEnderChestInventory(), contents)) {
            return;
        }

        if (!mc.isSingleplayer()) {
            return;
        }

        if (mc.getSingleplayerServer() == null) {
            return;
        }

        ServerPlayer serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (serverPlayer != null) {
            copyEnderChestContents(serverPlayer.getEnderChestInventory(), contents);
        }
    }

    private static List<ItemStack> createEmptyPreviewContents() {
        List<ItemStack> contents = new ArrayList<>(PREVIEW_SLOT_COUNT);
        for (int slot = 0; slot < PREVIEW_SLOT_COUNT; slot++) {
            contents.add(ItemStack.EMPTY);
        }
        return contents;
    }

    private static boolean copyContainerComponent(ItemStack stack, List<ItemStack> contents) {
        ItemContainerContents containerContents = stack.get(DataComponents.CONTAINER);
        if (containerContents == null) {
            return false;
        }

        NonNullList<ItemStack> slotItems = NonNullList.withSize(PREVIEW_SLOT_COUNT, ItemStack.EMPTY);
        containerContents.copyInto(slotItems);
        boolean hasAnyItem = false;

        for (int slot = 0; slot < Math.min(slotItems.size(), contents.size()); slot++) {
            ItemStack slotStack = slotItems.get(slot).copy();
            contents.set(slot, slotStack);
            hasAnyItem |= !slotStack.isEmpty();
        }

        return hasAnyItem;
    }

    private static boolean copyItemsListFromTag(CompoundTag tag, List<ItemStack> contents) {
        if (tag == null) {
            return false;
        }

        ListTag itemList = tag.getList(ContainerHelper.TAG_ITEMS).orElse(null);
        if (itemList == null || itemList.isEmpty()) {
            return false;
        }

        boolean hasAnyItem = false;
        for (int index = 0; index < itemList.size(); index++) {
            CompoundTag itemTag = itemList.getCompound(index).orElse(null);
            if (itemTag == null) {
                continue;
            }

            int slot = itemTag.getByte(SLOT_KEY).map(value -> value & 255).orElse(index);
            if (slot < 0 || slot >= contents.size()) {
                continue;
            }

            ItemStack stack = decodeStack(itemTag);
            contents.set(slot, stack);
            hasAnyItem |= !stack.isEmpty();
        }

        return hasAnyItem;
    }

    private static ItemStack decodeStack(CompoundTag itemTag) {
        CompoundTag stackTag = itemTag.copy();
        stackTag.remove(SLOT_KEY);
        return decode(ItemStack.CODEC, stackTag, ItemStack.EMPTY);
    }

    private static boolean copyEnderChestContents(PlayerEnderChestContainer inventory, List<ItemStack> contents) {
        boolean hasAnyItem = false;
        int maxSlots = Math.min(PREVIEW_SLOT_COUNT, inventory.getContainerSize());
        for (int slot = 0; slot < maxSlots; slot++) {
            ItemStack stack = inventory.getItem(slot).copy();
            contents.set(slot, stack);
            hasAnyItem |= !stack.isEmpty();
        }
        return hasAnyItem;
    }

    private static boolean hasContainerData(ItemStack stack) {
        if (stack.has(DataComponents.CONTAINER)) {
            return true;
        }

        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            if (containsItemsList(blockEntityData.copyTagWithoutId())) {
                return true;
            }

            if (isSupportedContainerType(blockEntityData.type())) {
                return true;
            }
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag customTag = customData.copyTag();
        boolean supportedContainerItem = isSupportedContainerItem(stack);
        return containsItemsList(customTag)
            || containsContainerTag(customTag.getCompound(BLOCK_ENTITY_TAG_KEY).orElse(null), supportedContainerItem)
            || containsContainerTag(customTag.getCompound(TAG_KEY).orElse(null), supportedContainerItem)
            || supportedContainerItem && !customTag.isEmpty();
    }

    private static boolean containsItemsList(CompoundTag tag) {
        return tag != null && tag.getList(ContainerHelper.TAG_ITEMS).map(list -> !list.isEmpty()).orElse(false);
    }

    private static boolean containsContainerTag(CompoundTag tag, boolean supportedContainerItem) {
        return tag != null && (containsItemsList(tag) || supportedContainerItem && !tag.isEmpty());
    }

    private static <T> T decode(Codec<T> codec, CompoundTag tag, T fallback) {
        return codec.parse(NbtOps.INSTANCE, tag).result().orElse(fallback);
    }

    private static boolean isSupportedContainerType(BlockEntityType<?> type) {
        return type == BlockEntityType.CHEST
            || type == BlockEntityType.TRAPPED_CHEST
            || type == BlockEntityType.BARREL
            || type == BlockEntityType.SHULKER_BOX
            || type == BlockEntityType.HOPPER
            || type == BlockEntityType.DISPENSER
            || type == BlockEntityType.DROPPER
            || type == BlockEntityType.CRAFTER;
    }

    private static boolean isSupportedContainerItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        return blockItem.getBlock() instanceof ChestBlock
            || blockItem.getBlock() instanceof BarrelBlock
            || blockItem.getBlock() instanceof ShulkerBoxBlock
            || blockItem.getBlock() instanceof HopperBlock
            || blockItem.getBlock() instanceof DispenserBlock
            || blockItem.getBlock() instanceof DropperBlock
            || blockItem.getBlock() instanceof CrafterBlock;
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean shouldIncludeSidebarItem(ItemStack stack) {
        return !stack.is(Items.ENDER_CHEST);
    }

    private static boolean isNestedPreviewExcluded(ItemStack stack) {
        return stack != null && stack.is(Items.ENDER_CHEST);
    }

    private boolean isContainerSlot(Slot slot) {
        if (mc.player != null && slot.container == mc.player.getInventory()) {
            return false;
        }

        return slot.container != null
            && slot.index >= 0
            && slot.index < slot.container.getContainerSize()
            && slot.getContainerSlot() == slot.index;
    }

    private static int getAccentColor(ItemStack stack) {
        if (stack.is(Items.ENDER_CHEST)) {
            return 0xFF7156C9;
        }

        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock) {
            DyeColor color = shulkerBoxBlock.getColor();
            if (color != null) {
                return 0xFF000000 | color.getTextColor();
            }

            return 0xFFB06AE2;
        }

        return 0xFF9B6AD8;
    }

    private static boolean isLightAccent(int accentColor) {
        int red = (accentColor >> 16) & 255;
        int green = (accentColor >> 8) & 255;
        int blue = accentColor & 255;
        int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
        return luminance >= 170;
    }

    private Comparator<ItemStack> createSidebarComparator() {
        return switch (sidebarSortMode.getValue()) {
            case Original -> null;
            case NameAsc -> Comparator.comparing(ShulkerViewer::getSidebarSortName, String.CASE_INSENSITIVE_ORDER);
            case NameDesc -> Comparator.comparing(ShulkerViewer::getSidebarSortName, String.CASE_INSENSITIVE_ORDER).reversed();
            case CountAsc -> Comparator.comparingInt(ItemStack::getCount);
            case CountDesc -> Comparator.comparingInt(ItemStack::getCount).reversed();
        };
    }

    private static List<ItemStack> buildSidebarContents(List<ItemStack> contents, Comparator<ItemStack> comparator) {
        List<ItemStack> sidebarContents = createEmptyPreviewContents();
        Map<SidebarItemKey, ItemStack> mergedContents = new LinkedHashMap<>();
        for (ItemStack stack : contents) {
            if (stack.isEmpty() || !shouldIncludeSidebarItem(stack)) {
                continue;
            }

            SidebarItemKey key = new SidebarItemKey(stack);
            ItemStack mergedStack = mergedContents.get(key);
            if (mergedStack == null) {
                mergedContents.put(key, stack.copy());
                continue;
            }

            mergedStack.setCount(mergedStack.getCount() + stack.getCount());
        }

        List<ItemStack> mergedStacks = new ArrayList<>(mergedContents.values());
        if (comparator != null) {
            mergedStacks.sort(
                comparator
                    .thenComparing(ShulkerViewer::getSidebarSortName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(ShulkerViewer::getSidebarSortHash)
            );
        }

        int slot = 0;
        for (ItemStack mergedStack : mergedStacks) {
            if (slot >= sidebarContents.size()) {
                break;
            }

            sidebarContents.set(slot++, mergedStack);
        }
        return List.copyOf(sidebarContents);
    }

    private static List<ItemStack> copyContents(List<ItemStack> contents) {
        List<ItemStack> copy = new ArrayList<>(Math.min(contents.size(), PREVIEW_SLOT_COUNT));
        int size = Math.min(contents.size(), PREVIEW_SLOT_COUNT);
        for (int slot = 0; slot < size; slot++) {
            copy.add(contents.get(slot).copy());
        }
        return List.copyOf(copy);
    }

    private static boolean shouldRenderCompactSidebar(List<ItemStack> sidebarContents) {
        int nonEmptyCount = 0;
        for (ItemStack stack : sidebarContents) {
            if (!stack.isEmpty()) {
                nonEmptyCount++;
                if (nonEmptyCount > 1) {
                    return false;
                }
            }
        }

        return nonEmptyCount == 1;
    }

    private static int getSidebarVisibleRows(List<ItemStack> sidebarContents, boolean compactSidebar) {
        if (compactSidebar) {
            return 1;
        }

        int nonEmptyCount = countNonEmptyStacks(sidebarContents);
        if (nonEmptyCount == 0) {
            return 1;
        }

        return Math.max(1, Math.min(3, (nonEmptyCount + 8) / 9));
    }

    private static int countNonEmptyStacks(List<ItemStack> stacks) {
        int nonEmptyCount = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                nonEmptyCount++;
            }
        }
        return nonEmptyCount;
    }

    private static boolean hasAnyPreviewContents(List<ItemStack> contents) {
        return countNonEmptyStacks(contents) > 0;
    }

    private static String getSidebarSortName(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    private static int getSidebarSortHash(ItemStack stack) {
        return ItemStack.hashItemAndComponents(stack);
    }

    public record ContainerPreviewEntry(
        int slotX,
        int slotY,
        ItemStack stack,
        List<ItemStack> contents,
        List<ItemStack> sidebarContents,
        int accentColor,
        boolean compactSidebar,
        int visibleRows
    ) {
    }

    private record ContainerPreviewKey(ItemStack stack, List<ItemStack> contents, boolean compactSidebar) {
        private ContainerPreviewKey(ItemStack stack, List<ItemStack> contents, boolean compactSidebar) {
            this.stack = stack.copy();
            this.contents = copyContents(contents);
            this.compactSidebar = compactSidebar;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof ContainerPreviewKey other)) {
                return false;
            }

            return compactSidebar == other.compactSidebar
                && ItemStack.isSameItemSameComponents(stack, other.stack)
                && entityinfo$matchesContents(contents, other.contents);
        }

        @Override
        public int hashCode() {
            int hash = 31 * Boolean.hashCode(compactSidebar) + entityinfo$stackIdentityHash(stack);
            for (ItemStack content : contents) {
                hash = 31 * hash + entityinfo$contentHash(content);
            }
            return hash;
        }
    }

    private record SidebarItemKey(ItemStack stack) {
        private SidebarItemKey(ItemStack stack) {
            this.stack = stack.copy();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof SidebarItemKey other && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return entityinfo$stackIdentityHash(stack);
        }
    }

    @Environment(EnvType.CLIENT)
    private enum SidebarSortMode {
        Original,
        NameAsc,
        NameDesc,
        CountAsc,
        CountDesc
    }

    private static boolean entityinfo$matchesContents(List<ItemStack> left, List<ItemStack> right) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            ItemStack leftStack = left.get(index);
            ItemStack rightStack = right.get(index);
            if (leftStack.isEmpty() && rightStack.isEmpty()) {
                continue;
            }

            if (leftStack.isEmpty() != rightStack.isEmpty()) {
                return false;
            }

            if (!ItemStack.isSameItemSameComponents(leftStack, rightStack) || leftStack.getCount() != rightStack.getCount()) {
                return false;
            }
        }

        return true;
    }

    private static int entityinfo$contentHash(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return 31 * ItemStack.hashItemAndComponents(stack) + stack.getCount();
    }

    private static int entityinfo$stackIdentityHash(ItemStack stack) {
        return stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack);
    }
}
