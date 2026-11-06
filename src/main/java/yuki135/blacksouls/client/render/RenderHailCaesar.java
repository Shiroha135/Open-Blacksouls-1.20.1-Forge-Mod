package yuki135.blacksouls.client.render;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityHailCaesar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class RenderHailCaesar extends RenderRMSpriteMonster<EntityHailCaesar> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(BlackSouls.MODID, "textures/entity/hail_caesar.png");

    public RenderHailCaesar(EntityRendererProvider.Context context) {
        super(context, TEXTURE, 417.0F / 288.0F, 3.0F, 1.2F);
    }
}
