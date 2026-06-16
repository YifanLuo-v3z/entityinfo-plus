package com.github.entityinfo.mixin;

import com.github.epsilon.events.impl.KeyboardInputEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.movement.GUIMove")
public abstract class MixinGUIMove extends Module {

    @Shadow
    private BoolSetting sneakValue;

    protected MixinGUIMove(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "onKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void entityinfo$syncBoundMovementKeys(KeyboardInputEvent event, CallbackInfo ci) {
        if (mc.screen == null || mc.screen instanceof ChatScreen || mc.getWindow() == null) {
            return;
        }

        boolean up = entityinfo$isKeyDown(mc.options.keyUp);
        boolean down = entityinfo$isKeyDown(mc.options.keyDown);
        boolean left = entityinfo$isKeyDown(mc.options.keyLeft);
        boolean right = entityinfo$isKeyDown(mc.options.keyRight);
        boolean jump = entityinfo$isKeyDown(mc.options.keyJump);
        boolean sneak = sneakValue != null && sneakValue.getValue() && entityinfo$isKeyDown(mc.options.keyShift);
        boolean sprint = entityinfo$isKeyDown(mc.options.keySprint);

        float forward = (up == down) ? 0.0F : (up ? 1.0F : -1.0F);
        float strafe = (left == right) ? 0.0F : (left ? 1.0F : -1.0F);

        event.setForward(forward);
        event.setStrafe(strafe);
        event.setJump(jump);
        event.setSneak(sneak);
        event.setSprint(sprint);

        mc.options.keyUp.setDown(up);
        mc.options.keyDown.setDown(down);
        mc.options.keyLeft.setDown(left);
        mc.options.keyRight.setDown(right);
        mc.options.keyJump.setDown(jump);
        mc.options.keyShift.setDown(sneak);
        mc.options.keySprint.setDown(sprint);

        ci.cancel();
    }

    @Unique
    private boolean entityinfo$isKeyDown(KeyMapping mapping) {
        if (mapping.isUnbound() || mc.getWindow() == null) {
            return false;
        }

        InputConstants.Key boundKey = InputConstants.getKey(mapping.saveString());
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(mc.getWindow().handle(), boundKey.getValue()) == GLFW.GLFW_PRESS;
        }

        return InputConstants.isKeyDown(mc.getWindow(), boundKey.getValue());
    }
}
