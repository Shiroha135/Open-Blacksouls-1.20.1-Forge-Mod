package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import com.BlackSouls.BlackSoulsMod.entity.EntityCheshireCat;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.entity.EntityRabbitKnight;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcTextures;
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

    public static String start(ServerPlayer player, EntityOriginalNpc npc, int target, JsonObject route) {
        OriginalNpcMoveRouteRunner runner = new OriginalNpcMoveRouteRunner(route);
        Entity subject = resolve(player, npc, target);
        RouteContext context = new RouteContext(player, subject);
        runner.validate(context);
        OriginalNpcService.refreshPage(npc, player);
        if (subject instanceof EntityOriginalNpc original && subject != npc) OriginalNpcService.refreshPage(original, player);
        return enqueue(player, subject, subject instanceof EntityOriginalNpc original ? original.getNpcId() : "", runner);
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
        OriginalNpcMovementData data = OriginalNpcMovementData.get(player.server);
        String id = UUID.randomUUID().toString();
        OriginalNpcMovementData.Ticket ticket = new OriginalNpcMovementData.Ticket(id, player.getUUID(),
                subject.getUUID(), subject.level().dimension().location().toString(),
                sourceId, runner);
        ticket.validated = true;
        data.add(ticket);
        if (subject instanceof EntityOriginalNpc original) original.beginForcedRoute(id);
        else if (subject instanceof EntityCheshireCat cat) cat.beginForcedRoute(id);
        else if (subject instanceof EntityRabbitKnight rabbit) rabbit.beginForcedRoute(id);
        data.setDirty();
        return id;
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
            BSOriginalNpcData.Entry entry = BSOriginalNpcData.get(npc.getNpcId());
            if (entry != null) {
                Integer selected = offlinePage(entry, OriginalNpcProgress.get(level.getServer()), level.getServer());
                if (selected != null) npc.setPageIndex(selected);
            }
        }
        return true;
    }

    private static Integer offlinePage(BSOriginalNpcData.Entry entry, OriginalNpcProgress progress, MinecraftServer server) {
        for (int index = entry.pages().size() - 1; index >= 0; index--) {
            JsonObject condition = entry.pages().get(index).condition();
            if (flag(condition, "switch1_valid") && !progress.switchValue(condition.get("switch1_id").getAsInt())) continue;
            if (flag(condition, "switch2_valid") && !progress.switchValue(condition.get("switch2_id").getAsInt())) continue;
            if (flag(condition, "self_switch_valid")
                    && !progress.selfSwitch(entry.id(), condition.get("self_switch_ch").getAsString())) continue;
            boolean unknown = flag(condition, "actor_valid") || flag(condition, "item_valid");
            if (flag(condition, "variable_valid")) {
                int id = condition.get("variable_id").getAsInt();
                if (id == 38) {
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
            if (subject instanceof EntityOriginalNpc npc) {
                if (!npc.getNpcId().equals(ticket.npcId)) continue;
                refreshPageWhileForcing(npc);
                if (!npc.forcedRouteTicket().equals(ticket.id)) {
                    data.finish(ticket);
                    collect = true;
                    continue;
                }
            } else if (subject instanceof EntityCheshireCat cat) {
                if (!OriginalNpcCharacterBindings.matches(cat, ticket.npcId)) continue;
                cat.refreshOriginalRoutePage();
                if (!cat.forcedRouteTicket().equals(ticket.id)) {
                    data.finish(ticket);
                    collect = true;
                    continue;
                }
            } else if (subject instanceof EntityRabbitKnight rabbit) {
                if (!OriginalRabbitKnightBinding.SOURCE_ID.equals(ticket.npcId) || !OriginalRabbitKnightBinding.matches(rabbit)) continue;
                rabbit.refreshOriginalSourcePage();
                if (!rabbit.forcedRouteTicket().equals(ticket.id)) {
                    data.finish(ticket);
                    collect = true;
                    continue;
                }
            } else if (!(subject instanceof ServerPlayer) || !ticket.npcId.isEmpty()) {
                continue;
            }
            if (owner == null || owner.serverLevel() != level || !owner.isAlive()) continue;
            RouteContext context = new RouteContext(owner, subject);
            try {
                if (!ticket.validated) {
                    ticket.runner.validate(context);
                    ticket.validated = true;
                }
                for (int frame = 0; frame < 3; frame++) {
                    if (subject instanceof EntityOriginalNpc npc) {
                        refreshPageWhileForcing(npc);
                        npc.advanceOriginalAnimationFrame();
                        if (!npc.forcedRouteTicket().equals(ticket.id)) {
                            data.finish(ticket);
                            collect = true;
                            break;
                        }
                    } else if (subject instanceof EntityCheshireCat cat) {
                        cat.refreshOriginalRoutePage();
                        if (!cat.forcedRouteTicket().equals(ticket.id)) {
                            data.finish(ticket);
                            collect = true;
                            break;
                        }
                    }
                    if (subject instanceof EntityRabbitKnight rabbit) {
                        rabbit.refreshOriginalSourcePage();
                        if (!rabbit.forcedRouteTicket().equals(ticket.id)) {
                            data.finish(ticket);
                            collect = true;
                            break;
                        }
                    }
                    if (!ticket.runner.isComplete()) ticket.runner.advanceFrame(context);
                }
                if (ticket.runner.isComplete()) {
                    data.finish(ticket);
                    collect = true;
                    if (subject instanceof EntityOriginalNpc npc) npc.endForcedRoute(ticket.id);
                    else if (subject instanceof EntityCheshireCat cat) cat.endForcedRoute(ticket.id);
                    else if (subject instanceof EntityRabbitKnight rabbit) rabbit.endForcedRoute(ticket.id);
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
        if (collect) data.collect();
    }

    private static Entity resolve(ServerPlayer player, EntityOriginalNpc npc, int target) {
        if (target < -1 || npc == null || npc.isRemoved() || npc.level() != player.level()) {
            throw new UnsupportedOperationException("route_target:" + target);
        }
        if (target == -1) return player;
        if (target == 0) return npc;
        BSOriginalNpcData.Entry source = BSOriginalNpcData.get(npc.getNpcId());
        if (source == null) throw new UnsupportedOperationException("route_source_map");
        EntityOriginalNpc nearest = null;
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof EntityOriginalNpc candidate) || candidate.isRemoved()) continue;
            BSOriginalNpcData.Entry definition = BSOriginalNpcData.get(candidate.getNpcId());
            if (definition != null && definition.mapId() == source.mapId() && definition.eventId() == target
                    && (nearest == null || npc.distanceToSqr(candidate) < npc.distanceToSqr(nearest))) {
                nearest = candidate;
            }
        }
        if (nearest == null) throw new UnsupportedOperationException("route_target_unloaded:" + target);
        return nearest;
    }

    private static final class RouteContext implements OriginalNpcMoveRouteRunner.Context {
        private final ServerPlayer owner;
        private final Entity subject;

        private RouteContext(ServerPlayer owner, Entity subject) {
            this.owner = owner;
            this.subject = subject;
        }

        @Override
        public void validate(int code, List<JsonElement> parameters) {
            if (subject instanceof EntityCheshireCat || subject instanceof EntityRabbitKnight) {
                if (code != 0 && code != 15 && code != 37 && code != 39 && code != 40 && code != 42) {
                    throw new UnsupportedOperationException("bound_character_route_command:" + code);
                }
                return;
            }
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
            if (code == 27 || code == 28) {
                OriginalNpcProgress.get(owner.server).setSwitch(parameters.get(0).getAsInt(), code == 27);
            } else if (code == 44) {
                NetworkHandler.sendToPlayer(OriginalNpcPresentationService.createPacket(owner, subject, 250, parameters), owner);
            } else if (subject instanceof EntityOriginalNpc npc) {
                npc.applyOriginalMoveCommand(code, parameters);
            } else if (subject instanceof EntityCheshireCat cat) {
                cat.applyOriginalMoveCommand(code, parameters);
            } else if (subject instanceof EntityRabbitKnight rabbit) {
                rabbit.applyOriginalMoveCommand(code, parameters);
            } else {
                throw new UnsupportedOperationException("player_route_sprite:" + code);
            }
        }
    }
}
