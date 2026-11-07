package yuki135.blacksouls.compat.scene;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.BSEntityRegistry;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OriginalSnowmanPlacement {
    public static final String NPC_ID = "map031_ev004";
    public static final BlockPos NATIVE_BLOCK_POSITION = new BlockPos(1146, -22, -4);
    public static final Vec3 POSITION = new Vec3(1146.5D, -22.0D, -3.5D);
    private static final AABB SEARCH = new AABB(POSITION, POSITION).inflate(2.0D);
    private static final OriginalNpcPlacements.Site SITE = new OriginalNpcPlacements.Site(NPC_ID, POSITION, 0.0F,
            "mcwholidays:snowy_grass_block", List.of(
            landmark(1146, -22, -3, "blacksouls2:l_lu_deng_1", Map.of("facing", "north")),
            landmark(1146, -19, -3, "blacksouls2:l_lu_deng_3", Map.of("facing", "north")),
            landmark(1146, -28, -17, "minecraft:command_block", Map.of("facing", "north", "conditional", "false")),
            landmark(1146, -26, -17, "minecraft:stone_pressure_plate", Map.of()),
            landmark(1097, -22, -27, "minecraft:campfire", Map.of("facing", "south"))));

    private OriginalSnowmanPlacement() {
    }

    public static OriginalNpcPlacements.Site site() {
        return SITE;
    }

    public static CompoundTag originalBlockTag() {
        CompoundTag state = new CompoundTag();
        state.putString("Name", "decoration_mod:snowman");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "south");
        state.put("Properties", properties);
        return state;
    }

    public static boolean isOriginalBlock(BlockState state) {
        return state != null && BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("decoration_mod:snowman")
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.SOUTH;
    }

    public static OriginalNpcPlacements.Outcome install(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!HokoniwaDestination.DIMENSION.equals(level.dimension())) return OriginalNpcPlacements.Outcome.WRONG_DIMENSION;
        if (!OriginalNpcPlacements.hasDeployedMap(level)) return OriginalNpcPlacements.Outcome.MAP_NOT_DEPLOYED;
        if (player.position().distanceToSqr(POSITION) > 4096.0D) return OriginalNpcPlacements.Outcome.TOO_FAR;
        if (!OriginalNpcPlacements.chunksReady(level, SITE)) return OriginalNpcPlacements.Outcome.CHUNKS_UNAVAILABLE;
        OriginalNpcPlacementData data = OriginalNpcPlacementData.get(level);
        OriginalNpcPlacementData.VisualMigration receipt = data.visualMigration(NPC_ID);
        if (receipt != null) return recover(level, data, receipt);
        if (data.isInstalled(NPC_ID)) return OriginalNpcPlacements.Outcome.ALREADY_RECORDED;
        if (!terrainMatches(level)) return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        BlockState original = level.getBlockState(NATIVE_BLOCK_POSITION);
        if (!isOriginalBlock(original)) return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        List<EntityOriginalNpc> existing = level.getEntitiesOfClass(EntityOriginalNpc.class, SEARCH,
                entity -> !entity.isRemoved() && NPC_ID.equals(entity.getNpcId()));
        if (existing.size() > 1 || existing.size() == 1 && !validOwner(existing.get(0), existing.get(0).getUUID())) {
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        boolean created = existing.isEmpty();
        EntityOriginalNpc npc = created ? BSEntityRegistry.ORIGINAL_NPC.get().create(level) : existing.get(0);
        if (npc == null) return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        if (created) {
            npc.moveTo(POSITION.x, POSITION.y, POSITION.z, SITE.yaw(), 0.0F);
            npc.setYHeadRot(SITE.yaw());
            npc.yBodyRot = SITE.yaw();
            npc.setNpcId(NPC_ID);
        }
        if (!clearance(level, npc)) {
            if (created) npc.discard();
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        if (!data.prepareVisualMigration(NPC_ID, NATIVE_BLOCK_POSITION, NbtUtils.writeBlockState(original), npc.getUUID())) {
            if (created) npc.discard();
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
        try {
            if (!level.setBlock(NATIVE_BLOCK_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
                    || !level.noCollision(npc, npc.getBoundingBox()) || created && !level.addFreshEntity(npc)) {
                rollback(level, data, original, npc, created);
                return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
            }
            data.completeVisualMigration(NPC_ID);
            BlackSouls.LOGGER.info("Migrated original snowman {} at {} with UUID {}", NPC_ID, NATIVE_BLOCK_POSITION, npc.getUUID());
            return created ? OriginalNpcPlacements.Outcome.INSTALLED : OriginalNpcPlacements.Outcome.REUSED_EXISTING;
        } catch (RuntimeException failure) {
            rollback(level, data, original, npc, created);
            BlackSouls.LOGGER.error("Original snowman visual migration failed", failure);
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
    }

    private static OriginalNpcPlacements.Outcome recover(ServerLevel level, OriginalNpcPlacementData data,
                                                          OriginalNpcPlacementData.VisualMigration receipt) {
        if (!NATIVE_BLOCK_POSITION.equals(receipt.position()) || !receipt.state().equals(originalBlockTag())) {
            return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        }
        UUID recorded = data.entityId(NPC_ID);
        if (recorded != null && !recorded.equals(receipt.entityId())
                || receipt.complete() && !receipt.entityId().equals(recorded)) {
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        BlockState current = level.getBlockState(NATIVE_BLOCK_POSITION);
        if (!terrainMatches(level) || !current.isAir() && !isOriginalBlock(current)) {
            return OriginalNpcPlacements.Outcome.TERRAIN_MISMATCH;
        }
        if (receipt.complete()) {
            Entity owner = level.getEntity(receipt.entityId());
            if (owner != null && (!(owner instanceof EntityOriginalNpc npc) || !validOwner(npc, receipt.entityId()))) {
                return OriginalNpcPlacements.Outcome.COLLISION;
            }
            if (isOriginalBlock(current) && !level.setBlock(NATIVE_BLOCK_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
                return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
            }
            return OriginalNpcPlacements.Outcome.ALREADY_RECORDED;
        }
        Entity entity = level.getEntity(receipt.entityId());
        if (entity == null) {
            if (current.isAir()) {
                BlockState original = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), receipt.state());
                if (!isOriginalBlock(original) || !level.setBlock(NATIVE_BLOCK_POSITION, original, Block.UPDATE_CLIENTS)) {
                    return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
                }
            }
            data.cancelVisualMigration(NPC_ID);
            return OriginalNpcPlacements.Outcome.RECOVERED_MIGRATION;
        }
        if (!(entity instanceof EntityOriginalNpc npc) || !validOwner(npc, receipt.entityId()) || !clearance(level, npc)) {
            return OriginalNpcPlacements.Outcome.COLLISION;
        }
        if (isOriginalBlock(current) && !level.setBlock(NATIVE_BLOCK_POSITION, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
            return OriginalNpcPlacements.Outcome.SPAWN_FAILED;
        }
        data.completeVisualMigration(NPC_ID);
        return OriginalNpcPlacements.Outcome.RECOVERED_MIGRATION;
    }

    private static boolean terrainMatches(ServerLevel level) {
        BlockPos support = NATIVE_BLOCK_POSITION.below();
        BlockState state = level.getBlockState(support);
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(SITE.supportBlock())
                && state.isFaceSturdy(level, support, Direction.UP)
                && SITE.landmarks().stream().allMatch(landmark -> landmark.matches(level));
    }

    private static boolean validOwner(EntityOriginalNpc npc, UUID id) {
        return npc.isAlive() && !npc.isRemoved() && npc.getUUID().equals(id) && NPC_ID.equals(npc.getNpcId())
                && npc.getType() == BSEntityRegistry.ORIGINAL_NPC.get()
                && npc.position().distanceToSqr(POSITION) <= 1.0E-8D;
    }

    private static boolean clearance(ServerLevel level, EntityOriginalNpc npc) {
        AABB bounds = npc.getBoundingBox();
        if (level.containsAnyLiquid(bounds) || !level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != npc && entity.isAlive() && !entity.isSpectator()).isEmpty()) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX - 1.0E-6D, bounds.minY - 1.0E-6D, bounds.minZ - 1.0E-6D),
                BlockPos.containing(bounds.maxX + 1.0E-6D, bounds.maxY + 1.0E-6D, bounds.maxZ + 1.0E-6D))) {
            if (!pos.equals(NATIVE_BLOCK_POSITION) && Shapes.joinIsNotEmpty(Shapes.create(bounds),
                    level.getBlockState(pos).getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) return false;
        }
        BlockPos support = NATIVE_BLOCK_POSITION.below();
        return Shapes.joinIsNotEmpty(Shapes.create(bounds.move(0.0D, -0.02D, 0.0D)),
                level.getBlockState(support).getCollisionShape(level, support).move(support.getX(), support.getY(), support.getZ()), BooleanOp.AND);
    }

    private static void rollback(ServerLevel level, OriginalNpcPlacementData data, BlockState original,
                                 EntityOriginalNpc npc, boolean created) {
        if (created) npc.discard();
        if (level.getBlockState(NATIVE_BLOCK_POSITION).isAir()) level.setBlock(NATIVE_BLOCK_POSITION, original, Block.UPDATE_CLIENTS);
        if (level.getBlockState(NATIVE_BLOCK_POSITION).equals(original)) data.cancelVisualMigration(NPC_ID);
    }

    private static OriginalNpcPlacements.Landmark landmark(int x, int y, int z, String block, Map<String, String> properties) {
        return new OriginalNpcPlacements.Landmark(new BlockPos(x, y, z), block, properties);
    }
}
