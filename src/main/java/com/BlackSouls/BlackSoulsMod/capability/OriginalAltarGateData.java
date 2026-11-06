package com.BlackSouls.BlackSoulsMod.capability;

import com.BlackSouls.BlackSoulsMod.block.OriginalAltarGateBlock;
import com.BlackSouls.BlackSoulsMod.handler.OriginalWorldBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

public final class OriginalAltarGateData extends SavedData {
    public static final String DATA_NAME = "blacksouls_original_altar_gate";
    private static final List<BlockPos> POSITIONS = createPositions();
    private static final AABB VOLUME = new AABB(464, -37, -501, 469, -34, -500);
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private boolean captured;

    public static OriginalAltarGateData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OriginalAltarGateData::load, OriginalAltarGateData::new, DATA_NAME);
    }

    public static List<BlockPos> positions() {
        return POSITIONS;
    }

    public boolean synchronize(ServerLevel level, boolean open) {
        if (open && !captured) return true;
        for (BlockPos position : POSITIONS) {
            ChunkPos chunk = new ChunkPos(position);
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) return false;
        }
        Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        boolean alreadyClosed = true;
        for (BlockPos position : POSITIONS) {
            BlockState state = level.getBlockState(position);
            BlockState original = captured ? originals.get(position) : Blocks.AIR.defaultBlockState();
            BlockState closed = closedState(position);
            if (!state.equals(original) && (!captured || !state.equals(closed))) return false;
            before.put(position, state);
            alreadyClosed &= state.equals(closed);
        }
        if (!open && alreadyClosed) return true;
        if (!open && !level.getEntitiesOfClass(LivingEntity.class, VOLUME,
                entity -> entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc npc
                        && com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(npc.getNpcId())
                        && npc.isInvisible() && !npc.canBeCollidedWith())).isEmpty()) return false;
        boolean firstCapture = !captured;
        if (firstCapture) {
            originals.putAll(before);
            captured = true;
            setDirty();
        }
        List<BlockPos> changed = new ArrayList<>();
        for (BlockPos position : POSITIONS) {
            BlockState target = open ? originals.get(position) : closedState(position);
            if (target.equals(before.get(position))) continue;
            if (!level.setBlock(position, target, Block.UPDATE_ALL)) {
                for (int index = changed.size() - 1; index >= 0; index--) {
                    BlockPos rollback = changed.get(index);
                    BlockState expected = open ? originals.get(rollback) : closedState(rollback);
                    if (level.getBlockState(rollback).equals(expected)) level.setBlock(rollback, before.get(rollback), Block.UPDATE_ALL);
                }
                if (firstCapture) {
                    originals.clear();
                    captured = false;
                    setDirty();
                }
                return false;
            }
            changed.add(position);
        }
        return true;
    }

    public static OriginalAltarGateData load(CompoundTag tag) {
        OriginalAltarGateData data = new OriginalAltarGateData();
        if (tag.isEmpty()) return data;
        if (!tag.getAllKeys().equals(Set.of("Version", "Captured", "Original"))
                || !tag.contains("Version", Tag.TAG_INT) || tag.getInt("Version") != 1
                || !tag.contains("Captured", Tag.TAG_BYTE) || tag.getByte("Captured") < 0 || tag.getByte("Captured") > 1
                || !tag.contains("Original", Tag.TAG_LIST)) throw new IllegalArgumentException("Invalid original altar gate state");
        data.captured = tag.getBoolean("Captured");
        ListTag entries = (ListTag) tag.get("Original");
        if (!entries.isEmpty() && entries.getElementType() != Tag.TAG_COMPOUND
                || entries.size() != (data.captured ? POSITIONS.size() : 0)) throw new IllegalArgumentException("Incomplete original altar gate capture");
        CompoundTag air = NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState());
        for (Tag value : entries) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.getAllKeys().equals(Set.of("Pos", "State")) || !entry.contains("Pos", Tag.TAG_LONG)
                    || !entry.contains("State", Tag.TAG_COMPOUND) || !entry.getCompound("State").equals(air)) {
                throw new IllegalArgumentException("Invalid original altar gate block");
            }
            BlockPos position = BlockPos.of(entry.getLong("Pos"));
            if (!POSITIONS.contains(position) || data.originals.put(position, Blocks.AIR.defaultBlockState()) != null) {
                throw new IllegalArgumentException("Unknown or duplicate original altar gate position");
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", 1);
        tag.putBoolean("Captured", captured);
        ListTag entries = new ListTag();
        for (var original : originals.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", original.getKey().asLong());
            entry.put("State", NbtUtils.writeBlockState(original.getValue()));
            entries.add(entry);
        }
        tag.put("Original", entries);
        return tag;
    }

    private static BlockState closedState(BlockPos position) {
        return OriginalWorldBlocks.ALTAR_GATE.get().defaultBlockState().setValue(OriginalAltarGateBlock.PART, 468 - position.getX());
    }

    private static List<BlockPos> createPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = 464; x <= 468; x++) for (int y = -37; y <= -35; y++) positions.add(new BlockPos(x, y, -501));
        return List.copyOf(positions);
    }
}
