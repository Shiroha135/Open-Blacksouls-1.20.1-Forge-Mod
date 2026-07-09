package com.BlackSouls.BlackSoulsMod.item.consumables;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemWeaponResin extends Item {
    private final RegistryObject<MobEffect> effect;
    private final int durationTicks;
    private final int animationId;
    private final RegistryObject<SoundEvent> firstSound;
    private final @Nullable RegistryObject<SoundEvent> delayedSound;
    private final int delayedSoundTicks;
    private final boolean clearsFrostbite;
    private final float animationYOffset;

    public ItemWeaponResin(Properties properties, RegistryObject<MobEffect> effect, int durationTicks, int animationId,
                           RegistryObject<SoundEvent> firstSound, @Nullable RegistryObject<SoundEvent> delayedSound,
                           int delayedSoundTicks, boolean clearsFrostbite, float animationYOffset) {
        super(properties);
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.animationId = animationId;
        this.firstSound = firstSound;
        this.delayedSound = delayedSound;
        this.delayedSoundTicks = delayedSoundTicks;
        this.clearsFrostbite = clearsFrostbite;
        this.animationYOffset = animationYOffset;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (clearsFrostbite && BlackSouls.BUFF_FROSTBITE.isPresent() && player.hasEffect(BlackSouls.BUFF_FROSTBITE.get())) {
                player.removeEffect(BlackSouls.BUFF_FROSTBITE.get());
            }

            player.addEffect(new MobEffectInstance(effect.get(), durationTicks, 0));

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

            NetworkHandler.sendToAllAround(
                    new PacketPlayAnim(animationId, player.getX(), player.getY() + player.getBbHeight() / 2.0F + animationYOffset, player.getZ()),
                    player
            );

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.1").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
