package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCursingFlower extends Item {

    public ItemCursingFlower(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!player.level().isClientSide) {

            StatEventHandler.applyDefenseDown(target, 1000);
            StatEventHandler.applyMagicDefenseDown(target, 1000);

            if (player.level() instanceof ServerLevel serverLevel) {
                playDelayedSound(serverLevel, target, BlackSouls.TWINE_EVENT.get(), 0);   
                playDelayedSound(serverLevel, target, BlackSouls.TWINE_EVENT.get(), 5);   
                playDelayedSound(serverLevel, target, BlackSouls.DOWN2_EVENT.get(), 22);  
                playDelayedSound(serverLevel, target, BlackSouls.WIND1_EVENT.get(), 24);  
                playDelayedSound(serverLevel, target, BlackSouls.WIND1_EVENT.get(), 25);  
            }

            PacketPlayAnim animPacket = new PacketPlayAnim(
                    556,
                    target.getX(),
                    target.getY() + 0.05F,
                    target.getZ()
            );

            NetworkHandler.sendToAllAround(animPacket, target);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.CONSUME;
    }

    private void playDelayedSound(ServerLevel level, LivingEntity target, SoundEvent sound, int delayTicks) {
        level.getServer().tell(new TickTask(
                level.getServer().getTickCount() + delayTicks,
                () -> level.playSound(null,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F)
        ));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}