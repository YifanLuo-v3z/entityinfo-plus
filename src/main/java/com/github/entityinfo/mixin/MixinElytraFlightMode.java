package com.github.entityinfo.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.movement.elytrafly.ElytraFlightMode")
public abstract class MixinElytraFlightMode {

    @Shadow
    @Final
    protected Minecraft mc;

    @Inject(method = "shouldCancelRightClick", at = @At("HEAD"), cancellable = true)
    private void entityinfo$allowFoodRightClick(CallbackInfoReturnable<Boolean> cir) {
        if (mc.screen != null || entityinfo$isHoldingFood()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useFirework", at = @At("HEAD"), cancellable = true)
    private void entityinfo$doNotFireworkWhileEating(CallbackInfoReturnable<Boolean> cir) {
        if (entityinfo$isUsingFood()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleUnbreaking", at = @At("HEAD"), cancellable = true)
    private void entityinfo$doNotSwapElytraWhileEating(CallbackInfo ci) {
        if (entityinfo$isUsingFood()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean entityinfo$isHoldingFood() {
        return mc.player != null
            && (entityinfo$isFood(mc.player.getItemInHand(InteractionHand.MAIN_HAND))
                || entityinfo$isFood(mc.player.getItemInHand(InteractionHand.OFF_HAND)));
    }

    @Unique
    private boolean entityinfo$isUsingFood() {
        return mc.player != null
            && mc.player.isUsingItem()
            && entityinfo$isFood(mc.player.getUseItem());
    }

    @Unique
    private static boolean entityinfo$isFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }
}
