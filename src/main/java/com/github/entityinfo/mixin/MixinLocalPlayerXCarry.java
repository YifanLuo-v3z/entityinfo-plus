package com.github.entityinfo.mixin;

import com.github.entityinfo.modules.XCarry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerXCarry {

    @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
    private void entityinfo$cancelInventoryClose(CallbackInfo ci) {
        if (XCarry.INSTANCE.shouldCancelInventoryClose()) {
            ci.cancel();
        }
    }
}
