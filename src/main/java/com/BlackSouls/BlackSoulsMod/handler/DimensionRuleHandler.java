package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityCheshireCat;
import com.BlackSouls.BlackSoulsMod.util.HokoniwaDestination;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DimensionRuleHandler {

    private static final String PREVIOUS_GAME_MODE_KEY = "BlacksoulsLibraryPreviousGameMode";
    private static final String DEATH_INVENTORY_KEY = "BlacksoulsLibraryDeathInventory";
    private static final String DEATH_CURIOS_KEY = "BlacksoulsLibraryDeathCurios";
    private static final Map<UUID, DeathInventorySnapshot> DEATH_INVENTORIES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (HokoniwaDestination.isHokoniwa(player.level().dimension())
                && isAdventure(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (HokoniwaDestination.isHokoniwa(player.level().dimension())
                    && isAdventure(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            HokoniwaDestination.migrateLegacyPlayer(player);
            applyPlayerRules(player);
            EntityCheshireCat.recoverLegacyRabbitHoleEntity(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HokoniwaDestination.migrateLegacyPlayer(player);
            applyPlayerRules(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HokoniwaDestination.migrateLegacyPlayer(player);
            applyPlayerRules(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalData = event.getOriginal().getPersistentData();
        if (originalData.contains(PREVIOUS_GAME_MODE_KEY, Tag.TAG_STRING)) {
            event.getEntity().getPersistentData().putString(
                    PREVIOUS_GAME_MODE_KEY,
                    originalData.getString(PREVIOUS_GAME_MODE_KEY));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        captureDeathInventory(player);
    }

    public static void captureDeathInventory(ServerPlayer player) {
        if (!HokoniwaDestination.isHokoniwa(player.level().dimension())) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        ListTag inventory = player.getInventory().save(new ListTag());
        DeathInventorySnapshot snapshot = new DeathInventorySnapshot(inventory.copy());
        DEATH_INVENTORIES.put(player.getUUID(), snapshot);
        saveDeathInventory(data, snapshot);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && HokoniwaDestination.isHokoniwa(player.level().dimension())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeathInventoryClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag originalData = event.getOriginal().getPersistentData();
        DeathInventorySnapshot snapshot = DEATH_INVENTORIES.get(event.getEntity().getUUID());
        if (snapshot == null) {
            snapshot = loadDeathInventory(originalData);
        }
        if (snapshot == null) {
            return;
        }
        Player player = event.getEntity();
        restoreDeathInventory(player, snapshot);
        saveDeathInventory(player.getPersistentData(), snapshot);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        DeathInventorySnapshot snapshot = DEATH_INVENTORIES.remove(player.getUUID());
        if (snapshot == null) {
            snapshot = loadDeathInventory(player.getPersistentData());
        }
        if (snapshot != null) {
            restoreDeathInventory(player, snapshot);
        }
        clearDeathInventory(player.getPersistentData());
    }

    private static void restoreDeathInventory(Player player, DeathInventorySnapshot snapshot) {
        player.getInventory().clearContent();
        player.getInventory().load(snapshot.inventory().copy());
    }

    private static void saveDeathInventory(CompoundTag data, DeathInventorySnapshot snapshot) {
        data.put(DEATH_INVENTORY_KEY, snapshot.inventory().copy());
        data.remove(DEATH_CURIOS_KEY);
    }

    private static DeathInventorySnapshot loadDeathInventory(CompoundTag data) {
        if (!data.contains(DEATH_INVENTORY_KEY, Tag.TAG_LIST)) {
            return null;
        }
        return new DeathInventorySnapshot(
                data.getList(DEATH_INVENTORY_KEY, Tag.TAG_COMPOUND).copy());
    }

    private static void clearDeathInventory(CompoundTag data) {
        data.remove(DEATH_INVENTORY_KEY);
        data.remove(DEATH_CURIOS_KEY);
    }

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        MobSpawnType spawnType = event.getSpawnType();
        if (HokoniwaDestination.isHokoniwa(event.getLevel().getLevel().dimension())
                && (spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.PATROL
                || spawnType == MobSpawnType.REINFORCEMENT)) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onForbiddenEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && HokoniwaDestination.isHokoniwa(event.getLevel().dimension())
                && (event.getEntity().getType() == EntityType.BAT
                || event.getEntity().getType() == EntityType.GLOW_SQUID)) {
            event.setCanceled(true);
        }
    }

    private static void applyPlayerRules(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (HokoniwaDestination.isHokoniwa(player.level().dimension())) {
            if (!data.contains(PREVIOUS_GAME_MODE_KEY, Tag.TAG_STRING)) {
                data.putString(PREVIOUS_GAME_MODE_KEY, player.gameMode.getGameModeForPlayer().getName());
                if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                    player.setGameMode(GameType.ADVENTURE);
                }
            }
            if (player.getFoodData().getFoodLevel() != 20) {
                player.getFoodData().setFoodLevel(20);
            }
            if (player.getFoodData().getSaturationLevel() != 20.0F) {
                player.getFoodData().setSaturation(20.0F);
            }
            return;
        }
        if (data.contains(PREVIOUS_GAME_MODE_KEY, Tag.TAG_STRING)) {
            GameType previous = gameType(data.getString(PREVIOUS_GAME_MODE_KEY));
            data.remove(PREVIOUS_GAME_MODE_KEY);
            if (player.gameMode.getGameModeForPlayer() != previous) {
                player.setGameMode(previous);
            }
        }
    }

    private static GameType gameType(String name) {
        for (GameType gameType : GameType.values()) {
            if (gameType.getName().equals(name)) {
                return gameType;
            }
        }
        return GameType.SURVIVAL;
    }

    private static boolean isAdventure(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
    }

    private record DeathInventorySnapshot(ListTag inventory) {
    }
}
