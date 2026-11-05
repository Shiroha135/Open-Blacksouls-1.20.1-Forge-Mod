package com.BlackSouls.BlackSoulsMod.compat.scene;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalWhiteBear;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcService;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OriginalWhiteBearPlacement {
    public static final String NPC_ID = "map101_ev005";
    public static final BlockPos NATIVE_BLOCK_POSITION = new BlockPos(-40, -43, 43);
    public static final Vec3 POSITION = new Vec3(-39.5D, -43.0D, 43.5D);
    private static final Set<ChunkPos> CHUNKS = Set.of(new ChunkPos(-3, 2), new ChunkPos(-4, 2), new ChunkPos(-4, 3));
    private static final List<OriginalNpcPlacements.Landmark> LANDMARKS = List.of(
            new OriginalNpcPlacements.Landmark(new BlockPos(-40, -44, 43), "blacksouls2:tu_shu_guan_zhi_meng_di_ban_lu", Map.of()),
            new OriginalNpcPlacements.Landmark(new BlockPos(-40, -44, 42), "blacksouls2:tu_shu_guan_zhi_meng_di_ban_lu", Map.of()),
            new OriginalNpcPlacements.Landmark(new BlockPos(-49, -43, 47), "minecraft:campfire", Map.of("facing", "south")),
            new OriginalNpcPlacements.Landmark(new BlockPos(-49, -40, 56), "minecraft:birch_door", Map.of("facing", "south", "half", "lower")));

    private OriginalWhiteBearPlacement() {
    }

    public static Set<ChunkPos> requiredChunks() {
        return CHUNKS;
    }

    public static boolean chunksReady(ServerLevel level) {
        for (ChunkPos chunk : CHUNKS) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) return false;
        }
        return true;
    }

    public static boolean isNativeBlock(BlockState state) {
        return state != null && BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("blacksouls2:bai_xiong_mao_shop")
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.NORTH;
    }

    public static boolean siteReady(ServerLevel level) {
        return HokoniwaDestination.DIMENSION.equals(level.dimension()) && OriginalNpcPlacements.hasDeployedMap(level)
                && chunksReady(level) && isNativeBlock(level.getBlockState(NATIVE_BLOCK_POSITION))
                && LANDMARKS.stream().allMatch(landmark -> landmark.matches(level));
    }

    public static boolean matches(EntityOriginalWhiteBear npc) {
        if (!(npc.level() instanceof ServerLevel level) || !validActor(npc) || !siteReady(level)
                || !npc.getUUID().equals(OriginalNpcPlacementData.get(level).entityId(NPC_ID))
                || level.getEntity(npc.getUUID()) != npc) return false;
        List<EntityOriginalNpc> actors = sourceActors(level);
        return actors.size() == 1 && actors.get(0) == npc;
    }

    public static boolean isOriginalSiteContext(ServerPlayer player) {
        return HokoniwaDestination.DIMENSION.equals(player.level().dimension())
                && player.position().distanceToSqr(POSITION) <= 64.0D;
    }

    public static void interact(ServerPlayer player) {
        EntityOriginalWhiteBear npc = resolve(player);
        if (npc != null && npc.claimInteractionTick(player.getUUID(), player.serverLevel().getGameTime())) {
            OriginalNpcService.interact(player, npc);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % 100 == 0 && isOriginalSiteContext(player)) resolve(player);
    }

    public static EntityOriginalWhiteBear resolve(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!player.isAlive() || player.isSpectator() || player.position().distanceToSqr(POSITION) > 64.0D
                || TurnBattleManager.isInBattle(player) || !siteReady(level)) return null;
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        UUID recorded = data.entityId(NPC_ID);
        if (recorded != null) {
            Entity entity = level.getEntity(recorded);
            return entity instanceof EntityOriginalWhiteBear npc && matches(npc) ? npc : null;
        }
        if (!sourceActors(level).isEmpty()) return null;
        EntityOriginalWhiteBear npc = BSEntityRegistry.ORIGINAL_WHITE_BEAR.get().create(level);
        if (npc == null) return null;
        npc.moveTo(POSITION.x, POSITION.y, POSITION.z, 180.0F, 0.0F);
        npc.bindNativeBlock();
        try {
            if (!level.addFreshEntity(npc) || !data.record(NPC_ID, npc.getUUID())) {
                npc.discard();
                return null;
            }
        } catch (RuntimeException exception) {
            npc.discard();
            BlackSouls.LOGGER.warn("Failed to bind the native white bear interaction", exception);
            return null;
        }
        return matches(npc) ? npc : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !HokoniwaDestination.DIMENSION.equals(event.getLevel().dimension())
                || !NATIVE_BLOCK_POSITION.equals(event.getPos())) return;
        if (!BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock())
                .toString().equals("blacksouls2:bai_xiong_mao_shop")) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) return;
        interact(player);
    }

    private static boolean validActor(EntityOriginalWhiteBear npc) {
        return npc.isAlive() && !npc.isRemoved() && npc.isNativeBlockBound() && NPC_ID.equals(npc.getNpcId())
                && npc.position().distanceToSqr(POSITION) <= 1.0E-8D;
    }

    private static List<EntityOriginalNpc> sourceActors(ServerLevel level) {
        List<EntityOriginalNpc> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EntityOriginalNpc npc && !npc.isRemoved() && NPC_ID.equals(npc.getNpcId())) result.add(npc);
        }
        return result;
    }
}
