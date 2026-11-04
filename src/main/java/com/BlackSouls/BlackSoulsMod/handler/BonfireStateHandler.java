package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BonfireStateHandler {
    private static final Map<ServerLevel, Map<Long, Pending>> PENDING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        Set<BlockPos> positions = chunk.getBlockEntitiesPos().stream()
                .filter(pos -> chunk.getBlockState(pos).is(BlockTags.CAMPFIRES))
                .map(BlockPos::immutable).collect(Collectors.toUnmodifiableSet());
        if (!positions.isEmpty()) {
            PENDING.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>())
                    .put(chunk.getPos().toLong(), new Pending(chunk, positions));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        Map<Long, Pending> pending = PENDING.get(level);
        if (pending != null) {
            pending.computeIfPresent(chunk.getPos().toLong(), (key, value) -> value.chunk() == chunk ? null : value);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Map.Entry<ServerLevel, Map<Long, Pending>> levelEntry : PENDING.entrySet()) {
            ServerLevel level = levelEntry.getKey();
            if (level.getServer() != event.getServer()) {
                continue;
            }
            for (Map.Entry<Long, Pending> entry : levelEntry.getValue().entrySet()) {
                Pending pending = entry.getValue();
                LevelChunk current = level.getChunkSource().getChunkNow(pending.chunk().getPos().x,
                        pending.chunk().getPos().z);
                if (current == null) {
                    continue;
                }
                if (levelEntry.getValue().remove(entry.getKey(), pending) && current == pending.chunk()) {
                    synchronize(level, current, pending.positions());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.keySet().removeIf(level -> level.getServer() == event.getServer());
    }

    public static void light(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.CAMPFIRES)
                && state.hasProperty(CampfireBlock.LIT)
                && state.hasProperty(CampfireBlock.WATERLOGGED)
                && !state.getValue(CampfireBlock.WATERLOGGED)
                && !state.getValue(CampfireBlock.LIT)) {
            level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
        }
    }

    private static void synchronize(ServerLevel level, LevelChunk chunk, Set<BlockPos> positions) {
        BSWorldData data = BSWorldData.get(level.getServer().overworld());
        for (BlockPos pos : positions) {
            if (pos.getX() >> 4 != chunk.getPos().x || pos.getZ() >> 4 != chunk.getPos().z) {
                continue;
            }
            BlockState state = chunk.getBlockState(pos);
            if (!state.is(BlockTags.CAMPFIRES)
                    || !state.hasProperty(CampfireBlock.LIT)
                    || !state.hasProperty(CampfireBlock.WATERLOGGED)) {
                continue;
            }
            boolean shouldBeLit = data.isBonfireActivated(level, pos)
                    && !state.getValue(CampfireBlock.WATERLOGGED);
            if (state.getValue(CampfireBlock.LIT) != shouldBeLit) {
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, shouldBeLit), Block.UPDATE_ALL);
            }
        }
    }

    private BonfireStateHandler() {
    }

    private record Pending(LevelChunk chunk, Set<BlockPos> positions) {
    }
}
