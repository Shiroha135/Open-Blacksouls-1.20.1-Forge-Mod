package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.model.runtime.ManagedModel;
import com.shiroha.mmdskin.model.runtime.ModelRequestKey;
import com.shiroha.mmdskin.render.bootstrap.ClientRenderRuntime;
import com.shiroha.mmdskin.render.entity.MmdSkinRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class SwitchableMmdRenderer<T extends Entity> extends EntityRenderer<T> {

    private final String modelName;
    private final EntityRenderer<T> mmdRenderer;
    private final EntityRenderer<T> fallbackRenderer;

    public SwitchableMmdRenderer(EntityRendererProvider.Context context, String modelName, EntityRendererProvider<T> fallbackFactory) {
        super(context);
        this.modelName = modelName.replace(':', '.');
        this.mmdRenderer = new MmdSkinRenderer<>(context, modelName);
        this.fallbackRenderer = fallbackFactory.create(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(T entity, float entityYaw, float tickDelta, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        if (BSConfig.ENABLE_MMD_MODELS.get() && isMmdReady(entity)) {
            this.mmdRenderer.render(entity, entityYaw, tickDelta, matrixStack, buffer, packedLight);
        } else {
            this.fallbackRenderer.render(entity, entityYaw, tickDelta, matrixStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (BSConfig.ENABLE_MMD_MODELS.get() && isMmdReady(entity)) {
            return this.mmdRenderer.getTextureLocation(entity);
        }
        return this.fallbackRenderer.getTextureLocation(entity);
    }

    private boolean isMmdReady(T entity) {
        ManagedModel model = ClientRenderRuntime.get().modelRepository()
                .acquire(ModelRequestKey.mob(entity, this.modelName));
        return model != null && model.modelInstance() != null;
    }
}
