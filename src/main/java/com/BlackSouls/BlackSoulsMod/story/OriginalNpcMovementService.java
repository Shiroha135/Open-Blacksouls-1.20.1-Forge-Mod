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
        var source = BSOriginalNpcData.get(npc.getNpcId());
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
        var source = BSOriginalNpcData.get(sourceId);
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
            if (flag(condition, "switch1_valid") && com.BlackSouls.BlackSoulsMod.util.OriginalCovenantData.forSwitch(condition.get("switch1_id").getAsInt()) != null
                    || flag(condition, "switch2_valid") && com.BlackSouls.BlackSoulsMod.util.OriginalCovenantData.forSwitch(condition.get("switch2_id").getAsInt()) != null) return null;
            if (flag(condition, "switch1_valid") && !progress.switchValue(condition.get("switch1_id").getAsInt())) continue;
            if (flag(condition, "switch2_valid") && !progress.switchValue(condition.get("switch2_id").getAsInt())) continue;
            if (flag(condition, "self_switch_valid")
                    && !progress.selfSwitch(entry.id(), condition.get("self_switch_ch").getAsString())) continue;
            boolean unknown = flag(condition, "actor_valid") || flag(condition, "item_valid");
            if (flag(condition, "variable_valid")) {
                int id = condition.get("variable_id").getAsInt();
                if (id == 38 || com.BlackSouls.BlackSoulsMod.util.OriginalCovenantData.forVariable(id) != null) {
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
                    stopMotion(subject, ticket);
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
                        npc.advanceOriginalAnimationFrame();
                        if (!npc.forcedRouteTicket().equals(ticket.id)) {
                            stopMotion(subject, ticket);
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
                }
                if (ticket.runner.isComplete()) {
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
        if (collect) data.collect();
    }

    private static Entity resolve(ServerPlayer player, EntityOriginalNpc npc, int target) {
        return OriginalNpcCharacterBindings.resolveEvent(player, npc, target);
    }

    private static void stopMotion(Entity subject, OriginalNpcMovementData.Ticket ticket) {
        if (ticket.gravityCaptured) subject.setNoGravity(ticket.originalNoGravity);
        ticket.motion = null;
    }

    private static final class RouteContext implements OriginalNpcMoveRouteRunner.Context {
        private final ServerPlayer owner;
        private final Entity subject;
        private final int sourceMap;
        private final OriginalNpcMovementData.Ticket ticket;
        private boolean succeeded = true;

        private RouteContext(ServerPlayer owner, Entity subject) {
            this(owner, subject, 0, null);
        }

        private RouteContext(ServerPlayer owner, Entity subject, int sourceMap, OriginalNpcMovementData.Ticket ticket) {
            this.owner = owner;
            this.subject = subject;
            this.sourceMap = sourceMap;
            this.ticket = ticket;
        }

        @Override
        public void validate(int code, List<JsonElement> parameters) {
            if (code >= 1 && code <= 14) {
                if (sourceMap != 62 || !(subject instanceof EntityOriginalNpc || subject instanceof ServerPlayer)) throw new UnsupportedOperationException("physical_route_map:" + sourceMap);
                return;
            }
            if (subject instanceof EntityCheshireCat || subject instanceof EntityRabbitKnight) {
                if (code != 0 && code != 15 && code != 37 && code != 39 && code != 40 && code != 42) {
                    throw new UnsupportedOperationException("bound_character_route_command:" + code);
                }
                return;
            }
            if (subject instanceof ServerPlayer && sourceMap == 62 && (code >= 16 && code <= 30 || code >= 35 && code <= 38)) return;
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
            } else if (subject instanceof ServerPlayer && sourceMap == 62) {
                var state = playerState();
                if (code >= 16 && code <= 19) face(code == 16 ? 2 : code == 17 ? 4 : code == 18 ? 6 : 8);
                else if (code == 29 || code == 30) state.putInt(code == 29 ? "MoveSpeed" : "MoveFrequency", parameters.get(0).getAsInt());
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
            if (ticket == null || ticket.motion == null) return false;
            var motion = ticket.motion;
            subject.setNoGravity(true);
            motion.advanceFrame();
            subject.setPos(motion.x(), motion.y(), motion.z());
            subject.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            subject.fallDistance = 0;
            if (motion.complete()) stopMotion(subject, ticket);
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
                    : com.BlackSouls.BlackSoulsMod.compat.scene.OriginalBillPlacement.playerDirection(subject);
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
            if (ticket == null) throw new IllegalStateException("Missing physical movement ticket");
            int dx = 0, dy = 0;
            if (code == 14) {
                dx = parameters.get(0).getAsInt();dy = parameters.get(1).getAsInt();
                if (Math.abs(dx) > Math.abs(dy)) face(dx < 0 ? 4 : 6);
                else if (dy != 0) face(dy < 0 ? 8 : 2);
            }
            else {
                int direction = code == 9 ? 2 + owner.getRandom().nextInt(4) * 2 : direction();
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
                } else {tryStraight(direction, code != 13 && code != 9);return;}
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
            if (!jump && !state().getBoolean("Through") && !clear(dx, dy)) {succeeded = false;return false;}
            int speed = state().getInt("MoveSpeed");
            if (speed < 1 || speed > 6) throw new UnsupportedOperationException("physical_move_speed:" + speed);
            if (!jump && subject instanceof ServerPlayer player && player.isSprinting()) speed++;
            if (!ticket.gravityCaptured) {ticket.originalNoGravity = subject.isNoGravity();ticket.gravityCaptured = true;}
            subject.setNoGravity(true);
            ticket.motion = jump ? OriginalNpcMotion.jump(subject.getX(), subject.getY(), subject.getZ(), tx, subject.getY(), tz, speed, dx, dy)
                    : OriginalNpcMotion.walk(subject.getX(), subject.getY(), subject.getZ(), tx, subject.getY(), tz, speed);
            succeeded = true;
            return true;
        }
    }
}
