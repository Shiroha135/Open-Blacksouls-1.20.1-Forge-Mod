package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcAnimationPacket;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalAnimationData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalNpcAnimationService {
    private OriginalNpcAnimationService() {
    }

    public static String start(ServerPlayer player, EntityOriginalNpc npc, int target, int animationId, boolean wait) {
        if (animationId > 0) OriginalNpcAnimationState.validate(BSOriginalAnimationData.get(animationId), animationId);
        Entity subject = resolve(player, npc, target);
        return enqueue(player, subject, subject instanceof EntityOriginalNpc original ? original.getNpcId() : "", animationId, wait);
    }

    public static String startBoundCharacter(ServerPlayer player, String sourceId, int animationId, boolean wait) {
        if (player == null || player.isRemoved() || !player.isAlive()) {
            throw new UnsupportedOperationException("animation_source_context");
        }
        if (animationId > 0) OriginalNpcAnimationState.validate(BSOriginalAnimationData.get(animationId), animationId);
        Entity subject = OriginalNpcCharacterBindings.resolve(player.serverLevel(), sourceId);
        return enqueue(player, subject, sourceId, animationId, wait);
    }

    public static String startRabbitCharacter(ServerPlayer player, com.BlackSouls.BlackSoulsMod.entity.EntityRabbitKnight rabbit, int animationId, boolean wait) {
        if (!OriginalRabbitKnightBinding.matches(rabbit) || player.level() != rabbit.level()) throw new UnsupportedOperationException("rabbit_animation_identity");
        if (animationId > 0) OriginalNpcAnimationState.validate(BSOriginalAnimationData.get(animationId), animationId);
        return enqueue(player, rabbit, OriginalRabbitKnightBinding.SOURCE_ID, animationId, wait);
    }

    private static String enqueue(ServerPlayer player, Entity subject, String sourceId, int animationId, boolean wait) {
        String dimension = subject.level().dimension().location().toString();
        String key = dimension + ":" + subject.getUUID();
        OriginalNpcAnimationData data = OriginalNpcAnimationData.get(player.server);
        OriginalNpcAnimationData.Channel channel = data.channels.get(key);
        if (channel != null && !channel.failure.isEmpty()) {
            throw new UnsupportedOperationException("animation_channel_failed:" + channel.failure);
        }
        if (channel != null && !channel.npcId.equals(sourceId)) {
            throw new UnsupportedOperationException("animation_character_identity_changed");
        }
        if (channel == null) {
            if (animationId <= 0) return "";
            channel = new OriginalNpcAnimationData.Channel(subject.getUUID(), dimension, sourceId, player.getUUID(),
                    new OriginalNpcAnimationState());
            data.channels.put(key, channel);
        }
        channel.requester = player.getUUID();
        rememberPosition(channel, subject);
        channel.state.request(animationId);
        if (channel.state.hasWork()) data.activeChannels.add(key);
        else data.activeChannels.remove(key);
        String ticket = "";
        if (wait && animationId > 0) {
            ticket = UUID.randomUUID().toString();
            data.tickets.put(ticket, new OriginalNpcAnimationData.Ticket(ticket, player.getUUID(), key));
        }
        data.setDirty();
        data.collect();
        return ticket;
    }

    public static boolean isComplete(MinecraftServer server, String ticket) {
        OriginalNpcAnimationData data = OriginalNpcAnimationData.get(server);
        OriginalNpcAnimationData.Ticket waiting = data.tickets.get(ticket);
        if (waiting == null || waiting.released) return false;
        OriginalNpcAnimationData.Channel channel = data.channels.get(waiting.channel);
        return channel != null && channel.failure.isEmpty() && channel.state.isWaitComplete();
    }

    public static void release(MinecraftServer server, String ticket) {
        OriginalNpcAnimationData data = OriginalNpcAnimationData.get(server);
        OriginalNpcAnimationData.Ticket waiting = data.tickets.get(ticket);
        if (waiting == null || waiting.released || !isComplete(server, ticket)) return;
        waiting.released = true;
        data.setDirty();
        data.collect();
    }

    public static void forgetObserver(MinecraftServer server, UUID player) {
        OriginalNpcAnimationData data = OriginalNpcAnimationData.get(server);
        for (String key : data.activeChannels) {
            OriginalNpcAnimationData.Channel channel = data.channels.get(key);
            if (channel != null) channel.observers.remove(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        OriginalNpcAnimationData.Channel channel = trackingChannel(player.server, event.getTarget());
        if (channel == null) return;
        channel.stoppedTracking.remove(player.getUUID());
        channel.observers.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        OriginalNpcAnimationData.Channel channel = trackingChannel(player.server, event.getTarget());
        if (channel == null) return;
        channel.stoppedTracking.add(player.getUUID());
        channel.observers.remove(player.getUUID());
    }

    private static OriginalNpcAnimationData.Channel trackingChannel(MinecraftServer server, Entity target) {
        return OriginalNpcAnimationData.get(server).channels.get(target.level().dimension().location() + ":" + target.getUUID());
    }

    public static void tick(MinecraftServer server) {
        OriginalNpcAnimationData data = OriginalNpcAnimationData.get(server);
        if (data.lastServerTick == server.getTickCount()) return;
        data.lastServerTick = server.getTickCount();
        boolean collect = false;
        for (String key : List.copyOf(data.activeChannels)) {
            OriginalNpcAnimationData.Channel channel = data.channels.get(key);
            if (channel == null || !channel.state.hasWork()) {
                data.activeChannels.remove(key);
                collect = true;
                continue;
            }
            if (!channel.failure.isEmpty()) continue;
            ResourceLocation dimension = ResourceLocation.tryParse(channel.dimension);
            ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level == null) continue;
            UUID ownerId = channel.state.activeId() > 0 ? channel.activeOwner : channel.requester;
            ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
            Entity subject = channel.npcId.isEmpty() ? server.getPlayerList().getPlayer(channel.target) : level.getEntity(channel.target);
            boolean targetAvailable = subject != null && subject.level() == level && !subject.isRemoved() && subject.isAlive()
                    && (channel.npcId.isEmpty() ? subject instanceof ServerPlayer
                    : OriginalNpcCharacterBindings.matches(subject, channel.npcId));
            if (targetAvailable) rememberPosition(channel, subject);
            if (owner == null || owner.serverLevel() != level || !owner.isAlive() || !targetAvailable) {
                if (!channel.paused) {
                    channel.paused = true;
                    channel.observers.clear();
                    data.setDirty();
                }
                if (channel.state.activeId() > 0) sendUpdates(server, level, channel, false);
                continue;
            }
            try {
                if (channel.paused) {
                    channel.paused = false;
                    channel.observers.clear();
                }
                channel.state.validateActive(BSOriginalAnimationData::get);
                boolean started = false;
                for (int frame = 0; frame < 3; frame++) {
                    int finishingId = channel.state.activeId();
                    int finishingPosition = channel.state.position();
                    int finishingOffset = channel.state.elapsedFrames() + channel.state.remainingFrames();
                    OriginalNpcAnimationState.Change change = channel.state.advanceFrame(BSOriginalAnimationData::get);
                    if (change == OriginalNpcAnimationState.Change.STARTED) {
                        channel.activeOwner = channel.requester;
                        channel.playbackId = UUID.randomUUID();
                        started = true;
                        channel.observers.clear();
                        channel.listeners.clear();
                    } else if (change == OriginalNpcAnimationState.Change.ENDED) {
                        sendEnd(server, level, channel, finishingId, finishingPosition, finishingOffset);
                    }
                }
                if (channel.state.activeId() > 0) sendUpdates(server, level, channel, started);
                if (!channel.state.hasWork()) {
                    data.activeChannels.remove(key);
                    channel.activeOwner = null;
                    channel.playbackId = null;
                    channel.paused = false;
                    channel.observers.clear();
                    channel.listeners.clear();
                    collect = true;
                }
                data.setDirty();
            } catch (RuntimeException exception) {
                channel.failure = exception.getClass().getSimpleName() + ":" + exception.getMessage();
                data.setDirty();
                BlackSouls.LOGGER.error("Original NPC animation for {} paused at source offset {}", channel.target,
                        channel.state.elapsedFrames(), exception);
            }
        }
        if (collect) data.collect();
    }

    private static void sendUpdates(MinecraftServer server, ServerLevel level,
                                    OriginalNpcAnimationData.Channel channel, boolean started) {
        channel.listeners.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            return player == null || player.connection != entry.getValue() || player.serverLevel() != level;
        });
        Set<UUID> present = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() != level || player.connection == null
                    || channel.stoppedTracking.contains(player.getUUID())
                    || !player.getUUID().equals(channel.activeOwner)
                    && player.distanceToSqr(channel.x, channel.y, channel.z) > 4096.0D) continue;
            present.add(player.getUUID());
            if (started || channel.observers.get(player.getUUID()) != player.connection) {
                NetworkHandler.sendToPlayer(new ClientboundOriginalNpcAnimationPacket(channel.playbackId, channel.entityId,
                        level.dimension().location(), channel.state.activeId(), channel.state.elapsedFrames(),
                        channel.state.position(), channel.x, channel.y, channel.z, started, channel.paused), player);
                channel.observers.put(player.getUUID(), player.connection);
                channel.listeners.put(player.getUUID(), player.connection);
            }
        }
        channel.observers.keySet().removeIf(id -> !present.contains(id));
    }

    private static void sendEnd(MinecraftServer server, ServerLevel level, OriginalNpcAnimationData.Channel channel,
                                int animationId, int position, int elapsedFrames) {
        for (var listener : channel.listeners.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(listener.getKey());
            if (player != null && player.connection == listener.getValue() && player.serverLevel() == level) {
                NetworkHandler.sendToPlayer(new ClientboundOriginalNpcAnimationPacket(channel.playbackId, channel.entityId,
                        level.dimension().location(), animationId, elapsedFrames, position,
                        channel.x, channel.y, channel.z, false, false), player);
            }
        }
    }

    private static void rememberPosition(OriginalNpcAnimationData.Channel channel, Entity subject) {
        if (channel.entityId != subject.getId()) channel.observers.clear();
        channel.entityId = subject.getId();
        channel.x = subject.getX();
        channel.y = subject.getY();
        channel.z = subject.getZ();
    }

    private static Entity resolve(ServerPlayer player, EntityOriginalNpc npc, int target) {
        if (player == null || npc == null || npc.level() != player.level() || npc.isRemoved()) {
            throw new UnsupportedOperationException("animation_source_context");
        }
        if (target < 0) return player;
        if (target == 0) return npc;
        BSOriginalNpcData.Entry source = BSOriginalNpcData.get(npc.getNpcId());
        if (source == null) throw new UnsupportedOperationException("animation_source_map");
        boolean mapped = BSOriginalNpcData.values().stream()
                .anyMatch(entry -> entry.mapId() == source.mapId() && entry.eventId() == target);
        if (!mapped) throw new UnsupportedOperationException("animation_target_unmapped:" + target);
        EntityOriginalNpc nearest = null;
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof EntityOriginalNpc candidate) || candidate.isRemoved()) continue;
            BSOriginalNpcData.Entry definition = BSOriginalNpcData.get(candidate.getNpcId());
            if (definition != null && definition.mapId() == source.mapId() && definition.eventId() == target
                    && (nearest == null || npc.distanceToSqr(candidate) < npc.distanceToSqr(nearest))) nearest = candidate;
        }
        if (nearest == null) throw new UnsupportedOperationException("animation_target_unloaded:" + target);
        return nearest;
    }
}
