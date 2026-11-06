package yuki135.blacksouls.network.packets;

import yuki135.blacksouls.BlackSouls;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketWhiteFlash {
    private final boolean isFirstTime;

    public PacketWhiteFlash(boolean isFirstTime) { 
        this.isFirstTime = isFirstTime; 
    }
    
    public PacketWhiteFlash(FriendlyByteBuf buf) { 
        this.isFirstTime = buf.readBoolean(); 
    }
    
    public void toBytes(FriendlyByteBuf buf) { 
        buf.writeBoolean(this.isFirstTime); 
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        context.setPacketHandled(true);
        return true;
    }

    private static class ClientHandler {
        public static void handle(PacketWhiteFlash msg) {
            yuki135.blacksouls.client.render.BonfireEffectRenderer.whiteFlashTicks = 20;
            if (msg.isFirstTime) {
                yuki135.blacksouls.client.render.BonfireEffectRenderer.darkOverlayTicks = 60;
            }

            if (BlackSouls.FIRE6_EVENT.isPresent()) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(BlackSouls.FIRE6_EVENT.get(), 1.0F, 1.0F)
                );
            }
        }
    }
}