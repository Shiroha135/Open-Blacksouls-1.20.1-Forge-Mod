package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import yuki135.blacksouls.entity.EntityCheshireCat;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.entity.EntityRabbitKnight;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.util.BSOriginalNpcData;
import yuki135.blacksouls.util.OriginalNpcTextures;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class OriginalNpcMovementService {
    private OriginalNpcMovementService() {
    }

    public static void forgetActor(net.minecraft.world.entity.PathfinderMob actor) {
        if (!(actor.level() instanceof ServerLevel level)) return;
        OriginalNpcMovementData data = OriginalNpcMovementData.get(level.getServer());
        for (OriginalNpcMovementData.Ticket ticket : List.copyOf(data.tickets.values())) {
            if (actor.getUUID().equals(ticket.target)) {
                stopMotion(actor, ticket);
                ticket.released = true;
                data.finish(ticket);
            }
        }
        data.collect();
    }

    public static String start(ServerPlayer player, EntityOriginalNpc npc, int target, JsonObject route) {
        OriginalNpcMoveRouteRunner runner = new OriginalNpcMoveRouteRunner(route);
        Entity subject = resolve(player, npc, target);
        var source = BSOriginalNpcData.getEvent(npc.getNpcId());
        int sourceMap = source == null ? 0 : source.mapId();
        RouteContext context = new RouteContext(player, subject, sourceMap, null);
        runner.validate(context);
        OriginalNpcService.refreshPage(npc, player);
        if (subject instanceof EntityOriginalNpc original && subject != npc) OriginalNpcService.refreshPage(original, player);
        return enqueue(player, subject, subject instanceof EntityOriginalNpc original ? original.getNpcId() : "", runner, sourceMap);
    }

    public static String startBoundCharacter(ServerPlayer player, String sourceId, JsonObject route) {
        if (player == null || player.isRemoved() || !player.isAlive()) {
            throw new UnsupportedOperationException("route_source_context");
        }
        OriginalNpcMoveRouteRunner runner = new OriginalNpcMoveRouteRunner(route);
        EntityCheshireCat subject = OriginalNpcCharacterBindings.resolve(player.serverLevel(), sourceId);
        runner.validate(new RouteContext(player, subject));
        subject.refreshOriginalRoutePage();
        return enqueue(player, subject, sourceId, runner);
    }

    public static String startRabbitCharacter(ServerPlayer player, EntityRabbitKnight rabbit, JsonObject route) {
        if (!OriginalRabbitKnightBinding.matches(rabbit) || player.level() != rabbit.level()) throw new UnsupportedOperationException("rabbit_route_identity");
        var runner = new OriginalNpcMoveRouteRunner(route);
        runner.validate(new RouteContext(player, rabbit));
        rabbit.refreshOriginalSourcePage();
        return enqueue(player, rabbit, OriginalRabbitKnightBinding.SOURCE_ID, runner);
    }

    private static String enqueue(ServerPlayer player, Entity subject, String sourceId, OriginalNpcMoveRouteRunner runner) {
        var source = BSOriginalNpcData.getEvent(sourceId);
        return enqueue(player, subject, sourceId, runner, source == null ? 0 : source.mapId());
    }

    private static String enqueue(ServerPlayer player, Entity subject, String sourceId, OriginalNpcMoveRouteRunner runner, int sourceMap) {
        OriginalNpcMovementData data = OriginalNpcMovementData.get(player.server);
        String id = UUID.randomUUID().toString();
        OriginalNpcMovementData.Ticket ticket = new OriginalNpcMovementData.Ticket(id, player.getUUID(),
                subject.getUUID(), subject.level().dimension().location().toString(),
                sourceId, runner);
        ticket.sourceMap = sourceMap;
        ticket.validated = true;
        if (subject instanceof EntityOriginalNpc original && original.hasOriginalAutonomy()) {
            var autonomous = original.originalAutonomousRoute();
            if (autonomous.motion != null) {
                ticket.motion = autonomous.motion;
                ticket.gravityCaptured = autonomous.gravityCaptured;
                ticket.originalNoGravity = autonomous.originalNoGravity;
                autonomous.motion = null;
                autonomous.gravityCaptured = false;
            }
        }
        data.add(ticket);
        if (subject instanceof EntityOriginalNpc original) original.beginForcedRoute(id);
        else if (subject instanceof EntityCheshireCat cat) cat.beginForcedRoute(id);
        else if (subject instanceof EntityRabbitKnight rabbit) rabbit.beginForcedRoute(id);
        data.setDirty();
        return id;
    }

    public static void clearAutonomousMotion(EntityOriginalNpc npc) {
        var autonomous = npc.originalAutonomousRoute();
        if (autonomous == null) return;
        if (autonomous.gravityCaptured) npc.setNoGravity(autonomous.originalNoGravity);
        autonomous.motion = null;
        autonomous.gravityCaptured = false;
    }

    public static boolean tickAutonomous(EntityOriginalNpc npc) {
        if (!npc.hasOriginalAutonomy() || !(npc.level() instanceof ServerLevel level)) return false;
        if (!level.dimension().equals(yuki135.blacksouls.util.HokoniwaDestination.DIMENSION)) return false;
        if (OriginalPoolCrowdSurface.isCrowd(npc.getNpcId())) return OriginalPoolCrowdMovementService.usesSharedScheduler(npc);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player.serverLevel() == level && player.isAlive() && !player.isSpectator() && player.distanceToSqr(npc) <= 1024.0D)
                .min(java.util.Comparator.comparingDouble(player -> player.distanceToSqr(npc))).orElse(null);
        var context = new RouteContext(owner, npc, npc.originalAutonomousRoute());
        for (int frame = 0; frame < 3; frame++) npc.originalAutonomousRoute().advanceFrame(context);
        return true;
    }

    public static OriginalNpcMotion originalMotion(EntityOriginalNpc npc) {
        if (npc.level() instanceof ServerLevel level && !npc.forcedRouteTicket().isEmpty()) {
            var ticket = OriginalNpcMovementData.get(level.getServer()).tickets.get(npc.forcedRouteTicket());
            if (ticket != null && ticket.target.equals(npc.getUUID()) && !ticket.complete) return ticket.motion;
        }
        return npc.originalAutonomousRoute().motion;
    }

    public static net.minecraft.world.phys.Vec3 logicalPosition(EntityOriginalNpc npc) {
        var motion = originalMotion(npc);
        if (motion == null) return npc.position();
        var state = motion.snapshot();
        return new net.minecraft.world.phys.Vec3(state.get("targetX").getAsDouble(), state.get("targetY").getAsDouble(), state.get("targetZ").getAsDouble());
    }

    public static boolean isOriginalWalking(EntityOriginalNpc npc) {
        var motion = originalMotion(npc);
        return motion != null && !motion.snapshot().get("jump").getAsBoolean();
    }

    public static void refreshAutonomousPage(EntityOriginalNpc npc) {
        if (!(npc.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = OriginalNpcService.choosePageOwner(npc, npc.getNpcId(), npc.forcedRouteTicket(),
                OriginalPoolCrowdMovementService.nearestContext(npc));
        if (owner != null) OriginalNpcService.refreshPage(npc, owner);
        else {
            var entry = BSOriginalNpcData.getEvent(npc.getNpcId());
            Integer page = entry == null ? -1 : offlinePage(entry, OriginalNpcProgress.get(level.getServer()), level.getServer(), OriginalSenService.cachedPageThresholds(npc, npc.getNpcId()));
            if (page != null) npc.setPageIndex(page);
        }
    }

    public static boolean refreshPersonalOfflinePage(EntityOriginalNpc npc) {
        if (!(npc.level() instanceof ServerLevel level) || !OriginalSenService.hasPersonalPage(npc)) return false;
        var entry = BSOriginalNpcData.getEvent(npc.getNpcId());
        if (entry == null) return false;
        Integer selected = offlinePage(entry, OriginalNpcProgress.get(level.getServer()), level.getServer(), OriginalSenService.cachedPageThresholds(npc, npc.getNpcId()));
        if (selected != null) npc.setPageIndex(selected);
        return true;
    }

    public static void cancelForVisitReset(EntityOriginalNpc npc, String id) {
        if (id.isEmpty() || !(npc.level() instanceof ServerLevel level)) return;
        var data = OriginalNpcMovementData.get(level.getServer());
        var ticket = data.tickets.get(id);
        if (ticket == null || !ticket.target.equals(npc.getUUID())) return;
        stopMotion(npc, ticket);
        ticket.gravityCaptured = false;
        ticket.failure = "visit_reset";
        ticket.released = true;
        data.finish(ticket);
        data.collect();
    }

    static void advanceCrowdFrame(EntityOriginalNpc npc, ServerPlayer nearest, OriginalNpcMovementData data) {
        String previous = npc.forcedRouteTicket();
        refreshAutonomousPage(npc);
        if (!previous.isEmpty() && !previous.equals(npc.forcedRouteTicket())) {
            var cancelled = data.tickets.get(previous);
            if (cancelled != null && cancelled.target.equals(npc.getUUID())) {
                stopCancelledMotion(npc, cancelled);
                data.finish(cancelled);
            }
        }
        if (npc.forcedRouteTicket().isEmpty()) {
            npc.originalAutonomousRoute().advanceFrame(new RouteContext(nearest, npc, npc.originalAutonomousRoute()));
            return;
        }
        var ticket = data.tickets.get(npc.forcedRouteTicket());
        if (ticket == null || !ticket.target.equals(npc.getUUID()) || !ticket.npcId.equals(npc.getNpcId())
                || ticket.complete || !ticket.failure.isEmpty() || !ticket.replacement.isEmpty()) return;
        var level = (ServerLevel) npc.level();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ticket.owner);
        if (owner != null && (owner.serverLevel() != level || !owner.isAlive())) owner = null;
        RouteContext context = new RouteContext(owner, npc, ticket.sourceMap, ticket);
        try {
            if (!ticket.validated) { ticket.runner.validate(context); ticket.validated = true; }
            npc.advanceOriginalForcedAnimationFrame(ticket.motion != null);
            if (!ticket.runner.isComplete()) ticket.runner.advanceFrame(context);
            if (ticket.runner.isComplete()) {
                stopMotion(npc, ticket);
                data.finish(ticket);
                npc.endForcedRoute(ticket.id);
                npc.originalAutonomousRoute().advanceAfterForcedFrame(new RouteContext(nearest, npc, npc.originalAutonomousRoute()));
            }
            data.setDirty();
        } catch (RuntimeException exception) {
            ticket.failure = exception.getClass().getSimpleName() + ":" + exception.getMessage();
            data.setDirty();
            BlackSouls.LOGGER.error("Original pool crowd route {} paused at source frame {}", ticket.id, ticket.runner.elapsedFrames(), exception);
            if (owner != null) owner.sendSystemMessage(Component.literal("原作移动路线已暂停：" + ticket.failure));
        }
    }

    public static boolean isOriginalMap37Position(String identity, double x, double z) {
        return OriginalPoolCrowdSurface.isCrowd(identity) ? OriginalPoolCrowdSurface.isLowerPoolPosition(x, z)
                : identity.equals("map037_ev053") && isDodoLocalPosition(x, z);
    }

    public static boolean isDodoLocalPosition(double x, double z) {
        return x >= 821.0D && x < 886.0D && z >= -613.0D && z < -583.0D;
    }

    public static void validateAutonomousState(EntityOriginalNpc npc) {
        var motion = npc.originalAutonomousRoute().motion;
        if (motion == null) return;
        if (!npc.level().dimension().equals(yuki135.blacksouls.util.HokoniwaDestination.DIMENSION)
                || !isOriginalMap37Position(npc.getNpcId(), motion.x(), motion.z())
                || !isOriginalMap37Position(npc.getNpcId(), motion.snapshot().get("startX").getAsDouble(), motion.snapshot().get("startZ").getAsDouble())
                || !isOriginalMap37Position(npc.getNpcId(), motion.snapshot().get("targetX").getAsDouble(), motion.snapshot().get("targetZ").getAsDouble())) {
            throw new IllegalArgumentException("Invalid original autonomous movement map");
        }
    }

    public static boolean waitRequested(JsonObject route) {
        return OriginalNpcMoveRouteRunner.waitRequested(route);
    }

    public static boolean isComplete(MinecraftServer server, String ticket) {
        OriginalNpcMovementData data = OriginalNpcMovementData.get(server);
        Set<String> visited = new HashSet<>();
        String current = ticket;
        while (current != null && !current.isEmpty() && visited.add(current)) {
            OriginalNpcMovementData.Ticket route = data.tickets.get(current);
            if (route == null) return false;
            if (!route.replacement.isEmpty()) {
                current = route.replacement;
            } else {
                return route.complete && route.failure.isEmpty();
            }
        }
        return false;
    }

    public static void release(MinecraftServer server, String ticket) {
        OriginalNpcMovementData data = OriginalNpcMovementData.get(server);
        OriginalNpcMovementData.Ticket route = data.tickets.get(ticket);
        if (route == null || route.released || !isComplete(server, ticket)) return;
        route.released = true;
        data.setDirty();
        data.collect();
    }

    public static boolean refreshPageWhileForcing(EntityOriginalNpc npc) {
        if (npc.forcedRouteTicket().isEmpty() || !(npc.level() instanceof ServerLevel level)) return false;
        OriginalNpcMovementData.Ticket ticket = OriginalNpcMovementData.get(level.getServer()).tickets.get(npc.forcedRouteTicket());
        if (ticket == null || !ticket.target.equals(npc.getUUID())) return true;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ticket.owner);
        if (owner != null && owner.serverLevel() == level && owner.isAlive()) {
            OriginalNpcService.refreshPage(npc, owner);
        } else {
            BSOriginalNpcData.Entry entry = BSOriginalNpcData.getEvent(npc.getNpcId());
            if (entry != null) {
                Integer selected = offlinePage(entry, OriginalNpcProgress.get(level.getServer()), level.getServer(), OriginalSenService.cachedPageThresholds(npc, npc.getNpcId()));
                if (selected != null) npc.setPageIndex(selected);
            }
        }
        return true;
    }

    private static Integer offlinePage(BSOriginalNpcData.Entry entry, OriginalNpcProgress progress, MinecraftServer server, Integer senMask) {
        for (int index = entry.pages().size() - 1; index >= 0; index--) {
            JsonObject condition = entry.pages().get(index).condition();
            if (flag(condition, "switch1_valid") && yuki135.blacksouls.util.OriginalCovenantData.forSwitch(condition.get("switch1_id").getAsInt()) != null
                    || flag(condition, "switch2_valid") && yuki135.blacksouls.util.OriginalCovenantData.forSwitch(condition.get("switch2_id").getAsInt()) != null) return null;
            if (flag(condition, "switch1_valid") && !offlineSwitch(progress, condition.get("switch1_id").getAsInt(), senMask)) continue;
            if (flag(condition, "switch2_valid") && !offlineSwitch(progress, condition.get("switch2_id").getAsInt(), senMask)) continue;
            if (flag(condition, "self_switch_valid")
                    && !progress.selfSwitch(entry.id(), condition.get("self_switch_ch").getAsString())) continue;
            boolean unknown = flag(condition, "actor_valid") || flag(condition, "item_valid");
            if (flag(condition, "variable_valid")) {
                int id = condition.get("variable_id").getAsInt();
                if (id == 38 || yuki135.blacksouls.util.OriginalCovenantData.forVariable(id) != null) {
                    unknown = true;
                } else {
                    long value = id == 11 ? SceneSpawnerBossData.get(server).getStoryProgress() : progress.variable(id);
                    if (value < condition.get("variable_value").getAsLong()) continue;
                }
            }
            if (unknown) return null;
            return index;
        }
        return -1;
    }

    private static boolean offlineSwitch(OriginalNpcProgress progress, int id, Integer senMask) {
        return senMask != null && OriginalSenService.isThresholdSwitch(id)
                ? (senMask & 1 << (id - 14)) != 0 : progress.switchValue(id);
    }

    private static boolean flag(JsonObject object, String key) {
        return object.has(key) && object.get(key).getAsBoolean();
    }

    public static void tick(MinecraftServer server) {
        OriginalNpcMovementData data = OriginalNpcMovementData.get(server);
        int tick = server.getTickCount();
        if (data.lastServerTick == tick) return;
        data.lastServerTick = tick;
        boolean collect = false;
        for (String id : List.copyOf(data.activeTickets)) {
            OriginalNpcMovementData.Ticket ticket = data.tickets.get(id);
            if (ticket == null || ticket.complete || !ticket.replacement.isEmpty()) {
                data.activeTickets.remove(id);
                continue;
            }
            if (!ticket.failure.isEmpty()) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(ticket.owner);
            ResourceLocation dimension = ResourceLocation.tryParse(ticket.dimension);
            ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level == null) continue;
            Entity subject = owner != null && ticket.target.equals(owner.getUUID()) ? owner : level.getEntity(ticket.target);
            if (subject == null || subject.isRemoved() || !subject.isAlive()) continue;
            if (subject instanceof yuki135.mmdskin.render.entity.CustomEntityPoseProvider pose && pose.isSitting()) continue;
            if (subject instanceof EntityOriginalNpc npc) {
                if (OriginalPoolCrowdMovementService.usesSharedScheduler(npc)) continue;
                if (!npc.getNpcId().equals(ticket.npcId)) continue;
                refreshPageWhileForcing(npc);
                if (!npc.forcedRouteTicket().equals(ticket.id)) {
                    stopCancelledMotion(npc, ticket);
                    data.finish(ticket);
                    collect = true;
                    continue;
                }
            } else if (subject instanceof EntityCheshireCat cat) {
                if (!OriginalNpcCharacterBindings.matches(cat, ticket.npcId)) continue;
                cat.refreshOriginalRoutePage();
                if (!cat.forcedRouteTicket().equals(ticket.id)) {
                    stopMotion(subject, ticket);
                    data.finish(ticket);
                    collect = true;
                    continue;
                }
            } else if (subject instanceof EntityRabbitKnight rabbit) {
                if (!OriginalRabbitKnightBinding.SOURCE_ID.equals(ticket.npcId) || !OriginalRabbitKnightBinding.matches(rabbit)) continue;
                rabbit.refreshOriginalSourcePage();
                if (!rabbit.forcedRouteTicket().equals(ticket.id)) {
                    stopMotion(subject, ticket);
                    data.finish(ticket);
                    collect = true;
                    continue;
                }
            } else if (!(subject instanceof ServerPlayer) || !ticket.npcId.isEmpty()) {
                continue;
            }
            if (owner == null || owner.serverLevel() != level || !owner.isAlive()) continue;
            RouteContext context = new RouteContext(owner, subject, ticket.sourceMap, ticket);
            var before = subject.position();
            try {
                if (!ticket.validated) {
                    ticket.runner.validate(context);
                    ticket.validated = true;
                }
                for (int frame = 0; frame < 3; frame++) {
                    if (subject instanceof EntityOriginalNpc npc) {
                        refreshPageWhileForcing(npc);
                        npc.advanceOriginalForcedAnimationFrame(ticket.motion != null);
                        if (!npc.forcedRouteTicket().equals(ticket.id)) {
                            stopCancelledMotion(npc, ticket);
                            data.finish(ticket);
                            collect = true;
                            break;
                        }
                    } else if (subject instanceof EntityCheshireCat cat) {
                        cat.refreshOriginalRoutePage();
                        if (!cat.forcedRouteTicket().equals(ticket.id)) {
                            stopMotion(subject, ticket);
                            data.finish(ticket);
                            collect = true;
                            break;
                        }
                    }
                    if (subject instanceof EntityRabbitKnight rabbit) {
                        rabbit.refreshOriginalSourcePage();
                        if (!rabbit.forcedRouteTicket().equals(ticket.id)) {
                            stopMotion(subject, ticket);
                            data.finish(ticket);
                            collect = true;
                            break;
                        }
                    }
                    if (!ticket.runner.isComplete()) ticket.runner.advanceFrame(context);
                    if (ticket.runner.isComplete() && subject instanceof EntityOriginalNpc npc && npc.hasOriginalAutonomy()
                            && (!OriginalPoolCrowdSurface.isCrowd(npc.getNpcId()) || OriginalPoolCrowdMovementService.usesSharedScheduler(npc))) {
                        stopMotion(subject, ticket);
                        data.finish(ticket);
                        collect = true;
                        npc.endForcedRoute(ticket.id);
                        RouteContext autonomous = new RouteContext(owner, npc, npc.originalAutonomousRoute());
                        npc.originalAutonomousRoute().advanceAfterForcedFrame(autonomous);
                        for (int remainder = frame + 1; remainder < 3; remainder++) npc.originalAutonomousRoute().advanceFrame(autonomous);
                        break;
                    }
                }
                if (ticket.runner.isComplete() && !ticket.complete) {
                    stopMotion(subject, ticket);
                    data.finish(ticket);
                    collect = true;
                    if (subject instanceof EntityOriginalNpc npc) npc.endForcedRoute(ticket.id);
                    else if (subject instanceof EntityCheshireCat cat) cat.endForcedRoute(ticket.id);
                    else if (subject instanceof EntityRabbitKnight rabbit) rabbit.endForcedRoute(ticket.id);
                }
                if (subject instanceof ServerPlayer movingPlayer && !before.equals(subject.position())) {
                    movingPlayer.connection.teleport(movingPlayer.getX(), movingPlayer.getY(), movingPlayer.getZ(), movingPlayer.getYRot(), movingPlayer.getXRot());
                }
                data.setDirty();
            } catch (RuntimeException exception) {
                ticket.failure = exception.getClass().getSimpleName() + ":" + exception.getMessage();
                data.setDirty();
                BlackSouls.LOGGER.error("Original NPC route {} paused at source frame {}", ticket.id,
                        ticket.runner.elapsedFrames(), exception);
                owner.sendSystemMessage(Component.literal("原作移动路线已暂停：" + ticket.failure));
            }
        }
        OriginalPoolCrowdMovementService.tick(server, data);
        if (collect) data.collect();
    }

    private static Entity resolve(ServerPlayer player, EntityOriginalNpc npc, int target) {
        return OriginalNpcCharacterBindings.resolveEvent(player, npc, target);
    }

    private static void stopMotion(Entity subject, OriginalNpcMovementData.Ticket ticket) {
        if (ticket.gravityCaptured) subject.setNoGravity(ticket.originalNoGravity);
        ticket.motion = null;
    }

    private static void stopCancelledMotion(EntityOriginalNpc subject, OriginalNpcMovementData.Ticket ticket) {
        if (subject.hasOriginalAutonomy() && ticket.motion != null) {
            var autonomous = subject.originalAutonomousRoute();
            autonomous.motion = ticket.motion;
            autonomous.gravityCaptured = ticket.gravityCaptured;
            autonomous.originalNoGravity = ticket.originalNoGravity;
            ticket.motion = null;
            ticket.gravityCaptured = false;
        } else {
            stopMotion(subject, ticket);
        }
    }

    private static final class RouteContext implements OriginalNpcAutonomousRoute.Context {
        private final ServerPlayer owner;
        private final Entity subject;
        private final int sourceMap;
        private final OriginalNpcMovementData.Ticket ticket;
        private final OriginalNpcAutonomousRoute autonomous;
        private boolean succeeded = true;
        private boolean symbolTouchAttempt = true;

        private RouteContext(ServerPlayer owner, Entity subject) {
            this(owner, subject, 0, null);
        }

        private RouteContext(ServerPlayer owner, Entity subject, int sourceMap, OriginalNpcMovementData.Ticket ticket) {
            this.owner = owner;
            this.subject = subject;
            this.sourceMap = sourceMap;
            this.ticket = ticket;
            this.autonomous = null;
        }

        private RouteContext(ServerPlayer owner, EntityOriginalNpc subject, OriginalNpcAutonomousRoute autonomous) {
            this.owner = owner;
            this.subject = subject;
            this.sourceMap = 37;
            this.ticket = null;
            this.autonomous = autonomous;
        }

        @Override
        public boolean nearScreen() {
            if (subject instanceof EntityOriginalNpc npc && OriginalPoolCrowdSurface.isCrowd(npc.getNpcId())) {
                var source = BSOriginalNpcData.getEvent(npc.getNpcId());
                if (source != null && source.name().contains("<uninhibited>")) return true;
            }
            return owner != null && Math.abs(owner.getX() - subject.getX()) <= 12.0D
                    && Math.abs(owner.getZ() - subject.getZ()) <= 8.0D;
        }

        @Override
        public int moveFrequency() {
            return state().getInt("MoveFrequency");
        }

        @Override
        public boolean symbolHasPlayer() { return owner != null; }

        @Override
        public boolean symbolDashMoving() {
            return owner != null && owner.isSprinting() && (owner.getDeltaMovement().horizontalDistanceSqr() > 1.0E-8D
                    || Math.abs(owner.getX() - owner.xo) + Math.abs(owner.getZ() - owner.zo) > 1.0E-6D);
        }

        @Override
        public boolean symbolInterpreterRunning() { return owner != null && OriginalNpcService.isConversing(owner); }

        @Override
        public boolean symbolErased() { return ((EntityOriginalNpc) subject).isErasedForCurrentVisit(); }

        @Override
        public boolean symbolStealth() { return owner != null && OriginalNpcSymbolService.stealthSteps(owner) > 0; }

        @Override
        public boolean symbolBattlePaused() { return owner != null && yuki135.blacksouls.combat.TurnBattleManager.isInBattle(owner); }

        private OriginalNpcMotion currentMotion() { return ticket != null ? ticket.motion : autonomous == null ? null : autonomous.motion; }
        private double logicalX() { var motion = currentMotion(); return motion == null ? subject.getX() : motion.snapshot().get("targetX").getAsDouble(); }
        private double logicalZ() { var motion = currentMotion(); return motion == null ? subject.getZ() : motion.snapshot().get("targetZ").getAsDouble(); }

        @Override
        public int symbolX() { return 885 - net.minecraft.util.Mth.floor(logicalX()); }

        @Override
        public int symbolY() { return -549 - net.minecraft.util.Mth.floor(logicalZ()); }

        @Override
        public int symbolPlayerX() { return owner == null ? Integer.MAX_VALUE / 4 : 885 - owner.blockPosition().getX(); }

        @Override
        public int symbolPlayerY() { return owner == null ? Integer.MAX_VALUE / 4 : -549 - owner.blockPosition().getZ(); }

        @Override
        public int symbolDirection() { return ((EntityOriginalNpc) subject).getSpriteDirection(); }

        @Override
        public int symbolRandom(int bound) { return ((EntityOriginalNpc) subject).getRandom().nextInt(bound); }

        @Override
        public void symbolRate(int speed, int frequency) {
            var npc = (EntityOriginalNpc) subject;
            npc.applyOriginalMoveCommand(29, List.of(new com.google.gson.JsonPrimitive(speed)));
            npc.applyOriginalMoveCommand(30, List.of(new com.google.gson.JsonPrimitive(frequency)));
        }

        @Override
        public void symbolAlert() {
            var sound = new JsonObject();
            sound.addProperty("name", "Decision1"); sound.addProperty("volume", 50); sound.addProperty("pitch", 150);
            NetworkHandler.sendToPlayer(OriginalNpcPresentationService.createPacket(owner, subject, 250, List.of(sound)), owner);
            NetworkHandler.sendToPlayer(OriginalNpcPresentationService.createPacket(owner, subject, 213,
                    List.of(new com.google.gson.JsonPrimitive(0), new com.google.gson.JsonPrimitive(1), new com.google.gson.JsonPrimitive(false))), owner);
        }

        @Override
        public boolean symbolStep(int direction) {
            return symbolStep(direction, true);
        }

        @Override
        public boolean symbolStep(int direction, boolean turnOnFailure) {
            if (!List.of(2,4,6,8).contains(direction)) throw new IllegalArgumentException("symbol_direction");
            symbolTouchAttempt = turnOnFailure;
            try {
                boolean success = tryStraight(direction, false);
                if (success || turnOnFailure) { face(direction); autonomous.resetStopCount(); }
                return success;
            } finally { symbolTouchAttempt = true; }
        }

        @Override
        public void animate(int stopCount) {
            var npc = (EntityOriginalNpc) subject;
            if (OriginalPoolCrowdSurface.isCrowd(npc.getNpcId())) npc.advanceOriginalAutonomousAnimationFrame(stopCount, isOriginalWalking(npc));
            else npc.advanceOriginalAutonomousAnimationFrame(stopCount);
        }

        @Override
        public void validate(int code, List<JsonElement> parameters) {
            if (code >= 1 && code <= 14) {
                if (sourceMap != 62 && !(sourceMap == 37 && subject instanceof EntityOriginalNpc npc
                        && npc.hasOriginalAutonomy() && subject.level().dimension().equals(yuki135.blacksouls.util.HokoniwaDestination.DIMENSION)
                        && isOriginalMap37Position(npc.getNpcId(), subject.getX(), subject.getZ())
                        && (!OriginalPoolCrowdSurface.isCrowd(npc.getNpcId()) || OriginalPoolCrowdMovementService.usesSharedScheduler(npc)))
                        || !(subject instanceof EntityOriginalNpc || subject instanceof ServerPlayer)) throw new UnsupportedOperationException("physical_route_map:" + sourceMap);
                return;
            }
            if (subject instanceof EntityCheshireCat || subject instanceof EntityRabbitKnight) {
                if (code != 0 && code != 15 && code != 37 && code != 39 && code != 40 && code != 42) {
                    throw new UnsupportedOperationException("bound_character_route_command:" + code);
                }
                return;
            }
            if (subject instanceof ServerPlayer && (code == 29 || code == 30
                    || (sourceMap == 62 || sourceMap == 37) && (code >= 16 && code <= 28 || code >= 35 && code <= 38))) return;
            if (!(subject instanceof EntityOriginalNpc) && code != 0 && code != 15 && code != 27 && code != 28 && code != 44) {
                throw new UnsupportedOperationException("player_route_sprite:" + code);
            }
            if (code == 41) {
                String name = parameters.get(0).getAsString();
                if (name.isEmpty()) return;
                ResourceLocation texture = OriginalNpcTextures.texture(OriginalNpcTextures.character(name));
                if (texture == null) throw new UnsupportedOperationException("route_character:" + name);
                String path = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
                try (InputStream stream = OriginalNpcMovementService.class.getResourceAsStream(path)) {
                    if (stream == null) throw new UnsupportedOperationException("route_texture:" + name);
                } catch (IOException exception) {
                    throw new UnsupportedOperationException("route_texture:" + name, exception);
                }
            } else if (code == 44) {
                OriginalNpcPresentationService.createPacket(owner, subject, 250, parameters);
            }
        }

        @Override
        public void apply(int code, List<JsonElement> parameters) {
            succeeded = true;
            if (code >= 1 && code <= 14) {
                move(code, parameters);
            } else if (code == 27 || code == 28) {
                OriginalNpcProgress.writeSwitch(owner, parameters.get(0).getAsInt(), code == 27);
            } else if (code == 44) {
                NetworkHandler.sendToPlayer(OriginalNpcPresentationService.createPacket(owner, subject, 250, parameters), owner);
            } else if (subject instanceof EntityOriginalNpc npc) {
                if (code == 25 || code == 26) {
                    double dx = owner.getX() - npc.getX(), dz = owner.getZ() - npc.getZ();
                    if (dx == 0 && dz == 0) return;
                    float facing = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 270.0F : 90.0F) : (dz > 0 ? 0.0F : 180.0F);
                    if (code == 26) facing += 180.0F;
                    int direction = npc.getSpriteDirection();
                    int quarter = direction == 4 ? 1 : direction == 8 ? 2 : direction == 6 ? 3 : 0;
                    int turns = Math.round(net.minecraft.util.Mth.wrapDegrees(facing - npc.getMmdBodyYaw(npc.getYRot())) / 90.0F);
                    int command = switch (Math.floorMod(quarter + turns, 4)) {
                        case 1 -> 17;
                        case 2 -> 19;
                        case 3 -> 18;
                        default -> 16;
                    };
                    npc.applyOriginalMoveCommand(command, List.of());
                } else {
                    npc.applyOriginalMoveCommand(code, parameters);
                }
            } else if (subject instanceof EntityCheshireCat cat) {
                cat.applyOriginalMoveCommand(code, parameters);
            } else if (subject instanceof EntityRabbitKnight rabbit) {
                rabbit.applyOriginalMoveCommand(code, parameters);
            } else if (subject instanceof ServerPlayer) {
                var state = playerState();
                if (code >= 16 && code <= 19) face(code == 16 ? 2 : code == 17 ? 4 : code == 18 ? 6 : 8);
                else if (code == 29) OriginalPlayerMoveSpeed.set(owner, parameters.get(0).getAsInt());
                else if (code == 30) state.putInt("MoveFrequency", parameters.get(0).getAsInt());
                else if (code == 35 || code == 36) state.putBoolean("DirectionFixed", code == 35);
                else if (code == 37 || code == 38) state.putBoolean("Through", code == 37);
                else if (code >= 20 && code <= 26) {
                    int direction = direction();
                    if (code == 24) face(2 + owner.getRandom().nextInt(4) * 2);
                    else if (code == 25 || code == 26) {
                        double dx = owner.getX() - subject.getX(), dz = owner.getZ() - subject.getZ();
                        if (dx != 0 || dz != 0) face((Math.abs(dx) > Math.abs(dz) ? dx > 0 ? 4 : 6 : dz > 0 ? 8 : 2) * (code == 26 ? -1 : 1));
                    } else {
                        int turn = code == 23 ? owner.getRandom().nextBoolean() ? 20 : 21 : code;
                        face(turn == 22 ? 10 - direction : turn == 20 ? switch (direction) {case 2 -> 4;case 4 -> 8;case 6 -> 2;default -> 6;} : switch (direction) {case 2 -> 6;case 4 -> 2;case 6 -> 8;default -> 4;});
                    }
                }
            } else {
                throw new UnsupportedOperationException("player_route_sprite:" + code);
            }
        }

        @Override
        public boolean advanceMovementFrame() {
            var motion = ticket == null ? autonomous == null ? null : autonomous.motion : ticket.motion;
            if (motion == null) return false;
            if (subject instanceof EntityOriginalNpc npc && OriginalPoolCrowdSurface.isCrowd(npc.getNpcId())) {
                motion = OriginalPoolCrowdMotion.retimeWalk(motion, state().getInt("MoveSpeed"));
                if (ticket != null) ticket.motion = motion;
                else autonomous.motion = motion;
            }
            subject.setNoGravity(true);
            motion.advanceFrame();
            subject.setPos(motion.x(), motion.y(), motion.z());
            subject.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            subject.fallDistance = 0;
            if (motion.complete()) {
                if (subject instanceof ServerPlayer player) OriginalNpcSymbolService.synchronizeStealthAnchor(player);
                if (ticket != null) stopMotion(subject, ticket);
                else {
                    subject.setNoGravity(autonomous.originalNoGravity);
                    autonomous.motion = null;
                    autonomous.gravityCaptured = false;
                }
            }
            return true;
        }

        @Override
        public boolean movementSucceeded() {
            return succeeded;
        }

        private net.minecraft.nbt.CompoundTag playerState() {
            var data = owner.getPersistentData();
            if (!data.contains("BSOriginalRouteState", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var state = new net.minecraft.nbt.CompoundTag();
                state.putInt("MoveSpeed", 4);
                state.putInt("MoveFrequency", 6);
                data.put("BSOriginalRouteState", state);
            }
            return data.getCompound("BSOriginalRouteState");
        }

        private net.minecraft.nbt.CompoundTag state() {
            return subject instanceof EntityOriginalNpc npc ? npc.originalMovementState() : playerState();
        }

        private int direction() {
            return subject instanceof EntityOriginalNpc npc ? npc.getSpriteDirection()
                    : yuki135.blacksouls.compat.scene.OriginalBillPlacement.playerDirection(subject);
        }

        private void face(int direction) {
            if (direction < 0) direction = 10 + direction;
            if (subject instanceof EntityOriginalNpc npc) {
                npc.applyOriginalMoveCommand(direction == 2 ? 16 : direction == 4 ? 17 : direction == 6 ? 18 : 19, List.of());
            } else if (!state().getBoolean("DirectionFixed")) {
                subject.setYRot(direction == 2 ? 180 : direction == 4 ? 270 : direction == 6 ? 90 : 0);
                if (subject instanceof net.minecraft.world.entity.LivingEntity living) living.setYHeadRot(subject.getYRot());
            }
        }

        private void move(int code, List<JsonElement> parameters) {
            if (ticket == null && autonomous == null) throw new IllegalStateException("Missing physical movement ticket");
            int dx = 0, dy = 0;
            if (code == 14) {
                dx = parameters.get(0).getAsInt();dy = parameters.get(1).getAsInt();
                if (Math.abs(dx) > Math.abs(dy)) face(dx < 0 ? 4 : 6);
                else if (dy != 0) face(dy < 0 ? 8 : 2);
            }
            else {
                int direction = code == 9 ? 2 + (subject instanceof EntityOriginalNpc npc ? npc.getRandom() : owner.getRandom()).nextInt(4) * 2 : direction();
                if (code >= 1 && code <= 4) direction = code == 1 ? 2 : code == 2 ? 4 : code == 3 ? 6 : 8;
                if (code == 10 || code == 11) {
                    double wx = owner.getX() - subject.getX(), wz = owner.getZ() - subject.getZ();
                    if (wx == 0 && wz == 0) return;
                    int horizontal = wx > 0 ? 4 : 6, vertical = wz > 0 ? 8 : 2;
                    if (code == 11) {horizontal = 10 - horizontal;vertical = 10 - vertical;}
                    direction = Math.abs(wx) > Math.abs(wz) ? horizontal : vertical;
                    if (tryStraight(direction, true)) return;
                    if (Math.abs(wx) > Math.abs(wz) && wz != 0) tryStraight(vertical, true);
                    else if (Math.abs(wx) <= Math.abs(wz) && wx != 0) tryStraight(horizontal, true);
                    return;
                }
                if (code == 13) direction = 10 - direction;
                if (code >= 5 && code <= 8) {
                    dx = code == 5 || code == 7 ? -1 : 1;dy = code == 5 || code == 6 ? 1 : -1;
                    int horizontal = dx < 0 ? 4 : 6, vertical = dy < 0 ? 8 : 2;
                    if (direction() == 10 - horizontal) face(horizontal);
                    if (direction() == 10 - vertical) face(vertical);
                    if (!state().getBoolean("Through") && !clear(dx, 0) && !clear(0, dy)) {succeeded = false;return;}
                } else {
                    boolean success = tryStraight(direction, code != 13 && code != 9);
                    if (success && code == 9) face(direction);
                    return;
                }
            }
            startMotion(dx, dy, code == 14);
        }

        private boolean clear(int dx, int dy) {
            return subject.level().noCollision(subject, subject.getBoundingBox().move(-dx, 0, -dy));
        }

        private boolean tryStraight(int direction, boolean turn) {
            if (turn) face(direction);
            return startMotion(direction == 4 ? -1 : direction == 6 ? 1 : 0, direction == 2 ? 1 : direction == 8 ? -1 : 0, false);
        }

        private boolean startMotion(int dx, int dy, boolean jump) {
            double tx = subject.getX() - dx, tz = subject.getZ() - dy;
            if (sourceMap == 37 && (!(subject instanceof EntityOriginalNpc npc)
                    || !isOriginalMap37Position(npc.getNpcId(), subject.getX(), subject.getZ()) || !isOriginalMap37Position(npc.getNpcId(), tx, tz))) {
                throw new UnsupportedOperationException("physical_route_map37_local_bounds");
            }
            if (!jump && subject instanceof EntityOriginalNpc npc && npc.originalAutonomousRoute().symbol() != null
                    && npc.originalAutonomousRoute().symbol().enabled() && !state().getBoolean("Through") && owner != null
                    && owner.blockPosition().getX() == net.minecraft.util.Mth.floor(tx)
                    && owner.blockPosition().getZ() == net.minecraft.util.Mth.floor(tz)
                    && Math.abs(owner.getY() - subject.getY()) < 1.0D) {
                succeeded = false;
                if (symbolTouchAttempt) {
                    face(dx < 0 ? 4 : dx > 0 ? 6 : dy < 0 ? 8 : 2);
                    npc.originalAutonomousRoute().resetStopCount();
                    OriginalNpcSymbolService.tryContact(owner, npc);
                }
                return false;
            }
            double targetY = subject.getY();
            boolean symbolMoving = !jump && subject instanceof EntityOriginalNpc npc && npc.originalAutonomousRoute().symbol() != null
                    && npc.originalAutonomousRoute().symbol().enabled() && !state().getBoolean("Through");
            if (subject instanceof EntityOriginalNpc npc && OriginalPoolCrowdSurface.isCrowd(npc.getNpcId())) {
                if (!jump) {
                    int direction = dx < 0 ? 4 : dx > 0 ? 6 : dy < 0 ? 8 : 2;
                    if (!OriginalPoolCrowdMovementService.allowsSourceStep(npc, owner, symbolX(), symbolY(), direction)) { succeeded = false; return false; }
                }
                targetY = OriginalPoolCrowdMovementService.supportHeight(npc, tx, tz);
                if (!Double.isFinite(targetY)) { succeeded = false; return false; }
            } else if (symbolMoving) {
                int direction = dx < 0 ? 4 : dx > 0 ? 6 : dy < 0 ? 8 : 2;
                if (!OriginalSymbolMap37.allows(owner, (EntityOriginalNpc) subject, symbolX(), symbolY(), direction)) {
                    succeeded = false;
                    return false;
                }
                targetY = symbolSupportY(tx, tz);
                if (!Double.isFinite(targetY)) { succeeded = false; return false; }
            } else if (!jump && !state().getBoolean("Through") && !clear(dx, dy)) {succeeded = false;return false;}
            int speed = state().getInt("MoveSpeed");
            if (speed < 1 || speed > 6) throw new UnsupportedOperationException("physical_move_speed:" + speed);
            if (!jump && subject instanceof ServerPlayer player && player.isSprinting()) speed++;
            if (ticket != null) {
                if (!ticket.gravityCaptured) {ticket.originalNoGravity = subject.isNoGravity();ticket.gravityCaptured = true;}
            } else if (!autonomous.gravityCaptured) {
                autonomous.originalNoGravity = subject.isNoGravity();
                autonomous.gravityCaptured = true;
            }
            subject.setNoGravity(true);
            OriginalNpcMotion motion = jump ? OriginalNpcMotion.jump(subject.getX(), subject.getY(), subject.getZ(), tx, targetY, tz, speed, dx, dy)
                    : OriginalNpcMotion.walk(subject.getX(), subject.getY(), subject.getZ(), tx, targetY, tz, speed);
            if (ticket != null) ticket.motion = motion;
            else autonomous.motion = motion;
            if (subject instanceof EntityOriginalNpc npc && npc.hasOriginalAutonomy()) npc.originalAutonomousRoute().resetStopCount();
            if (!jump && subject instanceof ServerPlayer player) OriginalNpcSymbolService.consumeSourceStep(player);
            succeeded = true;
            return true;
        }

        private double symbolSupportY(double x, double z) {
            var level = (ServerLevel) subject.level();
            var bounds = subject.getBoundingBox().move(x-subject.getX(),0,z-subject.getZ());
            var scan = new net.minecraft.world.phys.AABB(bounds.minX,subject.getY()-1.05D,bounds.minZ,
                    bounds.maxX,subject.getY()+0.75D,bounds.maxZ);
            double selected = Double.NaN, distance = Double.MAX_VALUE;
            for (var shape : level.getBlockCollisions(subject,scan)) for (var box : shape.toAabbs()) {
                double y = box.maxY;
                if (y < subject.getY()-1.0D || y > subject.getY()+0.75D) continue;
                var candidate = bounds.move(0,y-subject.getY(),0);
                if (!level.noCollision(subject,candidate) || level.containsAnyLiquid(candidate)
                        || level.noCollision(subject,candidate.move(0,-0.02D,0))) continue;
                double delta = Math.abs(y-subject.getY());
                if (delta < distance) { selected = y; distance = delta; }
            }
            return selected;
        }
    }
}
