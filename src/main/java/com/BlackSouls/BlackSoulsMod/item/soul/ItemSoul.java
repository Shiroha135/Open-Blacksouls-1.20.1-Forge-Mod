package com.BlackSouls.BlackSoulsMod.item.soul;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundBannerPacket;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSoul extends Item {

    private final long soulValue;
    private final String effectKey;
    private final String loreKey;

    public ItemSoul(Properties properties, long value, String effectKey) {
        super(properties);
        this.soulValue = value;
        this.effectKey = effectKey;
        this.loreKey = null;
    }

    public ItemSoul(Properties properties, long value, String effectKey, String loreKey) {
        super(properties);
        this.soulValue = value;
        this.effectKey = effectKey;
        this.loreKey = loreKey;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.effectKey).withStyle(ChatFormatting.WHITE));

        if (this.loreKey != null && !this.loreKey.isEmpty()) {
            tooltip.add(Component.translatable(this.loreKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            player.getCapability(com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                stats.souls += soulValue;

                StatEventHandler.applyStats(player);
                StatEventHandler.syncToClient(player);

                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToPlayer(
                            new ClientboundBannerPacket(ClientboundBannerPacket.Type.SOUL_GAIN, soulValue),
                            serverPlayer
                    );
                }

                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        BlackSouls.ITEM1_EVENT.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            });
        }

        return InteractionResultHolder.consume(stack);
    }
}
