package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacementData;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacements;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalPoolCrowdPlacement;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OriginalPoolCrowdVisitService {
    private static final String GENERATION = "BlacksoulsOriginalPoolVisitGeneration";
    private static final AABB MAP_AREA = new AABB(821.0D, 8.0D, -618.0D, 886.0D, 80.0D, -549.0D);

    private OriginalPoolCrowdVisitService() {
    }

    public static void transferred(ServerPlayer player, String source, Vec3 destination) {
        if (player == null || destination == null || !HokoniwaDestination.DIMENSION.equals(player.level().dimension())
                || !OriginalNpcPlacements.hasDeployedMap(player.serverLevel())
                || !("map033_ev019".equals(source) && OriginalDodoEntry.POOL_CEMETERY_ARRIVAL.equals(destination)
                || "map063_ev017".equals(source) && OriginalDodoEntry.POOL_CAVE_ARRIVAL.equals(destination))) return;
        ServerLevel level = player.serverLevel();
        if (player.server.getPlayerList().getPlayers().stream().anyMatch(other -> other != player && other.level() == level
                && !other.isSpectator() && other.isAlive() && MAP_AREA.contains(other.position()))
                || OriginalNpcService.hasPoolMapConversation(level)) return;
        OriginalPoolCrowdVisitData.get(level).rebuild(player.getUUID());
        synchronize(level);
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(HokoniwaDestination.DIMENSION);
        if (level != null && OriginalPoolCrowdVisitData.get(level).generation() > 0) synchronize(level);
    }

    public static boolean ready(EntityOriginalNpc actor) {
        if (!(actor.level() instanceof ServerLevel level) || !OriginalPoolCrowdSurface.isCrowd(actor.getNpcId())) return true;
        UUID bound = OriginalNpcPlacementData.get(level).entityId(actor.getNpcId());
        return !actor.getUUID().equals(bound) || actor.getPersistentData().getLong(GENERATION)
                == OriginalPoolCrowdVisitData.get(level).generation();
    }

    private static void synchronize(ServerLevel level) {
        var data = OriginalPoolCrowdVisitData.get(level);
        var placements = OriginalNpcPlacementData.get(level);
        var pending = new ArrayList<Pending>();
        Set<UUID> resetting = new HashSet<>();
        for (var site : OriginalPoolCrowdPlacement.sites()) {
            UUID id = placements.entityId(site.npcId());
            if (id == null || !(level.getEntity(id) instanceof EntityOriginalNpc actor) || !actor.isAlive()
                    || actor.isRemoved() || !site.npcId().equals(actor.getNpcId()) || ready(actor)) continue;
            if (!OriginalPoolCrowdSurface.matchesSite(level, site)) return;
            AABB bounds = actor.getBoundingBox().move(site.position().subtract(actor.position()));
            if (!OriginalPoolCrowdSurface.supports(level, actor.getNpcId(), site.position(), bounds)) return;
            pending.add(new Pending(site, actor, bounds));
            resetting.add(id);
        }
        if (pending.isEmpty() || OriginalNpcService.hasPoolMapConversation(level)) return;
        for (var entry : pending) {
            if (!level.getEntities(entry.actor(), entry.bounds(), other -> !resetting.contains(other.getUUID())
                    && !other.isSpectator() && (entry.actor().canCollideWith(other) || other.canBeCollidedWith()
                    || other instanceof LivingEntity && !(other instanceof EntityOriginalNpc) && other.isAlive())).isEmpty()) return;
            if (level.getServer().getPlayerList().getPlayers().stream().anyMatch(player -> player.level() == level
                    && !player.isSpectator() && player.isAlive() && player.getBoundingBox().intersects(entry.bounds()))) return;
        }
        ServerPlayer owner = data.owner() == null ? null : level.getServer().getPlayerList().getPlayer(data.owner());
        if (owner != null && owner.level() != level) owner = null;
        for (var entry : pending) {
            EntityOriginalNpc actor = entry.actor();
            actor.moveTo(entry.site().position().x, entry.site().position().y, entry.site().position().z, entry.site().yaw(), 0);
            actor.setDeltaMovement(Vec3.ZERO);
            actor.fallDistance = 0;
            actor.resetOriginalVisitState();
            OriginalNpcService.refreshPage(actor, owner);
            actor.applyOriginalPoolSurfaceSupport(true);
            actor.getPersistentData().putLong(GENERATION, data.generation());
        }
    }

    private record Pending(OriginalNpcPlacements.Site site, EntityOriginalNpc actor, AABB bounds) {
    }
}
