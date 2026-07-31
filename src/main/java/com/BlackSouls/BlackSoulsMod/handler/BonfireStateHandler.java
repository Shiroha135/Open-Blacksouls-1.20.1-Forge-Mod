package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BonfireStateHandler {
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Set<BlockPos> positions = Set.copyOf(event.getChunk().getBlockEntitiesPos());
        level.getServer().execute(() -> synchronize(level, positions));
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

    private static void synchronize(ServerLevel level, Set<BlockPos> positions) {
        BSWorldData data = BSWorldData.get(level.getServer().overworld());
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
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
}
