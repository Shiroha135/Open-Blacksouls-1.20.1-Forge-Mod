package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityHellPrince;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class RenderHellPrince extends RenderRMSpriteMonster<EntityHellPrince> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/entity/hell_prince.png");

    public RenderHellPrince(EntityRendererProvider.Context context) {
        
        super(context, TEXTURE, 214f / 310f, 3.5F, 1.5F);
    }
}