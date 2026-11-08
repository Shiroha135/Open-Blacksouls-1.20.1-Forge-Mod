package yuki135.blacksouls.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.handler.AvatarUnlocks;

@Mod.EventBusSubscriber(modid="blacksouls")
public final class AvatarCommand {
    private AvatarCommand() { }

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bs2_avatar").requires(source->source.hasPermission(2))
                .then(action("unlock_all",true)).then(action("relock",false)));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> action(String name,boolean unlocked) {
        return Commands.literal(name)
                .executes(context->apply(context.getSource(),java.util.List.of(context.getSource().getPlayerOrException()),unlocked))
                .then(Commands.argument("players",EntityArgument.players()).executes(context->apply(context.getSource(),EntityArgument.getPlayers(context,"players"),unlocked)));
    }
    private static int apply(CommandSourceStack source,Collection<ServerPlayer> players,boolean unlocked) {
        int count=0;
        for(var player:players)if(AvatarUnlocks.setAll(player,unlocked))count++;
        int saved=count;
        if(saved>0)source.sendSuccess(()->Component.literal(unlocked?"已为 "+saved+" 名玩家解锁全部头像。":"已为 "+saved+" 名玩家恢复仅解锁职业头像。"),true);
        if(saved<players.size())source.sendFailure(Component.literal("部分玩家的头像状态未能保存，请重试。"));
        return saved;
    }
}
