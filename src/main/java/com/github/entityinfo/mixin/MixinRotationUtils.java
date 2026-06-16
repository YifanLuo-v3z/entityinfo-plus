package com.github.entityinfo.mixin;

import com.github.epsilon.utils.rotation.Rot2f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.utils.rotation.RotationUtils")
public abstract class MixinRotationUtils {

    @Inject(
        method = "smooth(Lcom/github/epsilon/utils/rotation/Rot2f;Lcom/github/epsilon/utils/rotation/Rot2f;D)Lcom/github/epsilon/utils/rotation/Rot2f;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void entityinfo$guardNullLastRotation(Rot2f lastRotation, Rot2f targetRotation, double speed, CallbackInfoReturnable<Rot2f> cir) {
        if (lastRotation != null && targetRotation != null) {
            return;
        }

        cir.setReturnValue(entityinfo$copyRotationOrZero(targetRotation != null ? targetRotation : lastRotation));
    }

    @Inject(
        method = "move(Lcom/github/epsilon/utils/rotation/Rot2f;Lcom/github/epsilon/utils/rotation/Rot2f;D)Lcom/github/epsilon/utils/rotation/Rot2f;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void entityinfo$guardNullMoveRotation(Rot2f lastRotation, Rot2f targetRotation, double speed, CallbackInfoReturnable<Rot2f> cir) {
        if (lastRotation == null || targetRotation == null) {
            cir.setReturnValue(new Rot2f(0.0F, 0.0F));
            return;
        }

        double deltaYaw = Mth.wrapDegrees(targetRotation.getYaw() - lastRotation.getYaw());
        double deltaPitch = targetRotation.getPitch() - lastRotation.getPitch();
        if (speed != 0.0D && Math.abs(deltaYaw) + Math.abs(deltaPitch) <= 1.0E-6D) {
            cir.setReturnValue(new Rot2f(0.0F, 0.0F));
        }
    }

    @Unique
    private static Rot2f entityinfo$copyRotationOrZero(Rot2f rotation) {
        return rotation != null ? new Rot2f(rotation.getYaw(), rotation.getPitch()) : new Rot2f(0.0F, 0.0F);
    }
}
