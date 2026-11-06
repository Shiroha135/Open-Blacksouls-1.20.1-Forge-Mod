package yuki135.mmdskin.render.entity;

import yuki135.mmdskin.player.render.InventoryRenderHelper;

import yuki135.mmdskin.MmdSkin;
import yuki135.mmdskin.config.ModelConfigManager;
import yuki135.mmdskin.model.runtime.ManagedModel;
import yuki135.mmdskin.model.runtime.ModelRequestKey;
import yuki135.mmdskin.render.bootstrap.ClientRenderRuntime;
import yuki135.mmdskin.render.scene.RenderScene;
import yuki135.mmdskin.render.material.EntityRenderOpacity;
import yuki135.mmdskin.render.scene.MutableRenderPose;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46C;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * MMD 自定义实体渲染器。
 */
@SuppressWarnings("removal")
public class MmdSkinRenderer<T extends Entity> extends EntityRenderer<T> {

    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            new ResourceLocation(MmdSkin.MOD_ID, "textures/entity/placeholder.png");

    protected final String modelName;

    private final MutableRenderPose reusablePose = new MutableRenderPose();
    private final Quaternionf reusableQuat = new Quaternionf();
    private final Vector3f reusableVec = new Vector3f();
    private final float[] reusableSize = new float[2];

    public MmdSkinRenderer(EntityRendererProvider.Context renderManager, String entityName) {
        super(renderManager);
        this.modelName = entityName.replace(':', '.');
    }

    @Override
    public void render(T entityIn, float entityYaw, float tickDelta, PoseStack matrixStackIn,
                       MultiBufferSource bufferIn, int packedLightIn) {
        float opacity = entityIn instanceof CustomEntityRenderStateProvider provider
                ? EntityRenderOpacity.clamp(provider.getMmdRenderOpacity()) : 1.0F;
        if (entityIn.isInvisible() || opacity <= 0.0F) return;
        ManagedModel model = ClientRenderRuntime.get().modelRepository()
                .acquire(ModelRequestKey.mob(entityIn, modelName));
        if (model == null || model.modelInstance() == null) {
            super.render(entityIn, entityYaw, tickDelta, matrixStackIn, bufferIn, packedLightIn);
            return;
        }

        float[] size = parseModelSize(model, reusableSize);
        float effectiveScale = size[0] * ModelConfigManager.getConfig(model.modelName()).modelScale;
        if (entityIn instanceof LivingEntity living && living.isBaby()) {
            effectiveScale *= 0.5f;
        }
        float labelOffset = entityIn.getBbHeight() * (effectiveScale - 1.0f);
        matrixStackIn.pushPose();
        try {
            matrixStackIn.translate(0.0D, labelOffset, 0.0D);
            super.render(entityIn, entityYaw, tickDelta, matrixStackIn, bufferIn, packedLightIn);
        } finally {
            matrixStackIn.popPose();
        }

        reusablePose.reset();
        EntityAnimationResolver.resolve(entityIn, model, entityYaw, tickDelta, reusablePose);
        if (entityIn instanceof CustomEntityRenderStateProvider provider) {
            reusablePose.bodyYaw = provider.getMmdBodyYaw(reusablePose.bodyYaw);
        }
        if (!InventoryRenderHelper.isInventoryScreen()) {
            yuki135.blacksouls.client.stage.StagePhysicsRuntime.prepare(entityIn, model, reusablePose.bodyYaw, tickDelta);
        }

        matrixStackIn.pushPose();
        DrawState previousState = entityIn instanceof CustomEntityRenderStateProvider ? DrawState.capture() : null;
        try (EntityRenderOpacity.Scope ignored = entityIn instanceof CustomEntityRenderStateProvider
                ? EntityRenderOpacity.open(opacity) : null) {
            if (entityIn instanceof LivingEntity living && living.isBaby()) {
                matrixStackIn.scale(0.5f, 0.5f, 0.5f);
            }
            if (InventoryRenderHelper.isInventoryScreen()) {
                renderInInventory(entityIn, model, entityYaw, tickDelta, matrixStackIn, packedLightIn, size);
            } else {
                if (entityIn instanceof CustomEntityModelOffsetProvider provider) {
                    var offset = provider.getMmdModelOffset();
                    matrixStackIn.translate(offset.x, offset.y, offset.z);
                }
                matrixStackIn.scale(size[0], size[0], size[0]);
                RenderSystem.setShader(entityIn instanceof CustomEntityRenderStateProvider
                        ? GameRenderer::getRendertypeEntityTranslucentShader : GameRenderer::getRendertypeEntityCutoutNoCullShader);
                model.modelInstance().render(entityIn, reusablePose.bodyYaw, reusablePose.bodyPitch, reusablePose.translation,
                        tickDelta, matrixStackIn, packedLightIn, RenderScene.WORLD);
            }
        } finally {
            matrixStackIn.popPose();
            if (previousState != null) previousState.restore();
        }
    }

    private record DrawState(float red, float green, float blue, float alpha, boolean depthMask,
                             boolean cull, int cullFace, boolean blend, int blendEquationRgb,
                             int blendEquationAlpha, int sourceRgb, int destinationRgb,
                             int sourceAlpha, int destinationAlpha, int activeTexture, int program,
                             ShaderInstance shader) {
        private static DrawState capture() {
            float[] color = RenderSystem.getShaderColor();
            return new DrawState(color[0], color[1], color[2], color[3], GL46C.glGetBoolean(GL46C.GL_DEPTH_WRITEMASK),
                    GL46C.glIsEnabled(GL46C.GL_CULL_FACE), GL46C.glGetInteger(GL46C.GL_CULL_FACE_MODE),
                    GL46C.glIsEnabled(GL46C.GL_BLEND), GL46C.glGetInteger(GL46C.GL_BLEND_EQUATION_RGB),
                    GL46C.glGetInteger(GL46C.GL_BLEND_EQUATION_ALPHA), GL46C.glGetInteger(GL46C.GL_BLEND_SRC_RGB),
                    GL46C.glGetInteger(GL46C.GL_BLEND_DST_RGB), GL46C.glGetInteger(GL46C.GL_BLEND_SRC_ALPHA),
                    GL46C.glGetInteger(GL46C.GL_BLEND_DST_ALPHA), GL46C.glGetInteger(GL46C.GL_ACTIVE_TEXTURE),
                    GL46C.glGetInteger(GL46C.GL_CURRENT_PROGRAM), RenderSystem.getShader());
        }

        private void restore() {
            RenderSystem.setShaderColor(red, green, blue, alpha);
            RenderSystem.depthMask(depthMask);
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            GL46C.glCullFace(cullFace);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            GL46C.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.setShader(() -> shader);
            GL46C.glUseProgram(program);
            GlStateManager._glUseProgram(program);
        }
    }

    private void renderInInventory(T entityIn, ManagedModel model, float entityYaw,
                                    float tickDelta, PoseStack matrixStack, int packedLight, float[] size) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();

        int posX = (mc.screen.width - 176) / 2;
        int posY = (mc.screen.height - 166) / 2;
        modelViewStack.pushPose();
        try {
            modelViewStack.translate(posX + 51, posY + 60, 50.0);
            modelViewStack.scale(20.0f, 20.0f, -20.0f);
            modelViewStack.scale(size[1], size[1], size[1]);

            reusableQuat.identity()
                    .rotateZ((float) Math.PI)
                    .rotateX(-entityIn.getXRot() * ((float) Math.PI / 180F))
                    .rotateY(-entityIn.getYRot() * ((float) Math.PI / 180F));
            modelViewStack.mulPose(reusableQuat);

            RenderSystem.setShader(entityIn instanceof CustomEntityRenderStateProvider
                            ? GameRenderer::getRendertypeEntityTranslucentShader : GameRenderer::getRendertypeEntityCutoutNoCullShader);
            reusableVec.set(0.0f);
            model.modelInstance().render(entityIn, entityYaw, 0.0f, reusableVec,
                              tickDelta, modelViewStack, packedLight, RenderScene.INVENTORY);
        } finally {
            modelViewStack.popPose();
        }
    }

    private static float[] parseModelSize(ManagedModel model, float[] out) {
        out[0] = model.renderProperties().modelScale();
        out[1] = model.renderProperties().inventoryScale();
        return out;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return PLACEHOLDER_TEXTURE;
    }
}
