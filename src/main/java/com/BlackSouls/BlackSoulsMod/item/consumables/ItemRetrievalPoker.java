package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundSimpleActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemRetrievalPoker extends Item {

    public ItemRetrievalPoker(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);

            if (stats != null) {
                if (stats.lostSouls <= 0) {
                    player.sendSystemMessage(Component.translatable("message.blacksouls.no_lost_souls").withStyle(ChatFormatting.RED));
                    return InteractionResultHolder.fail(stack);
                }

                long recoveredAmount = stats.lostSouls;
                stats.souls += recoveredAmount;
                stats.lostSouls = 0;
                stats.lostX = 0.0;
                stats.lostY = 0.0;
                stats.lostZ = 0.0;
                stats.lostDim = "";

                if (BlackSouls.SAINT7_EVENT.isPresent()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            BlackSouls.SAINT7_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new ClientboundSimpleActionPacket(ClientboundSimpleActionPacket.Action.SHOW_RETRIEVAL_BANNER)
                );

                serverPlayer.displayClientMessage(
                        Component.translatable("message.blacksouls.death.recovered", recoveredAmount).withStyle(ChatFormatting.GOLD), true
                );

                StatEventHandler.syncToClient(player);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.retrieval_poker.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.retrieval_poker.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
