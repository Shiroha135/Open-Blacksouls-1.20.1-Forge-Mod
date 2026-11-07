package yuki135.blacksouls.story;

import yuki135.blacksouls.story.OriginalOpeningProfile.WorldPoint;
import yuki135.blacksouls.util.HokoniwaDestination;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class OriginalOpeningArrival {
    private static final BlockPos FLOOR = new BlockPos(59,-61,157);
    private static final ResourceLocation TRIGGER = ResourceLocation.fromNamespaceAndPath("blacksouls2","map_trigger");
    private static final ResourceLocation BLOOD = ResourceLocation.fromNamespaceAndPath("blacksouls2","xie_shui");
    private static final String CURRENT_SCENE = "Blacksouls2CurrentScene";

    private OriginalOpeningArrival() {
    }

    public static WorldPoint destination() {
        return new WorldPoint(HokoniwaDestination.ID.toString(),59.5D,-60.0D,157.5D,0,0);
    }

    public static boolean ready(ServerLevel level, ServerPlayer player, WorldPoint destination) {
        if (level==null || player==null || destination==null || !destination().equals(destination)
                || !HokoniwaDestination.DIMENSION.equals(level.dimension()) || level.getServer()!=player.server
                || !level.hasChunkAt(FLOOR) || !level.areEntitiesLoaded(new ChunkPos(FLOOR).toLong())
                || !level.getWorldBorder().isWithinBounds(FLOOR) || FLOOR.getY()<level.getMinBuildHeight()
                || FLOOR.getY()+3>=level.getMaxBuildHeight()) return false;
        var state=level.getBlockState(FLOOR);
        var entity=level.getBlockEntity(FLOOR);
        if (!TRIGGER.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock())) || entity==null
                || !TRIGGER.equals(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(entity.getType()))) return false;
        var tag=entity.saveWithoutMetadata();
        if (!tag.getBoolean("Configured") || tag.getBoolean("Enabled") || tag.getBoolean("RedstoneEnabled")
                || !tag.getBoolean("SceneEnabled") || !"map_001".equals(tag.getString("SceneId"))
                || !"minecraft:stone".equals(tag.getString("Camouflage"))
                || tag.getDouble("TargetX")!=59.5D || tag.getDouble("TargetY")!=-60.0D || tag.getDouble("TargetZ")!=157.5D) return false;
        var feet=FLOOR.above();
        var fluid=level.getFluidState(feet);
        if (!fluid.isSource() || !BLOOD.equals(ForgeRegistries.FLUIDS.getKey(fluid.getType()))
                || !level.getFluidState(feet.above()).isEmpty()) return false;
        var body=new AABB(destination.x()-.3D,destination.y(),destination.z()-.3D,
                destination.x()+.3D,destination.y()+1.8D,destination.z()+.3D);
        if (!level.noCollision(player,body) || level.noCollision(player,body.move(0,-.05D,0))
                || !level.getEntities(player,body,other->!other.isRemoved()&&!other.isSpectator()).isEmpty()) return false;
        return level.getServer().getPlayerList().getPlayers().stream().noneMatch(other->other!=player && other.level()==level
                && !other.isRemoved() && other.isAlive() && !other.isSpectator() && other.getBoundingBox().intersects(body));
    }

    public static boolean acknowledge(ServerPlayer player, WorldPoint destination) {
        if (player==null || !ready(player.serverLevel(),player,destination)
                || player.position().distanceToSqr(new Vec3(destination.x(),destination.y(),destination.z()))>.16D) return false;
        var level=player.serverLevel();
        var state=level.getBlockState(FLOOR);
        state.getBlock().stepOn(level,FLOOR,state,player);
        return (level.dimension().location()+"#map_001").equals(player.getPersistentData().getString(CURRENT_SCENE));
    }
}
