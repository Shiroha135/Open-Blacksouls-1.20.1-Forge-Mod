package com.BlackSouls.BlackSoulsMod.network;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.packets.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Optional;

@SuppressWarnings("removal")
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

    private static <MSG> void register(Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler, NetworkDirection direction) {
        INSTANCE.registerMessage(id(), type, encoder, decoder, handler, Optional.of(direction));
    }

    public static void register() {
        register(PacketSyncSkill.class, PacketSyncSkill::toBytes, PacketSyncSkill::new, PacketSyncSkill::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketSyncStats.class, PacketSyncStats::toBytes, PacketSyncStats::new, PacketSyncStats::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketSyncMana.class, PacketSyncMana::toBytes, PacketSyncMana::new, PacketSyncMana::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketSpawnDamageText.class, PacketSpawnDamageText::toBytes, PacketSpawnDamageText::new, PacketSpawnDamageText::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketSyncDifficulty.class, PacketSyncDifficulty::toBytes, PacketSyncDifficulty::new, PacketSyncDifficulty::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketWhiteFlash.class, PacketWhiteFlash::toBytes, PacketWhiteFlash::new, PacketWhiteFlash::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketSyncBonfireList.class, PacketSyncBonfireList::toBytes, PacketSyncBonfireList::new, PacketSyncBonfireList::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundBonfireEditorPacket.class, ClientboundBonfireEditorPacket::toBytes, ClientboundBonfireEditorPacket::new, ClientboundBonfireEditorPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketOpenDialogue.class, PacketOpenDialogue::toBytes, PacketOpenDialogue::new, PacketOpenDialogue::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketCastSkill.class, PacketCastSkill::toBytes, PacketCastSkill::new, PacketCastSkill::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketBindSkill.class, PacketBindSkill::toBytes, PacketBindSkill::new, PacketBindSkill::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketSetDifficulty.class, PacketSetDifficulty::toBytes, PacketSetDifficulty::new, PacketSetDifficulty::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketSetExtraMode.class, PacketSetExtraMode::toBytes, PacketSetExtraMode::new, PacketSetExtraMode::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundNodenRewardPacket.class, ServerboundNodenRewardPacket::toBytes, ServerboundNodenRewardPacket::new, ServerboundNodenRewardPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundBannerPacket.class, ClientboundBannerPacket::toBytes, ClientboundBannerPacket::new, ClientboundBannerPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundTextBannerPacket.class, ClientboundTextBannerPacket::toBytes, ClientboundTextBannerPacket::new, ClientboundTextBannerPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundBossVictoryPacket.class, ClientboundBossVictoryPacket::toBytes, ClientboundBossVictoryPacket::new, ClientboundBossVictoryPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketClaimPurgeTaskReward.class, PacketClaimPurgeTaskReward::toBytes, PacketClaimPurgeTaskReward::new, PacketClaimPurgeTaskReward::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundSimpleActionPacket.class, ServerboundSimpleActionPacket::toBytes, ServerboundSimpleActionPacket::new, ServerboundSimpleActionPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketDevSetStats.class, PacketDevSetStats::toBytes, PacketDevSetStats::new, PacketDevSetStats::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketDevAction.class, PacketDevAction::toBytes, PacketDevAction::new, PacketDevAction::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketSetCovenant.class, PacketSetCovenant::toBytes, PacketSetCovenant::new, PacketSetCovenant::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketTeleportToBonfire.class, PacketTeleportToBonfire::toBytes, PacketTeleportToBonfire::new, PacketTeleportToBonfire::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketConvertSouls.class, PacketConvertSouls::toBytes, PacketConvertSouls::new, PacketConvertSouls::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundTradePacket.class, ServerboundTradePacket::toBytes, ServerboundTradePacket::new, ServerboundTradePacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketKillDialogueNPC.class, PacketKillDialogueNPC::toBytes, PacketKillDialogueNPC::new, PacketKillDialogueNPC::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundCheshireActionPacket.class, ServerboundCheshireActionPacket::toBytes, ServerboundCheshireActionPacket::new, ServerboundCheshireActionPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundCheshireGiftPacket.class, ServerboundCheshireGiftPacket::toBytes, ServerboundCheshireGiftPacket::new, ServerboundCheshireGiftPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundSimpleActionPacket.class, ClientboundSimpleActionPacket::toBytes, ClientboundSimpleActionPacket::new, ClientboundSimpleActionPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketUpdateBonfireName.class, PacketUpdateBonfireName::toBytes, PacketUpdateBonfireName::new, PacketUpdateBonfireName::handle, NetworkDirection.PLAY_TO_SERVER);
        register(PacketPlayAnim.class, PacketPlayAnim::toBytes, PacketPlayAnim::new, PacketPlayAnim::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketSyncUnlockedAvatars.class, PacketSyncUnlockedAvatars::toBytes, PacketSyncUnlockedAvatars::new, PacketSyncUnlockedAvatars::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(PacketOpenWhiteBearDialogue.class, PacketOpenWhiteBearDialogue::toBytes, PacketOpenWhiteBearDialogue::new, PacketOpenWhiteBearDialogue::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundWhiteBearActionPacket.class, ServerboundWhiteBearActionPacket::toBytes, ServerboundWhiteBearActionPacket::new, ServerboundWhiteBearActionPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundTurnBattlePacket.class, ClientboundTurnBattlePacket::toBytes, ClientboundTurnBattlePacket::new, ClientboundTurnBattlePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundTurnBattleActionPacket.class, ServerboundTurnBattleActionPacket::toBytes, ServerboundTurnBattleActionPacket::new, ServerboundTurnBattleActionPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundTurnBattlePresentationPacket.class, ServerboundTurnBattlePresentationPacket::toBytes, ServerboundTurnBattlePresentationPacket::new, ServerboundTurnBattlePresentationPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundLostItemPacket.class, ClientboundLostItemPacket::toBytes, ClientboundLostItemPacket::new, ClientboundLostItemPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundPlayerSizePacket.class, ClientboundPlayerSizePacket::toBytes, ClientboundPlayerSizePacket::new, ClientboundPlayerSizePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundStoryNamePacket.class, ClientboundStoryNamePacket::toBytes, ClientboundStoryNamePacket::new, ClientboundStoryNamePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundConfirmStoryNamePacket.class, ServerboundConfirmStoryNamePacket::toBytes, ServerboundConfirmStoryNamePacket::new, ServerboundConfirmStoryNamePacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundRedHoodDialogueCompletePacket.class, ServerboundRedHoodDialogueCompletePacket::toBytes, ServerboundRedHoodDialogueCompletePacket::new, ServerboundRedHoodDialogueCompletePacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundSetRedHoodAnimationPacket.class, ServerboundSetRedHoodAnimationPacket::toBytes, ServerboundSetRedHoodAnimationPacket::new, ServerboundSetRedHoodAnimationPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundOpenFogGatePromptPacket.class, ClientboundOpenFogGatePromptPacket::toBytes, ClientboundOpenFogGatePromptPacket::new, ClientboundOpenFogGatePromptPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundFogGateProceedPacket.class, ServerboundFogGateProceedPacket::toBytes, ServerboundFogGateProceedPacket::new, ServerboundFogGateProceedPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundFogGateWalkPacket.class, ClientboundFogGateWalkPacket::toBytes, ClientboundFogGateWalkPacket::new, ClientboundFogGateWalkPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundSetSceneSpawnerBoundsPacket.class, ServerboundSetSceneSpawnerBoundsPacket::toBytes, ServerboundSetSceneSpawnerBoundsPacket::new, ServerboundSetSceneSpawnerBoundsPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundCurrentScenePacket.class, ClientboundCurrentScenePacket::toBytes, ClientboundCurrentScenePacket::new, ClientboundCurrentScenePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundRequestCurrentScenePacket.class, ServerboundRequestCurrentScenePacket::toBytes, ServerboundRequestCurrentScenePacket::new, ServerboundRequestCurrentScenePacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundAdviceVisibilityPacket.class, ClientboundAdviceVisibilityPacket::toBytes, ClientboundAdviceVisibilityPacket::new, ClientboundAdviceVisibilityPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundDoorEditorPacket.class, ClientboundDoorEditorPacket::encode, ClientboundDoorEditorPacket::decode, ClientboundDoorEditorPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundSaveDoorConfigPacket.class, ServerboundSaveDoorConfigPacket::encode, ServerboundSaveDoorConfigPacket::decode, ServerboundSaveDoorConfigPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        register(ClientboundPartyStatePacket.class, ClientboundPartyStatePacket::toBytes, ClientboundPartyStatePacket::new, ClientboundPartyStatePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundPartySyncPacket.class, ServerboundPartySyncPacket::toBytes, ServerboundPartySyncPacket::new, ServerboundPartySyncPacket::handle, NetworkDirection.PLAY_TO_SERVER);

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
