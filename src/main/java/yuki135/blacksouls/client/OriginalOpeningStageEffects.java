package yuki135.blacksouls.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalOpeningProfile.Stage;
import yuki135.blacksouls.story.OriginalOpeningScenes;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT)
public final class OriginalOpeningStageEffects extends DimensionSpecialEffects.OverworldEffects {
    public static boolean active() {
        var minecraft=Minecraft.getInstance();
        var state=OriginalOpeningController.state();
        return minecraft.level!=null&&OriginalOpeningScenes.DIMENSION.equals(minecraft.level.dimension())&&state!=null
                &&(state.stage()==Stage.NAME_PENDING||state.stage()==Stage.ORIGIN_SELECTION||state.stage()==Stage.ORIGIN_CONFIRMED);
    }

    @Override public SkyType skyType() { return active()?SkyType.NONE:super.skyType(); }
    @Override public boolean forceBrightLightmap() { return active()||super.forceBrightLightmap(); }
    @Override public float getCloudHeight() { return active()?Float.NaN:super.getCloudHeight(); }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float brightness) {
        return active()?new Vec3(.12D,.12D,.13D):super.getBrightnessDependentFogColor(color,brightness);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void color(ViewportEvent.ComputeFogColor event) {
        if(!active()||event.getCamera().getFluidInCamera()!=FogType.NONE)return;
        event.setRed(.12F);event.setGreen(.12F);event.setBlue(.13F);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void fog(ViewportEvent.RenderFog event) {
        if(!active()||event.getType()!=FogType.NONE)return;
        event.setNearPlaneDistance(2F);event.setFarPlaneDistance(18F);
        event.setFogShape(FogShape.CYLINDER);event.setCanceled(true);
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Pre event) {
        if(active()&&Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera()==FogType.NONE)
            yuki135.blacksouls.client.render.OpeningRichFogRenderer.render(event.getGuiGraphics(),event.getPartialTick());
    }

    @Mod.EventBusSubscriber(modid=BlackSouls.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterDimensionSpecialEffectsEvent event) {
            event.register(ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"opening"),new OriginalOpeningStageEffects());
        }
    }
}
