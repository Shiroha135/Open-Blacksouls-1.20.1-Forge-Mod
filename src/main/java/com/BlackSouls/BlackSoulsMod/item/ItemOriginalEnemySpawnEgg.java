package com.BlackSouls.BlackSoulsMod.item;

import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalDatabaseEnemy;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemOriginalEnemySpawnEgg extends ForgeSpawnEggItem {
    private final int profileId;

    public ItemOriginalEnemySpawnEgg(BSOriginalEnemyData.Entry profile, Properties properties) {
        super(BSEntityRegistry.ORIGINAL_ENEMY, profile.primaryColor(), profile.secondaryColor(), properties);
        this.profileId = profile.id();
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.literal(BSOriginalEnemyData.get(this.profileId).name() + " 召唤");
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityOriginalDatabaseEnemy enemy = BSEntityRegistry.ORIGINAL_ENEMY.get().create(level);
        if (enemy == null) {
            return InteractionResult.FAIL;
        }
        enemy.setProfileId(this.profileId);
        enemy.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player == null ? 0.0F : player.getYRot(), 0.0F);
        if (!level.noCollision(enemy)) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(enemy);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, @NotNull TooltipFlag flag) {
        BSOriginalEnemyData.Entry profile = BSOriginalEnemyData.get(this.profileId);
        tooltip.add(Component.literal("原作敌人 ID: " + profile.id()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("HP " + format(profile.health())
                + "  ATK " + format(profile.attack())
                + "  DEF " + format(profile.defense())
                + "  AGI " + format(profile.agility())).withStyle(ChatFormatting.GRAY));
        int phaseCount = BSOriginalEnemyPhaseData.countPhasesFrom(this.profileId);
        BSOriginalEnemyData.Entry finalProfile = BSOriginalEnemyData.get(
                BSOriginalEnemyPhaseData.finalProfileId(this.profileId));
        tooltip.add(Component.literal("击败获得 " + finalProfile.souls() + "S 魂")
                .withStyle(ChatFormatting.GOLD));
        if (phaseCount > 1) {
            tooltip.add(Component.literal("原作阶段数：" + phaseCount)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }
}
