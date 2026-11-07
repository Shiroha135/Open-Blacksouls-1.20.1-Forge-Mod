package yuki135.blacksouls.compat.scene;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.OriginalWorldBlocks;
import yuki135.blacksouls.util.HokoniwaDestination;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalPoolTile65Placement {
    public static final String DATA_NAME = "blacksouls_original_pool_tile65";
    public static final BlockPos POSITION = new BlockPos(861, 18, -567);
    public static final AABB VOLUME = new AABB(861, 17, -567, 862, 19.5D, -566);

    private OriginalPoolTile65Placement() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % 100 == 0 && player.isAlive() && !player.isSpectator()
                && HokoniwaDestination.DIMENSION.equals(player.level().dimension())
                && player.distanceToSqr(POSITION.getCenter()) <= 4096.0D) install(player);
    }

    public static Outcome install(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!HokoniwaDestination.DIMENSION.equals(level.dimension())) return Outcome.WRONG_DIMENSION;
        if (!OriginalNpcPlacements.hasDeployedMap(level)) return Outcome.MAP_NOT_DEPLOYED;
        if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(POSITION.getCenter()) > 4096.0D) return Outcome.TOO_FAR;
        for (ChunkPos chunk : OriginalPoolCheshirePlacement.runtimeRequiredChunks()) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null || !level.areEntitiesLoaded(chunk.toLong())) return Outcome.CHUNKS_UNAVAILABLE;
        }
        if (!OriginalPoolCheshirePlacement.terrainMatches(level)) return Outcome.TERRAIN_MISMATCH;
        State data = get(level);
        if (data.installed) return level.getBlockState(POSITION).is(OriginalWorldBlocks.POOL_TILE65.get())
                ? Outcome.ALREADY_RECORDED : Outcome.COLLISION;
        if (!level.getBlockState(POSITION).is(Blocks.AIR) || !level.getBlockState(POSITION.above()).is(Blocks.AIR)) return Outcome.COLLISION;
        if (level.getServer().getPlayerList().getPlayers().stream().anyMatch(candidate -> candidate.level() == level
                && candidate.isAlive() && !candidate.isSpectator() && candidate.getBoundingBox().intersects(VOLUME))) return Outcome.COLLISION;
        if (!level.getEntities((Entity) null, VOLUME, entity -> !entity.isSpectator()
                && !(entity instanceof EntityOriginalNpc npc && OriginalPoolCheshirePlacement.isOwnedProxy(npc))).isEmpty()) return Outcome.COLLISION;
        if (!level.setBlock(POSITION, OriginalWorldBlocks.POOL_TILE65.get().defaultBlockState(), Block.UPDATE_ALL)) return Outcome.PLACEMENT_FAILED;
        if (!level.getBlockState(POSITION).is(OriginalWorldBlocks.POOL_TILE65.get())) return Outcome.PLACEMENT_FAILED;
        for (EntityOriginalNpc actor : level.getEntitiesOfClass(EntityOriginalNpc.class, VOLUME,
                OriginalPoolCheshirePlacement::isOwnedProxy)) actor.setStaticMapProxy(true);
        data.installed = true;
        data.setDirty();
        return Outcome.INSTALLED;
    }

    public static boolean isInstalled(ServerLevel level) {
        return get(level).installed && level.getBlockState(POSITION).is(OriginalWorldBlocks.POOL_TILE65.get());
    }

    public static State get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(State::load, State::new, DATA_NAME);
    }

    public enum Outcome {
        INSTALLED, ALREADY_RECORDED, WRONG_DIMENSION, MAP_NOT_DEPLOYED, TOO_FAR,
        CHUNKS_UNAVAILABLE, TERRAIN_MISMATCH, COLLISION, PLACEMENT_FAILED
    }

    public static final class State extends SavedData {
        private boolean installed;

        public static State load(CompoundTag tag) {
            State data = new State();
            if (tag.isEmpty()) return data;
            if (!tag.getAllKeys().equals(Set.of("Version", "Installed"))
                    || !tag.contains("Version", Tag.TAG_INT) || tag.getInt("Version") != 1
                    || !tag.contains("Installed", Tag.TAG_BYTE) || tag.getByte("Installed") < 0 || tag.getByte("Installed") > 1) {
                throw new IllegalArgumentException("Invalid original pool tile65 placement");
            }
            data.installed = tag.getBoolean("Installed");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("Version", 1);
            tag.putBoolean("Installed", installed);
            return tag;
        }
    }
}
