package com.github.entityinfo.modules;

import com.github.entityinfo.utils.render.EntityRenderUtils;
import com.github.entityinfo.utils.render.EntityRenderUtils.AggregatedDroppedItemEntry;
import com.github.entityinfo.utils.render.EntityRenderUtils.DroppedItemEntry;
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
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.google.common.base.Suppliers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

@Environment(EnvType.CLIENT)
public final class DroppedItemHUD extends HudModule {

    public static final DroppedItemHUD INSTANCE = new DroppedItemHUD();

    private static final float ICON_SIZE = 16.0F;
    private static final float CONTENT_GAP = 4.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_Y = 5.0F;

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgDisplay = settingGroup("Display");
    private final SettingGroup sgAppearance = settingGroup("Appearance");
    private final SettingGroup sgEffect = settingGroup("Effect");
    private final SettingGroup sgPerformance = settingGroup("Performance");

    private final DoubleSetting maxDistance = doubleSetting("Max Distance", 24.0D, 4.0D, 128.0D, 1.0D).group(sgGeneral);
    private final IntSetting maxEntries = intSetting("Max Entries", 8, 1, 64, 1).group(sgGeneral);
    private final EnumSetting<SortMode> sortMode = enumSetting("Sort Mode", SortMode.DistanceAsc).group(sgGeneral);
    private final BoolSetting showType = boolSetting("Show Type", false).group(sgDisplay);
    private final BoolSetting showDistance = boolSetting("Show Distance", true).group(sgDisplay);
    private final BoolSetting textShadow = boolSetting("Text Shadow", true).group(sgDisplay);
    private final BoolSetting showIcons = boolSetting("Show Icons", true).group(sgDisplay);
    private final ColorSetting textColor = colorSetting("Text Color", Color.WHITE, true).group(sgDisplay);
    private final BoolSetting renderInScreens = boolSetting("Render In Screens", false).group(sgDisplay);
    private final DoubleSetting scale = doubleSetting("Scale", 1.0D, 0.5D, 2.0D, 0.1D).group(sgAppearance);
    private final DoubleSetting cornerRadius = doubleSetting("Corner Radius", 4.0D, 0.0D, 14.0D, 0.5D).group(sgAppearance);
    private final BoolSetting showBackground = boolSetting("Background", true).group(sgAppearance);
    private final ColorSetting backgroundColor = colorSetting("Background Color", new Color(15, 15, 15, 135), showBackground::getValue).group(sgAppearance);
    private final BoolSetting drawShadow = boolSetting("Drop Shadow", true, showBackground::getValue).group(sgEffect);
    private final DoubleSetting shadowBlur = doubleSetting("Shadow Blur", 2.2D, 0.1D, 32.0D, 0.5D, drawShadow::getValue).group(sgEffect);
    private final ColorSetting shadowColor = colorSetting("Shadow Color", new Color(0, 0, 0, 70), drawShadow::getValue).group(sgEffect);
    private final BoolSetting backgroundBlur = boolSetting("Background Blur", true, showBackground::getValue).group(sgEffect);
    private final IntSetting blurStrength = intSetting("Blur Strength", 5, 1, 16, 1, backgroundBlur::getValue).group(sgEffect);
    private final IntSetting refreshDelay = intSetting("Refresh Delay", 100, 0, 1000, 10).group(sgPerformance);

    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::create);
    private final Supplier<ShadowRenderer> shadowRendererSupplier = Suppliers.memoize(ShadowRenderer::create);
    private final List<CachedEntry> cachedEntries = new ArrayList<>();
    private float cachedWidth;
    private float cachedHeight;
    private long lastRefreshTime;
    private CacheState lastCacheState;

    private DroppedItemHUD() {
        super("Dropped Item HUD", Category.HUD, 8.0F, 44.0F);
    }

    @Override
    protected void onEnable() {
        clearCache();
    }

    @Override
    protected void onDisable() {
        clearCache();
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (nullCheck() || shouldSkipScreenRender()) {
            return;
        }

        refreshCacheIfNeeded();
        if (cachedEntries.isEmpty()) {
            setBounds(0.0F, 0.0F);
            return;
        }

        float renderScale = scale.getValue().floatValue();
        float radius = cornerRadius.getValue().floatValue() * renderScale;
        float rowHeight = entityinfo$getRowHeight(renderScale);
        float rowGap = ROW_GAP * renderScale;
        float iconSize = showIcons.getValue() ? ICON_SIZE * renderScale : 0.0F;
        float contentGap = showIcons.getValue() ? CONTENT_GAP * renderScale : 0.0F;
        float paddingX = showBackground.getValue() ? PADDING_X * renderScale : 0.0F;
        float paddingY = showBackground.getValue() ? PADDING_Y * renderScale : 0.0F;

        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();
        ShadowRenderer shadowRenderer = shadowRendererSupplier.get();

        if (showBackground.getValue() && backgroundBlur.getValue()) {
            BlurShader.INSTANCE.render(this.x, this.y, cachedWidth, cachedHeight, radius, blurStrength.getValue());
        }

        if (showBackground.getValue() && drawShadow.getValue()) {
            shadowRenderer.addShadow(
                this.x,
                this.y,
                cachedWidth,
                cachedHeight,
                radius,
                shadowBlur.getValue().floatValue(),
                shadowColor.getValue()
            );
            shadowRenderer.drawAndClear();
        }

        if (showBackground.getValue()) {
            roundRectRenderer.addRoundRect(this.x, this.y, cachedWidth, cachedHeight, radius, backgroundColor.getValue());
            roundRectRenderer.drawAndClear();
        }

        for (int index = 0; index < cachedEntries.size(); index++) {
            CachedEntry entry = cachedEntries.get(index);
            float rowY = this.y + paddingY + index * (rowHeight + rowGap);

            if (showIcons.getValue()) {
                float iconY = rowY + (rowHeight - iconSize) * 0.5F;
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(this.x + paddingX, iconY);
                guiGraphics.pose().scale(renderScale, renderScale);
                guiGraphics.item(entry.previewStack(), 0, 0);
                guiGraphics.pose().popMatrix();
            }

            float textX = this.x + paddingX + iconSize + contentGap;
            float textY = rowY + Math.max(0.0F, (rowHeight - mc.font.lineHeight * renderScale) * 0.5F);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(textX, textY);
            guiGraphics.pose().scale(renderScale, renderScale);
            guiGraphics.text(mc.font, entry.line(), 0, 0, textColor.getValue().getRGB(), textShadow.getValue());
            guiGraphics.pose().popMatrix();
        }

        setBounds(cachedWidth, cachedHeight);
    }

    private void refreshCacheIfNeeded() {
        CacheState cacheState = new CacheState(
            maxDistance.getValue(),
            maxEntries.getValue(),
            sortMode.getValue(),
            showType.getValue(),
            showDistance.getValue(),
            showIcons.getValue(),
            scale.getValue(),
            showBackground.getValue()
        );
        long now = System.currentTimeMillis();
        int delay = refreshDelay.getValue();
        if (cacheState.equals(lastCacheState) && delay > 0 && now - lastRefreshTime < delay) {
            return;
        }

        lastRefreshTime = now;
        lastCacheState = cacheState;
        cachedEntries.clear();
        cachedWidth = 0.0F;
        cachedHeight = 0.0F;

        List<DroppedItemEntry> entries = EntityRenderUtils.collectDroppedItemEntries(
            mc,
            maxDistance.getValue(),
            Comparator.comparingDouble(DroppedItemEntry::distance)
        );

        if (entries.isEmpty()) {
            return;
        }

        List<AggregatedDroppedItemEntry> aggregatedEntries = EntityRenderUtils.aggregateDroppedItemEntries(entries, createComparator());

        if (aggregatedEntries.isEmpty()) {
            return;
        }

        int renderCount = Math.min(maxEntries.getValue(), aggregatedEntries.size());
        float renderScale = scale.getValue().floatValue();
        float rowHeight = entityinfo$getRowHeight(renderScale);
        float rowGap = ROW_GAP * renderScale;
        float iconWidth = showIcons.getValue() ? ICON_SIZE * renderScale + CONTENT_GAP * renderScale : 0.0F;
        float paddingX = showBackground.getValue() ? PADDING_X * renderScale : 0.0F;
        float paddingY = showBackground.getValue() ? PADDING_Y * renderScale : 0.0F;

        for (int index = 0; index < renderCount; index++) {
            AggregatedDroppedItemEntry entry = aggregatedEntries.get(index);
            String line = EntityRenderUtils.buildDroppedItemHudLine(entry, showType.getValue(), showDistance.getValue());
            cachedEntries.add(new CachedEntry(line, entry.previewStack()));
            cachedWidth = Math.max(cachedWidth, iconWidth + mc.font.width(line) * renderScale);
        }

        if (renderCount <= 0) {
            return;
        }

        cachedWidth += paddingX * 2.0F;
        cachedHeight = paddingY * 2.0F + renderCount * rowHeight + Math.max(0, renderCount - 1) * rowGap;
    }

    private void clearCache() {
        cachedEntries.clear();
        cachedWidth = 0.0F;
        cachedHeight = 0.0F;
        lastRefreshTime = 0L;
        lastCacheState = null;
    }

    private Comparator<AggregatedDroppedItemEntry> createComparator() {
        Comparator<AggregatedDroppedItemEntry> comparator = switch (sortMode.getValue()) {
            case DistanceAsc -> Comparator.comparingDouble(AggregatedDroppedItemEntry::distance);
            case DistanceDesc -> Comparator.comparingDouble(AggregatedDroppedItemEntry::distance).reversed();
            case NameAsc -> Comparator.comparing(AggregatedDroppedItemEntry::displayName, String.CASE_INSENSITIVE_ORDER);
            case NameDesc -> Comparator.comparing(AggregatedDroppedItemEntry::displayName, String.CASE_INSENSITIVE_ORDER).reversed();
            case CountAsc -> Comparator.comparingInt(AggregatedDroppedItemEntry::count);
            case CountDesc -> Comparator.comparingInt(AggregatedDroppedItemEntry::count).reversed();
        };

        return comparator
            .thenComparing(AggregatedDroppedItemEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AggregatedDroppedItemEntry::distance)
            .thenComparingInt(AggregatedDroppedItemEntry::mergeKey);
    }

    private boolean shouldSkipScreenRender() {
        return mc.screen instanceof CreativeModeInventoryScreen
            || !renderInScreens.getValue() && mc.screen != null && !(mc.screen instanceof HudEditorScreen);
    }

    private float entityinfo$getRowHeight(float renderScale) {
        float textHeight = mc.font.lineHeight * renderScale;
        float iconHeight = showIcons.getValue() ? ICON_SIZE * renderScale : 0.0F;
        return Math.max(textHeight, iconHeight);
    }

    @Environment(EnvType.CLIENT)
    private record CachedEntry(String line, net.minecraft.world.item.ItemStack previewStack) {
    }

    @Environment(EnvType.CLIENT)
    private record CacheState(
        Double maxDistance,
        Integer maxEntries,
        SortMode sortMode,
        Boolean showType,
        Boolean showDistance,
        Boolean showIcons,
        Double scale,
        Boolean showBackground
    ) {
    }

    @Environment(EnvType.CLIENT)
    private enum SortMode {
        DistanceAsc,
        DistanceDesc,
        NameAsc,
        NameDesc,
        CountAsc,
        CountDesc
    }
}
