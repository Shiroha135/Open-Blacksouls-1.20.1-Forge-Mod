package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
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

public class ItemThiefsDagger extends ItemBSWeaponBase {

    public ItemThiefsDagger(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4f, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.thiefs_dagger.lore.1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.blacksouls.thiefs_dagger.lore.2")
                .withStyle(ChatFormatting.WHITE));
        super.appendHoverText(stack, level, tooltip, flagIn);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {
            PacketPlayAnim animPacket = new PacketPlayAnim(150, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());
            NetworkHandler.sendToAllAround(animPacket, target);

            playWeaponSound(target, BlackSouls.WIND7_EVENT.get());

            net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
            server.tell(new net.minecraft.server.TickTask(3, () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.SLASH1_EVENT.get());
                }
            }));
            server.tell(new net.minecraft.server.TickTask(5, () -> {
                if (!target.isRemoved()) {
                    playWeaponSound(target, BlackSouls.SLASH3_EVENT.get());
                }
            }));
            for (int tick : getExtraHitTicks()) {
                server.tell(new net.minecraft.server.TickTask(tick, () -> {
                    if (!target.isRemoved() && target.isAlive()) {
                        StatEventHandler.performDaggerExtraHit(player, target);
                    }
                }));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    protected int[] getExtraHitTicks() {
        return new int[]{3};
    }

    private void playWeaponSound(LivingEntity target, SoundEvent sound) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
