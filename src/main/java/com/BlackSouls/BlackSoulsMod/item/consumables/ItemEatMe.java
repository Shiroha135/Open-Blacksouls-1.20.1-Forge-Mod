package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.util.EatMeSizeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class ItemEatMe extends ItemSimpleLore {
    public ItemEatMe(Properties properties) {
        super(properties, 2);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!EatMeSizeManager.isSmall(player)) {
            EatMeSizeManager.setSmall(player, true);
            sync(player);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.15F);
            player.sendSystemMessage(Component.translatable("message.blacksouls.eat_me.shrunk").withStyle(ChatFormatting.GRAY));
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.consume(stack);
        }

        AABB normalBounds = Player.STANDING_DIMENSIONS.makeBoundingBox(player.position()).deflate(1.0E-5D);
        if (!level.noCollision(player, normalBounds)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.eat_me.no_room").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        EatMeSizeManager.setSmall(player, false);
        sync(player);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 0.9F);
        player.sendSystemMessage(Component.translatable("message.blacksouls.eat_me.restored").withStyle(ChatFormatting.GRAY));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.consume(stack);
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            EatMeSizeManager.sync(serverPlayer);
        }
    }
}
