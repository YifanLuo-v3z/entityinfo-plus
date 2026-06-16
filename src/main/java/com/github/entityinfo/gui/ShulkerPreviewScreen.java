package com.github.entityinfo.gui;

import com.github.entityinfo.modules.ShulkerViewer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class ShulkerPreviewScreen extends Screen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 0;
    private static final int PANEL_PADDING = 3;
    private static final int TITLE_HEIGHT = 11;
    private static final int TITLE_GAP = 2;
    private static final int FOOTER_GAP = 3;
    private static final int GRID_WIDTH = COLUMNS * SLOT_SIZE;
    private static final int GRID_HEIGHT = ROWS * SLOT_SIZE;
    private static final int PANEL_WIDTH = PANEL_PADDING * 2 + GRID_WIDTH;
    private static final int PANEL_HEIGHT = PANEL_PADDING * 2 + TITLE_HEIGHT + TITLE_GAP + GRID_HEIGHT + FOOTER_GAP;
    private static final int PANEL_BACKGROUND = 0xF1050505;
    private static final int PANEL_OUTLINE = 0xCC000000;
    private static final int PANEL_DIVIDER = 0x55000000;
    private static final int SLOT_BACKGROUND = 0xFF080808;
    private static final int SLOT_OUTLINE = 0xFF202020;
    private static final int SLOT_HOVER = 0x14FFFFFF;

    private final Screen parentScreen;
    private final ItemStack sourceStack;
    private final List<ItemStack> contents;
    private final boolean drawEmptySlots;
    private final boolean showNestedHint;

    public ShulkerPreviewScreen(
        Screen parentScreen,
        ItemStack sourceStack,
        List<ItemStack> contents,
        boolean drawEmptySlots,
        boolean showNestedHint
    ) {
        super(sourceStack.getHoverName());
        this.parentScreen = parentScreen;
        this.sourceStack = sourceStack;
        this.contents = contents;
        this.drawEmptySlots = drawEmptySlots;
        this.showNestedHint = showNestedHint;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        extractTransparentBackground(graphics);
        renderWindow(graphics, panelX, panelY, sourceStack, contents, drawEmptySlots, showNestedHint, this.font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean allowOutsideClick) {
        if (event.button() != 2) {
            return super.mouseClicked(event, allowOutsideClick);
        }

        if (!ShulkerViewer.INSTANCE.shouldOpenOnMiddleClick()) {
            return super.mouseClicked(event, allowOutsideClick);
        }

        HoveredSlot hoveredSlot = getHoveredSlot(event.x(), event.y());
        if (hoveredSlot == null || hoveredSlot.stack().isEmpty()) {
            return super.mouseClicked(event, allowOutsideClick);
        }

        if (!ShulkerViewer.INSTANCE.canPreviewNested(hoveredSlot.stack())) {
            return super.mouseClicked(event, allowOutsideClick);
        }

        ShulkerViewer.INSTANCE.openPreview(hoveredSlot.stack(), this);
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void renderPreviewTooltip(
        GuiGraphicsExtractor graphics,
        Font font,
        ItemStack stack,
        List<ItemStack> contents,
        int mouseX,
        int mouseY,
        boolean drawEmptySlots,
        boolean showNestedHint
    ) {
        int x = Math.max(8, Math.min(mouseX + 14, graphics.guiWidth() - PANEL_WIDTH - 8));
        int y = mouseY - PANEL_HEIGHT - 10;
        if (y < 8) {
            y = Math.max(8, Math.min(mouseY + 14, graphics.guiHeight() - PANEL_HEIGHT - 8));
        }

        renderWindow(graphics, x, y, stack, contents, drawEmptySlots, showNestedHint, font, mouseX, mouseY);
    }

    private static void renderWindow(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        ItemStack sourceStack,
        List<ItemStack> contents,
        boolean drawEmptySlots,
        boolean showNestedHint,
        Font font,
        int mouseX,
        int mouseY
    ) {
        Component title = sourceStack.getHoverName();
        int accentColor = ShulkerViewer.INSTANCE.getPreviewAccentColor(sourceStack);
        int titleColor = ShulkerViewer.INSTANCE.getPreviewHeaderTextColor(sourceStack);
        int hintColor = ShulkerViewer.INSTANCE.getPreviewHeaderHintColor(sourceStack);
        int hoverOutline = outlineFromAccent(accentColor);
        boolean drawBackground = ShulkerViewer.INSTANCE.shouldDrawPreviewBackground();
        boolean drawSlotFrames = ShulkerViewer.INSTANCE.shouldDrawPreviewSlotFrames();
        if (drawBackground) {
            graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, PANEL_BACKGROUND);
            graphics.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_OUTLINE);
        }
        graphics.fill(x, y, x + PANEL_WIDTH, y + TITLE_HEIGHT, accentColor);
        if (drawBackground) {
            graphics.fill(x + 1, y + TITLE_HEIGHT, x + PANEL_WIDTH - 1, y + TITLE_HEIGHT + 1, PANEL_DIVIDER);
        }
        int hintWidth = 0;
        if (showNestedHint) {
            Component hint = Component.translatable("entityinfo_addon.hints.middle_click_nested");
            hintWidth = font.width(hint);
            graphics.text(font, hint, x + PANEL_WIDTH - 4 - hintWidth, y + 2, hintColor, false);
        }
        String titleText = font.plainSubstrByWidth(title.getString(), PANEL_WIDTH - 8 - hintWidth - (hintWidth > 0 ? 4 : 0));
        graphics.text(font, titleText, x + 4, y + 2, titleColor, false);

        HoveredSlot hoveredSlot = null;
        int gridX = x + PANEL_PADDING;
        int gridY = y + PANEL_PADDING + TITLE_HEIGHT + TITLE_GAP;
        for (int slot = 0; slot < ROWS * COLUMNS; slot++) {
            int slotX = gridX + (slot % COLUMNS) * (SLOT_SIZE + SLOT_GAP);
            int slotY = gridY + (slot / COLUMNS) * (SLOT_SIZE + SLOT_GAP);
            ItemStack slotStack = slot < contents.size() ? contents.get(slot) : ItemStack.EMPTY;
            boolean hovered = isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            if (drawSlotFrames && (!slotStack.isEmpty() || drawEmptySlots)) {
                drawSlotBackground(graphics, slotX, slotY, hovered, hoverOutline);
            }

            if (!slotStack.isEmpty()) {
                graphics.item(slotStack, slotX + 1, slotY + 1);
                graphics.itemDecorations(font, slotStack, slotX + 1, slotY + 1);
            }

            if (hovered && !slotStack.isEmpty()) {
                hoveredSlot = new HoveredSlot(slot, slotX, slotY, slotStack);
            }
        }

        if (hoveredSlot != null) {
            graphics.setTooltipForNextFrame(font, hoveredSlot.stack(), mouseX, mouseY);
        }
    }

    private HoveredSlot getHoveredSlot(double mouseX, double mouseY) {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int gridX = panelX + PANEL_PADDING;
        int gridY = panelY + PANEL_PADDING + TITLE_HEIGHT + TITLE_GAP;
        for (int slot = 0; slot < ROWS * COLUMNS; slot++) {
            ItemStack stack = slot < contents.size() ? contents.get(slot) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                continue;
            }

            int slotX = gridX + (slot % COLUMNS) * (SLOT_SIZE + SLOT_GAP);
            int slotY = gridY + (slot / COLUMNS) * (SLOT_SIZE + SLOT_GAP);
            if (isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                return new HoveredSlot(slot, slotX, slotY, stack);
            }
        }

        return null;
    }

    private static void drawSlotBackground(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, int hoverOutline) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND);
        graphics.outline(x, y, SLOT_SIZE, SLOT_SIZE, hovered ? hoverOutline : SLOT_OUTLINE);
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_HOVER);
        }
    }

    private static int outlineFromAccent(int accentColor) {
        int red = Math.min(255, ((accentColor >> 16) & 255) + 24);
        int green = Math.min(255, ((accentColor >> 8) & 255) + 18);
        int blue = Math.min(255, (accentColor & 255) + 28);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record HoveredSlot(int index, int x, int y, ItemStack stack) {
    }
}
