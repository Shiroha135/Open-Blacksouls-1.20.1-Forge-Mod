package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundAdviceVisibilityPacket;
import yuki135.blacksouls.util.KnightStartingKit;
import yuki135.blacksouls.util.OriginalOpeningPersistence;
import yuki135.blacksouls.util.SkillUtils;
import yuki135.blacksouls.util.StoryNameData;
import yuki135.blacksouls.util.skill.SkillSeekAdvice;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Mod.EventBusSubscriber(modid=BlackSouls.MODID)
public final class OriginalLegacyNameCompletion {
    public static final String TAG_KEY="bs2_legacy_name_completion";
    private static final String KIT="bs2_knight_starting_kit_granted";
    private static final Map<ServerPlayer,UUID> LOADED=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,Tickets>> TICKETS=new WeakHashMap<>();
    private static final TicketType<UUID> TICKET=TicketType.create("blacksouls_legacy_name_completion",Comparator.comparing(UUID::toString));
    private static final Set<String> START_ITEMS=Set.of("black_ash","dev_revenge_mode","dev_death_mode","dev_legendary_mode","dev_malice_mode","dev_eternity_mode");

    private OriginalLegacyNameCompletion() {
    }

    public static void loaded(ServerPlayer player,CompoundTag saved) {
        LOADED.remove(player);
        if (player==null || player.server==null || !player.server.isSameThread() || saved==null
                || !saved.hasUUID("UUID") || !player.getUUID().equals(saved.getUUID("UUID"))
                || player.server.getPlayerList().getPlayer(player.getUUID())!=null || LOADED.size()>=2048) return;
        var persisted=saved.getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG);
        var capability=saved.getCompound("ForgeCaps").getCompound("blacksouls:bs_stats");
        if (capability.isEmpty() || capability.contains(OriginalOpeningProfile.TAG_KEY) || persisted.contains(TAG_KEY)
                || !persisted.getBoolean("bs2_story_name_started") || persisted.getBoolean("bs2_story_name_confirmed")
                || persisted.getBoolean(KIT) || capability.getBoolean("HasVisitedLibrary") || !unprogressed(player)) return;
        LOADED.put(player,player.getUUID());
    }

    public static boolean eligible(ServerPlayer player) {
        return live(player) && player.getUUID().equals(LOADED.get(player)) && StoryNameData.hasStarted(player)
                && !StoryNameData.isConfirmed(player) && !data(player).contains(TAG_KEY) && unprogressed(player);
    }

    public static boolean reserve(ServerPlayer player) {
        if (!eligible(player) || busy(player)) return false;
        var marker=new CompoundTag();
        marker.putInt("Version",1);
        marker.putUUID("Owner",player.getUUID());
        marker.putString("State","NAME");
        data(player).put(TAG_KEY,marker);
        if (!save(player)) { data(player).remove(TAG_KEY);return false; }
        LOADED.remove(player);
        return true;
    }

    public static void confirmed(ServerPlayer player) {
        if (!live(player) || !StoryNameData.isConfirmed(player)) return;
        var marker=marker(player);
        if (marker==null || !marker.getString("State").equals("NAME")) return;
        marker.putString("State","KIT");
        save(player);
    }

    public static boolean pending(ServerPlayer player) {
        if (player==null) return false;
        var marker=marker(player);
        return marker!=null && !Set.of("DONE","CANCELLED").contains(marker.getString("State"));
    }

    public static boolean complete(ServerPlayer player) {
        var marker=player==null?null:marker(player);
        return marker!=null && marker.getString("State").equals("DONE");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || event.getServer().getTickCount()%10!=0) return;
        for (var player:List.copyOf(event.getServer().getPlayerList().getPlayers())) {
            if (!pending(player)) continue;
            try { advance(player); }
            catch(RuntimeException failure) { release(player);BlackSouls.LOGGER.warn("Legacy name completion waits for {}",player.getUUID(),failure); }
        }
    }

    private static void advance(ServerPlayer player) {
        if (!live(player)) return;
        if (busy(player) || !StoryNameData.isConfirmed(player)) { release(player);return; }
        var stats=stats(player);
        var marker=marker(player);
        if (stats==null || marker==null || !stats.openingProfile().isLegacy() || stats.openingProfile().hasRejectedData()) return;
        if (stats.hasVisitedLibrary) {
            marker.putString("State","CANCELLED");save(player);release(player);return;
        }
        String state=marker.getString("State");
        if (state.equals("NAME")) { marker.putString("State","KIT");if(!save(player))return;state="KIT"; }
        if (state.equals("KIT")) {
            if (!data(player).getBoolean(KIT) && !unprogressed(player)) { marker.putString("State","CANCELLED");save(player);return; }
            if (!commitKit(player)) return;
            marker=marker(player);
            state=marker.getString("State");
        }
        if (!state.equals("ARRIVAL") || !data(player).getBoolean(KIT)) return;
        var destination=OriginalOpeningArrival.destination();
        var target=player.server.getLevel(yuki135.blacksouls.util.HokoniwaDestination.DIMENSION);
        if (target==null) { release(player);return; }
        tickets(player,target,new ChunkPos(net.minecraft.core.BlockPos.containing(destination.x(),destination.y(),destination.z())));
        if (!OriginalOpeningArrival.ready(target,player,destination)) return;
        player.stopRiding();
        player.teleportTo(target,destination.x(),destination.y(),destination.z(),destination.yaw(),destination.pitch());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance=0;
        if (!OriginalOpeningArrival.acknowledge(player,destination)) return;
        stats.hasVisitedLibrary=true;
        marker.putString("State","DONE");
        if (!save(player)) { stats.hasVisitedLibrary=false;marker.putString("State","ARRIVAL");return; }
        release(player);
        StatEventHandler.syncToClient(player);
    }

    private static boolean commitKit(ServerPlayer player) {
        if (!save(player)) return false;
        var before=new Snapshot(player);
        try {
            KnightStartingKit.grant(player);
            SkillSeekAdvice.resetVisibility(player);
            player.setHealth(player.getMaxHealth());
            marker(player).putString("State","ARRIVAL");
            if (save(player)) return true;
        } catch(RuntimeException failure) { BlackSouls.LOGGER.warn("Legacy starter rollback for {}",player.getUUID(),failure); }
        before.restore(player);
        save(player);
        return false;
    }

    private static boolean unprogressed(ServerPlayer player) {
        var stats=stats(player);
        if (stats==null || !stats.openingProfile().isLegacy() || stats.openingProfile().hasStoredData() || stats.hasVisitedLibrary
                || stats.level!=1 || stats.currentExp!=0 || stats.souls!=0 || stats.lostSouls!=0 || stats.whiteBearIntroduced
                || stats.whiteBearFreeSoulsClaimed || stats.whiteBearProgress!=0 || !stats.unlockedSkills.isEmpty()
                || !stats.unlockedCovenants.isEmpty() || !stats.unlockedBonfires.isEmpty() || !stats.originalSpirits.isEmpty()
                || stats.originalSpiritPartner!=0 || !stats.activeCovenant.isEmpty() || stats.purgeMobKills!=0 || stats.purgeOreBreaks!=0
                || stats.lastTradeTime!=0 || !stats.lostDim.isEmpty() || player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM,net.minecraft.stats.Stats.DEATHS)!=0
                || stats.bonusHp!=0 || stats.bonusMp!=0 || stats.bonusAtk!=0 || stats.bonusDef!=0 || stats.bonusMatk!=0
                || stats.bonusMdef!=0 || stats.bonusLuc!=0 || stats.bonusSpeed!=0 || data(player).getBoolean(KIT)) return false;
        var seen=new java.util.HashSet<String>();
        for(var stack:player.getInventory().items) {
            if(stack.isEmpty())continue;
            var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
            if(id==null || !id.getNamespace().equals(BlackSouls.MODID) || !START_ITEMS.contains(id.getPath()) || stack.getCount()!=1 || !seen.add(id.getPath()))return false;
        }
        if(player.getInventory().armor.stream().anyMatch(stack->!stack.isEmpty()) || !player.getOffhandItem().isEmpty())return false;
        var curios=CuriosApi.getCuriosInventory(player).orElse(null);
        if(curios==null)return false;
        return curios.getCurios().values().stream().allMatch(group->{
            for(var handler:List.of(group.getStacks(),group.getCosmeticStacks()))for(int slot=0;slot<handler.getSlots();slot++)if(!handler.getStackInSlot(slot).isEmpty())return false;
            return true;
        });
    }

    private static boolean busy(ServerPlayer player) {
        return !player.isAlive() || player.isSpectator() || player.isSleeping() || player.isPassenger() || player.isVehicle()
                || player.containerMenu!=player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
                || OriginalOpeningService.managed(player) || TurnBattleManager.isInBattle(player) || OriginalNpcService.isConversing(player)
                || OriginalDodoRunService.pending(player) || OriginalWorldTransferService.pending(player)
                || !OriginalNpcProgress.get(player.server).conversationIds(player.getUUID()).isEmpty();
    }

    private static boolean live(ServerPlayer player) {
        return player!=null && player.server!=null && player.server.isSameThread() && player.connection!=null
                && player.getUUID().equals(player.getGameProfile().getId()) && !player.isRemoved() && player.server.getPlayerList().getPlayer(player.getUUID())==player;
    }

    private static CompoundTag data(ServerPlayer player) { return SkillUtils.getPersistedData(player); }
    private static BSPlayerStats stats(ServerPlayer player) { return player==null?null:player.getCapability(BSPlayerStats.CAPABILITY).orElse(null); }
    private static CompoundTag marker(ServerPlayer player) {
        if(!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG,Tag.TAG_COMPOUND))return null;
        var persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(!persisted.contains(TAG_KEY,Tag.TAG_COMPOUND))return null;
        var tag=persisted.getCompound(TAG_KEY);
        if(!tag.getAllKeys().equals(Set.of("Version","Owner","State")) || !tag.contains("Version",Tag.TAG_INT) || tag.getInt("Version")!=1
                || !tag.hasUUID("Owner") || !player.getUUID().equals(tag.getUUID("Owner")) || !tag.contains("State",Tag.TAG_STRING)
                || !Set.of("NAME","KIT","ARRIVAL","DONE","CANCELLED").contains(tag.getString("State")))return null;
        return tag;
    }

    private static boolean save(ServerPlayer player) {
        if(!OriginalOpeningPersistence.saveAndVerify(player))return false;
        try {
            Path file=player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID()+".dat");
            var saved=NbtIo.readCompressed(file.toFile());
            var expected=player.saveWithoutId(new CompoundTag());
            return java.util.Objects.equals(saved.get("Dimension"),expected.get("Dimension")) && java.util.Objects.equals(saved.get("Pos"),expected.get("Pos"))
                    && java.util.Objects.equals(saved.get("Rotation"),expected.get("Rotation"));
        } catch(java.io.IOException failure) { return false; }
    }

    private record Tickets(ServerLevel level,List<ChunkPos> chunks) { }
    private static void tickets(ServerPlayer player,ServerLevel level,ChunkPos center) {
        var byOwner=TICKETS.computeIfAbsent(player.server,ignored->new LinkedHashMap<>());
        var previous=byOwner.get(player.getUUID());
        if(previous!=null && previous.level()==level)return;
        release(player);
        var chunks=new ArrayList<ChunkPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var chunk=new ChunkPos(center.x+x,center.z+z);chunks.add(chunk);
            level.getChunkSource().addRegionTicket(TICKET,chunk,2,player.getUUID());
        }
        TICKETS.computeIfAbsent(player.server,ignored->new LinkedHashMap<>()).put(player.getUUID(),new Tickets(level,List.copyOf(chunks)));
    }
    private static void release(ServerPlayer player) {
        var byOwner=TICKETS.get(player.server);
        var tickets=byOwner==null?null:byOwner.remove(player.getUUID());
        if(tickets!=null)for(var chunk:tickets.chunks())tickets.level().getChunkSource().removeRegionTicket(TICKET,chunk,2,player.getUUID());
        if(byOwner!=null&&byOwner.isEmpty())TICKETS.remove(player.server);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) { LOADED.remove(player);release(player); }
    }
    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        LOADED.keySet().removeIf(player->player.server==event.getServer());
        var byOwner=TICKETS.remove(event.getServer());
        if(byOwner!=null)byOwner.forEach((owner,tickets)->tickets.chunks().forEach(chunk->tickets.level().getChunkSource().removeRegionTicket(TICKET,chunk,2,owner)));
    }

    private record Curio(IDynamicStackHandler handler,int slot,ItemStack stack) { }
    private static final class Snapshot {
        final ListTag inventory;
        final int selected;
        final CompoundTag stats,persistent;
        final float health;
        final List<Curio> curios=new ArrayList<>();
        Snapshot(ServerPlayer player) {
            inventory=player.getInventory().save(new ListTag());selected=player.getInventory().selected;
            stats=stats(player).serializeNBT().copy();persistent=player.getPersistentData().copy();health=player.getHealth();
            CuriosApi.getCuriosInventory(player).orElseThrow(()->new IllegalStateException("Missing legacy Curios inventory")).getCurios().values().forEach(group->{
                for(var handler:List.of(group.getStacks(),group.getCosmeticStacks()))for(int slot=0;slot<handler.getSlots();slot++)curios.add(new Curio(handler,slot,handler.getStackInSlot(slot).copy()));
            });
        }
        void restore(ServerPlayer player) {
            player.getInventory().load(inventory.copy());player.getInventory().selected=selected;
            curios.forEach(row->row.handler().setStackInSlot(row.slot(),row.stack().copy()));
            stats(player).deserializeNBT(stats.copy());
            var data=player.getPersistentData();
            for(var key:Set.copyOf(data.getAllKeys()))data.remove(key);
            persistent.getAllKeys().forEach(key->data.put(key,persistent.get(key).copy()));
            StatEventHandler.applyStats(player);stats(player).mp=stats.getDouble("MP");player.setHealth(Math.min(health,player.getMaxHealth()));
            StatEventHandler.syncToClient(player);
            var saved=data(player);
            NetworkHandler.sendToPlayer(new ClientboundAdviceVisibilityPacket(saved.getBoolean(SkillSeekAdvice.CONTROLLED_TAG),
                    !saved.contains(SkillSeekAdvice.VISIBLE_TAG)||saved.getBoolean(SkillSeekAdvice.VISIBLE_TAG)),player);
        }
    }
}
