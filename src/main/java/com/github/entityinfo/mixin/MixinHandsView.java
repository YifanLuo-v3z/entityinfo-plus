package com.github.entityinfo.mixin;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.BoolSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.render.HandsView")
public abstract class MixinHandsView extends Module {

    @Unique
    private BoolSetting entityinfo$disableSpearBlockAnimation;

    @Unique
    private BoolSetting entityinfo$blockingAnimationSetting;

    protected MixinHandsView(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectSettings(CallbackInfo ci) {
        entityinfo$blockingAnimationSetting = entityinfo$findBoolSetting("Blocking Animation");
        entityinfo$disableSpearBlockAnimation = this.boolSetting(
            "Disable Spear Block Animation",
            false,
            this::entityinfo$isBlockingAnimationSettingVisible
        );
        entityinfo$moveSettingAfter(entityinfo$disableSpearBlockAnimation, entityinfo$blockingAnimationSetting);
    }

    @Inject(method = "shouldApplyBlockingAnimation", at = @At("HEAD"), cancellable = true)
    private void entityinfo$disableFirstPersonSpearBlocking(InteractionHand hand, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Spear stab use should keep the vanilla hand-use animation instead of Epsilon's blocking pose.
        if (hand == InteractionHand.MAIN_HAND && entityinfo$shouldDisableSpearBlocking(stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldApplyThirdPersonBlockingAnim", at = @At("HEAD"), cancellable = true)
    private void entityinfo$disableThirdPersonSpearBlocking(Avatar avatar, HumanoidArm arm, CallbackInfoReturnable<Boolean> cir) {
        if (mc.player == null || avatar != mc.player || arm != mc.player.getMainArm()) {
            return;
        }

        if (entityinfo$shouldDisableSpearBlocking(avatar.getItemInHand(InteractionHand.MAIN_HAND))) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean entityinfo$isBlockingAnimationSettingVisible() {
        return entityinfo$blockingAnimationSetting == null || entityinfo$blockingAnimationSetting.getValue();
    }

    @Unique
    private boolean entityinfo$shouldDisableSpearBlocking(ItemStack stack) {
        return entityinfo$disableSpearBlockAnimation != null
            && entityinfo$disableSpearBlockAnimation.getValue()
            && stack != null
            && !stack.isEmpty()
            && stack.is(ItemTags.SPEARS);
    }

    @Unique
    private void entityinfo$moveSettingAfter(Setting<?> setting, Setting<?> anchor) {
        if (setting == null || anchor == null || setting == anchor || !settings.remove(setting)) {
            return;
        }

        int anchorIndex = settings.indexOf(anchor);
        settings.add(anchorIndex < 0 ? settings.size() : anchorIndex + 1, setting);
    }

    @Unique
    private BoolSetting entityinfo$findBoolSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equals(name) && setting instanceof BoolSetting boolSetting) {
                return boolSetting;
            }
        }

        return null;
    }
}
