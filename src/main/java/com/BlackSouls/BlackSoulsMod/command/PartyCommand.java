package com.BlackSouls.BlackSoulsMod.command;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.party.PartyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class PartyCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("party")
                .then(inviteNode())
                .then(Commands.literal("accept").executes(context -> accept(context.getSource())))
                .then(Commands.literal("leave").executes(context -> leave(context.getSource()))));
        dispatcher.register(Commands.literal("inv")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> invite(context.getSource(), EntityArgument.getPlayer(context, "player")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inviteNode() {
        return Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> invite(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private static int invite(CommandSourceStack source, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PartyManager.invite(source.getPlayerOrException(), target);
        return 1;
    }

    private static int accept(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PartyManager.accept(source.getPlayerOrException());
        return 1;
    }

    private static int leave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PartyManager.leave(source.getPlayerOrException());
        return 1;
    }

    private PartyCommand() {}
}
