package com.BlackSouls.BlackSoulsMod.party;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundPartyStatePacket;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyManager {
    public static final int MAX_MEMBERS = 4;
    private static final long INVITE_TICKS = 20L * 30L;
    private static final Map<UUID, Party> PARTIES = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();
    private static final Map<UUID, String> AVATARS = new HashMap<>();
    private static MinecraftServer activeServer;

    public static void invite(ServerPlayer sender, ServerPlayer target) {
        restore(sender.server);
        invite(sender, target, sender.serverLevel().getGameTime());
    }

    public static void accept(ServerPlayer joining) {
        restore(joining.server);
        Invite invite = INVITES.get(joining.getUUID());
        long now = joining.serverLevel().getGameTime();
        if (invite == null || invite.expiresAt() < now) {
            INVITES.remove(joining.getUUID());
            joining.displayClientMessage(Component.translatable("message.blacksouls.party.no_invite").withStyle(ChatFormatting.RED), false);
            return;
        }
        ServerPlayer inviter = joining.server.getPlayerList().getPlayer(invite.inviter());
        if (inviter == null) {
            INVITES.remove(joining.getUUID());
            joining.displayClientMessage(Component.translatable("message.blacksouls.party.no_invite").withStyle(ChatFormatting.RED), false);
            return;
        }
        accept(joining, inviter);
    }

    public static boolean arePartyMembers(ServerPlayer first, ServerPlayer second) {
        restore(first.server);
        Party party = PARTIES.get(first.getUUID());
        return party != null && party == PARTIES.get(second.getUUID());
    }

    public static void updateAvatar(ServerPlayer player, String avatar) {
        restore(player.server);
        AVATARS.put(player.getUUID(), sanitizeAvatar(avatar));
        Party party = PARTIES.get(player.getUUID());
        if (party != null) sync(party, player.server);
        else syncSolo(player);
    }

    public static void syncFor(ServerPlayer player, String avatar) {
        restore(player.server);
        AVATARS.put(player.getUUID(), sanitizeAvatar(avatar));
        Party party = PARTIES.get(player.getUUID());
        if (party == null) syncSolo(player);
        else sync(party, player.server);
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 10 != 0) return;
        restore(player.server);
        Party party = PARTIES.get(player.getUUID());
        if (party != null && isFirstOnline(player, party)) sync(party, player.server);
    }

    public static void login(ServerPlayer player) {
        restore(player.server);
        Party party = PARTIES.get(player.getUUID());
        if (party == null) {
            syncSolo(player);
            return;
        }
        broadcastExcept(party, player.server, player.getUUID(),
                Component.translatable("message.blacksouls.party.rejoined", player.getDisplayName()).withStyle(ChatFormatting.GREEN));
        player.displayClientMessage(Component.translatable("message.blacksouls.party.rejoined_self").withStyle(ChatFormatting.GREEN), false);
        sync(party, player.server);
    }

    public static void leave(ServerPlayer player) {
        restore(player.server);
        Party party = PARTIES.get(player.getUUID());
        if (party == null) {
            player.displayClientMessage(Component.translatable("message.blacksouls.party.not_in_party").withStyle(ChatFormatting.RED), false);
            return;
        }
        detach(player, party, true);
        syncSolo(player);
        player.displayClientMessage(Component.translatable("message.blacksouls.party.left_self").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void disconnect(ServerPlayer player) {
        UUID id = player.getUUID();
        INVITES.remove(id);
        INVITES.entrySet().removeIf(entry -> entry.getValue().inviter().equals(id));
        AVATARS.remove(id);
        Party party = PARTIES.get(id);
        if (party != null) {
            broadcastExcept(party, player.server, id,
                    Component.translatable("message.blacksouls.party.offline", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
            sync(party, player.server);
        }
    }

    private static void invite(ServerPlayer sender, ServerPlayer target, long now) {
        Party senderParty = PARTIES.get(sender.getUUID());
        if (senderParty != null && !senderParty.leader().equals(sender.getUUID())) {
            sender.displayClientMessage(Component.translatable("message.blacksouls.party.only_leader").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (senderParty != null && senderParty.members().size() >= MAX_MEMBERS) {
            sender.displayClientMessage(Component.translatable("message.blacksouls.party.full").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (arePartyMembers(sender, target)) {
            sender.displayClientMessage(Component.translatable("message.blacksouls.party.already_member").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        INVITES.put(target.getUUID(), new Invite(sender.getUUID(), now + INVITE_TICKS));
        sender.displayClientMessage(Component.translatable("message.blacksouls.party.invite_sent", target.getDisplayName()).withStyle(ChatFormatting.GREEN), false);
        Component accept = Component.translatable("message.blacksouls.party.accept_button")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept")));
        target.displayClientMessage(Component.translatable("message.blacksouls.party.invited", sender.getDisplayName()).withStyle(ChatFormatting.GOLD).append(" ").append(accept), false);
    }

    private static void accept(ServerPlayer joining, ServerPlayer inviter) {
        Party targetParty = PARTIES.get(joining.getUUID());
        if (targetParty != null) {
            joining.displayClientMessage(Component.translatable("message.blacksouls.party.already_in_party").withStyle(ChatFormatting.RED), true);
            return;
        }
        Party party = PARTIES.get(inviter.getUUID());
        if (party == null) {
            Set<UUID> members = new LinkedHashSet<>();
            members.add(inviter.getUUID());
            party = new Party(UUID.randomUUID(), inviter.getUUID(), members);
        }
        if (!party.leader().equals(inviter.getUUID()) || party.members().size() >= MAX_MEMBERS) {
            joining.displayClientMessage(Component.translatable("message.blacksouls.party.full").withStyle(ChatFormatting.RED), true);
            return;
        }
        party.members().add(joining.getUUID());
        for (UUID member : party.members()) PARTIES.put(member, party);
        persist(joining.server);
        INVITES.remove(joining.getUUID());
        broadcast(party, joining.server, Component.translatable("message.blacksouls.party.joined", joining.getDisplayName()).withStyle(ChatFormatting.GREEN));
        sync(party, joining.server);
    }

    private static void detach(ServerPlayer leaving, Party oldParty, boolean voluntary) {
        UUID leavingId = leaving.getUUID();
        PARTIES.remove(leavingId);
        Set<UUID> remaining = new LinkedHashSet<>(oldParty.members());
        remaining.remove(leavingId);
        if (remaining.isEmpty()) {
            persist(leaving.server);
            return;
        }

        Component leftMessage = Component.translatable(
                voluntary ? "message.blacksouls.party.left" : "message.blacksouls.party.disconnected",
                leaving.getDisplayName()).withStyle(ChatFormatting.YELLOW);

        if (remaining.size() == 1) {
            UUID lastId = remaining.iterator().next();
            PARTIES.remove(lastId);
            ServerPlayer last = leaving.server.getPlayerList().getPlayer(lastId);
            if (last != null) {
                last.displayClientMessage(leftMessage, false);
                last.displayClientMessage(Component.translatable("message.blacksouls.party.disbanded").withStyle(ChatFormatting.YELLOW), false);
                syncSolo(last);
            }
            persist(leaving.server);
            return;
        }

        UUID leader = oldParty.leader().equals(leavingId) ? remaining.iterator().next() : oldParty.leader();
        Party party = new Party(oldParty.id(), leader, remaining);
        for (UUID member : remaining) PARTIES.put(member, party);
        broadcast(party, leaving.server, leftMessage);
        if (oldParty.leader().equals(leavingId)) {
            ServerPlayer newLeader = leaving.server.getPlayerList().getPlayer(leader);
            if (newLeader != null) {
                broadcast(party, leaving.server, Component.translatable("message.blacksouls.party.new_leader", newLeader.getDisplayName()).withStyle(ChatFormatting.GOLD));
            }
        }
        sync(party, leaving.server);
        persist(leaving.server);
    }

    private static void broadcast(Party party, MinecraftServer server, Component message) {
        for (UUID id : party.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) player.displayClientMessage(message, false);
        }
    }

    private static void broadcastExcept(Party party, MinecraftServer server, UUID excluded, Component message) {
        for (UUID id : party.members()) {
            if (id.equals(excluded)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) player.displayClientMessage(message, false);
        }
    }

    private static boolean isFirstOnline(ServerPlayer player, Party party) {
        for (UUID id : party.members()) {
            ServerPlayer online = player.server.getPlayerList().getPlayer(id);
            if (online != null) return id.equals(player.getUUID());
        }
        return false;
    }

    private static void restore(MinecraftServer server) {
        if (activeServer != server) {
            PARTIES.clear();
            INVITES.clear();
            AVATARS.clear();
            activeServer = server;
        }
        for (PartySavedData.Entry entry : PartySavedData.get(server).parties()) {
            if (entry.members().stream().anyMatch(PARTIES::containsKey)) continue;
            Party party = new Party(entry.id(), entry.leader(), new LinkedHashSet<>(entry.members()));
            for (UUID member : party.members()) PARTIES.put(member, party);
        }
    }

    private static void persist(MinecraftServer server) {
        List<PartySavedData.Entry> entries = PARTIES.values().stream()
                .distinct()
                .filter(party -> party.members().size() >= 2)
                .map(party -> new PartySavedData.Entry(party.id(), party.leader(), party.members()))
                .toList();
        PartySavedData.get(server).replace(entries);
    }

    private static void syncSolo(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new ClientboundPartyStatePacket(List.of(snapshot(player, true))), player);
    }

    private static void sync(Party party, MinecraftServer server) {
        List<ClientboundPartyStatePacket.Member> members = new ArrayList<>();
        for (UUID id : party.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) members.add(snapshot(player, party.leader().equals(id)));
        }
        ClientboundPartyStatePacket packet = new ClientboundPartyStatePacket(members);
        for (UUID id : party.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) NetworkHandler.sendToPlayer(packet, player);
        }
    }

    private static ClientboundPartyStatePacket.Member snapshot(ServerPlayer player, boolean leader) {
        String name = StoryNameData.isConfirmed(player) ? StoryNameData.get(player) : player.getGameProfile().getName();
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        double mp = stats == null ? 0.0D : stats.mp;
        double maxMp = stats == null ? 0.0D : stats.maxMp;
        int level = stats == null ? 1 : stats.level;
        return new ClientboundPartyStatePacket.Member(player.getUUID(), name,
                AVATARS.getOrDefault(player.getUUID(), "knight_face"), player.getHealth(), player.getMaxHealth(),
                mp, maxMp, level, leader);
    }

    private static String sanitizeAvatar(String avatar) {
        if (avatar == null || avatar.isBlank() || avatar.length() > 64) return "knight_face";
        String sanitized = avatar.replaceAll("[^a-z0-9_./-]", "");
        return sanitized.isBlank() ? "knight_face" : sanitized;
    }

    private record Invite(UUID inviter, long expiresAt) {}
    private record Party(UUID id, UUID leader, Set<UUID> members) {}
    private PartyManager() {}
}
