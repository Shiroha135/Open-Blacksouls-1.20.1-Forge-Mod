package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("DataFlowIssue") 
public class PacketDevSetStats {
    private static final double MAX_ABSOLUTE_BONUS = 1_000_000_000.0D;

    public int level;
    public double bonusHp, bonusMp, bonusAtk, bonusDef, bonusMagicAttack, bonusMagicDefense, bonusLuck, bonusSpeed;
    public long souls;
    public int sen;

    public PacketDevSetStats() {} 

    public PacketDevSetStats(int level, double hp, double mp, double atk, double def, double mAtk, double mDef, double luck, double speed, long souls, int sen) {
        this.level = level;
        this.bonusHp = hp;
        this.bonusMp = mp;
        this.bonusAtk = atk;
        this.bonusDef = def;
        this.bonusMagicAttack = mAtk;
        this.bonusMagicDefense = mDef;
        this.bonusLuck = luck;
        this.bonusSpeed = speed;
        this.souls = souls;
        this.sen = sen;
    }

    public PacketDevSetStats(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        this.bonusHp = buf.readDouble();
        this.bonusMp = buf.readDouble();
        this.bonusAtk = buf.readDouble();
        this.bonusDef = buf.readDouble();
        this.bonusMagicAttack = buf.readDouble();
        this.bonusMagicDefense = buf.readDouble();
        this.bonusLuck = buf.readDouble();
        this.bonusSpeed = buf.readDouble();
        this.souls = buf.readLong();
        this.sen = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeDouble(bonusHp);
        buf.writeDouble(bonusMp);
        buf.writeDouble(bonusAtk);
        buf.writeDouble(bonusDef);
        buf.writeDouble(bonusMagicAttack);
        buf.writeDouble(bonusMagicDefense);
        buf.writeDouble(bonusLuck);
        buf.writeDouble(bonusSpeed);
        buf.writeLong(souls);
        buf.writeInt(sen);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                if (!player.isCreative() || !player.hasPermissions(4)) {
                    player.sendSystemMessage(Component.translatable("message.blacksouls.dev.no_permission").withStyle(ChatFormatting.RED));
                    return;
                }
                if (!hasValidValues()) {
                    return;
                }

                BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);

                if (stats != null) {
                    stats.level = level;
                    stats.bonusHp = bonusHp;
                    stats.bonusMp = bonusMp;
                    stats.bonusAtk = bonusAtk;
                    stats.bonusDef = bonusDef;
                    stats.bonusMatk = bonusMagicAttack;
                    stats.bonusMdef = bonusMagicDefense;
                    stats.bonusLuc = bonusLuck;
                    stats.bonusSpeed = bonusSpeed; 
                    stats.souls = souls;        
                    stats.sen = sen;            

                    stats.recalculateStats();
                    player.setHealth(player.getMaxHealth());
                    stats.mp = stats.maxMp;

                    StatEventHandler.syncToClient(player);
                    player.sendSystemMessage(Component.translatable("message.blacksouls.dev.success").withStyle(ChatFormatting.GREEN));
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private boolean hasValidValues() {
        return level >= 1 && level <= 999 && souls >= 0L && sen >= 0
                && isValidBonus(bonusHp) && isValidBonus(bonusMp)
                && isValidBonus(bonusAtk) && isValidBonus(bonusDef)
                && isValidBonus(bonusMagicAttack) && isValidBonus(bonusMagicDefense)
                && isValidBonus(bonusLuck) && isValidBonus(bonusSpeed);
    }

    private static boolean isValidBonus(double value) {
        return Double.isFinite(value) && Math.abs(value) <= MAX_ABSOLUTE_BONUS;
    }
}
