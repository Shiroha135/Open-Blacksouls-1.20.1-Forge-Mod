package com.BlackSouls.BlackSoulsMod.command;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecordingAssistCommand {
    private static final String DODGE_GUARANTEED_TAG = "bs2_recording_dodge_100";

    private RecordingAssistCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("bs2_dodge100")
                .requires(source -> source.hasPermission(2))
                .executes(context -> setDodgeGuaranteed(
                        context.getSource(), !isDodgeGuaranteed(context.getSource().getPlayerOrException())))
                .then(Commands.literal("on")
                        .executes(context -> setDodgeGuaranteed(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setDodgeGuaranteed(context.getSource(), false))));
    }

    public static boolean isDodgeGuaranteed(Player player) {
        return SkillUtils.getPersistedData(player).getBoolean(DODGE_GUARANTEED_TAG);
    }

    private static int setDodgeGuaranteed(CommandSourceStack source, boolean enabled) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("该指令需要由玩家执行。"));
            return 0;
        }
        SkillUtils.getPersistedData(player).putBoolean(DODGE_GUARANTEED_TAG, enabled);
        StatEventHandler.applyStats(player);
        StatEventHandler.syncToClient(player);
        source.sendSuccess(() -> Component.literal(
                enabled ? "录像辅助闪避：已开启" : "录像辅助闪避：已关闭"), false);
        return 1;
    }
}
