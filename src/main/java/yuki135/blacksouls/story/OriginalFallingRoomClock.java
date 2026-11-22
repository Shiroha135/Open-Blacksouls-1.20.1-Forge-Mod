package yuki135.blacksouls.story;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPresentationPacket;
import yuki135.blacksouls.util.HokoniwaDestination;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalFallingRoomClock {
    private static final String PLAYING = "BlacksoulsFallingRoomClock";
    private static final String TRACK = "blacksouls2music:music_track_150";
    private static final BlockPos LOWER_CLOCK = new BlockPos(66, -38, 164);
    private static final BlockPos UPPER_CLOCK = new BlockPos(65, -31, 164);
    private static final BlockPos LOWER_LEDGE = new BlockPos(63, -40, 164);
    private static final BlockPos UPPER_LEDGE = new BlockPos(63, -29, 164);
    private static final ResourceLocation CUCKOO = ResourceLocation.fromNamespaceAndPath("blacksouls2", "bu_gu_zhong");
    private static final ResourceLocation DESK_CLOCK = ResourceLocation.fromNamespaceAndPath("blacksouls2", "zuo_zhong");
    private static final Map<ServerPlayer, Vec3> PREVIOUS = new WeakHashMap<>();

    static int eventAt(BlockPos feet) {
        if (feet.getZ() != 164) return 0;
        int x = feet.getX(), y = feet.getY();
        if (x == 64) return switch (y) {
            case -38 -> 34;
            case -39 -> 35;
            case -28 -> 36;
            case -29 -> 37;
            default -> 0;
        };
        if (x == 65 && y == -39) return 70;
        if (x == 65 && y == -28) return 72;
        return x == 63 && y == -29 ? 75 : 0;
    }

    private static boolean clockWall(ServerLevel level) {
        if (!level.hasChunkAt(LOWER_CLOCK) || !level.hasChunkAt(LOWER_LEDGE)) return false;
        var lower = level.getBlockState(LOWER_LEDGE);
        var upper = level.getBlockState(UPPER_LEDGE);
        return CUCKOO.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(LOWER_CLOCK).getBlock()))
                && DESK_CLOCK.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(UPPER_CLOCK).getBlock()))
                && lower.is(Blocks.BIRCH_SLAB) && lower.getValue(SlabBlock.TYPE) == SlabType.TOP
                && upper.is(Blocks.BIRCH_SLAB) && upper.getValue(SlabBlock.TYPE) == SlabType.TOP;
    }

    private static boolean onLadder(ServerLevel level, BlockPos feet) {
        var state = level.getBlockState(feet);
        return state.is(Blocks.LADDER) && state.getValue(LadderBlock.FACING) == Direction.NORTH;
    }

    static boolean inside(BlockPos feet) {
        return feet.getX() == 64 && feet.getZ() == 164 && feet.getY() >= -38 && feet.getY() <= -29;
    }

    private static void send(ServerPlayer player, boolean play) {
        if (play) OriginalOpeningService.allowInitialRoomClock(player);
        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPresentationPacket(play ? 241 : 242, player.getId(),
                player.level().dimension().location(), play ? List.of(80.0D, 100.0D) : List.of(1.0D), TRACK), player);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        var data = player.getPersistentData();
        boolean playing = data.getBoolean(PLAYING);
        String scene = data.getString("Blacksouls2CurrentScene");
        boolean inRoom = HokoniwaDestination.DIMENSION.equals(level.dimension()) && "blacksouls:hokoniwa#map_001".equals(scene)
                || Level.OVERWORLD.equals(level.dimension()) && "minecraft:overworld#map_001".equals(scene);
        if (!inRoom || !player.isAlive() || player.isSpectator()) {
            boolean interrupted = PREVIOUS.remove(player) != null;
            if (playing) {
                if (!OriginalOpeningService.managed(player) || !player.isAlive() || player.isSpectator()) {
                    data.remove(PLAYING);
                    send(player, false);
                } else if (interrupted) send(player, false);
            }
            return;
        }
        if (OriginalOpeningService.managed(player)) {
            if (PREVIOUS.remove(player) != null && playing) send(player, false);
            return;
        }

        BlockPos feet = player.blockPosition();
        boolean nearby = feet.getX() >= 62 && feet.getX() <= 66 && feet.getZ() >= 163 && feet.getZ() <= 165
                && feet.getY() >= -40 && feet.getY() <= -27;
        Vec3 position = player.position();
        Vec3 before = nearby ? PREVIOUS.put(player, position) : PREVIOUS.remove(player);
        boolean natural = before != null && before.distanceToSqr(position) <= 2.25D;
        int id = eventAt(feet);
        if (playing) {
            if (id == 35 || id == 36 || id == 70 || id == 72 || id == 75 || id == 0 && !inside(feet)
                    || !onLadder(level, feet) || before != null && !natural) {
                data.remove(PLAYING);
                send(player, false);
            } else if (before == null) {
                if (clockWall(level)) send(player, true);
                else data.remove(PLAYING);
            }
            return;
        }
        if (!natural || !nearby || player.getAbilities().flying || BlockPos.containing(before).equals(feet)) return;
        if ((id == 34 || id == 37) && onLadder(level, feet) && clockWall(level)
                && !OriginalNpcProgress.readSwitch(player, 14)) {
            data.putBoolean(PLAYING, true);
            send(player, true);
        }
    }

    private OriginalFallingRoomClock() {
    }
}
