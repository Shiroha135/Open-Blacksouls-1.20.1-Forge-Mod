package yuki135.blacksouls.util;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSWorldData;
import yuki135.blacksouls.capability.BonfireEntry;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundDevBonfiresPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DevBonfireService {
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final Map<ServerLevel, Map<Long, LevelChunk>> LOADED = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, CompletableFuture<List<BonfireEntry>>> SCANS = new HashMap<>();

    private record LevelScan(ResourceKey<Level> dimension, Path directory, ChunkScanAccess scanner) {
    }

    public static boolean canUse(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!player.isCreative() || !player.hasPermissions(4)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.dev.no_permission").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    public static void request(ServerPlayer player, boolean refresh) {
        if (!canUse(player)) {
            return;
        }
        MinecraftServer server = player.server;
        CompletableFuture<List<BonfireEntry>> scan = SCANS.get(server);
        if (scan == null || scan.isCompletedExceptionally() || refresh && scan.isDone()) {
            List<LevelScan> levels = new ArrayList<>();
            for (ServerLevel level : server.getAllLevels()) {
                Path directory = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT)).resolve("region");
                levels.add(new LevelScan(level.dimension(), directory, level.getChunkSource().chunkScanner()));
            }
            scan = CompletableFuture.supplyAsync(() -> scanSavedBonfires(server, levels), Util.backgroundExecutor());
            SCANS.put(server, scan);
        }
        scan.whenComplete((entries, failure) -> {
            if (!server.isStopped()) {
                server.execute(() -> {
                    if (player.hasDisconnected() || !canUse(player)) {
                        return;
                    }
                    if (failure != null) {
                        BlackSouls.LOGGER.warn("Could not index developer bonfires", failure);
                    }
                    NetworkHandler.sendToPlayer(new ClientboundDevBonfiresPacket(
                            collectCurrentBonfires(server, entries == null ? List.of() : entries, player.level().dimension()), failure == null), player);
                });
            }
        });
    }

    private static List<BonfireEntry> scanSavedBonfires(MinecraftServer server, List<LevelScan> levels) {
        List<BonfireEntry> result = new ArrayList<>();
        try {
            for (LevelScan level : levels) {
                for (ChunkPos chunk : savedChunks(level.directory())) {
                    if (server.isStopped()) {
                        return List.of();
                    }
                    CollectFields collector = new CollectFields(new FieldSelector(ListTag.TYPE, "block_entities"));
                    level.scanner().scanChunk(chunk, collector).join();
                    if (!(collector.getResult() instanceof CompoundTag data)) {
                        continue;
                    }
                    ListTag entities = data.getList("block_entities", Tag.TAG_COMPOUND);
                    for (int i = 0; i < entities.size(); i++) {
                        CompoundTag entity = entities.getCompound(i);
                        if (!entity.getString("id").equals("minecraft:campfire")) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(entity.getInt("x"), entity.getInt("y"), entity.getInt("z"));
                        CompoundTag metadata = entity.getCompound("ForgeData");
                        String name = metadata.getString("BlackSoulsBonfireName");
                        String description = metadata.getString("BlackSoulsBonfireDescription");
                        if (HokoniwaDestination.isHokoniwa(level.dimension()) && pos.equals(HokoniwaDestination.FIRST_BONFIRE_POS)) {
                            name = "gui.blacksouls.bonfire.library_name";
                            description = "gui.blacksouls.bonfire.library_desc";
                        }
                        result.add(new BonfireEntry(GlobalPos.of(level.dimension(), pos), name.isBlank() ? BonfireMetadata.DEFAULT_NAME : name, description));
                    }
                }
            }
            return List.copyOf(result);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static List<ChunkPos> savedChunks(Path directory) throws IOException {
        List<ChunkPos> chunks = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return chunks;
        }
        try (var regions = Files.list(directory)) {
            for (Path region : regions.toList()) {
                var matcher = REGION_NAME.matcher(region.getFileName().toString());
                if (!matcher.matches()) {
                    continue;
                }
                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));
                ByteBuffer header = ByteBuffer.allocate(4096);
                try (FileChannel file = FileChannel.open(region, StandardOpenOption.READ)) {
                    while (header.hasRemaining() && file.read(header) > 0) {
                    }
                }
                for (int i = 0; i < header.position() / 4; i++) {
                    if (header.getInt(i * 4) != 0) {
                        chunks.add(new ChunkPos(regionX * 32 + i % 32, regionZ * 32 + i / 32));
                    }
                }
            }
        }
        return chunks;
    }

    private static List<ClientboundDevBonfiresPacket.Entry> collectCurrentBonfires(MinecraftServer server, List<BonfireEntry> saved, ResourceKey<Level> preferred) {
        Map<GlobalPos, BonfireEntry> entries = new LinkedHashMap<>();
        saved.forEach(entry -> entries.put(entry.pos, entry));
        BSWorldData worldData = BSWorldData.get(server.overworld());
        worldData.activatedBonfires.forEach(entry -> entries.put(entry.pos, entry));
        entries.values().removeIf(entry -> {
            ServerLevel level = server.getLevel(entry.pos.dimension());
            if (level == null) {
                return true;
            }
            BlockPos pos = entry.pos.pos();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk != null && !chunk.getBlockState(pos).is(BlockTags.CAMPFIRES);
        });
        for (ServerLevel level : server.getAllLevels()) {
            for (LevelChunk chunk : LOADED.getOrDefault(level, Map.of()).values()) {
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (chunk.getBlockState(pos).is(BlockTags.CAMPFIRES)) {
                        BonfireMetadata.Data metadata = BonfireMetadata.read(level, pos);
                        GlobalPos location = GlobalPos.of(level.dimension(), pos);
                        entries.put(location, new BonfireEntry(location, metadata.name(), metadata.description()));
                    }
                }
            }
        }
        var activated = worldData.activatedBonfires.stream().map(entry -> entry.pos).collect(java.util.stream.Collectors.toSet());
        return displayEntries(List.copyOf(entries.values()), activated, preferred);
    }

    private record MirrorKey(BlockPos position,String name,String description) { }

    static List<ClientboundDevBonfiresPacket.Entry> displayEntries(List<BonfireEntry> entries, java.util.Set<GlobalPos> activated,
                                                                  ResourceKey<Level> preferred) {
        Map<Object,ClientboundDevBonfiresPacket.Entry> result = new LinkedHashMap<>();
        for (var entry : entries) {
            Object key = OriginalMapBonfires.mirrored(entry.pos.dimension(), entry.pos.pos())
                    ? new MirrorKey(entry.pos.pos(),entry.name,entry.description) : entry.pos;
            var previous = result.get(key);
            boolean active = activated.contains(entry.pos) || previous != null && previous.activated();
            boolean replace = previous == null || preference(entry.pos.dimension(),preferred) > preference(previous.pos().dimension(),preferred);
            var chosen = replace ? entry : new BonfireEntry(previous.pos(),previous.name(),previous.description());
            result.put(key,new ClientboundDevBonfiresPacket.Entry(chosen.pos,chosen.name,chosen.description,active));
        }
        return result.values().stream().limit(ClientboundDevBonfiresPacket.MAX_ENTRIES).toList();
    }

    private static int preference(ResourceKey<Level> dimension,ResourceKey<Level> preferred) {
        if (dimension.equals(preferred)) return 3;
        if (dimension.equals(HokoniwaDestination.DIMENSION)) return 2;
        return dimension.equals(Level.OVERWORLD) ? 1 : 0;
    }

    public static boolean teleport(ServerPlayer player, GlobalPos destination) {
        if (!canUse(player) || destination == null) {
            return false;
        }
        ServerLevel level = player.server.getLevel(destination.dimension());
        BlockPos fire = destination.pos();
        if (level == null || !level.isInWorldBounds(fire) || !level.getWorldBorder().isWithinBounds(fire)
                || TurnBattleManager.isInBattle(player) || !level.getBlockState(fire).is(BlockTags.CAMPFIRES)) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.dev.bonfire.unavailable").withStyle(ChatFormatting.RED));
            return false;
        }
        for (int radius = 1; radius <= 3; radius++) {
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos feet = fire.offset(dx, dy, dz);
                        Vec3 landing = Vec3.atBottomCenterOf(feet);
                        if (!level.isInWorldBounds(feet) || !level.getWorldBorder().isWithinBounds(feet)
                                || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                                || level.getBlockState(feet.below()).is(BlockTags.CAMPFIRES)
                                || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()
                                || !level.noCollision(player, player.getBoundingBox().move(landing.subtract(player.position())))) {
                            continue;
                        }
                        float yaw = (float) Math.toDegrees(Math.atan2(fire.getZ() - feet.getZ(), fire.getX() - feet.getX())) - 90.0F;
                        player.stopRiding();
                        player.teleportTo(level, landing.x, landing.y, landing.z, yaw, 15.0F);
                        player.fallDistance = 0.0F;
                        player.setDeltaMovement(Vec3.ZERO);
                        return true;
                    }
                }
            }
        }
        player.sendSystemMessage(Component.translatable("message.blacksouls.dev.bonfire.no_landing").withStyle(ChatFormatting.RED));
        return false;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            LOADED.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>()).put(chunk.getPos().toLong(), chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            Map<Long, LevelChunk> loaded = LOADED.get(level);
            if (loaded != null) {
                loaded.remove(chunk.getPos().toLong(), chunk);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SCANS.remove(event.getServer());
        LOADED.keySet().removeIf(level -> level.getServer() == event.getServer());
    }

    private DevBonfireService() {
    }
}
