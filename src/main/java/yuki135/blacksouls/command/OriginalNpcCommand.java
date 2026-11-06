package yuki135.blacksouls.command;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.BSEntityRegistry;
import yuki135.blacksouls.util.BSOriginalNpcData;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalNpcCommand {
    private OriginalNpcCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bs2_npc").requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                BSOriginalNpcData.values().stream().map(BSOriginalNpcData.Entry::id), builder))
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (BSOriginalNpcData.get(id) == null) {
                                context.getSource().sendFailure(Component.literal("没有这个原作角色事件。"));
                                return 0;
                            }
                            EntityOriginalNpc npc = BSEntityRegistry.ORIGINAL_NPC.get().create(player.serverLevel());
                            if (npc == null) {
                                return 0;
                            }
                            npc.moveTo(player.getX() + 1.5D, player.getY(), player.getZ(), player.getYRot(), 0.0F);
                            npc.setNpcId(id);
                            return player.serverLevel().addFreshEntity(npc) ? 1 : 0;
                        })))
                .then(Commands.literal("switch").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                            OriginalNpcProgress.get(context.getSource().getServer()).setSwitch(
                                    IntegerArgumentType.getInteger(context, "id"), BoolArgumentType.getBool(context, "value"));
                            return 1;
                        }))))
                .then(Commands.literal("variable").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("value", IntegerArgumentType.integer()).executes(context -> {
                            int id = IntegerArgumentType.getInteger(context, "id");
                            int value = IntegerArgumentType.getInteger(context, "value");
                            OriginalNpcProgress.get(context.getSource().getServer()).setVariable(id, value);
                            if (id == 11) {
                                SceneSpawnerBossData.get(context.getSource().getServer()).setStoryProgress(value);
                            }
                            return 1;
                        })))));
    }
}
