package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalSkillInfoPacket;
import yuki135.blacksouls.util.OriginalSkillAcquisitionData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalSkillAcquisitionService {
    private static final Map<UUID, Long> REVISIONS = new HashMap<>();

    private OriginalSkillAcquisitionService() {
    }

    public static Presentation prepare(ServerPlayer player, int skillId, int actorOperand, boolean learned) {
        if (!OriginalNpcProgress.readSwitch(player, 22)) return null;
        OriginalSkillAcquisitionData.validate(skillId, actorOperand);
        return new Presentation(skillId, actorOperand, learned);
    }

    public static void publish(ServerPlayer player, Presentation presentation) {
        if (presentation == null) return;
        long revision = next(player);
        NetworkHandler.sendToPlayer(new ClientboundOriginalSkillInfoPacket(revision, player.level().dimension().location(),
                presentation.skillId(), presentation.actorOperand(), presentation.learned(), false), player);
    }

    public static void clear(ServerPlayer player) {
        OriginalSenService.clear(player);
        if (!REVISIONS.containsKey(player.getUUID())) return;
        NetworkHandler.sendToPlayer(new ClientboundOriginalSkillInfoPacket(next(player), player.level().dimension().location(),
                0, 0, false, true), player);
    }

    private static long next(ServerPlayer player) {
        long revision = Math.addExact(REVISIONS.getOrDefault(player.getUUID(), 0L), 1L);
        REVISIONS.put(player.getUUID(), revision);
        return revision;
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REVISIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        REVISIONS.clear();
    }

    public record Presentation(int skillId, int actorOperand, boolean learned) {
        public Presentation {
            OriginalSkillAcquisitionData.validate(skillId, actorOperand);
        }
    }
}
