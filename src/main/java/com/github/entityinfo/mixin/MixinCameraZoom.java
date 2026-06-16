package com.github.entityinfo.mixin;

import com.github.entityinfo.modules.Zoom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class MixinCameraZoom {

    @Inject(method = "update", at = @At("HEAD"))
    private void entityinfo$updateZoomAmount(DeltaTracker deltaTracker, CallbackInfo ci) {
        Zoom.INSTANCE.updateZoomState();
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void entityinfo$applyWorldZoomFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!Zoom.INSTANCE.isZoomActive()) {
            return;
        }

        cir.setReturnValue(Zoom.INSTANCE.applyZoom(cir.getReturnValueF()));
    }

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void entityinfo$applyHudZoomFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!Zoom.INSTANCE.isZoomActive()) {
            return;
        }

        cir.setReturnValue(Zoom.INSTANCE.applyZoom(cir.getReturnValueF()));
    }

}
