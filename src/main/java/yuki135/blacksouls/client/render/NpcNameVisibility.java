package yuki135.blacksouls.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid="blacksouls", value=Dist.CLIENT)
public final class NpcNameVisibility {
    private record Sample(long tick, Vec3 camera, Vec3 target, boolean clear) { }
    private static final Map<LivingEntity, Sample> SAMPLES = new WeakHashMap<>();

    private NpcNameVisibility() { }

    public static boolean visible(LivingEntity entity, Vec3 camera, float partialTicks) {
        if (entity.isInvisible() || !entity.isAlive()) return false;
        Vec3 target = entity.getPosition(partialTicks).add(0, entity.getEyeHeight(), 0);
        if (camera.distanceToSqr(target) > 4096D) return false;
        long tick = entity.level().getGameTime();
        var previous = SAMPLES.get(entity);
        if (previous != null && previous.tick() == tick && previous.camera().distanceToSqr(camera) < 0.0001D
                && previous.target().distanceToSqr(target) < 0.0001D) return previous.clear();
        var minecraft = Minecraft.getInstance();
        var viewer = minecraft.getCameraEntity();
        HitResult hit = entity.level().clip(new ClipContext(camera, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, viewer == null ? entity : viewer));
        boolean clear = hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(target) < 0.0001D;
        SAMPLES.put(entity, new Sample(tick, camera, target, clear));
        return clear;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void name(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player) return;
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null || !id.getNamespace().equals("blacksouls") || event.getContent().getString().isBlank()) return;
        if (!entity.hasCustomName() && !entity.shouldShowName() && event.getResult() != Event.Result.ALLOW) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != entity.level() || !visible(entity, minecraft.gameRenderer.getMainCamera().getPosition(), event.getPartialTick())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { SAMPLES.clear(); }
}
