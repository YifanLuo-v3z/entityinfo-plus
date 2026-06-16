package com.github.entityinfo.modules;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.settings.impl.KeybindSetting;
import com.github.epsilon.utils.client.KeybindUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class Zoom extends Module {

    public static final Zoom INSTANCE = new Zoom();

    private final SettingGroup sgGeneral = settingGroup("General");

    private final DoubleSetting zoomFov = doubleSetting("Zoom Fov", 30.0D, 1.0D, 120.0D, 1.0D).group(sgGeneral);
    private final DoubleSetting smoothFactor = doubleSetting("Smooth Factor", 0.18D, 0.01D, 1.0D, 0.01D).group(sgGeneral);
    private final EnumSetting<AnimationMode> animationMode = enumSetting("Animation Mode", AnimationMode.Smooth).group(sgGeneral);
    private final IntSetting animationTime = intSetting("Animation Time", 220, 0, 1000, 10, () -> animationMode.getValue() == AnimationMode.Timed)
        .group(sgGeneral);
    private final EnumSetting<Ease> ease = enumSetting("Ease", Ease.CubicInOut, () -> animationMode.getValue() == AnimationMode.Timed)
        .group(sgGeneral);
    private final BoolSetting requireKey = boolSetting("Require Key", false).group(sgGeneral);
    private final KeybindSetting zoomKey = keybindSetting("Zoom Key", GLFW.GLFW_KEY_C).group(sgGeneral);

    private double rawZoomProgress;
    private double zoomProgress;
    private long lastUpdateNanos;

    private Zoom() {
        super("Zoom", Category.RENDER);
    }

    public void updateZoomState() {
        if (!isEnabled() || nullCheck()) {
            rawZoomProgress = 0.0D;
            zoomProgress = 0.0D;
            lastUpdateNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        double deltaSeconds = lastUpdateNanos == 0L ? 0.0D : (now - lastUpdateNanos) / 1_000_000_000.0D;
        lastUpdateNanos = now;

        double target = shouldZoom() ? 1.0D : 0.0D;
        if (animationMode.getValue() == AnimationMode.Timed) {
            updateTimedZoom(deltaSeconds, target);
            return;
        }

        zoomProgress += (target - zoomProgress) * smoothFactor.getValue();
        rawZoomProgress = zoomProgress;
        zoomProgress = clampTerminalProgress(zoomProgress, target);
    }

    public float applyZoom(float baseFov) {
        if (!isEnabled() || zoomProgress <= 0.001D) {
            return baseFov;
        }

        float targetFov = (float) Math.clamp(zoomFov.getValue(), 1.0D, 177.0D);
        return (float) Math.clamp(baseFov + (targetFov - baseFov) * zoomProgress, 1.0D, 177.0D);
    }

    public boolean isZoomActive() {
        return isEnabled() && zoomProgress > 0.001D;
    }

    @Override
    protected void onDisable() {
        rawZoomProgress = 0.0D;
        zoomProgress = 0.0D;
        lastUpdateNanos = 0L;
    }

    private boolean shouldZoom() {
        return !requireKey.getValue() || isZoomKeyPressed();
    }

    private boolean isZoomKeyPressed() {
        int key = zoomKey.getValue();
        return key != KeybindUtils.NONE && KeybindUtils.isPressed(key);
    }

    private void updateTimedZoom(double deltaSeconds, double target) {
        int durationMs = animationTime.getValue();
        if (durationMs <= 0 || deltaSeconds <= 0.0D) {
            rawZoomProgress = target;
            zoomProgress = target;
            return;
        }

        double step = deltaSeconds * 1000.0D / durationMs;
        if (target > rawZoomProgress) {
            rawZoomProgress = Math.min(1.0D, rawZoomProgress + step);
        } else if (target < rawZoomProgress) {
            rawZoomProgress = Math.max(0.0D, rawZoomProgress - step);
        }

        rawZoomProgress = clampTerminalProgress(rawZoomProgress, target);
        zoomProgress = clampTerminalProgress(ease.getValue().apply(rawZoomProgress), target);
    }

    private static double clampTerminalProgress(double progress, double target) {
        if (progress < 0.001D && target == 0.0D) {
            return 0.0D;
        }
        if (progress > 0.999D && target == 1.0D) {
            return 1.0D;
        }
        return Math.clamp(progress, 0.0D, 1.0D);
    }

    @Environment(EnvType.CLIENT)
    private enum AnimationMode {
        Smooth,
        Timed
    }

    @Environment(EnvType.CLIENT)
    private enum Ease {
        Linear {
            @Override
            protected double ease(double progress) {
                return progress;
            }
        },
        SineInOut {
            @Override
            protected double ease(double progress) {
                return -(Math.cos(Math.PI * progress) - 1.0D) / 2.0D;
            }
        },
        CubicInOut {
            @Override
            protected double ease(double progress) {
                return progress < 0.5D
                    ? 4.0D * progress * progress * progress
                    : 1.0D - Math.pow(-2.0D * progress + 2.0D, 3.0D) / 2.0D;
            }
        },
        QuintOut {
            @Override
            protected double ease(double progress) {
                return 1.0D - Math.pow(1.0D - progress, 5.0D);
            }
        },
        ExpoOut {
            @Override
            protected double ease(double progress) {
                return progress >= 1.0D ? 1.0D : 1.0D - Math.pow(2.0D, -10.0D * progress);
            }
        };

        private double apply(double progress) {
            return ease(Math.clamp(progress, 0.0D, 1.0D));
        }

        protected abstract double ease(double progress);
    }
}
