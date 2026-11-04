package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalTurnBattleEnemy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderOriginalTurnBattleEnemy extends RenderRMSpriteMonster<EntityOriginalTurnBattleEnemy> {
    public RenderOriginalTurnBattleEnemy(EntityRendererProvider.Context context,
                                         EntityOriginalTurnBattleEnemy.Profile profile) {
        super(context, profile.texture(), profile.aspectRatio(),
                profile.worldRenderHeight(), profile.shadowRadius());
    }

    @Override
    protected ResourceLocation resolveTexture(EntityOriginalTurnBattleEnemy entity) {
        return entity.getTurnBattleTexture();
    }

    @Override
    protected float resolveAspectRatio(EntityOriginalTurnBattleEnemy entity) {
        return (float) entity.getTurnBattleTextureWidth() / entity.getTurnBattleTextureHeight();
    }
}
