package yuki135.blacksouls.compat.scene;

import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.entity.EntityNoden;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.BSEntityRegistry;
import yuki135.blacksouls.story.OriginalNpcInterpreter;
import yuki135.blacksouls.story.OriginalNpcService;
import yuki135.blacksouls.util.BSOriginalNpcData;

@Mod.EventBusSubscriber(modid = "blacksouls")
public final class OriginalLibraryProgress {
    public static final BooleanProperty VISIBLE = BooleanProperty.create("blacksouls_visible");
    public static final BlockPos BOX = new BlockPos(-60, -43, 43);
    public static final String NODEN = "map101_ev009";
    private static final String BOX_EVENT = "map101_ev012";
    private static final AABB NODEN_BOUNDS = new AABB(-51, -44, 40, -48, -40, 43);
    private static final List<OriginalNpcPlacements.Landmark> LANDMARKS = List.of(
            new OriginalNpcPlacements.Landmark(new BlockPos(-50, -43, 41), "decocraft:stool_4_spruce", java.util.Map.of("facing", "west")),
            new OriginalNpcPlacements.Landmark(new BlockPos(-49, -43, 47), "minecraft:campfire", java.util.Map.of("facing", "south")),
            new OriginalNpcPlacements.Landmark(new BlockPos(-40, -43, 43), "blacksouls2:bai_xiong_mao_shop", java.util.Map.of("facing", "north")));

    private OriginalLibraryProgress() {
    }

    public static boolean terrainMatches(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        return (dimension.equals("minecraft:overworld") || dimension.equals("blacksouls:hokoniwa")
                || dimension.equals("blacksouls:library"))
                && LANDMARKS.stream().allMatch(site -> level.hasChunkAt(site.position()) && site.matches(level));
    }

    public static boolean isNoden(EntityOriginalNpc npc) {
        return NODEN.equals(npc.getNpcId()) && NODEN_BOUNDS.intersects(npc.getBoundingBox());
    }

    public static boolean hidden(BlockState state) {
        return state.hasProperty(VISIBLE) && !state.getValue(VISIBLE);
    }

    public static boolean boundBox(ServerLevel level, BlockPos pos) {
        return BOX.equals(pos) && level.hasChunkAt(pos) && terrainMatches(level)
                && blockId(level.getBlockState(pos)).equals("blacksouls2:e_zuo_ju_he_zi");
    }

    public static boolean canUseBox(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !boundBox(server, pos)) return !hidden(level.getBlockState(pos));
        return boxPage(server) >= 0;
    }

    public static int boxPage(ServerLevel level) {
        var progress = OriginalNpcProgress.get(level.getServer());
        boolean introduced = level.getBlockEntity(BOX) instanceof LibrarySoulBoxState state && state.blacksouls$introduced();
        OriginalNpcInterpreter.Context context = (OriginalNpcInterpreter.Context) Proxy.newProxyInstance(
                OriginalLibraryProgress.class.getClassLoader(), new Class<?>[]{OriginalNpcInterpreter.Context.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "switchValue" -> (Integer) args[0] == 31 && introduced || progress.switchValue((Integer) args[0]);
                    case "variable" -> (Integer) args[0] == 11 ? SceneSpawnerBossData.get(level.getServer()).getStoryProgress()
                            : progress.variable((Integer) args[0]);
                    case "selfSwitch" -> progress.selfSwitch(BOX_EVENT, (String) args[0]);
                    default -> throw new UnsupportedOperationException("library_box_page:" + method.getName());
                });
        var entry = BSOriginalNpcData.getEvent(BOX_EVENT);
        var page = OriginalNpcInterpreter.selectPage(entry, context);
        return page == null ? -1 : entry.pages().indexOf(page);
    }

    public static void boxOpened(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && boundBox(server, pos)) {
            OriginalNpcProgress.get(server.getServer()).setSwitch(31, true);
            refreshBox(server);
        }
    }

    public static void boxReset(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && boundBox(server, pos)) {
            OriginalNpcProgress.get(server.getServer()).setSwitch(31, false);
            refreshBox(server);
        }
    }

    public static void refresh(ServerLevel level) {
        if (!terrainMatches(level)) return;
        refreshBox(level);
        int progress = SceneSpawnerBossData.get(level.getServer()).getStoryProgress();
        for (int i = 0; i < 10; i++) {
            BlockPos pos = new BlockPos(-45 - i, -36, 55);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!blockId(state).equals("blacksouls2:jingduhuoju") || !state.hasProperty(BlockStateProperties.LIT)
                    || state.getValue(BlockStateProperties.HORIZONTAL_FACING) != Direction.NORTH) continue;
            boolean lit = progress >= i + 1;
            if (state.getValue(BlockStateProperties.LIT) != lit) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
            }
        }
        var actors = level.getEntitiesOfClass(EntityOriginalNpc.class, NODEN_BOUNDS,
                actor -> !actor.isRemoved() && NODEN.equals(actor.getNpcId()));
        if (actors.isEmpty() && level.getEntitiesOfClass(EntityNoden.class, NODEN_BOUNDS).isEmpty()
                && level.getNearestPlayer(-49.5D, -42.0D, 41.5D, 48.0D, entity -> entity.isAlive() && !entity.isSpectator()) != null) {
            EntityOriginalNpc npc = BSEntityRegistry.ORIGINAL_NPC.get().create(level);
            if (npc != null) {
                npc.moveTo(-49.5D, -42.25937497615814D, 41.5D, 270.0F, 0.0F);
                npc.setYHeadRot(270.0F);
                npc.yBodyRot = 270.0F;
                npc.setSitting(true);
                npc.setNpcId(NODEN);
                if (level.addFreshEntity(npc)) OriginalNpcPlacementData.get(level).record(NODEN, npc.getUUID());
            }
        }
        for (EntityOriginalNpc actor : actors) OriginalNpcService.refreshPage(actor, null);
    }

    private static void refreshBox(ServerLevel level) {
        if (!boundBox(level, BOX)) return;
        BlockState state = level.getBlockState(BOX);
        boolean visible = boxPage(level) >= 0;
        if (state.hasProperty(VISIBLE) && state.getValue(VISIBLE) != visible) {
            level.setBlock(BOX, state.setValue(VISIBLE, visible), Block.UPDATE_ALL);
        }
    }

    private static String blockId(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id == null ? "" : id.toString();
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 20L == 0L) {
            refresh(level);
        }
    }
}
