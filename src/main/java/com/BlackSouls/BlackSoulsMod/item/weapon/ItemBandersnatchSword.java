package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemBandersnatchSword extends ItemBSWeaponBase {

    public ItemBandersnatchSword(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {
            NetworkHandler.sendToAllAround(new PacketPlayAnim(
                    314,
                    target.getX(),
                    target.getY() + target.getBbHeight() / 2.0F,
                    target.getZ()
            ), target);
            playSound(target, BlackSouls.SLASH9_EVENT.get(), 1.0F);
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(3, () -> {
                if (!target.isRemoved()) {
                    playSound(target, BlackSouls.EARTH1_EVENT.get(), 1.0F);
                    playSound(target, BlackSouls.EARTH5_EVENT.get(), 1.0F);
                }
            }));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private static void playSound(LivingEntity target, SoundEvent sound, float pitch) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.bandersnatch_sword.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        int upgradeLevel = stack.hasTag() ? stack.getTag().getInt("bs2_upgrade_level") : 0;
        tooltip.add(Component.translatable(upgradeLevel >= 5
                        ? "item.blacksouls.bandersnatch_sword.lore.2.plus5"
                        : "item.blacksouls.bandersnatch_sword.lore.2")
                .withStyle(ChatFormatting.WHITE));
        super.appendHoverText(stack, level, tooltip, flagIn);
    }
}
