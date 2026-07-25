package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncDifficulty {
    private int difficulty;
    private int deathCount;
    private int loopCount;
    private boolean revengeMode;
    private boolean deathMode;
    private boolean legendaryMode;
    private boolean maliceMode;
    private boolean eternityMode;
    private boolean revengeUnlocked;
    private boolean deathUnlocked;
    private boolean legendaryUnlocked;
    private boolean maliceUnlocked;
    private boolean eternityUnlocked;

    public PacketSyncDifficulty(BSWorldData data) {
        this.difficulty = data.difficulty;
        this.deathCount = data.deathCount;
        this.loopCount = data.loopCount;
        this.revengeMode = data.isRevengeMode();
        this.deathMode = data.isDeathMode();
        this.legendaryMode = data.isLegendaryMode();
        this.maliceMode = data.isMaliceMode();
        this.eternityMode = data.isEternityMode();
        this.revengeUnlocked = data.isRevengeUnlocked();
        this.deathUnlocked = data.isDeathUnlocked();
        this.legendaryUnlocked = data.isLegendaryUnlocked();
        this.maliceUnlocked = data.isMaliceUnlocked();
        this.eternityUnlocked = data.isEternityUnlocked();
    }

    public PacketSyncDifficulty(FriendlyByteBuf buf) {
        this.difficulty = buf.readInt();
        this.deathCount = buf.readInt();
        this.loopCount = buf.readInt();
        this.revengeMode = buf.readBoolean();
        this.deathMode = buf.readBoolean();
        this.legendaryMode = buf.readBoolean();
        this.maliceMode = buf.readBoolean();
        this.eternityMode = buf.readBoolean();
        this.revengeUnlocked = buf.readBoolean();
        this.deathUnlocked = buf.readBoolean();
        this.legendaryUnlocked = buf.readBoolean();
        this.maliceUnlocked = buf.readBoolean();
        this.eternityUnlocked = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.difficulty);
        buf.writeInt(this.deathCount);
        buf.writeInt(this.loopCount);
        buf.writeBoolean(this.revengeMode);
        buf.writeBoolean(this.deathMode);
        buf.writeBoolean(this.legendaryMode);
        buf.writeBoolean(this.maliceMode);
        buf.writeBoolean(this.eternityMode);
        buf.writeBoolean(this.revengeUnlocked);
        buf.writeBoolean(this.deathUnlocked);
        buf.writeBoolean(this.legendaryUnlocked);
        buf.writeBoolean(this.maliceUnlocked);
        buf.writeBoolean(this.eternityUnlocked);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DifficultyManager.currentDifficulty = this.difficulty;
            DifficultyManager.deathCount = this.deathCount;
            DifficultyManager.loopCount = this.loopCount;
            DifficultyManager.revengeMode = this.revengeMode;
            DifficultyManager.deathMode = this.deathMode;
            DifficultyManager.legendaryMode = this.legendaryMode;
            DifficultyManager.maliceMode = this.maliceMode;
            DifficultyManager.eternityMode = this.eternityMode;
            DifficultyManager.revengeUnlocked = this.revengeUnlocked;
            DifficultyManager.deathUnlocked = this.deathUnlocked;
            DifficultyManager.legendaryUnlocked = this.legendaryUnlocked;
            DifficultyManager.maliceUnlocked = this.maliceUnlocked;
            DifficultyManager.eternityUnlocked = this.eternityUnlocked;
        });
        ctx.get().setPacketHandled(true);
    }
}
