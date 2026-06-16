package com.github.entityinfo.modules;

import com.github.entityinfo.utils.render.EntityRenderUtils;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.Render3DEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class CrystalChams extends Module {

    public static final CrystalChams INSTANCE = new CrystalChams();

    private static final Identifier CRYSTAL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);
    private static final float SIN_45 = (float) Math.sin(Math.PI / 4.0D);

    private final SettingGroup sgGeneral = settingGroup("General");
    private final SettingGroup sgAnimation = settingGroup("Animation");
    private final SettingGroup sgFill = settingGroup("Fill");

    private final DoubleSetting range = doubleSetting("Range", 16.0D, 4.0D, 128.0D, 1.0D).group(sgGeneral);
    private final BoolSetting frustumCulling = boolSetting("Frustum Culling", true).group(sgGeneral);
    private final DoubleSetting scale = doubleSetting("Scale", 1.0D, 0.25D, 4.0D, 0.05D).group(sgAnimation);
    private final DoubleSetting spinSpeed = doubleSetting("Spin Speed", 1.0D, 0.0D, 4.0D, 0.05D).group(sgAnimation);
    private final DoubleSetting floatSpeed = doubleSetting("Float Speed", 1.0D, 0.0D, 4.0D, 0.05D).group(sgAnimation);
    private final BoolSetting filled = boolSetting("Filled", true).group(sgFill);
    private final ColorSetting fillColor = colorSetting("Fill Color", new Color(133, 255, 200, 63), true, filled::getValue).group(sgFill);

    private EndCrystalModel crystalModel;

    private CrystalChams() {
        super("Crystal Chams", Category.RENDER);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (nullCheck() || !filled.getValue()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        boolean renderedAnyCrystal = false;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double renderRange = range.getValue();
        double rangeSq = renderRange * renderRange;
        Vec3 cameraPosition = mc.gameRenderer.getMainCamera().position();
        AABB searchBox = new AABB(
            cameraPosition.x - renderRange,
            cameraPosition.y - renderRange,
            cameraPosition.z - renderRange,
            cameraPosition.x + renderRange,
            cameraPosition.y + renderRange,
            cameraPosition.z + renderRange
        );

        for (EndCrystal crystal : mc.level.getEntitiesOfClass(EndCrystal.class, searchBox)) {
            if (!shouldRenderCrystal(crystal, cameraPosition, rangeSq)) {
                continue;
            }

            if (frustumCulling.getValue() && !EntityRenderUtils.isInsideFrustum(mc, crystal)) {
                continue;
            }

            double renderX = lerp(crystal.xOld, crystal.getX(), partialTick) - cameraPosition.x;
            double renderY = lerp(crystal.yOld, crystal.getY(), partialTick) - cameraPosition.y;
            double renderZ = lerp(crystal.zOld, crystal.getZ(), partialTick) - cameraPosition.z;
            float ageInTicks = crystal.time + partialTick;

            poseStack.pushPose();
            poseStack.translate((float) renderX, (float) renderY, (float) renderZ);
            renderFilledPass(poseStack, bufferSource, crystal, ageInTicks, fillColor.getValue().getRGB());
            poseStack.popPose();
            renderedAnyCrystal = true;
        }

        if (renderedAnyCrystal) {
            bufferSource.endBatch(RenderTypes.entityTranslucent(CRYSTAL_TEXTURE));
        }
    }

    public boolean shouldOverrideRender(double distanceToCameraSq) {
        return isEnabled() && filled.getValue() && distanceToCameraSq <= range.getValue() * range.getValue();
    }

    private static boolean shouldRenderCrystal(EndCrystal crystal, Vec3 cameraPosition, double rangeSq) {
        return crystal.isAlive()
            && !crystal.isRemoved()
            && crystal.distanceToSqr(cameraPosition) <= rangeSq;
    }

    private void renderFilledPass(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        EndCrystal crystal,
        float ageInTicks,
        int color
    ) {
        EndCrystalModel model = getCrystalModel();
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(crystal, 0.0F);

        setupCrystalModel(model, crystal, ageInTicks);

        poseStack.pushPose();
        poseStack.scale(2.0F * scale.getValue().floatValue(), 2.0F * scale.getValue().floatValue(), 2.0F * scale.getValue().floatValue());
        poseStack.translate(0.0F, -0.5F, 0.0F);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.entityTranslucent(CRYSTAL_TEXTURE));
        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            light,
            OverlayTexture.NO_OVERLAY,
            color
        );
        poseStack.popPose();
    }

    private void setupCrystalModel(EndCrystalModel model, EndCrystal crystal, float ageInTicks) {
        float spin = ageInTicks * 3.0F * spinSpeed.getValue().floatValue();
        float verticalOffset = getCrystalYOffset(ageInTicks * floatSpeed.getValue().floatValue()) * 8.0F;

        model.resetPose();
        model.base.visible = crystal.showsBottom();
        model.outerGlass.y += verticalOffset;
        model.outerGlass.rotateBy(
            Axis.YP.rotationDegrees(spin).rotateAxis(1.0471976F, SIN_45, 0.0F, SIN_45)
        );

        Quaternionf rotatingCore = new Quaternionf()
            .setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45)
            .rotateY(spin * DEG_TO_RAD);

        model.innerGlass.rotateBy(new Quaternionf(rotatingCore));
        model.cube.rotateBy(new Quaternionf(rotatingCore));
    }

    private EndCrystalModel getCrystalModel() {
        if (crystalModel == null) {
            crystalModel = new EndCrystalModel(mc.getEntityModels().bakeLayer(ModelLayers.END_CRYSTAL));
        }

        return crystalModel;
    }

    private static float getCrystalYOffset(float ageInTicks) {
        float oscillation = (float) Math.sin(ageInTicks * 0.2F) / 2.0F + 0.5F;
        oscillation = (oscillation * oscillation + oscillation) * 0.4F;
        return oscillation - 1.4F;
    }

    private static double lerp(double start, double end, float delta) {
        return start + (end - start) * delta;
    }
}
