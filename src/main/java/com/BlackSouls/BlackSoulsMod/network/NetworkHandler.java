package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.packets.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BlackSouls.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    private static <MSG> void register(Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        INSTANCE.registerMessage(id(), type, encoder, decoder, handler);
    }

    public static void register() {
        register(PacketSyncSkill.class, PacketSyncSkill::toBytes, PacketSyncSkill::new, PacketSyncSkill::handle);
        register(PacketSyncStats.class, PacketSyncStats::toBytes, PacketSyncStats::new, PacketSyncStats::handle);
        register(PacketSyncMana.class, PacketSyncMana::toBytes, PacketSyncMana::new, PacketSyncMana::handle);
        register(PacketSpawnDamageText.class, PacketSpawnDamageText::toBytes, PacketSpawnDamageText::new, PacketSpawnDamageText::handle);
        register(PacketSyncDifficulty.class, PacketSyncDifficulty::toBytes, PacketSyncDifficulty::new, PacketSyncDifficulty::handle);
        register(PacketWhiteFlash.class, PacketWhiteFlash::toBytes, PacketWhiteFlash::new, PacketWhiteFlash::handle);
        register(PacketSyncBonfireList.class, PacketSyncBonfireList::toBytes, PacketSyncBonfireList::new, PacketSyncBonfireList::handle);
        register(PacketOpenDialogue.class, PacketOpenDialogue::toBytes, PacketOpenDialogue::new, PacketOpenDialogue::handle);
        register(PacketCastSkill.class, PacketCastSkill::toBytes, PacketCastSkill::new, PacketCastSkill::handle);
        register(PacketBindSkill.class, PacketBindSkill::toBytes, PacketBindSkill::new, PacketBindSkill::handle);
        register(PacketSetDifficulty.class, PacketSetDifficulty::toBytes, PacketSetDifficulty::new, PacketSetDifficulty::handle);
        register(PacketSetExtraMode.class, PacketSetExtraMode::toBytes, PacketSetExtraMode::new, PacketSetExtraMode::handle);
        register(ServerboundNodenRewardPacket.class, ServerboundNodenRewardPacket::toBytes, ServerboundNodenRewardPacket::new, ServerboundNodenRewardPacket::handle);
        register(ClientboundBannerPacket.class, ClientboundBannerPacket::toBytes, ClientboundBannerPacket::new, ClientboundBannerPacket::handle);
        register(PacketClaimPurgeTaskReward.class, PacketClaimPurgeTaskReward::toBytes, PacketClaimPurgeTaskReward::new, PacketClaimPurgeTaskReward::handle);
        register(ServerboundSimpleActionPacket.class, ServerboundSimpleActionPacket::toBytes, ServerboundSimpleActionPacket::new, ServerboundSimpleActionPacket::handle);
        register(PacketDevSetStats.class, PacketDevSetStats::toBytes, PacketDevSetStats::new, PacketDevSetStats::handle);
        register(PacketSetCovenant.class, PacketSetCovenant::toBytes, PacketSetCovenant::new, PacketSetCovenant::handle);
        register(PacketTeleportToBonfire.class, PacketTeleportToBonfire::toBytes, PacketTeleportToBonfire::new, PacketTeleportToBonfire::handle);
        register(PacketConvertSouls.class, PacketConvertSouls::toBytes, PacketConvertSouls::new, PacketConvertSouls::handle);
        register(ServerboundTradePacket.class, ServerboundTradePacket::toBytes, ServerboundTradePacket::new, ServerboundTradePacket::handle);
        register(PacketKillDialogueNPC.class, PacketKillDialogueNPC::toBytes, PacketKillDialogueNPC::new, PacketKillDialogueNPC::handle);
        register(ClientboundSimpleActionPacket.class, ClientboundSimpleActionPacket::toBytes, ClientboundSimpleActionPacket::new, ClientboundSimpleActionPacket::handle);
        register(PacketUpdateBonfireName.class, PacketUpdateBonfireName::toBytes, PacketUpdateBonfireName::new, PacketUpdateBonfireName::handle);
        register(PacketPlayAnim.class, PacketPlayAnim::toBytes, PacketPlayAnim::new, PacketPlayAnim::handle);
        register(PacketSyncUnlockedAvatars.class, PacketSyncUnlockedAvatars::toBytes, PacketSyncUnlockedAvatars::new, PacketSyncUnlockedAvatars::handle);

    }
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    public static <MSG> void sendToAllAround(MSG message, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
