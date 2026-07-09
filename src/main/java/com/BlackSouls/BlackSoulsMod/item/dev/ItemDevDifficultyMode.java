package com.BlackSouls.BlackSoulsMod.item.dev;

import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncDifficulty;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class ItemDevDifficultyMode extends Item {

    private final ModeType modeType;

    public ItemDevDifficultyMode(Properties properties, ModeType modeType) {
        super(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
        this.modeType = modeType;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }

        if (!BSConfig.ALLOW_PLAYER_EXTRA_MODES.get() && !serverPlayer.hasPermissions(4)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.difficulty.no_permission").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.consume(stack);
        }

        ServerLevel overworld = serverPlayer.server.overworld();
        BSWorldData data = BSWorldData.get(overworld);
        boolean enabled = switch (modeType) {
            case REVENGE -> {
                data.unlockRevengeMode();
                yield data.toggleRevengeMode();
            }
            case DEATH -> {
                data.unlockDeathMode();
                yield data.toggleDeathMode();
            }
            case LEGENDARY -> {
                data.unlockLegendaryMode();
                yield data.toggleLegendaryMode();
            }
            case MALICE -> {
                data.unlockMaliceMode();
                yield data.toggleMaliceMode();
            }
            case ETERNITY -> {
                data.unlockEternityMode();
                yield data.toggleEternityMode();
            }
        };

        DifficultyManager.currentDifficulty = data.difficulty;
        for (ServerLevel serverLevel : serverPlayer.server.getAllLevels()) {
            DifficultyManager.updateAllMonstersInstant(serverLevel);
        }
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDifficulty(data));

        Component message = enabled
                ? Component.translatable(modeType.enableMessageKey()).withStyle(modeType.style())
                : Component.translatable(modeType.disableMessageKey()).withStyle(ChatFormatting.GRAY);
        serverPlayer.displayClientMessage(message, false);
        return InteractionResultHolder.consume(stack);
    }

    public enum ModeType {
        REVENGE(ChatFormatting.LIGHT_PURPLE, "message.blacksouls.dev_mode.revenge.on", "message.blacksouls.dev_mode.revenge.off"),
        DEATH(ChatFormatting.RED, "message.blacksouls.dev_mode.death.on", "message.blacksouls.dev_mode.death.off"),
        LEGENDARY(ChatFormatting.GOLD, "message.blacksouls.dev_mode.legendary.on", "message.blacksouls.dev_mode.legendary.off"),
        MALICE(ChatFormatting.DARK_PURPLE, "message.blacksouls.dev_mode.malice.on", "message.blacksouls.dev_mode.malice.off"),
        ETERNITY(ChatFormatting.AQUA, "message.blacksouls.dev_mode.eternity.on", "message.blacksouls.dev_mode.eternity.off");

        private final ChatFormatting style;
        private final String enableMessageKey;
        private final String disableMessageKey;

        ModeType(ChatFormatting style, String enableMessageKey, String disableMessageKey) {
            this.style = style;
            this.enableMessageKey = enableMessageKey;
            this.disableMessageKey = disableMessageKey;
        }

        public ChatFormatting style() {
            return style;
        }

        public String enableMessageKey() {
            return enableMessageKey;
        }

        public String disableMessageKey() {
            return disableMessageKey;
        }
    }
}
