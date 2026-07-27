package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalDatabaseEnemy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderOriginalDatabaseEnemy extends RenderRMSpriteMonster<EntityOriginalDatabaseEnemy> {
    public RenderOriginalDatabaseEnemy(EntityRendererProvider.Context context) {
        super(context, new ResourceLocation("blacksouls", "textures/entity/headless_undead.png"),
                1.0F, 1.8F, 0.5F);
    }

    @Override
    protected ResourceLocation resolveTexture(EntityOriginalDatabaseEnemy entity) {
        return entity.getProfile().texture();
    }

    @Override
    protected float resolveRenderHeight(EntityOriginalDatabaseEnemy entity) {
        return entity.getProfile().worldRenderHeight();
    }

    @Override
    protected float resolveAspectRatio(EntityOriginalDatabaseEnemy entity) {
        return entity.getProfile().aspectRatio();
    }
}
