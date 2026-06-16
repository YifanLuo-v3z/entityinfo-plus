package com.github.entityinfo.modules;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.shaders.BlurShader;
import com.github.epsilon.gui.hudeditor.HudEditorScreen;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.google.common.base.Suppliers;
import java.awt.Color;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class ArmorHudPlus extends HudModule {

    public static final ArmorHudPlus INSTANCE = new ArmorHudPlus();

    private static final float SLOT_SIZE = 17.0F;
    private static final float SLOT_GAP = 2.0F;
    private static final float PADDING = 4.5F;

    private final SettingGroup sgLayout = settingGroup("Layout");
    private final SettingGroup sgAppearance = settingGroup("Appearance");
    private final SettingGroup sgEffect = settingGroup("Effect");
    private final SettingGroup sgDisplay = settingGroup("Display");

    private final DoubleSetting scale = doubleSetting("Scale", 1.0D, 0.5D, 2.0D, 0.1D).group(sgLayout);
    private final DoubleSetting cornerRadius = doubleSetting("Corner Radius", 3.0D, 0.0D, 14.0D, 0.5D).group(sgLayout);
    private final BoolSetting showBackground = boolSetting("Background", true).group(sgAppearance);
    private final BoolSetting showSlots = boolSetting("Slots", true).group(sgAppearance);
    private final ColorSetting backgroundColor = colorSetting("Background Color", new Color(15, 15, 15, 135), showBackground::getValue).group(sgAppearance);
    private final ColorSetting slotColor = colorSetting("Slot Color", new Color(0, 0, 0, 70), showSlots::getValue).group(sgAppearance);
    private final BoolSetting drawShadow = boolSetting("Drop Shadow", true).group(sgEffect);
    private final DoubleSetting shadowBlur = doubleSetting("Shadow Blur", 2.2D, 0.1D, 32.0D, 0.5D, drawShadow::getValue).group(sgEffect);
    private final ColorSetting shadowColor = colorSetting("Shadow Color", new Color(0, 0, 0, 70), drawShadow::getValue).group(sgEffect);
    private final BoolSetting backgroundBlur = boolSetting("Background Blur", true, showBackground::getValue).group(sgEffect);
    private final IntSetting blurStrength = intSetting("Blur Strength", 5, 1, 16, 1, backgroundBlur::getValue).group(sgEffect);
    private final BoolSetting showDurability = boolSetting("Show Durability", true).group(sgDisplay);
    private final BoolSetting renderInScreens = boolSetting("Render In Screens", false).group(sgDisplay);

    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::create);
    private final Supplier<ShadowRenderer> shadowRendererSupplier = Suppliers.memoize(ShadowRenderer::create);

    private ArmorHudPlus() {
        super("Armor HUD+", Category.HUD, 8.0F, 72.0F, 96.0F, 32.0F);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (nullCheck() || shouldSkipScreenRender()) {
            return;
        }

        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();
        ShadowRenderer shadowRenderer = shadowRendererSupplier.get();

        float renderScale = scale.getValue().floatValue();
        float slotSize = SLOT_SIZE * renderScale;
        float gap = SLOT_GAP * renderScale;
        float padding = PADDING * renderScale;
        float radius = cornerRadius.getValue().floatValue() * renderScale;
        float slotRadius = 2.0F * renderScale;
        float totalWidth = padding * 2.0F + 4.0F * slotSize + 3.0F * gap;
        float totalHeight = padding * 2.0F + slotSize;

        if (showBackground.getValue() && backgroundBlur.getValue()) {
            BlurShader.INSTANCE.render(this.x, this.y, totalWidth, totalHeight, radius, blurStrength.getValue());
        }

        if (showBackground.getValue() && drawShadow.getValue()) {
            shadowRenderer.addShadow(
                this.x,
                this.y,
                totalWidth,
                totalHeight,
                radius,
                shadowBlur.getValue().floatValue(),
                shadowColor.getValue()
            );
        }

        if (showBackground.getValue()) {
            roundRectRenderer.addRoundRect(this.x, this.y, totalWidth, totalHeight, radius, backgroundColor.getValue());
        }

        if (showSlots.getValue()) {
            for (int index = 0; index < 4; index++) {
                float slotX = this.x + padding + index * (slotSize + gap);
                float slotY = this.y + padding;
                roundRectRenderer.addRoundRect(slotX, slotY, slotSize, slotSize, slotRadius, slotColor.getValue());
            }
        }

        if (showBackground.getValue() && drawShadow.getValue()) {
            shadowRenderer.drawAndClear();
        }

        if (showBackground.getValue() || showSlots.getValue()) {
            roundRectRenderer.drawAndClear();
        }

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int index = 0; index < slots.length; index++) {
            ItemStack stack = mc.player.getItemBySlot(slots[index]);
            if (stack.isEmpty()) {
                continue;
            }

            float slotX = this.x + padding + index * (slotSize + gap);
            float slotY = this.y + padding;

            graphics.pose().pushMatrix();
            graphics.pose().translate(slotX + renderScale, slotY + renderScale);
            graphics.pose().scale(renderScale, renderScale);
            graphics.item(stack, 0, 0);

            if (showDurability.getValue() && stack.isDamageableItem()) {
                int percent = Math.max(
                    0,
                    Math.round((stack.getMaxDamage() - stack.getDamageValue()) * 100.0F / stack.getMaxDamage())
                );
                graphics.itemDecorations(mc.font, stack, 0, 0, String.valueOf(percent));
            }

            graphics.pose().popMatrix();
        }

        setBounds(totalWidth, totalHeight);
    }

    private boolean shouldSkipScreenRender() {
        return mc.screen instanceof CreativeModeInventoryScreen
            || !renderInScreens.getValue() && mc.screen != null && !(mc.screen instanceof HudEditorScreen);
    }
}
