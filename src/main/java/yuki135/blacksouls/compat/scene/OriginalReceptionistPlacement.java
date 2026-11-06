package yuki135.blacksouls.compat.scene;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.entity.EntityOriginalReceptionist;
import yuki135.blacksouls.handler.BSEntityRegistry;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OriginalReceptionistPlacement {
    public static final String NPC_ID = "map012_ev007";
    public static final BlockPos DOLL_POSITION = new BlockPos(712, -58, -434);
    public static final Vec3 POSITION = new Vec3(712.5D, -58.0D, -433.5D);
    private static final AABB SEARCH = new AABB(POSITION, POSITION).inflate(2.0D);
    private static final Set<ChunkPos> CHUNKS = Set.of(new ChunkPos(44, -28), new ChunkPos(44, -27));
    private static final List<OriginalNpcPlacements.Landmark> LANDMARKS = List.of(
            new OriginalNpcPlacements.Landmark(new BlockPos(716, -59, -435), "mcwfurnitures:spruce_covered_desk", Map.of("facing", "east", "connection", "right")),
            new OriginalNpcPlacements.Landmark(new BlockPos(708, -59, -435), "mcwfurnitures:spruce_covered_desk", Map.of("facing", "east", "connection", "left")),
            new OriginalNpcPlacements.Landmark(new BlockPos(712, -59, -441), "blacksouls2:pen_zai", Map.of("facing", "south")),
            new OriginalNpcPlacements.Landmark(new BlockPos(705, -59, -432), "minecraft:dark_oak_door", Map.of("facing", "south", "half", "lower")),
            new OriginalNpcPlacements.Landmark(new BlockPos(719, -59, -432), "minecraft:dark_oak_door", Map.of("facing", "south", "half", "lower")),
            new OriginalNpcPlacements.Landmark(DOLL_POSITION.below(), "blacksouls2:zhuo_zi", Map.of()));

    private OriginalReceptionistPlacement() {
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

    public static CompoundTag originalBlockTag() {
        CompoundTag state = new CompoundTag();
        state.putString("Name", "blacksouls2:ren_ou");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "north");
        state.put("Properties", properties);
        return state;
    }

    public static boolean isOriginalDoll(BlockState state) {
        return state != null && originalBlockTag().equals(NbtUtils.writeBlockState(state));
    }

    public static boolean matches(EntityOriginalReceptionist npc) {
        if (!(npc.level() instanceof ServerLevel level) || !HokoniwaDestination.DIMENSION.equals(level.dimension())
                || !NPC_ID.equals(npc.getNpcId()) || !npc.isOriginalVisualBound()
                || !isOriginalDoll(npc.visualBlockState()) || npc.position().distanceToSqr(POSITION) > 1.0E-8D) return false;
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        OriginalNpcPlacementData.VisualMigration receipt = data.visualMigration(NPC_ID);
        return receipt != null && receipt.complete() && npc.getUUID().equals(data.entityId(NPC_ID))
                && npc.getUUID().equals(receipt.entityId()) && DOLL_POSITION.equals(receipt.position())
                && receipt.state().equals(originalBlockTag()) && level.getBlockState(DOLL_POSITION).isAir();
    }

    public static OriginalNpcPlacements.Outcome install(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!HokoniwaDestination.DIMENSION.equals(level.dimension())) return OriginalNpcPlacements.Outcome.WRONG_DIMENSION;
        if (!OriginalNpcPlacements.hasDeployedMap(level)) return OriginalNpcPlacements.Outcome.MAP_NOT_DEPLOYED;
        if (player.position().distanceToSqr(POSITION) > 4096.0D) return OriginalNpcPlacements.Outcome.TOO_FAR;
        if (!chunksReady(level)) return OriginalNpcPlacements.Outcome.CHUNKS_UNAVAILABLE;
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        OriginalNpcPlacementData.VisualMigration receipt = data.visualMigration(NPC_ID);
        if (receipt != null) return recover(level, data, receipt);
        if (data.isInstalled(NPC_ID)) return OriginalNpcPlacements.Outcome.ALREADY_RECORDED;
        if (!LANDMARKS.stream().allMatch(mark -> mark.matches(level))) return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        BlockState original = level.getBlockState(DOLL_POSITION);
        List<EntityOriginalNpc> existing = level.getEntitiesOfClass(EntityOriginalNpc.class, SEARCH,
                value -> !value.isRemoved() && NPC_ID.equals(value.getNpcId()));
        if (existing.size() == 1 && existing.get(0) instanceof EntityOriginalReceptionist npc
                && validOwner(npc, npc.getUUID()) && (original.isAir() || isOriginalDoll(original))) {
            if (!clearance(level, npc)) return OriginalNpcPlacements.Outcome.COLLISION;
            data.prepareVisualMigration(NPC_ID, DOLL_POSITION, originalBlockTag(), npc.getUUID());
            if (isOriginalDoll(original) && !level.setBlock(DOLL_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
                data.cancelVisualMigration(NPC_ID);
                return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
            }
            data.completeVisualMigration(NPC_ID);
            return OriginalNpcPlacements.Outcome.REUSED_EXISTING;
        }
        if (!existing.isEmpty()) return OriginalNpcPlacements.Outcome.COLLISION;
        if (!isOriginalDoll(original)) return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        EntityOriginalReceptionist npc = BSEntityRegistry.ORIGINAL_RECEPTIONIST.get().create(level);
        if (npc == null) return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        npc.moveTo(POSITION.x, POSITION.y, POSITION.z, 180.0F, 0.0F);
        npc.bindOriginalVisual(original);
        if (!clearance(level, npc)) {
            npc.discard();
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        if (!data.prepareVisualMigration(NPC_ID, DOLL_POSITION, NbtUtils.writeBlockState(original), npc.getUUID())) {
            npc.discard();
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
        try {
            if (!level.setBlock(DOLL_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
                    || !level.noCollision(npc, npc.getBoundingBox()) || !level.addFreshEntity(npc)) {
                rollback(level, data, original, npc);
                return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
            }
            data.completeVisualMigration(NPC_ID);
            BlackSouls.LOGGER.info("Bound original receptionist {} to native doll model at {} with UUID {}", NPC_ID, DOLL_POSITION, npc.getUUID());
            return OriginalNpcPlacements.Outcome.INSTALLED;
        } catch (RuntimeException failure) {
            rollback(level, data, original, npc);
            BlackSouls.LOGGER.error("Original receptionist migration failed", failure);
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
    }

    private static OriginalNpcPlacements.Outcome recover(ServerLevel level, OriginalNpcPlacementData data,
                                                          OriginalNpcPlacementData.VisualMigration receipt) {
        if (!DOLL_POSITION.equals(receipt.position()) || !receipt.state().equals(originalBlockTag())) {
            return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        }
        BlockState current = level.getBlockState(DOLL_POSITION);
        if (receipt.complete()) {
            if (isOriginalDoll(current)) level.setBlock(DOLL_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return OriginalNpcPlacements.Outcome.ALREADY_RECORDED;
        }
        if (!LANDMARKS.stream().allMatch(mark -> mark.matches(level))
                || !current.isAir() && !isOriginalDoll(current)) return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        Entity entity = level.getEntity(receipt.entityId());
        if (entity == null) {
            if (current.isAir()) {
                BlockState original = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), receipt.state());
                if (!isOriginalDoll(original) || !level.setBlock(DOLL_POSITION, original, Block.UPDATE_CLIENTS)) {
                    return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
                }
            }
            data.cancelVisualMigration(NPC_ID);
            return OriginalNpcPlacements.Outcome.RECOVERED_MIGRATION;
        }
        if (!(entity instanceof EntityOriginalReceptionist npc) || !validOwner(npc, receipt.entityId())
                || !LANDMARKS.stream().allMatch(mark -> mark.matches(level)) || !clearance(level, npc)) {
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        if (isOriginalDoll(current) && !level.setBlock(DOLL_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
        data.completeVisualMigration(NPC_ID);
        return OriginalNpcPlacements.Outcome.RECOVERED_MIGRATION;
    }

    private static boolean validOwner(EntityOriginalReceptionist npc, UUID id) {
        return npc.isAlive() && !npc.isRemoved() && npc.getUUID().equals(id) && npc.isOriginalVisualBound()
                && NPC_ID.equals(npc.getNpcId()) && isOriginalDoll(npc.visualBlockState())
                && npc.position().distanceToSqr(POSITION) <= 1.0E-8D;
    }

    private static boolean clearance(ServerLevel level, EntityOriginalReceptionist npc) {
        AABB bounds = npc.getBoundingBox();
        if (level.containsAnyLiquid(bounds) || !level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != npc && entity.isAlive() && !entity.isSpectator()).isEmpty()) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX - 1.0E-6D, bounds.minY - 1.0E-6D, bounds.minZ - 1.0E-6D),
                BlockPos.containing(bounds.maxX + 1.0E-6D, bounds.maxY + 1.0E-6D, bounds.maxZ + 1.0E-6D))) {
            if (!pos.equals(DOLL_POSITION) && Shapes.joinIsNotEmpty(Shapes.create(bounds),
                    level.getBlockState(pos).getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) return false;
        }
        BlockPos support = DOLL_POSITION.below();
        return Shapes.joinIsNotEmpty(Shapes.create(bounds.move(0.0D, -0.02D, 0.0D)),
                level.getBlockState(support).getCollisionShape(level, support).move(support.getX(), support.getY(), support.getZ()), BooleanOp.AND);
    }

    private static void rollback(ServerLevel level, OriginalNpcPlacementData data, BlockState original, EntityOriginalReceptionist npc) {
        npc.discard();
        if (level.getBlockState(DOLL_POSITION).isAir()) level.setBlock(DOLL_POSITION, original, Block.UPDATE_CLIENTS);
        if (level.getBlockState(DOLL_POSITION).equals(original)) data.cancelVisualMigration(NPC_ID);
    }
}
