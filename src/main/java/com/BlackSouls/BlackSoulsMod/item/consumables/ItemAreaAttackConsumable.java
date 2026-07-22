package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.BSAttributeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemAreaAttackConsumable extends Item {
    public enum Mode {
        OUIJA_BOARD,
        COLD_VALLEY_BREATH
    }

    private final Mode mode;

    public ItemAreaAttackConsumable(Properties properties, Mode mode) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(15.0D), this::isEnemy);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.item.no_targets").withStyle(ChatFormatting.GRAY));
            return InteractionResultHolder.fail(stack);
        }

        for (LivingEntity target : targets) {
            if (mode == Mode.OUIJA_BOARD) {
                float damage = 6666.0F * BSAttributeManager.getResistance(target, BSAttributeManager.DARK);
                StatEventHandler.hurtWithSkillDamage(serverPlayer, target, damage, false, 90.0D);
                play(target, 500, BlackSouls.DARKNESS6_EVENT.get(), 0.7F);
            } else {
                float damage = (float) (10000.0D * (0.8D + Math.random() * 0.4D)
                        * BSAttributeManager.getResistance(target, BSAttributeManager.ICE));
                if (StatEventHandler.hurtWithSkillDamage(serverPlayer, target, damage, true, 0.0D)) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FROSTBITE.get(), 600 + target.getRandom().nextInt(2) * 200, 0));
                }
                play(target, 63, BlackSouls.WIND8_EVENT.get(), 1.0F);
            }
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    private boolean isEnemy(LivingEntity entity) {
        if (!(entity instanceof Mob) || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        return !(entity instanceof TamableAnimal tamable) || !tamable.isTame();
    }

    private void play(LivingEntity target, int animationId, net.minecraft.sounds.SoundEvent sound, float pitch) {
        target.level().playSound(null, target.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, pitch);
        NetworkHandler.sendToAllAround(
                new PacketPlayAnim(animationId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()),
                target
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
