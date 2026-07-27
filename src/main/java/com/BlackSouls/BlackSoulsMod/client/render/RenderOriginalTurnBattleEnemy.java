package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalTurnBattleEnemy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RenderOriginalTurnBattleEnemy extends RenderRMSpriteMonster<EntityOriginalTurnBattleEnemy> {
    public RenderOriginalTurnBattleEnemy(EntityRendererProvider.Context context,
                                         EntityOriginalTurnBattleEnemy.Profile profile) {
        super(context, profile.texture(), profile.aspectRatio(),
                profile.worldRenderHeight(), profile.shadowRadius());
    }
}
