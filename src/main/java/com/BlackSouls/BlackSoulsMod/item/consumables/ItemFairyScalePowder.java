package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemFairyScalePowder extends Item {

    public ItemFairyScalePowder(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!player.level().isClientSide) {

            
            int duration = 600 + target.getRandom().nextInt(401);
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_SLEEP.get(), duration, 0));

            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    BlackSouls.SAINT7_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    BlackSouls.SLEEP_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            PacketPlayAnim animPacket = new PacketPlayAnim(54, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ());

            NetworkHandler.sendToAllAround(animPacket, target);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
