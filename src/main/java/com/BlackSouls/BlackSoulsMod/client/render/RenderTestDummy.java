package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityTestDummy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderTestDummy extends HumanoidMobRenderer<EntityTestDummy, HumanoidModel<EntityTestDummy>> {

    public RenderTestDummy(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTestDummy entity) {
        return DefaultPlayerSkin.getDefaultSkin();
    }
}
