package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityCorpseEatingRabbit;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class RenderCorpseEatingRabbit extends RenderRMSpriteMonster<EntityCorpseEatingRabbit> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/entity/corpse_eating_rabbit.png");

    public RenderCorpseEatingRabbit(EntityRendererProvider.Context context) {
        super(context, TEXTURE, 127.0F / 121.0F, 1.35F, 0.45F);
    }
}
