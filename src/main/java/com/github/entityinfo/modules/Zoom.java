package com.github.entityinfo.modules;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
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
    private final BoolSetting requireKey = boolSetting("Require Key", false).group(sgGeneral);
    private final KeybindSetting zoomKey = keybindSetting("Zoom Key", GLFW.GLFW_KEY_C).group(sgGeneral);

    private double zoomProgress;

    private Zoom() {
        super("Zoom", Category.RENDER);
    }

    public void updateZoomState() {
        if (!isEnabled() || nullCheck()) {
            zoomProgress = 0.0D;
            return;
        }

        double target = shouldZoom() ? 1.0D : 0.0D;
        zoomProgress += (target - zoomProgress) * smoothFactor.getValue();

        if (zoomProgress < 0.001D && target == 0.0D) {
            zoomProgress = 0.0D;
        } else if (zoomProgress > 0.999D && target == 1.0D) {
            zoomProgress = 1.0D;
        }
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
        zoomProgress = 0.0D;
    }

    private boolean shouldZoom() {
        return !requireKey.getValue() || isZoomKeyPressed();
    }

    private boolean isZoomKeyPressed() {
        int key = zoomKey.getValue();
        return key != KeybindUtils.NONE && KeybindUtils.isPressed(key);
    }
}
