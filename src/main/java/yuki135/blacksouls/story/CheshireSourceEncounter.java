package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.compat.scene.OriginalNpcPlacements;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import yuki135.blacksouls.entity.EntityCheshireCat;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CheshireSourceEncounter {
    private static final Map<MinecraftServer, Map<UUID, Observation>> OBSERVATIONS = new WeakHashMap<>();

    private CheshireSourceEncounter() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) observe(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        OBSERVATIONS.remove(event.getServer());
    }

    public static void forget(ServerPlayer player) {
        Map<UUID, Observation> players = OBSERVATIONS.get(player.server);
        if (players != null) players.remove(player.getUUID());
    }

    public static Outcome observe(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!HokoniwaDestination.DIMENSION.equals(level.dimension()) || !player.isAlive() || player.isRemoved()
                || player.isSpectator() || player.isPassenger() || player.getAbilities().flying || !player.onGround()
                || Math.abs(player.getY() + 28.0D) > 0.0625D
                || player.getX() < 76.0D || player.getX() >= 84.0D || player.getZ() < 169.0D || player.getZ() >= 171.0D) {
            forget(player);
            return Outcome.INELIGIBLE;
        }
        if (!OriginalNpcPlacements.hasDeployedMap(level) || !OriginalNpcCharacterBindings.cheshireChunksReady(level)) {
            forget(player);
            return Outcome.WORLD_UNAVAILABLE;
        }
        AABB bounds = player.getBoundingBox();
        if (!level.noCollision(player, bounds) || level.noCollision(player, bounds.move(0.0D, -0.08D, 0.0D))) {
            forget(player);
            return Outcome.INELIGIBLE;
        }
        int contact = contact(player.position());
        Observation current = new Observation(player, level, player.position(), bounds, player.tickCount, contact);
        Observation previous = OBSERVATIONS.computeIfAbsent(player.server, ignored -> new HashMap<>()).put(player.getUUID(), current);
        if (previous == null || previous.player() != player || previous.level() != level
                || player.tickCount != previous.tick() + 1) return Outcome.BASELINE;
        if (player.position().distanceToSqr(previous.position()) > 1.0D
                || !level.noCollision(player, bounds.minmax(previous.bounds()))) return Outcome.DISCONTINUOUS;
        if (contact == 0 || previous.contact() == contact) return Outcome.NO_ENTRY;
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        if (progress.switchValue(14)
                || SceneSpawnerBossData.get(player.server).getStoryProgress() >= 1) return Outcome.SOURCE_DISABLED;
        if (contact == 96 || contact == 97) {
            if (!progress.switchValue(108) || progress.switchValue(109)) return Outcome.SOURCE_DISABLED;
            try {
                EntityCheshireCat cat = OriginalNpcCharacterBindings.resolve(level, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE);
                if (!cat.forcedRouteTicket().isEmpty()) return Outcome.SOURCE_BUSY;
                return OriginalNpcService.startCheshireContact(player, cat, "map051_ev%03d".formatted(contact))
                        ? Outcome.TRIGGERED : Outcome.SOURCE_BUSY;
            } catch (UnsupportedOperationException exception) {
                return Outcome.TARGET_UNAVAILABLE;
            }
        }
        if (progress.switchValue(107)) return Outcome.SOURCE_DISABLED;
        try {
            OriginalNpcAnimationService.startBoundCharacter(player, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE, 31, false);
        } catch (UnsupportedOperationException exception) {
            return Outcome.TARGET_UNAVAILABLE;
        }
        progress.setSwitch(107, true);
        OriginalNpcCharacterBindings.resolve(level, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE).refreshVisibility();
        return Outcome.TRIGGERED;
    }

    private static int contact(Vec3 position) {
        if (position.z < 169.0D || position.z >= 171.0D) return 0;
        if (position.x >= 79.0D && position.x < 80.0D) return position.z < 170.0D ? 94 : 93;
        if (position.x >= 81.0D && position.x < 82.0D) return position.z < 170.0D ? 97 : 96;
        return 0;
    }

    private record Observation(ServerPlayer player, ServerLevel level, Vec3 position, AABB bounds, int tick, int contact) {
    }

    public enum Outcome {
        TRIGGERED, BASELINE, NO_ENTRY, DISCONTINUOUS, INELIGIBLE, WORLD_UNAVAILABLE, SOURCE_DISABLED, TARGET_UNAVAILABLE, SOURCE_BUSY
    }
}
