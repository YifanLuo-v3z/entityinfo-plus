package com.github.entityinfo.mixin;

import com.github.entityinfo.modules.CrystalChams;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EndCrystalRenderer.class)
public abstract class MixinEndCrystalRenderer extends EntityRenderer<EndCrystal, EndCrystalRenderState> {

    protected MixinEndCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void entityinfo$skipDefaultCrystalModel(
        EndCrystalRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        if (!CrystalChams.INSTANCE.shouldOverrideRender(state.distanceToCameraSq)) {
            return;
        }

        Vec3 beamOffset = state.beamOffset;
        if (beamOffset != null) {
            float crystalY = EndCrystalRenderer.getY(state.ageInTicks);
            float deltaX = (float) beamOffset.x;
            float deltaY = (float) beamOffset.y;
            float deltaZ = (float) beamOffset.z;
            poseStack.pushPose();
            poseStack.translate(beamOffset);
            EnderDragonRenderer.submitCrystalBeams(
                -deltaX,
                -deltaY + crystalY,
                -deltaZ,
                state.ageInTicks,
                poseStack,
                submitNodeCollector,
                state.lightCoords
            );
            poseStack.popPose();
        }

        super.submit(state, poseStack, submitNodeCollector, camera);
        ci.cancel();
    }
}
