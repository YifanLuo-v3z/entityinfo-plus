package com.github.entityinfo.mixin;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.Render2DEvent;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.combat.PacketMine")
public abstract class MixinPacketMineCountdown extends Module {

    @Shadow
    public static BlockPos targetPos;

    @Shadow
    public static BlockPos secondPos;

    @Shadow
    private static float progress;

    @Shadow
    private static float secondProgress;

    @Shadow
    private DoubleSetting mineDamage;

    @Shadow
    private BoolSetting checkGround;

    @Unique
    private final Supplier<TextRenderer> entityinfo$textRendererSupplier = Suppliers.memoize(() -> TextRenderer.create(16 * 1024));

    @Unique
    private final Supplier<RectRenderer> entityinfo$rectRendererSupplier = Suppliers.memoize(RectRenderer::create);

    @Unique
    private BoolSetting entityinfo$mineCountdown;

    @Unique
    private EnumSetting<CountdownMode> entityinfo$countdownMode;

    @Unique
    private DoubleSetting entityinfo$countdownScale;

    @Unique
    private DoubleSetting entityinfo$countdownYOffset;

    @Unique
    private BoolSetting entityinfo$countdownBackground;

    @Unique
    private ColorSetting entityinfo$countdownColor;

    @Unique
    private ColorSetting entityinfo$countdownReadyColor;

    @Unique
    private ColorSetting entityinfo$countdownBackgroundColor;

    protected MixinPacketMineCountdown(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectCountdownSettings(CallbackInfo ci) {
        entityinfo$mineCountdown = this.boolSetting("Mine Countdown", true);
        entityinfo$countdownMode = this.enumSetting("Countdown Mode", CountdownMode.SecondsPercent, this::entityinfo$isCountdownEnabled);
        entityinfo$countdownScale = this.doubleSetting("Countdown Scale", 0.75D, 0.35D, 2.0D, 0.05D, this::entityinfo$isCountdownEnabled);
        entityinfo$countdownYOffset = this.doubleSetting("Countdown Y Offset", 0.78D, -0.5D, 2.0D, 0.05D, this::entityinfo$isCountdownEnabled);
        entityinfo$countdownBackground = this.boolSetting("Countdown Background", true, this::entityinfo$isCountdownEnabled);
        entityinfo$countdownColor = this.colorSetting("Countdown Color", new Color(230, 255, 245, 245), true, this::entityinfo$isCountdownEnabled);
        entityinfo$countdownReadyColor = this.colorSetting("Countdown Ready Color", new Color(95, 255, 155, 245), true, this::entityinfo$isCountdownEnabled);
        entityinfo$countdownBackgroundColor = this.colorSetting(
            "Countdown Background Color",
            new Color(8, 12, 10, 150),
            true,
            this::entityinfo$shouldShowCountdownBackground
        );

        Setting<?> anchor = entityinfo$findSetting("Second Line End");
        entityinfo$moveSettingAfter(entityinfo$mineCountdown, anchor);
        entityinfo$moveSettingAfter(entityinfo$countdownMode, entityinfo$mineCountdown);
        entityinfo$moveSettingAfter(entityinfo$countdownScale, entityinfo$countdownMode);
        entityinfo$moveSettingAfter(entityinfo$countdownYOffset, entityinfo$countdownScale);
        entityinfo$moveSettingAfter(entityinfo$countdownBackground, entityinfo$countdownYOffset);
        entityinfo$moveSettingAfter(entityinfo$countdownColor, entityinfo$countdownBackground);
        entityinfo$moveSettingAfter(entityinfo$countdownReadyColor, entityinfo$countdownColor);
        entityinfo$moveSettingAfter(entityinfo$countdownBackgroundColor, entityinfo$countdownReadyColor);
    }

    @EventHandler
    private void entityinfo$renderMineCountdown(Render2DEvent.Level event) {
        if (nullCheck() || !entityinfo$isCountdownEnabled()) {
            return;
        }

        TextRenderer textRenderer = entityinfo$textRendererSupplier.get();
        List<CountdownDraw> draws = new ArrayList<>(2);

        entityinfo$collectCountdownDraw(draws, textRenderer, targetPos, progress, false);
        entityinfo$collectCountdownDraw(draws, textRenderer, secondPos, secondProgress, true);

        if (draws.isEmpty()) {
            return;
        }

        if (entityinfo$shouldShowCountdownBackground()) {
            RectRenderer rectRenderer = entityinfo$rectRendererSupplier.get();
            for (CountdownDraw draw : draws) {
                rectRenderer.addRect(draw.x(), draw.y(), draw.width(), draw.height(), entityinfo$countdownBackgroundColor.getValue());
                rectRenderer.addRect(
                    draw.x(),
                    draw.y() + draw.height() - Math.max(1.0F, 1.6F * draw.scale()),
                    draw.width() * draw.rawProgress(),
                    Math.max(1.0F, 1.6F * draw.scale()),
                    draw.accentColor()
                );
            }
            rectRenderer.drawAndClear();
        }

        for (CountdownDraw draw : draws) {
            textRenderer.addText(draw.text(), draw.textX(), draw.textY(), draw.scale(), draw.textColor());
        }
        textRenderer.drawAndClear();
    }

    @Unique
    private void entityinfo$collectCountdownDraw(
        List<CountdownDraw> draws,
        TextRenderer textRenderer,
        BlockPos pos,
        float currentProgress,
        boolean second
    ) {
        if (!entityinfo$isValidCountdownPos(pos)) {
            return;
        }

        int toolSlot = entityinfo$getToolSafe(pos);
        float mineTicks = second ? entityinfo$getMineTicksSecond(toolSlot) : entityinfo$getMineTicks(toolSlot);
        if (!Float.isFinite(mineTicks) || mineTicks == Float.MAX_VALUE) {
            return;
        }

        double requiredProgress = Math.max(0.05D, mineTicks * entityinfo$getMineDamage());
        double clampedProgress = Mth.clamp(currentProgress, 0.0D, requiredProgress);
        float rawProgress = (float) Mth.clamp(clampedProgress / requiredProgress, 0.0D, 1.0D);
        double remainingProgress = Math.max(0.0D, requiredProgress - currentProgress);
        double remainingSeconds = remainingProgress / entityinfo$getProgressTicksPerSecond();
        int remainingTicks = (int) Math.ceil(remainingProgress);
        String text = entityinfo$formatCountdownText(remainingSeconds, remainingTicks, rawProgress);

        Vector2f screen = entityinfo$projectToScreen(Vec3.atCenterOf(pos).add(0.0D, entityinfo$countdownYOffset.getValue(), 0.0D));
        if (screen == null) {
            return;
        }

        float scale = entityinfo$countdownScale.getValue().floatValue();
        float paddingX = 5.0F * scale;
        float paddingY = 3.0F * scale;
        float textWidth = textRenderer.getWidth(text, scale);
        float textHeight = textRenderer.getHeight(scale);
        float width = textWidth + paddingX * 2.0F;
        float height = textHeight + paddingY * 2.0F + (entityinfo$shouldShowCountdownBackground() ? Math.max(1.0F, 1.6F * scale) : 0.0F);
        float x = screen.x - width * 0.5F;
        float y = screen.y - height * 0.5F;

        if (x + width < 0.0F || y + height < 0.0F || x > LuminRenderSystem.getScaledWidth() || y > LuminRenderSystem.getScaledHeight()) {
            return;
        }

        Color textColor = rawProgress >= 0.999F ? entityinfo$countdownReadyColor.getValue() : entityinfo$countdownColor.getValue();
        draws.add(new CountdownDraw(
            text,
            x,
            y,
            width,
            height,
            x + paddingX,
            y + paddingY,
            scale,
            rawProgress,
            textColor,
            entityinfo$getAccentColor(rawProgress)
        ));
    }

    @Unique
    private String entityinfo$formatCountdownText(double remainingSeconds, int remainingTicks, float rawProgress) {
        if (rawProgress >= 0.999F) {
            return "READY 100%";
        }

        int percent = Math.round(rawProgress * 100.0F);
        return switch (entityinfo$countdownMode.getValue()) {
            case Seconds -> String.format(Locale.ROOT, "%.2fs", remainingSeconds);
            case Ticks -> remainingTicks + "t";
            case SecondsPercent -> String.format(Locale.ROOT, "%.2fs %d%%", remainingSeconds, percent);
            case TicksPercent -> remainingTicks + "t " + percent + "%";
        };
    }

    @Unique
    private Vector2f entityinfo$projectToScreen(Vec3 pos) {
        Vector3f projected = WorldToScreen.getWorldPositionToScreen(pos);
        if (projected.z < 0.0F || projected.z > 1.0F) {
            return null;
        }

        float guiScale = (float) LuminRenderSystem.getGuiScale();
        float x = projected.x / guiScale;
        float y = projected.y / guiScale;
        if (x < 0.0F || y < 0.0F || x > LuminRenderSystem.getScaledWidth() || y > LuminRenderSystem.getScaledHeight()) {
            return null;
        }

        return new Vector2f(x, y);
    }

    @Unique
    private boolean entityinfo$isValidCountdownPos(BlockPos pos) {
        if (pos == null || mc.level == null || !mc.level.hasChunkAt(pos)) {
            return false;
        }

        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    @Unique
    private double entityinfo$getMineDamage() {
        return mineDamage == null ? 0.8D : mineDamage.getValue();
    }

    @Unique
    private double entityinfo$getProgressTicksPerSecond() {
        if (checkGround != null && checkGround.getValue() && mc.player != null && !mc.player.onGround()) {
            return 4.0D;
        }

        return 20.0D;
    }

    @Unique
    private int entityinfo$getToolSafe(BlockPos pos) {
        if (pos == null || mc.player == null || mc.level == null) {
            return -1;
        }

        return entityinfo$getTool(pos);
    }

    @Unique
    private boolean entityinfo$isCountdownEnabled() {
        return entityinfo$mineCountdown != null && entityinfo$mineCountdown.getValue();
    }

    @Unique
    private boolean entityinfo$shouldShowCountdownBackground() {
        return entityinfo$isCountdownEnabled()
            && entityinfo$countdownBackground != null
            && entityinfo$countdownBackground.getValue();
    }

    @Unique
    private Color entityinfo$getAccentColor(float rawProgress) {
        Color start = new Color(255, 95, 80, 220);
        Color end = entityinfo$countdownReadyColor != null
            ? entityinfo$countdownReadyColor.getValue()
            : new Color(95, 255, 155, 245);
        float t = Mth.clamp(rawProgress, 0.0F, 1.0F);
        return new Color(
            Math.round(Mth.lerp(t, start.getRed(), end.getRed())),
            Math.round(Mth.lerp(t, start.getGreen(), end.getGreen())),
            Math.round(Mth.lerp(t, start.getBlue(), end.getBlue())),
            Math.round(Mth.lerp(t, start.getAlpha(), end.getAlpha()))
        );
    }

    @Unique
    private Setting<?> entityinfo$findSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    @Unique
    private void entityinfo$moveSettingAfter(Setting<?> setting, Setting<?> anchor) {
        if (setting == null || anchor == null || setting == anchor || !settings.remove(setting)) {
            return;
        }

        int anchorIndex = settings.indexOf(anchor);
        settings.add(anchorIndex < 0 ? settings.size() : anchorIndex + 1, setting);
    }

    @Shadow
    private float getMineTicks(int slot) {
        throw new AssertionError();
    }

    @Unique
    private float entityinfo$getMineTicks(int slot) {
        return getMineTicks(slot);
    }

    @Shadow
    private float getMineTicksSecond(int slot) {
        throw new AssertionError();
    }

    @Unique
    private float entityinfo$getMineTicksSecond(int slot) {
        return getMineTicksSecond(slot);
    }

    @Shadow
    private int getTool(BlockPos pos) {
        throw new AssertionError();
    }

    @Unique
    private int entityinfo$getTool(BlockPos pos) {
        return getTool(pos);
    }

    @Environment(EnvType.CLIENT)
    private enum CountdownMode {
        Seconds,
        Ticks,
        SecondsPercent,
        TicksPercent
    }

    @Unique
    private record CountdownDraw(
        String text,
        float x,
        float y,
        float width,
        float height,
        float textX,
        float textY,
        float scale,
        float rawProgress,
        Color textColor,
        Color accentColor
    ) {
    }
}
