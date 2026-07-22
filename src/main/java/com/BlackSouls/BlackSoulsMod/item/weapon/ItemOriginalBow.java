package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemOriginalBow extends BowItem {
    private static final String TAG_HANS_BURSTS = "bs2_hans_machine_gun_bursts";
    private static final String TAG_HANS_TARGET = "target";
    private static final String TAG_HANS_REMAINING = "remaining";
    private static final String TAG_HANS_NEXT_HIT = "next_hit";

    public enum Profile { HUNTING, BRAVE, HANS_MACHINE_GUN, MAD_BOW_JUBJUB }

    private final Profile profile;

    public ItemOriginalBow(Profile profile, Properties properties) {
        super(properties);
        this.profile = profile;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    public double getAttackMultiplier() {
        return profile == Profile.HANS_MACHINE_GUN ? 2.0D : profile == Profile.MAD_BOW_JUBJUB ? 3.0D : 4.0D;
    }

    public double getDefenseMultiplier() {
        return profile == Profile.HANS_MACHINE_GUN ? 1.0D : 2.0D;
    }

    public void onProjectileHit(ServerPlayer player, LivingEntity target, ItemStack stack) {
        playHitEffects(player, target);
        if (profile == Profile.HANS_MACHINE_GUN) {
            int extraHits = 1 + getUpgradeLevel(stack) * 2;
            queueHansMachineGunBurst(player, target, extraHits);
        } else if (profile == Profile.MAD_BOW_JUBJUB) {
            int level = getUpgradeLevel(stack);
            int totalHits = level >= 5 ? 4 : level >= 3 ? 3 : 2;
            for (int hit = 1; hit < totalHits; hit++) {
                int delay = hit * 4;
                schedule(player, delay, () -> {
                    LivingEntity randomTarget = findRandomTarget(player, target, 12.0D);
                    if (randomTarget != null) {
                        playHitEffects(player, randomTarget);
                        StatEventHandler.performOriginalWeaponExtraHit(player, randomTarget, 3.0D, 2.0D);
                    }
                });
            }
        }
    }

    public void playHitEffects(ServerPlayer player, LivingEntity target) {
        int animationId = switch (profile) {
            case HUNTING, BRAVE -> 226;
            case HANS_MACHINE_GUN -> 92;
            case MAD_BOW_JUBJUB -> 322;
        };
        NetworkHandler.sendToAllAround(new PacketPlayAnim(animationId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), target);
        if (profile == Profile.HANS_MACHINE_GUN) {
            playSound(target, BlackSouls.GUN2_EVENT.get(), 1.0F);
        } else if (profile == Profile.MAD_BOW_JUBJUB) {
            playSound(target, BlackSouls.BOW2_EVENT.get(), 0.7F);
            playSound(target, BlackSouls.BOW1_EVENT.get(), 0.7F);
            schedule(player, 6, () -> playSound(target, BlackSouls.DAMAGE4_EVENT.get(), 0.9F));
        } else {
            playSound(target, BlackSouls.BOW4_EVENT.get(), 1.5F);
            schedule(player, 8, () -> {
                if (!target.isRemoved()) {
                    playSound(target, BlackSouls.SLASH10_EVENT.get(), 1.0F);
                    playSound(target, BlackSouls.BOW2_EVENT.get(), 0.8F);
                    playSound(target, BlackSouls.BOW1_EVENT.get(), 1.0F);
                }
            });
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore.1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (profile == Profile.BRAVE) {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.2").withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.HUNTING) {
            int upgrade = stack.hasTag() ? Math.max(0, Math.min(9, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
            int[] criticalByLevel = {5, 10, 15, 20, 25, 30, 35, 45, 55, 65};
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", criticalByLevel[upgrade]).withStyle(ChatFormatting.WHITE));
        } else if (profile == Profile.HANS_MACHINE_GUN) {
            int upgrade = getUpgradeLevel(stack);
            tooltip.add(Component.translatable(getDescriptionId() + (upgrade >= 5 ? ".lore.max" : ".lore.dynamic"), 1 + upgrade * 2).withStyle(ChatFormatting.WHITE));
        } else {
            int upgrade = getUpgradeLevel(stack);
            int attacks = upgrade >= 5 ? 4 : upgrade >= 3 ? 3 : 2;
            tooltip.add(Component.translatable(getDescriptionId() + ".lore.dynamic", attacks).withStyle(ChatFormatting.WHITE));
        }
        super.appendHoverText(stack, level, tooltip, flagIn);
    }

    private static int getUpgradeLevel(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, Math.min(5, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
    }

    private static void queueHansMachineGunBurst(ServerPlayer player, LivingEntity target, int extraHits) {
        CompoundTag playerData = StatEventHandler.getPlayerPersistentData(player);
        ListTag bursts = playerData.getList(TAG_HANS_BURSTS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        CompoundTag burst = new CompoundTag();
        burst.putUUID(TAG_HANS_TARGET, target.getUUID());
        burst.putInt(TAG_HANS_REMAINING, extraHits);
        burst.putLong(TAG_HANS_NEXT_HIT, player.serverLevel().getGameTime() + 2L);
        bursts.add(burst);
        playerData.put(TAG_HANS_BURSTS, bursts);
    }

    public static void tickHansMachineGunBursts(ServerPlayer player) {
        CompoundTag playerData = StatEventHandler.getPlayerPersistentData(player);
        if (!playerData.contains(TAG_HANS_BURSTS, net.minecraft.nbt.Tag.TAG_LIST)) return;

        ListTag bursts = playerData.getList(TAG_HANS_BURSTS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        ListTag activeBursts = new ListTag();
        long gameTime = player.serverLevel().getGameTime();
        for (int index = 0; index < bursts.size(); index++) {
            CompoundTag burst = bursts.getCompound(index);
            if (!burst.hasUUID(TAG_HANS_TARGET)) continue;

            int remainingHits = burst.getInt(TAG_HANS_REMAINING);
            if (remainingHits <= 0) continue;
            if (gameTime < burst.getLong(TAG_HANS_NEXT_HIT)) {
                activeBursts.add(burst);
                continue;
            }

            net.minecraft.world.entity.Entity entity = player.serverLevel().getEntity(burst.getUUID(TAG_HANS_TARGET));
            if (!(entity instanceof LivingEntity target) || target.isRemoved() || !target.isAlive()) continue;

            NetworkHandler.sendToAllAround(new PacketPlayAnim(92, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), target);
            playSound(target, BlackSouls.GUN2_EVENT.get(), 1.0F);
            StatEventHandler.performOriginalWeaponExtraHit(player, target, 2.0D, 1.0D);
            remainingHits--;
            if (remainingHits > 0 && target.isAlive() && !target.isRemoved()) {
                burst.putInt(TAG_HANS_REMAINING, remainingHits);
                burst.putLong(TAG_HANS_NEXT_HIT, gameTime + 2L);
                activeBursts.add(burst);
            }
        }

        if (activeBursts.isEmpty()) {
            playerData.remove(TAG_HANS_BURSTS);
        } else {
            playerData.put(TAG_HANS_BURSTS, activeBursts);
        }
    }

    private static LivingEntity findRandomTarget(ServerPlayer player, LivingEntity fallback, double range) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                living -> living != player && living.isAlive() && !living.isSpectator()
        );
        return targets.isEmpty() ? fallback : targets.get(player.getRandom().nextInt(targets.size()));
    }

    private static void schedule(ServerPlayer player, int delay, Runnable task) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay, task));
    }

    private static void playSound(LivingEntity target, net.minecraft.sounds.SoundEvent sound, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }
}
