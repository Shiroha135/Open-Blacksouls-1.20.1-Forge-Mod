package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemBlackAsh extends Item {

    private static final ResourceKey<Level> LIBRARY_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation("blacksouls", "library")
    );

    public ItemBlackAsh(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {

            
            if (serverPlayer.level().dimension().equals(LIBRARY_KEY)) {
                teleportToOverworldSpawn(serverPlayer);
                return InteractionResultHolder.consume(stack);
            }

            
            boolean success = teleportToLibrary(serverPlayer);
            if (!success) {
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    private boolean teleportToLibrary(ServerPlayer serverPlayer) {
        BSPlayerStats stats = serverPlayer.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);

        ServerLevel targetLevel = serverPlayer.server.getLevel(LIBRARY_KEY);

        if (targetLevel == null || stats == null) {
            serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.no_bonfire").withStyle(ChatFormatting.RED));
            return false;
        }

        
        stats.souls = 0;
        StatEventHandler.syncToClient(serverPlayer);

        serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                BlackSouls.FIRE6_EVENT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        
        double bonfireX = 5.68;
        double bonfireY = -50.0;
        double bonfireZ = 12.4;
        float targetYaw = 90.0F;

        serverPlayer.teleportTo(targetLevel, bonfireX, bonfireY, bonfireZ, targetYaw, 0.0F);

        targetLevel.playSound(
                null,
                serverPlayer.blockPosition(),
                BlackSouls.FIRE6_EVENT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.black_ash_use").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    private void teleportToOverworldSpawn(ServerPlayer serverPlayer) {
        ServerLevel overworld = serverPlayer.server.overworld();

        BlockPos spawnPos = overworld.getSharedSpawnPos();
        float spawnYaw = overworld.getSharedSpawnAngle();

        serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                BlackSouls.FIRE6_EVENT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        serverPlayer.teleportTo(
                overworld,
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                spawnYaw,
                0.0F
        );

        overworld.playSound(
                null,
                serverPlayer.blockPosition(),
                BlackSouls.FIRE6_EVENT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.blacksouls.black_ash.lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.blacksouls.black_ash.lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}