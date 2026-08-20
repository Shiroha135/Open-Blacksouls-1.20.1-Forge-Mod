package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.util.LibraryDestination;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundBannerPacket;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("removal")
public class ServerEventHandler {

    private static final ResourceLocation CHEST_ABANDONED_MINESHAFT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");
    private static final ResourceLocation CHEST_RUINED_PORTAL = new ResourceLocation("minecraft", "chests/ruined_portal");
    private static final ResourceLocation CHEST_SHIPWRECK_SUPPLY = new ResourceLocation("minecraft", "chests/shipwreck_supply");
    private static final ResourceLocation CHEST_SIMPLE_DUNGEON = new ResourceLocation("minecraft", "chests/simple_dungeon");
    private static final ResourceLocation CHEST_STRONGHOLD_CORRIDOR = new ResourceLocation("minecraft", "chests/stronghold_corridor");
    private static final ResourceLocation CHEST_STRONGHOLD_CROSSING = new ResourceLocation("minecraft", "chests/stronghold_crossing");
    private static final ResourceLocation CHEST_STRONGHOLD_LIBRARY = new ResourceLocation("minecraft", "chests/stronghold_library");
    private static final ResourceLocation CHEST_ANCIENT_CITY = new ResourceLocation("minecraft", "chests/ancient_city");

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation lootTableId = event.getName();
        if (lootTableId == null) {
            return;
        }

        if (CHEST_ABANDONED_MINESHAFT.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_abandoned_trash_mineshaft", 18,
                    BlackSouls.ABANDONED_TRASH.get(), 1.0F, 2.0F));
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_mineshaft", 6,
                    BlackSouls.SOUL_FADING.get(), 1.0F, 2.0F));
            return;
        }

        if (CHEST_RUINED_PORTAL.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_abandoned_trash_ruined_portal", 14,
                    BlackSouls.ABANDONED_TRASH.get(), 1.0F, 2.0F));
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_ruined_portal", 4,
                    BlackSouls.SOUL_FADING.get(), 1.0F, 1.0F));
            return;
        }

        if (CHEST_SHIPWRECK_SUPPLY.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_abandoned_trash_shipwreck", 16,
                    BlackSouls.ABANDONED_TRASH.get(), 1.0F, 3.0F));
            return;
        }

        if (CHEST_SIMPLE_DUNGEON.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_abandoned_trash_dungeon", 10,
                    BlackSouls.ABANDONED_TRASH.get(), 1.0F, 2.0F));
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_dungeon", 10,
                    BlackSouls.SOUL_FADING.get(), 1.0F, 2.0F));
            return;
        }

        if (CHEST_STRONGHOLD_CORRIDOR.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_stronghold_corridor", 12,
                    BlackSouls.SOUL_FADING.get(), 1.0F, 2.0F));
            return;
        }

        if (CHEST_STRONGHOLD_CROSSING.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_stronghold_crossing", 14,
                    BlackSouls.SOUL_FADING.get(), 1.0F, 3.0F));
            return;
        }

        if (CHEST_STRONGHOLD_LIBRARY.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_stronghold_library", 16,
                    BlackSouls.SOUL_FADING.get(), 2.0F, 4.0F));
            return;
        }

        if (CHEST_ANCIENT_CITY.equals(lootTableId)) {
            event.getTable().addPool(createSingleRollPool("bs2_soul_fading_ancient_city", 8,
                    BlackSouls.SOUL_FADING.get(), 2.0F, 5.0F));
        }
    }

    private static LootPool createSingleRollPool(String poolName, int weight, net.minecraft.world.item.Item item, float minCount, float maxCount) {
        return LootPool.lootPool()
                .name(poolName)
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(item)
                        .setWeight(weight)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minCount, maxCount))))
                .build();
    }

    @SubscribeEvent
    public static void onBonfireInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());

        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            ResourceLocation currentDim = event.getLevel().dimension().location();

            if (currentDim.getNamespace().equals("blacksouls") && currentDim.getPath().equals("library")) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
                    com.BlackSouls.BlackSoulsMod.capability.BSWorldData data =
                            com.BlackSouls.BlackSoulsMod.capability.BSWorldData.get(serverPlayer.server.overworld());

                    com.BlackSouls.BlackSoulsMod.network.NetworkHandler.sendToPlayer(
                            new com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncBonfireList(data.activatedBonfires),
                            serverPlayer
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity deadEntity = event.getEntity();

        if (deadEntity instanceof Enemy && event.getSource().getEntity() instanceof Player player) {
            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                long baseSoul = com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedSoulReward(
                        deadEntity.level(),
                        com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.getSoulReward(deadEntity)
                );
                double multiplier = 1.0D;
                int silverSerpentCount = StatEventHandler.getBaubleCount(player, BlackSouls.RING_SILVER_SERPENT.get());
                if (silverSerpentCount > 0) {
                    multiplier *= (1.0 + silverSerpentCount);
                }
                long finalSouls = Math.round(baseSoul * multiplier);

                if (finalSouls > 0) {
                    stats.souls += finalSouls;
                    StatEventHandler.applyStats(player);
                    StatEventHandler.syncToClient(player);

                    if (player instanceof ServerPlayer serverPlayer) {
                        NetworkHandler.sendToPlayer(new ClientboundBannerPacket(ClientboundBannerPacket.Type.SOUL_GAIN, finalSouls), serverPlayer);
                    }

                    if (BlackSouls.ITEM1_EVENT != null) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                BlackSouls.ITEM1_EVENT.get(), SoundSource.PLAYERS, 0.5F, 1.2F);
                    }
                }
            });
        }
        if (deadEntity instanceof ServerPlayer serverPlayer) {
            com.BlackSouls.BlackSoulsMod.capability.BSWorldData data =
                    com.BlackSouls.BlackSoulsMod.capability.BSWorldData.get(serverPlayer.server.overworld());
            data.deathCount++;
            data.setDirty();
            com.BlackSouls.BlackSoulsMod.network.NetworkHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                    new com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncDifficulty(data)
            );
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                    new ResourceLocation(BlackSouls.MODID, "hell_prince_bgm"),
                    SoundSource.HOSTILE
            ));
        }
    }

    @SubscribeEvent
    public static void onMobTargetWeightTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || mob.level().isClientSide()
                || !(mob instanceof Enemy)
                || mob.getPersistentData().contains(SceneSpawnerBounds.ORIGIN_X_TAG)
                || !(mob.getTarget() instanceof Player currentTarget)) {
            return;
        }
        if (getTargetingWeight(currentTarget) <= 0.0D) {
            mob.setTarget(null);
            return;
        }
        if (mob.tickCount % 20 != 0) {
            return;
        }

        double followRange = 16.0;
        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) != null) {
            followRange = Math.max(followRange, mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).getValue());
        }

        List<Player> candidates = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(followRange),
                player -> player.isAlive()
                        && !player.isSpectator()
                        && !player.isCreative()
                        && getTargetingWeight(player) > 0.0D
                        && mob.hasLineOfSight(player));
        if (candidates.isEmpty()) {
            mob.setTarget(null);
            return;
        }
        if (candidates.size() == 1) {
            if (mob.getTarget() != candidates.get(0)) {
                mob.setTarget(candidates.get(0));
            }
            return;
        }

        Player chosen = chooseWeightedTarget(candidates, mob.getRandom().nextDouble());
        if (chosen != null && chosen != mob.getTarget()) {
            mob.setTarget(chosen);
        }
    }

    private static Player chooseWeightedTarget(List<Player> candidates, double roll) {
        double totalWeight = 0.0;
        for (Player candidate : candidates) {
            totalWeight += getTargetingWeight(candidate);
        }

        if (totalWeight <= 0.0) {
            return null;
        }

        double value = roll * totalWeight;
        Player lastEligible = null;
        for (Player candidate : candidates) {
            double weight = getTargetingWeight(candidate);
            if (weight <= 0.0D) {
                continue;
            }
            lastEligible = candidate;
            value -= weight;
            if (value <= 0.0) {
                return candidate;
            }
        }

        return lastEligible;
    }

    private static double getTargetingWeight(Player player) {
        return player.getCapability(BSPlayerStats.CAPABILITY)
                .map(stats -> Math.max(0.0, stats.targetingRate))
                .orElse(1.0);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                if (!stats.hasVisitedLibrary) {
                    MinecraftServer server = serverPlayer.getServer();
                    if (server == null) {
                        return;
                    }

                    ServerLevel libLevel = server.getLevel(LibraryDestination.DIMENSION);

                    if (libLevel != null && LibraryDestination.isLandingSafe(libLevel)) {
                        stats.hasVisitedLibrary = true;
                        serverPlayer.teleportTo(
                                libLevel,
                                LibraryDestination.X,
                                LibraryDestination.Y,
                                LibraryDestination.Z,
                                LibraryDestination.YAW,
                                0.0F
                        );
                        serverPlayer.sendSystemMessage(Component.translatable("message.blacksouls.library_awaken").withStyle(ChatFormatting.GRAY));
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onMobEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && !(entity instanceof Player) && entity.level() instanceof ServerLevel serverLevel) {
            net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket packet =
                    new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(entity.getId(), event.getEffectInstance());
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(entity) <= 4096) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectRemoved(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && !(entity instanceof Player) && entity.level() instanceof ServerLevel serverLevel) {
            net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket packet =
                    new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(entity.getId(), event.getEffect());
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(entity) <= 4096) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && !(entity instanceof Player) && entity.level() instanceof ServerLevel serverLevel) {
            net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket packet = null;
            if (event.getEffectInstance() != null) {
                packet = new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(
                        entity.getId(),
                        event.getEffectInstance().getEffect()
                );
            }
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(entity) <= 4096 && packet != null) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
        com.BlackSouls.BlackSoulsMod.util.MapDeployer.deploy(event.getServer());
    }
}
