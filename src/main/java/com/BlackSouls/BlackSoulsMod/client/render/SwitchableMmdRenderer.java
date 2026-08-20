package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.render.entity.MmdSkinRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class SwitchableMmdRenderer<T extends Entity> extends EntityRenderer<T> {

    private final EntityRenderer<T> mmdRenderer;
    private final EntityRenderer<T> fallbackRenderer;

    public SwitchableMmdRenderer(EntityRendererProvider.Context context, String modelName, EntityRendererProvider<T> fallbackFactory) {
        super(context);
        this.mmdRenderer = new MmdSkinRenderer<>(context, modelName);
        this.fallbackRenderer = fallbackFactory.create(context);
    }

    private EntityRenderer<T> active() {
        return BSConfig.ENABLE_MMD_MODELS.get() ? this.mmdRenderer : this.fallbackRenderer;
    }

    @Override
    public void render(T entity, float entityYaw, float tickDelta, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        this.active().render(entity, entityYaw, tickDelta, matrixStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.active().getTextureLocation(entity);
    }
}
