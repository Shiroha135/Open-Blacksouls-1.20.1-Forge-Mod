package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.entity.EntityThrownBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class ItemThrownBladeBase extends Item {
    private final int mode;
    private final boolean sureHit;
    private final int bleedTicks;
    private final int animationId;
    private final RegistryObject<SoundEvent> firstSound;
    private final @Nullable RegistryObject<SoundEvent> delayedSound;
    private final int delayedSoundTicks;

    protected ItemThrownBladeBase(Properties properties, int mode, boolean sureHit, int bleedTicks, int animationId,
                                  RegistryObject<SoundEvent> firstSound, @Nullable RegistryObject<SoundEvent> delayedSound,
                                  int delayedSoundTicks) {
        super(properties);
        this.mode = mode;
        this.sureHit = sureHit;
        this.bleedTicks = bleedTicks;
        this.animationId = animationId;
        this.firstSound = firstSound;
        this.delayedSound = delayedSound;
        this.delayedSoundTicks = delayedSoundTicks;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    firstSound.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (delayedSound != null && delayedSoundTicks > 0) {
                MinecraftServer server = level.getServer();
                if (server != null) {
                    double x = player.getX();
                    double y = player.getY();
                    double z = player.getZ();
                    server.tell(new TickTask(server.getTickCount() + delayedSoundTicks, () ->
                            level.playSound(null, x, y, z, delayedSound.get(), SoundSource.PLAYERS, 1.0F, 1.0F)));
                }
            }

            EntityThrownBlade projectile = new EntityThrownBlade(level, player, stack, mode, sureHit, bleedTicks);
            projectile.setAnimationId(animationId);
            projectile.setPos(player.getX(), player.getEyeY() - 0.2D, player.getZ());

            LivingEntity target = findTarget(player, 15.0D);
            if (target != null) {
                Vec3 targetPos = target.getBoundingBox().getCenter();
                Vec3 from = projectile.position();
                Vec3 delta = targetPos.subtract(from).normalize();
                projectile.shoot(delta.x, delta.y, delta.z, 2.2F, 0.0F);
            } else {
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.0F);
            }

            level.addFreshEntity(projectile);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    private LivingEntity findTarget(Player player, double range) {
        LivingEntity target = null;
        double closest = Double.MAX_VALUE;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetVec = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        List<Entity> list = player.level().getEntities(player, player.getBoundingBox().inflate(range));
        for (Entity entity : list) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                continue;
            }
            AABB aabb = entity.getBoundingBox().inflate(0.5D);
            if (aabb.contains(eyePos)) {
                return living;
            }
            Optional<Vec3> result = aabb.clip(eyePos, targetVec);
            if (result.isPresent()) {
                double dist = eyePos.distanceToSqr(result.get());
                if (dist < closest) {
                    closest = dist;
                    target = living;
                }
            }
        }
        return target;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
