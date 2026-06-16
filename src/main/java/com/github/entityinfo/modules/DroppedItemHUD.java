package com.github.entityinfo.modules;

import com.github.entityinfo.utils.render.EntityRenderUtils;
import com.github.entityinfo.utils.render.EntityRenderUtils.AggregatedDroppedItemEntry;
import com.github.entityinfo.utils.render.EntityRenderUtils.DroppedItemEntry;
import com.github.epsilon.gui.hudeditor.HudEditorScreen;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

@Environment(EnvType.CLIENT)
public final class DroppedItemHUD extends HudModule {

    public static final DroppedItemHUD INSTANCE = new DroppedItemHUD();

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgDisplay = settingGroup("Display");
    private final SettingGroup sgPerformance = settingGroup("Performance");

    private final DoubleSetting maxDistance = doubleSetting("Max Distance", 24.0D, 4.0D, 128.0D, 1.0D).group(sgGeneral);
    private final IntSetting maxEntries = intSetting("Max Entries", 8, 1, 64, 1).group(sgGeneral);
    private final EnumSetting<SortMode> sortMode = enumSetting("Sort Mode", SortMode.DistanceAsc).group(sgGeneral);
    private final BoolSetting showType = boolSetting("Show Type", false).group(sgDisplay);
    private final BoolSetting showDistance = boolSetting("Show Distance", true).group(sgDisplay);
    private final BoolSetting textShadow = boolSetting("Text Shadow", true).group(sgDisplay);
    private final ColorSetting textColor = colorSetting("Text Color", Color.WHITE, true).group(sgDisplay);
    private final BoolSetting renderInScreens = boolSetting("Render In Screens", false).group(sgDisplay);
    private final IntSetting refreshDelay = intSetting("Refresh Delay", 100, 0, 1000, 10).group(sgPerformance);

    private final List<String> cachedLines = new ArrayList<>();
    private float cachedWidth;
    private float cachedHeight;
    private long lastRefreshTime;

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
        if (cachedLines.isEmpty()) {
            setBounds(0.0F, 0.0F);
            return;
        }

        float lineHeight = mc.font.lineHeight + 2.0F;
        for (int index = 0; index < cachedLines.size(); index++) {
            guiGraphics.text(
                mc.font,
                cachedLines.get(index),
                (int) this.x,
                (int) (this.y + index * lineHeight),
                textColor.getValue().getRGB(),
                textShadow.getValue()
            );
        }

        setBounds(cachedWidth, cachedHeight);
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        int delay = refreshDelay.getValue();
        if (delay > 0 && now - lastRefreshTime < delay) {
            return;
        }

        lastRefreshTime = now;
        cachedLines.clear();
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
        float lineHeight = mc.font.lineHeight + 2.0F;

        for (int index = 0; index < renderCount; index++) {
            AggregatedDroppedItemEntry entry = aggregatedEntries.get(index);
            String line = EntityRenderUtils.buildDroppedItemHudLine(entry, showType.getValue(), showDistance.getValue());
            cachedLines.add(line);
            cachedWidth = Math.max(cachedWidth, mc.font.width(line));
        }

        cachedHeight = renderCount * lineHeight;
    }

    private void clearCache() {
        cachedLines.clear();
        cachedWidth = 0.0F;
        cachedHeight = 0.0F;
        lastRefreshTime = 0L;
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
