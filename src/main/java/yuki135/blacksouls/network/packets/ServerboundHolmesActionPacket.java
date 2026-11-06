package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.entity.EntityHolmes;
import yuki135.blacksouls.entity.HolmesDialogue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：福尔摩斯对话动作（推进剧情 / 杀害）。 */
public class ServerboundHolmesActionPacket {
    public enum Action {
        COMPLETE_INTRO,
        COMPLETE_WOUNDED,
        KILL;

        public static Action fromNetwork(int value) {
            return value >= 0 && value < values().length ? values()[value] : COMPLETE_INTRO;
        }
    }

    private final int entityId;
    private final Action action;

    public ServerboundHolmesActionPacket(int entityId, Action action) {
        this.entityId = entityId;
        this.action = action == null ? Action.COMPLETE_INTRO : action;
    }

    public ServerboundHolmesActionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.action = Action.fromNetwork(buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.action.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Entity target = player.level().getEntity(this.entityId);
            if (!(target instanceof EntityHolmes holmes) || !holmes.isAlive() || holmes.isRemoved()) {
                return;
            }
            switch (this.action) {
                case COMPLETE_INTRO -> holmes.completeStage(player, HolmesDialogue.Mode.INTRO);
                case COMPLETE_WOUNDED -> holmes.completeStage(player, HolmesDialogue.Mode.WOUNDED);
                case KILL -> holmes.killByPlayer(player);
            }
        });
        context.setPacketHandled(true);
    }
}
