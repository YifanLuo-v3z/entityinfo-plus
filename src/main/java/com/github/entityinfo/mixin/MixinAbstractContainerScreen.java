package com.github.entityinfo.mixin;

import com.github.entityinfo.gui.ShulkerPreviewScreen;
import com.github.entityinfo.modules.ShulkerViewer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    private static final int SIDEBAR_SLOT_SIZE = 18;
    private static final int SIDEBAR_SLOT_GAP = 0;
    private static final int SIDEBAR_COLUMNS = 9;
    private static final int SIDEBAR_ROWS = 3;
    private static final int SIDEBAR_PADDING = 3;
    private static final int SIDEBAR_HEADER_HEIGHT = 11;
    private static final int SIDEBAR_GRID_TOP_GAP = 2;
    private static final int SIDEBAR_ENTRY_WIDTH = SIDEBAR_COLUMNS * SIDEBAR_SLOT_SIZE + SIDEBAR_PADDING * 2;
    private static final int SIDEBAR_ENTRY_GAP = 4;
    private static final int SIDEBAR_COLUMN_GAP = 8;
    private static final int SIDEBAR_MAX_COLUMNS = 4;
    private static final int SIDEBAR_BODY_BACKGROUND = 0xF1050505;
    private static final int SIDEBAR_BODY_BACKGROUND_HOVER = SIDEBAR_BODY_BACKGROUND;
    private static final int SIDEBAR_OUTLINE = 0xCC000000;
    private static final int SIDEBAR_HOVER_OUTLINE = 0xFFF4F4F4;
    private static final int SIDEBAR_DIVIDER = 0x55000000;
    private static final int SIDEBAR_ROW_BACKGROUND = 0xFF080808;
    private static final int SIDEBAR_SLOT_OUTLINE = 0xFF202020;
    private static final int SIDEBAR_SLOT_HOVER = 0x14FFFFFF;
    private static final int SIDEBAR_HINT = 0xFFEAEAEA;

    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    protected Slot hoveredSlot;

    @Unique
    private int entityinfo$sidebarScrollOffset;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void entityinfo$openShulkerPreview(MouseButtonEvent event, boolean allowOutsideClick, CallbackInfoReturnable<Boolean> cir) {
        if (entityinfo$isCreativeInventoryScreen()) {
            return;
        }

        if (ShulkerViewer.INSTANCE.shouldRenderContainerSidebar()) {
            HoverState hoverState = entityinfo$getSidebarHoverState(event.x(), event.y());
            if (hoverState != null) {
                if (hoverState.emptySlot()) {
                    cir.setReturnValue(true);
                    return;
                }

                ItemStack nestedPreviewStack = !hoverState.item().isEmpty() && ShulkerViewer.INSTANCE.canPreviewNested(hoverState.item())
                    ? hoverState.item()
                    : ItemStack.EMPTY;

                if (event.button() == 2 && ShulkerViewer.INSTANCE.shouldOpenOnMiddleClick()) {
                    if (!nestedPreviewStack.isEmpty()) {
                        ShulkerViewer.INSTANCE.openPreview(nestedPreviewStack);
                    } else if (hoverState.item().isEmpty()) {
                        ShulkerViewer.INSTANCE.openPreview(hoverState.entry().stack());
                    }
                    cir.setReturnValue(true);
                    return;
                }

                if (event.button() == 0) {
                    ItemStack previewStack = !nestedPreviewStack.isEmpty() ? nestedPreviewStack : hoverState.entry().stack();
                    ShulkerViewer.INSTANCE.openPreview(previewStack);
                    cir.setReturnValue(true);
                    return;
                }

                cir.setReturnValue(true);
                return;
            }
        }

        if (event.button() != 2 || hoveredSlot == null) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        if (!ShulkerViewer.INSTANCE.shouldOpenOnMiddleClick() || !ShulkerViewer.INSTANCE.canPreview(stack)) {
            return;
        }

        ShulkerViewer.INSTANCE.openPreview(stack);
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void entityinfo$scrollSidebar(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (entityinfo$isCreativeInventoryScreen()) {
            return;
        }

        if (!ShulkerViewer.INSTANCE.shouldRenderContainerSidebar() || verticalAmount == 0.0D) {
            return;
        }

        List<ShulkerViewer.ContainerPreviewEntry> entries = ShulkerViewer.INSTANCE.collectContainerPreviewEntries(menu);
        SidebarLayout layout = entityinfo$getSidebarLayout(entries, net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth(), net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight());
        if (!layout.hasVisibleColumns() || !entityinfo$isInsideSidebar(mouseX, mouseY, layout)) {
            return;
        }

        int direction = verticalAmount > 0.0D ? -1 : 1;
        int maxScrollOffset = entityinfo$getMaxSidebarScrollOffset(
            entries,
            layout.columnPositions(),
            net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight()
        );
        entityinfo$sidebarScrollOffset = Math.max(0, Math.min(entityinfo$sidebarScrollOffset + direction, maxScrollOffset));
        cir.setReturnValue(true);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void entityinfo$renderShulkerTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (entityinfo$isCreativeInventoryScreen()) {
            return;
        }

        Font font = ((AbstractContainerScreen<?>) (Object) this).getFont();
        if (ShulkerViewer.INSTANCE.shouldRenderContainerSidebar() && entityinfo$renderContainerSidebar(graphics, font, mouseX, mouseY)) {
            ci.cancel();
            return;
        }

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        if (!ShulkerViewer.INSTANCE.shouldRenderTooltip() || !ShulkerViewer.INSTANCE.canPreview(stack)) {
            return;
        }

        ShulkerViewer.INSTANCE.renderTooltipPreview(graphics, stack, mouseX, mouseY);
        ci.cancel();
    }

    private boolean entityinfo$renderContainerSidebar(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        List<ShulkerViewer.ContainerPreviewEntry> entries = ShulkerViewer.INSTANCE.collectContainerPreviewEntries(menu);
        if (entries.isEmpty()) {
            entityinfo$sidebarScrollOffset = 0;
            return false;
        }

        SidebarLayout layout = entityinfo$getSidebarLayout(entries, graphics.guiWidth(), graphics.guiHeight());
        if (!layout.hasVisibleColumns()) {
            entityinfo$sidebarScrollOffset = 0;
            return false;
        }

        boolean insideSidebar = entityinfo$isInsideSidebar(mouseX, mouseY, layout);
        HoverState hoverState = entityinfo$getSidebarHoverState(layout, mouseX, mouseY);
        for (PositionedEntry positionedEntry : layout.positionedEntries()) {
            entityinfo$renderSidebarEntry(graphics, font, positionedEntry, mouseX, mouseY);
        }

        entityinfo$renderOverflowHints(graphics, font, layout);

        if (hoverState == null) {
            // Consume sidebar hover space so empty sidebar slots/body do not leak
            // through to the underlying container tooltip path.
            return insideSidebar;
        }

        if (hoverState.emptySlot()) {
            return true;
        }

        if (!hoverState.item().isEmpty()) {
            if (ShulkerViewer.INSTANCE.shouldRenderTooltip() && ShulkerViewer.INSTANCE.canPreviewNested(hoverState.item())) {
                List<ItemStack> nestedContents = ShulkerViewer.INSTANCE.getPreviewContents(hoverState.item());
                ShulkerPreviewScreen.renderPreviewTooltip(
                    graphics,
                    font,
                    hoverState.item(),
                    nestedContents,
                    mouseX,
                    mouseY,
                    ShulkerViewer.INSTANCE.shouldDrawEmptySlots(),
                    ShulkerViewer.INSTANCE.shouldShowNestedPreviewHint(nestedContents)
                );
                return true;
            }

            graphics.setTooltipForNextFrame(font, hoverState.item(), mouseX, mouseY);
            return true;
        }

        if (ShulkerViewer.INSTANCE.shouldRenderTooltip()) {
            ShulkerPreviewScreen.renderPreviewTooltip(
                graphics,
                font,
                hoverState.entry().stack(),
                hoverState.entry().contents(),
                mouseX,
                mouseY,
                ShulkerViewer.INSTANCE.shouldDrawEmptySlots(),
                ShulkerViewer.INSTANCE.shouldShowNestedPreviewHint(hoverState.entry().contents())
            );
            return true;
        }

        return true;
    }

    private void entityinfo$renderSidebarEntry(GuiGraphicsExtractor graphics, Font font, PositionedEntry positionedEntry, int mouseX, int mouseY) {
        int x = positionedEntry.x();
        int y = positionedEntry.y();
        int height = positionedEntry.height();
        ShulkerViewer.ContainerPreviewEntry entry = positionedEntry.entry();
        boolean hoveredEntry = entityinfo$isInside(mouseX, mouseY, x, y, SIDEBAR_ENTRY_WIDTH, height);
        int accentColor = entry.accentColor();
        int titleColor = ShulkerViewer.INSTANCE.getPreviewHeaderTextColor(accentColor);
        int outlineColor = hoveredEntry ? SIDEBAR_HOVER_OUTLINE : SIDEBAR_OUTLINE;
        boolean drawBackground = ShulkerViewer.INSTANCE.shouldDrawSidebarBackground();

        if (drawBackground) {
            graphics.fill(x, y, x + SIDEBAR_ENTRY_WIDTH, y + height, hoveredEntry ? SIDEBAR_BODY_BACKGROUND_HOVER : SIDEBAR_BODY_BACKGROUND);
        }
        graphics.fill(x, y, x + SIDEBAR_ENTRY_WIDTH, y + SIDEBAR_HEADER_HEIGHT, accentColor);
        if (drawBackground) {
            graphics.fill(x + 1, y + SIDEBAR_HEADER_HEIGHT, x + SIDEBAR_ENTRY_WIDTH - 1, y + SIDEBAR_HEADER_HEIGHT + 1, SIDEBAR_DIVIDER);
            graphics.outline(x, y, SIDEBAR_ENTRY_WIDTH, height, outlineColor);
        }

        String titleText = font.plainSubstrByWidth(entry.stack().getHoverName().getString(), SIDEBAR_ENTRY_WIDTH - 8);
        graphics.text(font, titleText, x + 4, y + 2, titleColor, false);
        entityinfo$renderGridEntry(graphics, font, entry, entry.sidebarContents(), x, y, mouseX, mouseY);
    }

    private void entityinfo$renderGridEntry(
        GuiGraphicsExtractor graphics,
        Font font,
        ShulkerViewer.ContainerPreviewEntry entry,
        List<ItemStack> sidebarContents,
        int x,
        int y,
        int mouseX,
        int mouseY
    ) {
        int gridX = entityinfo$getGridX(x, entry.compactSidebar());
        int gridY = y + SIDEBAR_HEADER_HEIGHT + SIDEBAR_GRID_TOP_GAP;
        int slotCount = entry.compactSidebar() ? 1 : entry.visibleRows() * SIDEBAR_COLUMNS;
        int slotHoverOutline = entityinfo$outlineFromAccent(entry.accentColor());
        boolean drawSlotFrames = ShulkerViewer.INSTANCE.shouldDrawSidebarSlotFrames();
        boolean drawEmptySlots = ShulkerViewer.INSTANCE.shouldDrawEmptySlots();

        for (int index = 0; index < slotCount; index++) {
            int slotX = gridX + (index % SIDEBAR_COLUMNS) * (SIDEBAR_SLOT_SIZE + SIDEBAR_SLOT_GAP);
            int slotY = gridY + (index / SIDEBAR_COLUMNS) * (SIDEBAR_SLOT_SIZE + SIDEBAR_SLOT_GAP);
            boolean hoveredSlot = entityinfo$isInside(mouseX, mouseY, slotX, slotY, SIDEBAR_SLOT_SIZE, SIDEBAR_SLOT_SIZE);
            ItemStack sidebarItem = index < sidebarContents.size() ? sidebarContents.get(index) : ItemStack.EMPTY;

            if (drawSlotFrames && (!sidebarItem.isEmpty() || drawEmptySlots)) {
                graphics.fill(slotX, slotY, slotX + SIDEBAR_SLOT_SIZE, slotY + SIDEBAR_SLOT_SIZE, SIDEBAR_ROW_BACKGROUND);
                graphics.outline(slotX, slotY, SIDEBAR_SLOT_SIZE, SIDEBAR_SLOT_SIZE, hoveredSlot ? slotHoverOutline : SIDEBAR_SLOT_OUTLINE);
                if (hoveredSlot) {
                    graphics.fill(slotX + 1, slotY + 1, slotX + SIDEBAR_SLOT_SIZE - 1, slotY + SIDEBAR_SLOT_SIZE - 1, SIDEBAR_SLOT_HOVER);
                }
            }

            if (!sidebarItem.isEmpty()) {
                graphics.item(sidebarItem, slotX + 1, slotY + 1);
                graphics.itemDecorations(font, sidebarItem, slotX + 1, slotY + 1);
            }
        }
    }

    private SidebarLayout entityinfo$getSidebarLayout(List<ShulkerViewer.ContainerPreviewEntry> entries, int guiWidth, int guiHeight) {
        List<Integer> baseColumns = entityinfo$getBaseColumnPositions(guiWidth);
        if (baseColumns.isEmpty()) {
            return SidebarLayout.empty();
        }

        List<Integer> extendedColumns = entityinfo$getExpandedColumnPositions(guiWidth, baseColumns);
        List<Integer> effectiveColumns = baseColumns;

        // Keep the column count stable across scroll positions so the sidebar
        // does not collapse back to two columns on the last page.
        if (extendedColumns.size() > baseColumns.size()
            && entityinfo$layoutEntries(entries, baseColumns, guiHeight, 0).hasOverflowBelow()) {
            effectiveColumns = extendedColumns;
        }

        int maxScrollOffset = entityinfo$getMaxSidebarScrollOffset(entries, effectiveColumns, guiHeight);
        entityinfo$sidebarScrollOffset = Math.max(0, Math.min(entityinfo$sidebarScrollOffset, maxScrollOffset));
        return entityinfo$layoutEntries(entries, effectiveColumns, guiHeight, entityinfo$sidebarScrollOffset);
    }

    private List<Integer> entityinfo$getBaseColumnPositions(int guiWidth) {
        List<Integer> columns = new ArrayList<>(2);
        int offsetX = ShulkerViewer.INSTANCE.getSidebarOffsetX();
        int leftEdgeX = offsetX;
        int rightEdgeX = guiWidth - offsetX - SIDEBAR_ENTRY_WIDTH;
        if (leftEdgeX + SIDEBAR_ENTRY_WIDTH > guiWidth - offsetX) {
            return columns;
        }

        columns.add(leftEdgeX);
        if (leftEdgeX + SIDEBAR_ENTRY_WIDTH <= rightEdgeX) {
            columns.add(rightEdgeX);
        }
        return columns;
    }

    private List<Integer> entityinfo$getExpandedColumnPositions(int guiWidth, List<Integer> baseColumns) {
        List<Integer> columns = new ArrayList<>(baseColumns);
        if (columns.size() >= SIDEBAR_MAX_COLUMNS) {
            return columns;
        }

        int offsetX = ShulkerViewer.INSTANCE.getSidebarOffsetX();
        int nextLeftX = offsetX + SIDEBAR_ENTRY_WIDTH + SIDEBAR_COLUMN_GAP;
        int nextRightX = guiWidth - offsetX - SIDEBAR_ENTRY_WIDTH * 2 - SIDEBAR_COLUMN_GAP;

        while (columns.size() < SIDEBAR_MAX_COLUMNS && nextLeftX + SIDEBAR_ENTRY_WIDTH <= nextRightX) {
            columns.add(nextLeftX);
            if (columns.size() >= SIDEBAR_MAX_COLUMNS) {
                break;
            }

            if (!columns.contains(nextRightX)) {
                columns.add(nextRightX);
            }

            nextLeftX += SIDEBAR_ENTRY_WIDTH + SIDEBAR_COLUMN_GAP;
            nextRightX -= SIDEBAR_ENTRY_WIDTH + SIDEBAR_COLUMN_GAP;
        }

        return columns;
    }

    private int entityinfo$getMaxSidebarScrollOffset(List<ShulkerViewer.ContainerPreviewEntry> entries, List<Integer> columnPositions, int guiHeight) {
        if (entries.isEmpty() || columnPositions.isEmpty()) {
            return 0;
        }

        int maxOffset = Math.max(0, entries.size() - 1);
        for (int startIndex = 0; startIndex <= maxOffset; startIndex++) {
            if (!entityinfo$layoutEntries(entries, columnPositions, guiHeight, startIndex).hasOverflowBelow()) {
                return startIndex;
            }
        }

        return maxOffset;
    }

    private SidebarLayout entityinfo$layoutEntries(
        List<ShulkerViewer.ContainerPreviewEntry> entries,
        List<Integer> columnPositions,
        int guiHeight,
        int startIndex
    ) {
        int topY = ShulkerViewer.INSTANCE.getSidebarOffsetY();
        int bottomY = guiHeight - 6;

        List<ColumnCursor> cursors = new ArrayList<>(columnPositions.size());
        for (int columnX : columnPositions) {
            cursors.add(new ColumnCursor(columnX, topY));
        }

        List<PositionedEntry> positionedEntries = new ArrayList<>();
        for (int index = startIndex; index < entries.size(); index++) {
            ShulkerViewer.ContainerPreviewEntry entry = entries.get(index);
            int entryHeight = entityinfo$getEntryHeight(entry);
            ColumnCursor targetColumn = entityinfo$getTargetColumn(cursors, entryHeight, bottomY);
            if (targetColumn == null) {
                break;
            }

            positionedEntries.add(new PositionedEntry(entry, targetColumn.x(), targetColumn.currentY(), SIDEBAR_ENTRY_WIDTH, entryHeight));
            targetColumn.advance(entryHeight + SIDEBAR_ENTRY_GAP);
        }

        boolean overflowBelow = startIndex + positionedEntries.size() < entries.size();
        return new SidebarLayout(positionedEntries, columnPositions, startIndex > 0, overflowBelow);
    }

    private static int entityinfo$getEntryHeight(ShulkerViewer.ContainerPreviewEntry entry) {
        if (entry.compactSidebar()) {
            return SIDEBAR_HEADER_HEIGHT
                + SIDEBAR_GRID_TOP_GAP
                + SIDEBAR_SLOT_SIZE
                + SIDEBAR_PADDING + 1;
        }

        return SIDEBAR_HEADER_HEIGHT
            + SIDEBAR_GRID_TOP_GAP
            + entry.visibleRows() * SIDEBAR_SLOT_SIZE
            + Math.max(0, entry.visibleRows() - 1) * SIDEBAR_SLOT_GAP
            + SIDEBAR_PADDING + 1;
    }

    private static ColumnCursor entityinfo$getTargetColumn(List<ColumnCursor> cursors, int entryHeight, int bottomY) {
        ColumnCursor target = null;
        for (ColumnCursor cursor : cursors) {
            if (cursor.currentY() + entryHeight > bottomY) {
                continue;
            }

            if (target == null || cursor.currentY() < target.currentY()) {
                target = cursor;
            }
        }
        return target;
    }

    private void entityinfo$renderOverflowHints(GuiGraphicsExtractor graphics, Font font, SidebarLayout layout) {
        if (layout.columnPositions().isEmpty()) {
            return;
        }

        int leftMostX = layout.leftMostX();
        int rightMostX = layout.rightMostX();
        if (layout.hasOverflowAbove()) {
            int topHintY = Math.max(0, ShulkerViewer.INSTANCE.getSidebarOffsetY() - 10);
            entityinfo$renderOverflowHint(graphics, font, leftMostX, topHintY, true);
            if (rightMostX != leftMostX) {
                entityinfo$renderOverflowHint(graphics, font, rightMostX, topHintY, true);
            }
        }

        if (layout.hasOverflowBelow()) {
            entityinfo$renderOverflowHint(graphics, font, leftMostX, graphics.guiHeight() - 10, false);
            if (rightMostX != leftMostX) {
                entityinfo$renderOverflowHint(graphics, font, rightMostX, graphics.guiHeight() - 10, false);
            }
        }
    }

    private HoverState entityinfo$getSidebarHoverState(double mouseX, double mouseY) {
        List<ShulkerViewer.ContainerPreviewEntry> entries = ShulkerViewer.INSTANCE.collectContainerPreviewEntries(menu);
        if (entries.isEmpty()) {
            return null;
        }

        int guiWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        SidebarLayout layout = entityinfo$getSidebarLayout(entries, guiWidth, guiHeight);
        return entityinfo$getSidebarHoverState(layout, (int) mouseX, (int) mouseY);
    }

    private HoverState entityinfo$getSidebarHoverState(SidebarLayout layout, int mouseX, int mouseY) {
        for (PositionedEntry positionedEntry : layout.positionedEntries()) {
            if (!entityinfo$isInside(mouseX, mouseY, positionedEntry.x(), positionedEntry.y(), positionedEntry.width(), positionedEntry.height())) {
                continue;
            }

            return entityinfo$resolveHoverState(positionedEntry, mouseX, mouseY);
        }
        return null;
    }

    private HoverState entityinfo$resolveHoverState(PositionedEntry positionedEntry, int mouseX, int mouseY) {
        ShulkerViewer.ContainerPreviewEntry entry = positionedEntry.entry();
        int x = positionedEntry.x();
        int y = positionedEntry.y();
        int gridX = entityinfo$getGridX(x, entry.compactSidebar());
        int gridY = y + SIDEBAR_HEADER_HEIGHT + SIDEBAR_GRID_TOP_GAP;
        List<ItemStack> sidebarContents = entry.sidebarContents();
        int slotCount = entry.compactSidebar() ? 1 : entry.visibleRows() * SIDEBAR_COLUMNS;
        for (int index = 0; index < slotCount; index++) {
            int slotX = gridX + (index % SIDEBAR_COLUMNS) * (SIDEBAR_SLOT_SIZE + SIDEBAR_SLOT_GAP);
            int slotY = gridY + (index / SIDEBAR_COLUMNS) * (SIDEBAR_SLOT_SIZE + SIDEBAR_SLOT_GAP);
            if (entityinfo$isInside(mouseX, mouseY, slotX, slotY, SIDEBAR_SLOT_SIZE, SIDEBAR_SLOT_SIZE)) {
                ItemStack hoveredStack = index < sidebarContents.size() ? sidebarContents.get(index) : ItemStack.EMPTY;
                return hoveredStack.isEmpty()
                    ? new HoverState(entry, ItemStack.EMPTY, true)
                    : new HoverState(entry, hoveredStack, false);
            }
        }

        return new HoverState(entry, ItemStack.EMPTY, false);
    }

    private boolean entityinfo$isInsideSidebar(double mouseX, double mouseY, SidebarLayout layout) {
        for (PositionedEntry positionedEntry : layout.positionedEntries()) {
            if (entityinfo$isInside(mouseX, mouseY, positionedEntry.x(), positionedEntry.y(), positionedEntry.width(), positionedEntry.height())) {
                return true;
            }
        }
        return false;
    }
    private static void entityinfo$renderOverflowHint(GuiGraphicsExtractor graphics, Font font, int x, int y, boolean above) {
        String text = above ? "^" : "v";
        graphics.text(font, text, x + SIDEBAR_ENTRY_WIDTH - 4 - font.width(text), y, SIDEBAR_HINT, false);
    }

    private static boolean entityinfo$isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int entityinfo$getGridX(int x, boolean compactSidebar) {
        if (compactSidebar) {
            return x + SIDEBAR_PADDING;
        }

        return x + SIDEBAR_PADDING;
    }

    private static int entityinfo$outlineFromAccent(int accentColor) {
        int red = Math.min(255, ((accentColor >> 16) & 255) + 24);
        int green = Math.min(255, ((accentColor >> 8) & 255) + 18);
        int blue = Math.min(255, (accentColor & 255) + 28);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    @Unique
    private boolean entityinfo$isCreativeInventoryScreen() {
        return (Object) this instanceof CreativeModeInventoryScreen;
    }

    private record PositionedEntry(ShulkerViewer.ContainerPreviewEntry entry, int x, int y, int width, int height) {
    }

    private record HoverState(ShulkerViewer.ContainerPreviewEntry entry, ItemStack item, boolean emptySlot) {
    }

    private record SidebarLayout(
        List<PositionedEntry> positionedEntries,
        List<Integer> columnPositions,
        boolean hasOverflowAbove,
        boolean hasOverflowBelow
    ) {
        private static SidebarLayout empty() {
            return new SidebarLayout(List.of(), List.of(), false, false);
        }

        private boolean hasVisibleColumns() {
            return !columnPositions.isEmpty();
        }

        private int leftMostX() {
            int leftMost = columnPositions.getFirst();
            for (int columnX : columnPositions) {
                leftMost = Math.min(leftMost, columnX);
            }
            return leftMost;
        }

        private int rightMostX() {
            int rightMost = columnPositions.getFirst();
            for (int columnX : columnPositions) {
                rightMost = Math.max(rightMost, columnX);
            }
            return rightMost;
        }
    }

    private static final class ColumnCursor {
        private final int x;
        private int currentY;

        private ColumnCursor(int x, int currentY) {
            this.x = x;
            this.currentY = currentY;
        }

        private int x() {
            return x;
        }

        private int currentY() {
            return currentY;
        }

        private void advance(int amount) {
            this.currentY += amount;
        }
    }
}
