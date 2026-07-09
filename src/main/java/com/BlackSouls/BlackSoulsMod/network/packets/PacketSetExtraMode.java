package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketSetExtraMode {

    private static final ItemMode[] ITEM_MODES = ItemMode.values();

    private final int modeOrdinal;
    private final boolean enabled;

    public PacketSetExtraMode(ItemMode mode, boolean enabled) {
        this.modeOrdinal = mode.ordinal();
        this.enabled = enabled;
    }

    public PacketSetExtraMode(FriendlyByteBuf buf) {
        this.modeOrdinal = buf.readInt();
        this.enabled = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.modeOrdinal);
        buf.writeBoolean(this.enabled);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.getSender();
            if (serverPlayer == null) {
                return;
            }
            if (!BSConfig.ALLOW_PLAYER_EXTRA_MODES.get() && !serverPlayer.hasPermissions(4)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.difficulty.no_permission").withStyle(ChatFormatting.RED));
                return;
            }

            ItemMode mode = ITEM_MODES[Math.max(0, Math.min(ITEM_MODES.length - 1, modeOrdinal))];
            ServerLevel overworld = serverPlayer.server.overworld();
            BSWorldData data = BSWorldData.get(overworld);

            if (!mode.isUnlocked(data)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.dev_mode.locked").withStyle(ChatFormatting.RED));
                return;
            }

            mode.setEnabled(data, enabled);
            DifficultyManager.currentDifficulty = data.difficulty;
            for (ServerLevel level : serverPlayer.server.getAllLevels()) {
                DifficultyManager.updateAllMonstersInstant(level);
            }

            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDifficulty(data));
        });
        ctx.setPacketHandled(true);
        return true;
    }

    public enum ItemMode {
        REVENGE {
            @Override
            public boolean isUnlocked(BSWorldData data) { return data.isRevengeUnlocked(); }
            @Override
            public void setEnabled(BSWorldData data, boolean enabled) { data.setRevengeMode(enabled); }
        },
        DEATH {
            @Override
            public boolean isUnlocked(BSWorldData data) { return data.isDeathUnlocked(); }
            @Override
            public void setEnabled(BSWorldData data, boolean enabled) { data.setDeathMode(enabled); }
        },
        LEGENDARY {
            @Override
            public boolean isUnlocked(BSWorldData data) { return data.isLegendaryUnlocked(); }
            @Override
            public void setEnabled(BSWorldData data, boolean enabled) { data.setLegendaryMode(enabled); }
        },
        MALICE {
            @Override
            public boolean isUnlocked(BSWorldData data) { return data.isMaliceUnlocked(); }
            @Override
            public void setEnabled(BSWorldData data, boolean enabled) { data.setMaliceMode(enabled); }
        },
        ETERNITY {
            @Override
            public boolean isUnlocked(BSWorldData data) { return data.isEternityUnlocked(); }
            @Override
            public void setEnabled(BSWorldData data, boolean enabled) { data.setEternityMode(enabled); }
        };

        public abstract boolean isUnlocked(BSWorldData data);
        public abstract void setEnabled(BSWorldData data, boolean enabled);
    }
}
