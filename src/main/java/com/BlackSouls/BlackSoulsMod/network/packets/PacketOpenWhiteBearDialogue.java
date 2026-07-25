package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.gui.GuiWhiteBearDialogue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketOpenWhiteBearDialogue {
    private final boolean firstVisit;
    private final boolean freeSouls;
    private final int progress;

    public PacketOpenWhiteBearDialogue(boolean firstVisit, boolean freeSouls, int progress) {
        this.firstVisit = firstVisit;
        this.freeSouls = freeSouls;
        this.progress = progress;
    }

    public PacketOpenWhiteBearDialogue(FriendlyByteBuf buf) {
        this.firstVisit = buf.readBoolean();
        this.freeSouls = buf.readBoolean();
        this.progress = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.firstVisit);
        buf.writeBoolean(this.freeSouls);
        buf.writeVarInt(this.progress);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.minecraft.client.Minecraft.getInstance().setScreen(
                        new GuiWhiteBearDialogue(this.firstVisit, this.freeSouls, this.progress)
                )
        ));
        context.setPacketHandled(true);
    }
}
