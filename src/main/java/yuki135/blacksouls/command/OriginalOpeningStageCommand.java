package yuki135.blacksouls.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.handler.OriginalOpeningVisualBlocks;
import yuki135.blacksouls.story.OriginalOpeningProfessionStage;
import yuki135.blacksouls.story.OriginalOpeningSceneData;
import yuki135.blacksouls.story.OriginalOpeningScenes;
import yuki135.blacksouls.story.OriginalOpeningService;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalOpeningStageCommand {
    private OriginalOpeningStageCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bs2_opening").requires(source -> source.hasPermission(2))
                .then(Commands.literal("stage").executes(context -> help(context.getSource()))
                        .then(Commands.literal("create").then(Commands.argument("origin", BlockPosArgument.blockPos()).executes(OriginalOpeningStageCommand::create)))
                        .then(Commands.literal("preset").then(Commands.argument("origin", BlockPosArgument.blockPos()).executes(OriginalOpeningStageCommand::preset)))
                        .then(Commands.literal("save").then(Commands.argument("origin", BlockPosArgument.blockPos()).executes(OriginalOpeningStageCommand::save)))
                        .then(Commands.literal("piece").then(Commands.argument("module",StringArgumentType.word())
                                .suggests((context,builder)->SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(OriginalOpeningVisualBlocks.DEFINITIONS.stream()
                                        .filter(definition->!definition.category().equals("baked_tree")).map(definition->definition.id().substring("original_opening_".length())),
                                        yuki135.blacksouls.handler.OriginalOpeningSourceBlocks.MODULES.keySet().stream()),builder))
                                .then(Commands.argument("origin",BlockPosArgument.blockPos()).executes(context->piece(context,Direction.NORTH))
                                        .then(Commands.argument("facing",StringArgumentType.word())
                                                .suggests((context,builder)->SharedSuggestionProvider.suggest(java.util.List.of("north","east","south","west"),builder))
                                                .executes(context->piece(context,Direction.byName(StringArgumentType.getString(context,"facing"))))))))
                        .then(Commands.literal("positions").then(Commands.argument("origin", BlockPosArgument.blockPos()).executes(context -> {
                            positions(context.getSource(), BlockPosArgument.getBlockPos(context, "origin")); return 1;
                        })))));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("出身布景：/bs2_opening stage create <西北角脚部坐标>；修改后用 stage save <同坐标>。范围 20×20，地面为指定 Y-1，采集高度 Y-4 至 Y+13，最多 "
                + OriginalOpeningProfessionStage.MAX_BLOCKS + " 个非空气方块。save 同时导出原版结构 blacksouls:opening/profession。"), false);
        source.sendSuccess(() -> Component.literal("请在普通创造模式建造区操作；已进入剧情的实例保留旧版。结束序章重播后，再次 replay 会采用最新模板。stage positions <同坐标> 查看出生和职业固定位置。"), false);
        source.sendSuccess(() -> Component.literal("完整原作布局：stage preset <西北角脚部坐标>。在空区域一次放置内置 20×20 选人场景，旧自建模板保持原样；确认后 stage save <同坐标> 导入。"),false);
        source.sendSuccess(() -> Component.literal("原作部件：stage piece <部件> <坐标> [north/east/south/west]；部件可按 Tab 补全，例如 root_base、trunk_segment、leaf_cluster、flower_cluster。只在空气中整组放置。"),false);
        return 1;
    }

    private static ServerPlayer builder(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!player.isCreative() || OriginalOpeningService.managed(player) || OriginalOpeningScenes.DIMENSION.equals(player.level().dimension())) {
            throw new IllegalArgumentException("请先退出序章重播，再在普通世界的创造模式下编辑出身布景。");
        }
        return player;
    }

    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        try {
            var player = builder(source); var level = player.server.getLevel(OriginalOpeningScenes.DIMENSION);
            if (level == null) throw new IllegalStateException("序章维度尚未加载。");
            var origin = BlockPosArgument.getBlockPos(context, "origin");
            int count = OriginalOpeningProfessionStage.create(player.serverLevel(), origin, OriginalOpeningSceneData.get(level).professionTemplate());
            source.sendSuccess(() -> Component.literal("已放置 " + count + " 个出身布景方块。西北角脚部坐标：" + origin.toShortString() + "；请在此直接搭建，完成后 stage save "
                    + origin.getX() + " " + origin.getY() + " " + origin.getZ() + "。"), false);
            positions(source, origin); return 1;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            source.sendFailure(Component.literal(failure.getMessage())); return 0;
        }
    }

    private static int save(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        try {
            var player = builder(source); var level = player.server.getLevel(OriginalOpeningScenes.DIMENSION);
            if (level == null) throw new IllegalStateException("序章维度尚未加载。");
            var origin = BlockPosArgument.getBlockPos(context, "origin");
            var template = OriginalOpeningProfessionStage.capture(player.serverLevel(), origin);
            var manager = player.serverLevel().getStructureManager(); var structure = manager.getOrCreate(OriginalOpeningProfessionStage.STRUCTURE);
            structure.fillFromWorld(player.serverLevel(), origin.offset(0, OriginalOpeningScenes.MIN_Y - OriginalOpeningScenes.FEET_Y, 0),
                    new Vec3i(20, OriginalOpeningScenes.MAX_Y - OriginalOpeningScenes.MIN_Y, 20), false, null);
            if (!manager.save(OriginalOpeningProfessionStage.STRUCTURE)) throw new IllegalStateException("原版结构文件写入失败，尚未替换出身模板。");
            var data=OriginalOpeningSceneData.get(level); data.professionTemplate(template); level.getDataStorage().save();
            var file=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT))
                    .resolve("data").resolve(OriginalOpeningSceneData.DATA_NAME+".dat");
            boolean verified;
            try { verified=NbtIo.readCompressed(file.toFile()).getCompound("data").getCompound("ProfessionTemplate").equals(template.save()); }
            catch (java.io.IOException failure) { verified=false; }
            if (!verified) {
                data.setDirty();
                throw new IllegalStateException("原版结构已导出，但世界出身模板未通过存档读回校验，请检查磁盘后重新 save。");
            }
            source.sendSuccess(() -> Component.literal("已保存 " + template.blocks().size() + " 个方块，模板用于下次新建的出身场景；已有剧情实例保持原样。结构文件：generated/blacksouls/structures/opening/profession.nbt。"), false);
            return 1;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            source.sendFailure(Component.literal(failure.getMessage())); return 0;
        }
    }

    private static int preset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source=context.getSource();
        try {
            var player=builder(source);
            var origin=BlockPosArgument.getBlockPos(context,"origin");
            int count=OriginalOpeningProfessionStage.create(player.serverLevel(),origin,OriginalOpeningProfessionStage.defaultTemplate());
            source.sendSuccess(()->Component.literal("已放置完整原作选人场景，共 "+count+" 个方块，西北角脚部坐标："+origin.toShortString()
                    +"。确认后 stage save "+origin.getX()+" "+origin.getY()+" "+origin.getZ()+" 导入世界模板。"),false);
            positions(source,origin);
            return 1;
        } catch(IllegalArgumentException|IllegalStateException failure) {
            source.sendFailure(Component.literal(failure.getMessage()));return 0;
        }
    }

    private static int piece(CommandContext<CommandSourceStack> context,Direction facing) throws CommandSyntaxException {
        var source=context.getSource();
        try {
            var player=builder(source);
            if (facing==null || facing.getAxis()==Direction.Axis.Y) throw new IllegalArgumentException("朝向只能是 north、east、south 或 west。");
            int count=OriginalOpeningProfessionStage.piece(player.serverLevel(),StringArgumentType.getString(context,"module"),BlockPosArgument.getBlockPos(context,"origin"),facing);
            source.sendSuccess(()->Component.literal("已放置 "+count+" 个原作部件方块。"),false); return 1;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            source.sendFailure(Component.literal(failure.getMessage())); return 0;
        }
    }

    private static void positions(CommandSourceStack source, BlockPos origin) {
        source.sendSuccess(() -> Component.literal("固定出生位置：" + origin.offset(10, 0, 9).toShortString() + "；保持地面完整及上方 4 格净空。"), false);
        var lease = new OriginalOpeningSceneData.Lease(new java.util.UUID(0, 0), 0);
        for (var anchor : OriginalOpeningScenes.professionAnchors(lease)) {
            source.sendSuccess(() -> Component.literal(anchor.label() + "：" + origin.offset(anchor.sourceCell().x(), 0, anchor.sourceCell().y()).toShortString()), false);
        }
    }
}
