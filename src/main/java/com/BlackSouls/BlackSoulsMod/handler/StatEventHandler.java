package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.api.event.BSStatsRecalcEvent;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.google.common.collect.HashMultimap;
import com.BlackSouls.BlackSoulsMod.entity.EntityThrownBlade;
import com.BlackSouls.BlackSoulsMod.entity.EntityMeatWall;
import com.BlackSouls.BlackSoulsMod.entity.InstantDeathImmuneEntity;
import com.BlackSouls.BlackSoulsMod.item.rings.ItemOriginalRing;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.*;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import com.BlackSouls.BlackSoulsMod.entity.EntityHellPrince;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"resource", "DataFlowIssue", "ConstantConditions", "UnnecessaryReturnStatement"})
@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public class StatEventHandler {
    private static final String TAG_FIRST_JOIN_BLACK_ASH = "bs2_first_join_black_ash";
    private static final String TAG_FIRST_JOIN_DEV_MODE_ITEMS = "bs2_first_join_dev_mode_items";
    private static final String TAG_DAGGER_EXTRA_HIT = "bs2_dagger_extra_hit";
    private static final String TAG_RING_EXTRA_HIT = "bs2_ring_extra_hit";
    private static final String TAG_RING_COMBAT_UNTIL = "bs2_ring_combat_until";
    private static final String TAG_PRECOMPUTED_SKILL_DAMAGE = "bs2_precomputed_skill_damage";
    private static final String TAG_SURE_HIT_SKILL = "bs2_sure_hit_skill";
    private static final String TAG_SKILL_INSTANT_DEATH_RATE = "bs2_skill_instant_death_rate";
    private static final String TAG_SPEAR_COUNTER = "bs2_spear_counter";
    private static final String TAG_PUPPET_RANGED_COOLDOWN = "bs2_puppet_ranged_cooldown";
    private static final UUID CHRONO_WATCH_SLOT_UUID = UUID.fromString("a7eecdcf-c4a0-4d4d-8831-fc8f7c80adf1");

    private static final double CAP_HP = BSPlayerStats.HARD_CAP_HP;
    private static final double CAP_MP = BSPlayerStats.HARD_CAP_MP;
    private static final double CAP_ATK = BSPlayerStats.HARD_CAP_OTHER;
    private static final double CAP_DEF = BSPlayerStats.HARD_CAP_OTHER;
    private static final double CAP_MATK = BSPlayerStats.HARD_CAP_OTHER;
    private static final double CAP_MDEF = BSPlayerStats.HARD_CAP_OTHER;
    private static final double CAP_LUCK = BSPlayerStats.HARD_CAP_OTHER;
    private static final double CAP_SPEED = BSPlayerStats.HARD_CAP_OTHER;
    private static final long PURGE_REFRESH_TICKS = 12000L;

    private record PurgeTemplate(String category, String targetId, int required) {}

    private record PurgeRewardTemplate(String itemId, int rewardCount) {}

    private static final PurgeTemplate[] PURGE_MOB_TEMPLATES = new PurgeTemplate[]{
            new PurgeTemplate("mob", "sheep", 5),
            new PurgeTemplate("mob", "cow", 4),
            new PurgeTemplate("mob", "pig", 5),
            new PurgeTemplate("mob", "chicken", 6),
            new PurgeTemplate("mob", "rabbit", 4),
            new PurgeTemplate("mob", "zombie", 6),
            new PurgeTemplate("mob", "skeleton", 6),
            new PurgeTemplate("mob", "spider", 5)
    };

    private static final PurgeTemplate[] PURGE_ORE_TEMPLATES = new PurgeTemplate[]{
            new PurgeTemplate("ore", "coal", 5),
            new PurgeTemplate("ore", "copper", 6),
            new PurgeTemplate("ore", "iron", 4),
            new PurgeTemplate("ore", "redstone", 4),
            new PurgeTemplate("ore", "lapis", 4)
    };

    private static final List<PurgeRewardTemplate> PURGE_REWARD_POOL = new ArrayList<>();

    private static final class BaubleCounter {
        private final Map<Item, Integer> cache = new IdentityHashMap<>();
        private boolean chronoClockEquipped;

        private BaubleCounter(LivingEntity entity) {
            CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                for (var result : handler.findCurios(stack -> !stack.isEmpty())) {
                    ItemStack stack = result.stack();
                    Item item = stack.getItem();
                    cache.put(item, cache.getOrDefault(item, 0) + 1);
                    if (SkillUtils.isChronoClockItem(stack)) {
                        chronoClockEquipped = true;
                    }
                }
            });
        }

        private int count(Item item) {
            return cache.getOrDefault(item, 0);
        }

        private int count(com.BlackSouls.BlackSoulsMod.item.rings.ItemOriginalRing.Profile profile) {
            int total = 0;
            for (Map.Entry<Item, Integer> entry : cache.entrySet()) {
                if (entry.getKey() instanceof com.BlackSouls.BlackSoulsMod.item.rings.ItemOriginalRing ring
                        && ring.getProfile() == profile) {
                    total += entry.getValue();
                }
            }
            return total;
        }

        private boolean has(Item item) {
            return count(item) > 0;
        }

        private boolean hasChronoClock() {
            return chronoClockEquipped;
        }
    }

    public static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> syncToClient(serverPlayer, stats));
        }
    }

    private static void syncToClient(ServerPlayer player, BSPlayerStats stats) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncStats(stats.serializeNBT()));
    }

    public static void ensurePurgeCommissions(ServerPlayer player) {
        long currentCycle = player.level().getDayTime() / PURGE_REFRESH_TICKS;
        long currentDay = player.level().getDayTime() / 24000L;
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            if (stats.purgeRefreshDay != currentDay) {
                stats.purgeRefreshDay = currentDay;
                stats.purgeRefreshesUsedToday = 0;
            }

            if (stats.purgeRefreshCycle == currentCycle && stats.purgeTasks.size() == 10) {
                return;
            }

            stats.purgeRefreshCycle = currentCycle;
            stats.purgeTasks.clear();
            stats.purgeTasks.addAll(rollPurgeTasks(player.getRandom()));
            syncToClient(player);
        });
    }

    private static void ensurePurgeRewardPool() {
        if (!PURGE_REWARD_POOL.isEmpty()) {
            return;
        }

        for (ResourceLocation key : ForgeRegistries.ITEMS.getKeys()) {
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item == null || !isValidPurgeReward(key, item)) {
                continue;
            }
            PURGE_REWARD_POOL.add(new PurgeRewardTemplate(key.toString(), getPurgeRewardCount(item)));
        }

        if (PURGE_REWARD_POOL.isEmpty()) {
            PURGE_REWARD_POOL.add(new PurgeRewardTemplate(BlackSouls.CANDY.getId().toString(), 1));
        }
    }

    private static boolean isValidPurgeReward(ResourceLocation key, Item item) {
        if (!BlackSouls.MODID.equals(key.getNamespace())) {
            return false;
        }

        String path = key.getPath();
        String packageName = item.getClass().getPackageName();
        ItemStack stack = new ItemStack(item);

        if (stack.isEmpty()) {
            return false;
        }

        if (packageName.contains(".item.weapon")
                || packageName.contains(".item.accessories")
                || packageName.contains(".item.rings")
                || packageName.contains(".item.skillbook")
                || packageName.contains(".item.soul")) {
            return false;
        }

        if (path.startsWith("book_")
                || path.startsWith("ring_")
                || path.startsWith("skill_book_")
                || path.startsWith("soul_")
                || path.startsWith("dev_")
                || path.endsWith("_avatar_pack")
                || path.equals("noden_spawn_egg")
                || path.startsWith("covenant_")) {
            return false;
        }

        return item.getRarity(stack) == net.minecraft.world.item.Rarity.COMMON
                || item.getRarity(stack) == net.minecraft.world.item.Rarity.UNCOMMON;
    }

    private static int getPurgeRewardCount(Item item) {
        int maxStackSize = item.getMaxStackSize();
        if (maxStackSize >= 64) {
            return 2;
        }
        return 1;
    }

    private static PurgeRewardTemplate rollPurgeReward(RandomSource random) {
        ensurePurgeRewardPool();
        return PURGE_REWARD_POOL.get(random.nextInt(PURGE_REWARD_POOL.size()));
    }

    private static List<BSPlayerStats.PurgeCommissionTask> rollPurgeTasks(RandomSource random) {
        List<BSPlayerStats.PurgeCommissionTask> tasks = new ArrayList<>();
        Set<String> usedTargets = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(createRandomPurgeTask(PURGE_MOB_TEMPLATES, usedTargets, random));
        }
        for (int i = 0; i < 5; i++) {
            tasks.add(createRandomPurgeTask(PURGE_ORE_TEMPLATES, usedTargets, random));
        }
        return tasks;
    }

    private static BSPlayerStats.PurgeCommissionTask createRandomPurgeTask(PurgeTemplate[] pool, Set<String> usedTargets, RandomSource random) {
        List<PurgeTemplate> available = new ArrayList<>();
        for (PurgeTemplate template : pool) {
            if (!usedTargets.contains(template.category + ":" + template.targetId)) {
                available.add(template);
            }
        }

        PurgeTemplate selected = available.isEmpty()
                ? pool[random.nextInt(pool.length)]
                : available.get(random.nextInt(available.size()));
        usedTargets.add(selected.category + ":" + selected.targetId);
        PurgeRewardTemplate reward = rollPurgeReward(random);
        return new BSPlayerStats.PurgeCommissionTask(
                selected.category,
                selected.targetId,
                selected.required,
                0,
                reward.itemId(),
                reward.rewardCount(),
                false
        );
    }

    private static void progressPurgeTasks(ServerPlayer player, String category, String targetId) {
        ensurePurgeCommissions(player);

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            boolean changed = false;
            for (BSPlayerStats.PurgeCommissionTask task : stats.purgeTasks) {
                if (!task.category.equals(category) || !task.targetId.equals(targetId) || task.rewarded || task.isComplete()) {
                    continue;
                }

                int oldProgress = task.progress;
                task.progress = Math.min(task.required, task.progress + 1);
                changed |= task.progress != oldProgress;
            }

            if (changed) {
                syncToClient(player);
            }
        });
    }

    public static void claimPurgeTaskReward(ServerPlayer player, int taskIndex) {
        ensurePurgeCommissions(player);

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            if (taskIndex < 0 || taskIndex >= stats.purgeTasks.size()) {
                return;
            }

            BSPlayerStats.PurgeCommissionTask task = stats.purgeTasks.get(taskIndex);
            if (!task.isComplete() || task.rewarded) {
                return;
            }

            givePurgeTaskReward(player, task);
            task.rewarded = true;
            stats.purgeTrashEarned += task.rewardCount;
            syncToClient(player);
        });
    }

    public static void rerollPurgeTasks(ServerPlayer player) {
        ensurePurgeCommissions(player);

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            if (stats.purgeRefreshesUsedToday >= 5) {
                return;
            }

            stats.purgeRefreshesUsedToday++;
            stats.purgeTasks.clear();
            stats.purgeTasks.addAll(rollPurgeTasks(player.getRandom()));
            syncToClient(player);
        });
    }

    private static void givePurgeTaskReward(ServerPlayer player, BSPlayerStats.PurgeCommissionTask task) {
        Item rewardItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(task.rewardItemId));
        if (rewardItem == null) {
            return;
        }

        ItemStack reward = new ItemStack(rewardItem, Math.max(1, task.rewardCount));
        boolean inserted = player.getInventory().add(reward);
        if (!inserted || !reward.isEmpty()) {
            player.drop(reward.copy(), false);
        }
    }

    private static String resolveOreTarget(BlockState state) {
        if (state.is(net.minecraftforge.common.Tags.Blocks.ORES_COAL)) return "coal";
        if (state.is(net.minecraftforge.common.Tags.Blocks.ORES_COPPER)) return "copper";
        if (state.is(net.minecraftforge.common.Tags.Blocks.ORES_IRON)) return "iron";
        if (state.is(net.minecraftforge.common.Tags.Blocks.ORES_REDSTONE)) return "redstone";
        if (state.is(net.minecraftforge.common.Tags.Blocks.ORES_LAPIS)) return "lapis";
        return "";
    }

    public static int getBaubleCount(LivingEntity player, Item targetRing) {
        if (player == null || targetRing == null) return 0;
        return CuriosApi.getCuriosInventory(player).map(handler ->
                handler.findCurios(targetRing).size()
        ).orElse(0);
    }

    public static int getOriginalRingCount(LivingEntity entity,
                                            com.BlackSouls.BlackSoulsMod.item.rings.ItemOriginalRing.Profile profile) {
        return entity == null || profile == null ? 0 : new BaubleCounter(entity).count(profile);
    }

    public static double getItemRecoveryMultiplier(LivingEntity entity) {
        return Math.pow(2.0D, getBaubleCount(entity, BlackSouls.RING_TOTO.get()))
                * Math.pow(0.5D, getBaubleCount(entity, BlackSouls.RING_RED_TEARSTONE.get()))
                * Math.pow(0.8D, getOriginalRingCount(entity, ItemOriginalRing.Profile.CUT_DOWN));
    }

    public static double getConsumableRecoveryMultiplier(LivingEntity entity) {
        return getItemRecoveryMultiplier(entity)
                * Math.pow(2.0D, getBaubleCount(entity, BlackSouls.RING_MIRACLE.get()));
    }

    public static double getPercentageDamageMultiplier(LivingEntity entity) {
        return Math.pow(2.0D, getBaubleCount(entity, BlackSouls.RING_MIRACLE.get()))
                * Math.pow(0.8D, getOriginalRingCount(entity, ItemOriginalRing.Profile.CUT_DOWN));
    }

    private static double getGuardEffectMultiplier(LivingEntity entity) {
        double multiplier = Math.pow(1.5D, getBaubleCount(entity, BlackSouls.RING_TENACIOUS.get()))
                * Math.pow(2.0D, getOriginalRingCount(entity, ItemOriginalRing.Profile.TENACIOUS_PLUS_1))
                * Math.pow(2.5D, getOriginalRingCount(entity, ItemOriginalRing.Profile.TENACIOUS_PLUS_2))
                * Math.pow(3.0D, getOriginalRingCount(entity, ItemOriginalRing.Profile.TENACIOUS_PLUS_3));
        if (entity instanceof Player player && hasPorcupineShield(player)) {
            multiplier *= 2.0D;
        }
        return multiplier;
    }

    private static boolean hasPorcupineShield(Player player) {
        return player.getMainHandItem().is(BlackSouls.PORCUPINE_SHIELD.get())
                || player.getOffhandItem().is(BlackSouls.PORCUPINE_SHIELD.get());
    }

    private static boolean hasRegisteredEffect(LivingEntity entity, RegistryObject<net.minecraft.world.effect.MobEffect> effect) {
        return effect != null && effect.isPresent() && entity.hasEffect(effect.get());
    }

    private static int getRemainingEffectDuration(LivingEntity entity, RegistryObject<net.minecraft.world.effect.MobEffect> effect) {
        if (!hasRegisteredEffect(entity, effect)) {
            return 0;
        }
        net.minecraft.world.effect.MobEffectInstance current = entity.getEffect(effect.get());
        return current != null ? current.getDuration() : 0;
    }

    private static double resolveTieredMultiplier(
            LivingEntity entity,
            RegistryObject<net.minecraft.world.effect.MobEffect> up1,
            RegistryObject<net.minecraft.world.effect.MobEffect> up2,
            RegistryObject<net.minecraft.world.effect.MobEffect> down1,
            RegistryObject<net.minecraft.world.effect.MobEffect> down2
    ) {
        if (hasRegisteredEffect(entity, up2)) return 1.50D;
        if (hasRegisteredEffect(entity, up1)) return 1.25D;
        if (hasRegisteredEffect(entity, down2)) return 0.50D;
        if (hasRegisteredEffect(entity, down1)) return 0.75D;
        return 1.0D;
    }

    private static void removeEffectIfPresent(LivingEntity entity, RegistryObject<net.minecraft.world.effect.MobEffect> effect) {
        if (hasRegisteredEffect(entity, effect)) {
            entity.removeEffect(effect.get());
        }
    }

    private static void applyTieredEffect(
            LivingEntity entity,
            RegistryObject<net.minecraft.world.effect.MobEffect> up1,
            RegistryObject<net.minecraft.world.effect.MobEffect> up2,
            RegistryObject<net.minecraft.world.effect.MobEffect> down1,
            RegistryObject<net.minecraft.world.effect.MobEffect> down2,
            boolean positive,
            int duration
    ) {
        if (entity == null || duration <= 0) {
            return;
        }

        int currentLevel = 0;
        if (hasRegisteredEffect(entity, up2)) {
            currentLevel = 2;
        } else if (hasRegisteredEffect(entity, up1)) {
            currentLevel = 1;
        } else if (hasRegisteredEffect(entity, down2)) {
            currentLevel = -2;
        } else if (hasRegisteredEffect(entity, down1)) {
            currentLevel = -1;
        }

        int delta = positive ? 1 : -1;
        int newLevel = Math.max(-2, Math.min(2, currentLevel + delta));

        int existingDuration = 0;
        if (currentLevel == 2) existingDuration = getRemainingEffectDuration(entity, up2);
        else if (currentLevel == 1) existingDuration = getRemainingEffectDuration(entity, up1);
        else if (currentLevel == -2) existingDuration = getRemainingEffectDuration(entity, down2);
        else if (currentLevel == -1) existingDuration = getRemainingEffectDuration(entity, down1);

        removeEffectIfPresent(entity, up1);
        removeEffectIfPresent(entity, up2);
        removeEffectIfPresent(entity, down1);
        removeEffectIfPresent(entity, down2);

        int finalDuration = Math.max(duration, existingDuration);

        if (newLevel == 2) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(up2.get(), finalDuration, 0));
        } else if (newLevel == 1) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(up1.get(), finalDuration, 0));
        } else if (newLevel == -1) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(down1.get(), finalDuration, 0));
        } else if (newLevel == -2) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(down2.get(), finalDuration, 0));
        }
    }

    public static void applyAttackUp(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_ATK_UP, BlackSouls.BUFF_ATK_UP_2, BlackSouls.BUFF_ATK_DOWN, BlackSouls.BUFF_ATK_DOWN_2, true, duration);
    }

    public static void applyAttackDown(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_ATK_UP, BlackSouls.BUFF_ATK_UP_2, BlackSouls.BUFF_ATK_DOWN, BlackSouls.BUFF_ATK_DOWN_2, false, duration);
    }

    public static void applyDefenseUp(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_DEF_UP, BlackSouls.BUFF_DEF_UP_2, BlackSouls.BUFF_DEF_DOWN, BlackSouls.BUFF_DEF_DOWN_2, true, duration);
    }

    public static void applyDefenseDown(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_DEF_UP, BlackSouls.BUFF_DEF_UP_2, BlackSouls.BUFF_DEF_DOWN, BlackSouls.BUFF_DEF_DOWN_2, false, duration);
    }

    public static void applyMagicAttackUp(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_MAGIC_ATK_UP, BlackSouls.BUFF_MAGIC_ATK_UP_2, BlackSouls.BUFF_MAGIC_ATK_DOWN, BlackSouls.BUFF_MAGIC_ATK_DOWN_2, true, duration);
    }

    public static void applyMagicAttackDown(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_MAGIC_ATK_UP, BlackSouls.BUFF_MAGIC_ATK_UP_2, BlackSouls.BUFF_MAGIC_ATK_DOWN, BlackSouls.BUFF_MAGIC_ATK_DOWN_2, false, duration);
    }

    public static void applyMagicDefenseUp(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_MAGIC_DEF_UP, BlackSouls.BUFF_MAGIC_DEF_UP_2, BlackSouls.BUFF_MAGIC_DEF_DOWN, BlackSouls.BUFF_MAGIC_DEF_DOWN_2, true, duration);
    }

    public static void applyMagicDefenseDown(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_MAGIC_DEF_UP, BlackSouls.BUFF_MAGIC_DEF_UP_2, BlackSouls.BUFF_MAGIC_DEF_DOWN, BlackSouls.BUFF_MAGIC_DEF_DOWN_2, false, duration);
    }

    public static void applyLuckUp(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_LUCK_UP, BlackSouls.BUFF_LUCK_UP_2, BlackSouls.BUFF_LUCK_DOWN, BlackSouls.BUFF_LUCK_DOWN_2, true, duration);
    }

    public static void applyLuckDown(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_LUCK_UP, BlackSouls.BUFF_LUCK_UP_2, BlackSouls.BUFF_LUCK_DOWN, BlackSouls.BUFF_LUCK_DOWN_2, false, duration);
    }

    public static void applySpeedUp(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_SPEED_UP, BlackSouls.BUFF_SPEED_UP_2, BlackSouls.BUFF_SPEED_DOWN, BlackSouls.BUFF_SPEED_DOWN_2, true, duration);
    }

    public static void applySpeedDown(LivingEntity entity, int duration) {
        applyTieredEffect(entity, BlackSouls.BUFF_SPEED_UP, BlackSouls.BUFF_SPEED_UP_2, BlackSouls.BUFF_SPEED_DOWN, BlackSouls.BUFF_SPEED_DOWN_2, false, duration);
    }

    private static double getAttackShiftMultiplier(LivingEntity entity) {
        return resolveTieredMultiplier(entity, BlackSouls.BUFF_ATK_UP, BlackSouls.BUFF_ATK_UP_2, BlackSouls.BUFF_ATK_DOWN, BlackSouls.BUFF_ATK_DOWN_2);
    }

    private static double getDefenseShiftMultiplier(LivingEntity entity) {
        return resolveTieredMultiplier(entity, BlackSouls.BUFF_DEF_UP, BlackSouls.BUFF_DEF_UP_2, BlackSouls.BUFF_DEF_DOWN, BlackSouls.BUFF_DEF_DOWN_2);
    }

    private static double getMagicAttackShiftMultiplier(LivingEntity entity) {
        return resolveTieredMultiplier(entity, BlackSouls.BUFF_MAGIC_ATK_UP, BlackSouls.BUFF_MAGIC_ATK_UP_2, BlackSouls.BUFF_MAGIC_ATK_DOWN, BlackSouls.BUFF_MAGIC_ATK_DOWN_2);
    }

    private static double getMagicDefenseShiftMultiplier(LivingEntity entity) {
        return resolveTieredMultiplier(entity, BlackSouls.BUFF_MAGIC_DEF_UP, BlackSouls.BUFF_MAGIC_DEF_UP_2, BlackSouls.BUFF_MAGIC_DEF_DOWN, BlackSouls.BUFF_MAGIC_DEF_DOWN_2);
    }

    private static double getLuckShiftMultiplier(LivingEntity entity) {
        return resolveTieredMultiplier(entity, BlackSouls.BUFF_LUCK_UP, BlackSouls.BUFF_LUCK_UP_2, BlackSouls.BUFF_LUCK_DOWN, BlackSouls.BUFF_LUCK_DOWN_2);
    }

    private static double getSpeedShiftMultiplier(LivingEntity entity) {
        return resolveTieredMultiplier(entity, BlackSouls.BUFF_SPEED_UP, BlackSouls.BUFF_SPEED_UP_2, BlackSouls.BUFF_SPEED_DOWN, BlackSouls.BUFF_SPEED_DOWN_2);
    }

    public static double getRpgPhysicalDefense(LivingEntity target) {
        if (target instanceof Player targetPlayer) {
            BSPlayerStats targetStats = targetPlayer.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            return targetStats != null ? targetStats.defense : target.getArmorValue();
        }
        if (com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.hasManagedStats(target)) {
            return com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(
                    target.level(),
                    com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.getStats(target).defense * getDefenseShiftMultiplier(target)
            );
        }
        return target.getArmorValue();
    }

    public static double getRpgMagicDefense(LivingEntity target) {
        if (target instanceof Player targetPlayer) {
            BSPlayerStats targetStats = targetPlayer.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            return targetStats != null ? targetStats.magicDefense : 0.0D;
        }
        if (com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.hasManagedStats(target)) {
            double magicDefense = com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(
                    target.level(),
                    com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.getStats(target).magicDefense * getMagicDefenseShiftMultiplier(target)
            );
            if (BlackSouls.BUFF_FROSTBITE.isPresent() && target.hasEffect(BlackSouls.BUFF_FROSTBITE.get())) {
                magicDefense *= 0.80D;
            }
            return magicDefense;
        }
        return 0.0D;
    }

    private static boolean isVictimImmuneToInstantDeath(LivingEntity victim) {
        return getBaubleCount(victim, BlackSouls.RING_RESURRECTOR.get()) > 0
                || (victim instanceof Player player
                && player.getMainHandItem().getItem() == BlackSouls.HOLY_GUNBLADE.get()
                && player.getMainHandItem().hasTag()
                && player.getMainHandItem().getTag().getInt("bs2_upgrade_level") >= 5)
                || victim instanceof InstantDeathImmuneEntity;
    }

    public static float rollSkillCrit(Player player, float baseDamage) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        double currentCritRate = (stats != null) ? stats.critRate : 5.0;

        if (player.level().random.nextDouble() * 100.0 < currentCritRate) {
            if (!player.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.combat.crit").withStyle(ChatFormatting.DARK_RED));
                player.getPersistentData().putBoolean("bs2_is_crit", true);
            }
            return baseDamage * 3.0F;
        }
        return baseDamage;
    }

    public static boolean hurtWithSkillDamage(ServerPlayer player, LivingEntity target, float damage, boolean sureHit, double instantDeathRate) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }
        if (target instanceof EntityMeatWall wall && wall.isOwnedBy(player)) {
            return false;
        }

        CompoundTag data = player.getPersistentData();
        data.putBoolean(TAG_PRECOMPUTED_SKILL_DAMAGE, true);
        if (sureHit) {
            data.putBoolean(TAG_SURE_HIT_SKILL, true);
        }
        if (instantDeathRate > 0.0D) {
            data.putDouble(TAG_SKILL_INSTANT_DEATH_RATE, instantDeathRate);
        }

        target.invulnerableTime = 0;
        try {
            DamageSource source = sureHit
                    ? player.damageSources().indirectMagic(player, player)
                    : player.damageSources().playerAttack(player);
            boolean damaged = target.hurt(source, damage);
            if (!damaged) {
                data.remove("bs2_is_crit");
            }
            return damaged;
        } finally {
            data.remove(TAG_PRECOMPUTED_SKILL_DAMAGE);
            data.remove(TAG_SURE_HIT_SKILL);
            data.remove(TAG_SKILL_INSTANT_DEATH_RATE);
        }
    }

    public static void performDaggerExtraHit(ServerPlayer player, LivingEntity target) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return;
        }

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            return;
        }

        double rawDamage = stats.attack * 3.0D - resolveVictimDirectDefense(target) * 2.0D;
        rawDamage = Math.max(1.0D, rawDamage);
        rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(
                target,
                buildPlayerAttackAttributes(player, stats)
        );
        rawDamage *= 0.8D + Math.random() * 0.4D;
        float finalDamage = rollSkillCrit(player, (float) rawDamage);

        CompoundTag data = player.getPersistentData();
        data.putBoolean(TAG_DAGGER_EXTRA_HIT, true);
        target.invulnerableTime = 0;
        try {
            if (!target.hurt(player.damageSources().playerAttack(player), finalDamage)) {
                data.remove("bs2_is_crit");
            }
        } finally {
            data.remove(TAG_DAGGER_EXTRA_HIT);
        }
    }

    public static void performOriginalWeaponExtraHit(ServerPlayer player, LivingEntity target, double attackMultiplier) {
        performOriginalWeaponExtraHit(player, target, attackMultiplier, 2.0D);
    }

    public static CompoundTag getPlayerPersistentData(Player player) {
        return player.getPersistentData();
    }

    public static void performOriginalWeaponExtraHit(ServerPlayer player, LivingEntity target, double attackMultiplier, double defenseMultiplier) {
        if (target == null || target.isRemoved() || !target.isAlive()) return;
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return;

        double rawDamage = stats.attack * attackMultiplier - resolveVictimDirectDefense(target) * defenseMultiplier;
        rawDamage = Math.max(1.0D, rawDamage);
        rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(
                target,
                buildPlayerAttackAttributes(player, stats)
        );
        rawDamage *= 0.8D + Math.random() * 0.4D;
        float finalDamage = rollSkillCrit(player, (float) rawDamage);

        CompoundTag data = player.getPersistentData();
        data.putBoolean(TAG_DAGGER_EXTRA_HIT, true);
        target.invulnerableTime = 0;
        try {
            if (!target.hurt(player.damageSources().playerAttack(player), finalDamage)) {
                data.remove("bs2_is_crit");
            }
        } finally {
            data.remove(TAG_DAGGER_EXTRA_HIT);
        }
    }

    public static void performHolyGunbladeGunfire(ServerPlayer player, LivingEntity primaryTarget, int ammoMode) {
        if (primaryTarget == null || primaryTarget.isRemoved() || !primaryTarget.isAlive()) return;
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return;
        List<LivingEntity> targets = ammoMode == 3
                ? player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0D),
                target -> target != player && target.isAlive() && !target.isSpectator())
                : List.of(primaryTarget);
        if (targets.isEmpty()) targets = List.of(primaryTarget);
        int animationId = ammoMode == 3 ? 488 : ammoMode == 2 ? 489 : 246;
        for (LivingEntity target : targets) {
            NetworkHandler.sendToAllAround(new PacketPlayAnim(animationId, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), target);
        }
        if (ammoMode == 3) {
            player.level().playSound(null, player.blockPosition(), BlackSouls.KEY_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            for (int hit = 0; hit < 7; hit++) {
                int delay = 5 + hit * 2;
                boolean firstHit = hit == 0;
                List<LivingEntity> volleyTargets = targets;
                player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(player.serverLevel().getServer().getTickCount() + delay, () -> {
                    player.level().playSound(null, player.blockPosition(), BlackSouls.GUN1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.5F);
                    for (LivingEntity target : volleyTargets) {
                        hitGunbladeShot(player, target, stats, 1.0D, 0.5D, 0.20D);
                        if (firstHit) target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 0));
                    }
                }));
            }
        } else if (ammoMode == 2) {
            player.level().playSound(null, player.blockPosition(), BlackSouls.GUN1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
            player.level().playSound(null, player.blockPosition(), BlackSouls.SWITCH2_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            hitGunbladeShot(player, primaryTarget, stats, 8.0D, 0.5D, 0.50D);
            primaryTarget.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 1));
        } else {
            player.level().playSound(null, player.blockPosition(), BlackSouls.KEY_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.level().playSound(null, player.blockPosition(), BlackSouls.SNIPER_RIFLE_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            hitGunbladeShot(player, primaryTarget, stats, 5.0D, 0.5D, 0.20D);
            primaryTarget.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 0));
        }
    }

    private static void hitGunbladeShot(ServerPlayer player, LivingEntity target, BSPlayerStats stats,
                                        double attackMultiplier, double defenseMultiplier, double variance) {
        if (target == null || target.isRemoved() || !target.isAlive()) return;
        double rawDamage = stats.attack * attackMultiplier - getRpgPhysicalDefense(target) * defenseMultiplier;
        rawDamage *= (1.0D - variance) + Math.random() * variance * 2.0D;
        hurtWithSkillDamage(player, target, rollSkillCrit(player, (float) Math.max(1.0D, rawDamage)), true, 0.0D);
    }

    public static void performMarySueSweep(ServerPlayer player, LivingEntity primaryTarget) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return;
        removeBeneficialEffects(primaryTarget);
        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(16.0D),
                target -> target != player && target != primaryTarget && target.isAlive() && !target.isSpectator()
        )) {
            removeBeneficialEffects(target);
            double rawDamage = stats.attack * 2.0D - getRpgPhysicalDefense(target) * 2.0D;
            rawDamage *= 0.8D + Math.random() * 0.4D;
            hurtWithSkillDamage(player, target, rollSkillCrit(player, (float) Math.max(1.0D, rawDamage)), true, 0.0D);
        }
    }

    private static void removeBeneficialEffects(LivingEntity target) {
        for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
            if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) {
                target.removeEffect(effect.getEffect());
            }
        }
    }

    public static void performMindEyeCounter(ServerPlayer player, LivingEntity target) {
        if (target == null || target.isRemoved() || !target.isAlive()) return;
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return;
        com.BlackSouls.BlackSoulsMod.util.skill.SkillEuniceRapierArt.playRapierEffects(player, target);
        double rawDamage = stats.attack * 4.0D - getRpgPhysicalDefense(target) * 2.0D;
        rawDamage *= 0.8D + Math.random() * 0.4D;
        hurtWithSkillDamage(player, target, rollSkillCrit(player, (float) Math.max(1.0D, rawDamage)), true, 0.0D);
    }

    public static void performIronBallSweep(ServerPlayer player, LivingEntity primaryTarget) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) return;
        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(8.0D),
                target -> target != player && target != primaryTarget && target.isAlive() && !target.isSpectator()
        )) {
            double rawDamage = stats.attack * 4.0D - resolveVictimDirectDefense(target) * 2.0D;
            rawDamage = Math.max(1.0D, rawDamage);
            rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(
                    target,
                    buildPlayerAttackAttributes(player, stats)
            );
            rawDamage *= 0.8D + Math.random() * 0.4D;
            float finalDamage = rollSkillCrit(player, (float) rawDamage);
            CompoundTag data = player.getPersistentData();
            data.putBoolean(TAG_DAGGER_EXTRA_HIT, true);
            target.invulnerableTime = 0;
            try {
                if (target.hurt(player.damageSources().playerAttack(player), finalDamage)) {
                    applyPlayerOnHitStatusEffects(player, target);
                } else {
                    data.remove("bs2_is_crit");
                }
            } finally {
                data.remove(TAG_DAGGER_EXTRA_HIT);
            }
        }
    }

    public static double getWeaponCounterRate(Player player) {
        if (player == null) return 0.0D;
        if (BlackSouls.BUFF_HASSO.isPresent() && player.hasEffect(BlackSouls.BUFF_HASSO.get())) return 100.0D;
        if (BlackSouls.BUFF_COUNTER_STANCE.isPresent() && player.hasEffect(BlackSouls.BUFF_COUNTER_STANCE.get())) return 100.0D;
        double rate = getBaubleCount(player, BlackSouls.RING_FIGHTER.get()) * 10.0D
                + getBaubleCount(player, BlackSouls.RING_SIN.get()) * 50.0D
                + getOriginalRingCount(player, ItemOriginalRing.Profile.SIN_PLUS_1) * 55.0D
                + getOriginalRingCount(player, ItemOriginalRing.Profile.SIN_PLUS_2) * 60.0D
                + getOriginalRingCount(player, ItemOriginalRing.Profile.SIN_PLUS_3) * 70.0D
                + getOriginalRingCount(player, ItemOriginalRing.Profile.COUNTERATTACK) * 100.0D;
        if (hasPorcupineShield(player)) rate += 30.0D;
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return Math.min(100.0D, rate);
        if (mainHand.getItem() == BlackSouls.GUNGNIR.get()) rate += 100.0D;
        if (mainHand.getItem() == BlackSouls.BROAD_SPEAR.get()) rate += 50.0D;
        if (mainHand.getItem() == BlackSouls.MIRANDA_AXE.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(5, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            rate += level >= 5 ? 40.0D : 30.0D;
        }
        return Math.min(100.0D, rate);
    }

    private static boolean tryTriggerSpearCounter(LivingAttackEvent event, Player victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer serverPlayer)
                || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == victim
                || source.getDirectEntity() != attacker
                || !(source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
            return false;
        }
        if (attacker instanceof Player attackingPlayer
                && attackingPlayer.getPersistentData().getBoolean(TAG_SPEAR_COUNTER)) {
            return false;
        }

        double counterRate = getWeaponCounterRate(victim);
        if (counterRate <= 0.0D || victim.getRandom().nextDouble() * 100.0D >= counterRate) {
            return false;
        }

        event.setCanceled(true);
        performSpearCounter(serverPlayer, attacker);
        return true;
    }

    private static void performSpearCounter(ServerPlayer player, LivingEntity target) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null || target.isRemoved() || !target.isAlive()) {
            return;
        }

        double rawDamage = stats.attack * 4.0D - resolveVictimDirectDefense(target) * 2.0D;
        rawDamage = Math.max(1.0D, rawDamage);
        rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(
                target,
                buildPlayerAttackAttributes(player, stats)
        );
        rawDamage *= 0.8D + Math.random() * 0.4D;
        float finalDamage = rollSkillCrit(player, (float) rawDamage);

        CompoundTag data = player.getPersistentData();
        data.putBoolean(TAG_SPEAR_COUNTER, true);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        boolean hasso = BlackSouls.BUFF_HASSO.isPresent() && player.hasEffect(BlackSouls.BUFF_HASSO.get());
        if (hasso) {
            NetworkHandler.sendToAllAround(new PacketPlayAnim(580, target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), target);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.SWORD5_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.SWORD4_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.serverLevel().getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.BLOOD_SPLATTER_EVENT.get(), SoundSource.PLAYERS, 1.0F, 0.65F);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.ABSORB1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), BlackSouls.DARKNESS7_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }));
        } else if (hasPorcupineShield(player)) {
            target.level().playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        } else if (player.getMainHandItem().getItem() == BlackSouls.HALBERD.get()
                || player.getMainHandItem().getItem() == BlackSouls.BAHAMUT.get()
                || player.getMainHandItem().getItem() == BlackSouls.MIRANDA_AXE.get()) {
            ((com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalWeapon) player.getMainHandItem().getItem()).playAttackEffects(player, target);
        } else {
            com.BlackSouls.BlackSoulsMod.item.weapon.ItemBroadSpear.playAttackEffects(player, target);
        }
        try {
            hurtWithSkillDamage(player, target, finalDamage, hasso, 0.0D);
        } finally {
            data.remove(TAG_SPEAR_COUNTER);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            DamageSource source = event.getSource();
            if (source.getEntity() instanceof LivingEntity opponent && opponent != player) {
                activateBattleStartRings(player);
            }

            if (EntityHellPrince.isOpeningComboDamage(source)) {
                return;
            }

            if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL) || "bs2_sure_hit".equals(source.getMsgId())) {
                return;
            }

            if (source.getDirectEntity() instanceof EntityThrownBlade thrownBlade && thrownBlade.isSureHit()) {
                return;
            }

            if (source.getEntity() instanceof Player attacker
                    && attacker.getPersistentData().getBoolean(TAG_SURE_HIT_SKILL)) {
                return;
            }

            if (getBaubleCount(player, BlackSouls.WINDLESS_CLOTHES.get()) > 0
                    && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                    && !source.is(DamageTypes.MAGIC)
                    && !source.is(DamageTypes.INDIRECT_MAGIC)) {
                event.setCanceled(true);
                if (!player.level().isClientSide()) {
                    ((ServerLevel) player.level()).sendParticles(
                            ParticleTypes.CLOUD,
                            player.getX(), player.getY() + 1.0D, player.getZ(),
                            3, 0.2D, 0.2D, 0.2D, 0.05D
                    );
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 2.0F);
                }
                return;
            }

            if (player instanceof ServerPlayer serverPlayer
                    && BlackSouls.BUFF_MIND_EYE.isPresent()
                    && player.hasEffect(BlackSouls.BUFF_MIND_EYE.get())
                    && source.getEntity() instanceof LivingEntity attacker
                    && attacker != player) {
                event.setCanceled(true);
                performMindEyeCounter(serverPlayer, attacker);
                return;
            }

            if (tryTriggerSpearCounter(event, player, source)) {
                return;
            }

            if (source.getEntity() instanceof Player attacker) {
                CompoundTag attackerData = attacker.getPersistentData();
                ItemStack mainHand = attacker.getMainHandItem();
                boolean spearAttack = !mainHand.isEmpty()
                        && (mainHand.getItem() == BlackSouls.BROAD_SPEAR.get()
                        || mainHand.getItem() == BlackSouls.GUNGNIR.get());
                boolean stormRulerAttack = !mainHand.isEmpty()
                        && mainHand.getItem() == BlackSouls.STORM_RULER.get();
                boolean mirandaAttack = !mainHand.isEmpty()
                        && mainHand.getItem() == BlackSouls.MIRANDA_AXE.get();
                boolean marySueAttack = !mainHand.isEmpty()
                        && mainHand.getItem() == BlackSouls.MARY_SUES_BRANCH_STAFF.get();
                if ((spearAttack || stormRulerAttack || mirandaAttack || marySueAttack) && (source.is(DamageTypes.PLAYER_ATTACK)
                        || attackerData.getBoolean(TAG_PRECOMPUTED_SKILL_DAMAGE))) {
                    return;
                }
            }

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                boolean magicAttack = source.is(DamageTypes.MAGIC)
                        || source.is(DamageTypes.INDIRECT_MAGIC);
                double evasionRate = magicAttack ? stats.magicEvasion : stats.evasion;
                if (evasionRate > 0 && Math.random() * 100 < evasionRate) {
                    event.setCanceled(true);
                    if (!player.level().isClientSide()) {
                        ((ServerLevel) player.level()).sendParticles(
                                ParticleTypes.CLOUD,
                                player.getX(), player.getY() + 1, player.getZ(),
                                3, 0.2, 0.2, 0.2, 0.05
                        );
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                magicAttack ? BlackSouls.EVASION2_EVENT.get() : BlackSouls.EVASION1_EVENT.get(),
                                SoundSource.PLAYERS, 0.8F, 1.0F);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onCampfireInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        net.minecraft.world.level.Level level = event.getLevel();
        net.minecraft.world.entity.player.Player player = event.getEntity();
        net.minecraft.core.BlockPos pos = event.getPos();
        net.minecraft.world.item.ItemStack handItem = event.getItemStack();

        if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.CAMPFIRES)) {

            if (player.isShiftKeyDown()) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

                if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.BlackSouls.BlackSoulsMod.capability.BSWorldData data = com.BlackSouls.BlackSoulsMod.capability.BSWorldData.get(level.getServer().overworld());
                    boolean isFirstTime = data.addBonfire(level, pos, player);
                    BonfireStateHandler.light(level, pos);
                    RedHoodStoryHandler.onBonfireRest(
                            serverPlayer,
                            (net.minecraft.server.level.ServerLevel) level,
                            pos
                    );

                    if (BlackSouls.FIRE6_EVENT != null) {
                        level.playSound(null, pos, BlackSouls.FIRE6_EVENT.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    }

                    com.BlackSouls.BlackSoulsMod.network.NetworkHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new com.BlackSouls.BlackSoulsMod.network.packets.PacketWhiteFlash(isFirstTime)
                    );

                    if (isFirstTime) {
                        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 40, 10));
                        com.BlackSouls.BlackSoulsMod.network.NetworkHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new com.BlackSouls.BlackSoulsMod.network.packets.ClientboundSimpleActionPacket(
                                        com.BlackSouls.BlackSoulsMod.network.packets.ClientboundSimpleActionPacket.Action.SHOW_BONFIRE_LIT
                                )
                        );

                        net.minecraft.server.MinecraftServer server = serverPlayer.server;
                        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(2500 / 50.0)), () -> NetworkHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncBonfireList(data.activatedBonfires)
                        )));
                    } else {
                        net.minecraft.server.MinecraftServer server = serverPlayer.server;
                        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(1000 / 50.0)), () -> NetworkHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncBonfireList(data.activatedBonfires)
                        )));
                    }

                    serverPlayer.setHealth(serverPlayer.getMaxHealth());
                    serverPlayer.getFoodData().setFoodLevel(20);
                    serverPlayer.getFoodData().setSaturation(5.0f);
                    serverPlayer.setRespawnPosition(level.dimension(), pos.above(), player.getYRot(), true, false);
                    com.BlackSouls.BlackSoulsMod.util.SkillUtils.setMana(serverPlayer, com.BlackSouls.BlackSoulsMod.util.SkillUtils.getMaxMana(serverPlayer));
                    serverPlayer.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                        double maxActionPoints = SkillUtils.getMaxActionPoints(serverPlayer, stats);
                        stats.restoreActionPoints(maxActionPoints, maxActionPoints);
                        syncToClient(serverPlayer, stats);
                    });
                }
            }
            else {
                if (handItem.getItem() == BlackSouls.HERB_BOTTLE.get() || handItem.getItem() == BlackSouls.HERB_BOTTLE_M.get()) {
                    if (handItem.getDamageValue() > 0) {
                        event.setCanceled(true);
                        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

                        if (!level.isClientSide()) {
                            handItem.setDamageValue(0);
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.herb_bottle.refill").withStyle(net.minecraft.ChatFormatting.GREEN));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player player) {
            event.setAmount(com.BlackSouls.BlackSoulsMod.util.VanillaHealthScaling.scaleVanillaDamage(
                    player, event.getSource(), event.getAmount()));
        }
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;

        if (victim instanceof Player player
                && event.getSource().getEntity() instanceof LivingEntity opponent
                && opponent != player) {
            activateBattleStartRings(player);
        }

        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker != victim) {
                activateBattleStartRings(attacker);
            }
            CompoundTag attackerData = attacker.getPersistentData();
            Item heldItem = attacker.getMainHandItem().getItem();
            boolean inaccurateWeapon = heldItem == BlackSouls.MEAT_CLEAVER_GREATAXE.get()
                    || heldItem == BlackSouls.WARHAMMER.get()
                    || heldItem == BlackSouls.ABERRANT_WARHAMMER.get();
            boolean aimed = BlackSouls.BUFF_AIM.isPresent() && attacker.hasEffect(BlackSouls.BUFF_AIM.get());
            double missChance = getBaubleCount(attacker, BlackSouls.RING_TROLL.get()) * 0.50D
                    - getBaubleCount(attacker, BlackSouls.RING_SNIPER.get()) * 0.30D
                    - getBaubleCount(attacker, BlackSouls.RING_SIN.get()) * 0.50D
                    - getOriginalRingCount(attacker, ItemOriginalRing.Profile.SNIPER_PLUS_1) * 0.40D
                    - getOriginalRingCount(attacker, ItemOriginalRing.Profile.SNIPER_PLUS_2) * 0.50D
                    - getOriginalRingCount(attacker, ItemOriginalRing.Profile.SNIPER_PLUS_3) * 0.60D
                    - getOriginalRingCount(attacker, ItemOriginalRing.Profile.SIN_PLUS_1) * 0.55D
                    - getOriginalRingCount(attacker, ItemOriginalRing.Profile.SIN_PLUS_2) * 0.60D
                    - getOriginalRingCount(attacker, ItemOriginalRing.Profile.SIN_PLUS_3) * 0.70D
                    - (BlackSouls.BUFF_PLAYWRIGHT.isPresent() && attacker.hasEffect(BlackSouls.BUFF_PLAYWRIGHT.get()) ? 1.0D : 0.0D);
            if (inaccurateWeapon && !aimed) {
                missChance += 0.30D;
            }
            if (attacker != victim
                    && !attackerData.getBoolean(TAG_SURE_HIT_SKILL)
                    && attacker.getRandom().nextDouble() < Math.min(1.0D, Math.max(0.0D, missChance))) {
                event.setCanceled(true);
                return;
            }
        }

        if (EntityHellPrince.isOpeningComboDamage(event.getSource())) {
            return;
        }

        if (BlackSouls.BUFF_KNIGHTS_GLORY.isPresent() && victim.hasEffect(BlackSouls.BUFF_KNIGHTS_GLORY.get())) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        if (BlackSouls.BUFF_DAGGER_GUARD.isPresent() && victim.hasEffect(BlackSouls.BUFF_DAGGER_GUARD.get())) {
            event.setAmount((float) (event.getAmount() * 0.5D
                    / getGuardEffectMultiplier(victim)));
        } else if (victim instanceof Player player && player.isBlocking()) {
            event.setAmount((float) (event.getAmount()
                    / getGuardEffectMultiplier(victim)));
        }

        if (victim instanceof Player player
                && event.getSource().getEntity() instanceof LivingEntity magicAttacker
                && magicAttacker != victim
                && (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC))) {
            int reflectCount = getOriginalRingCount(player, ItemOriginalRing.Profile.MOLASSES);
            int winterMageCoatCount = getBaubleCount(player, BlackSouls.WINTER_MAGE_COAT.get());
            double reflectChance = 1.0D
                    - Math.pow(0.90D, reflectCount)
                    * Math.pow(0.85D, winterMageCoatCount);
            if (reflectChance > 0.0D && player.getRandom().nextDouble() < reflectChance) {
                event.setCanceled(true);
                magicAttacker.hurt(player.damageSources().magic(), event.getAmount());
                return;
            }
        }

        applyManagedMobAttackOverride(event);

        if (BlackSouls.BUFF_COUNTER_STANCE.isPresent()
                && victim.hasEffect(BlackSouls.BUFF_COUNTER_STANCE.get())
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            event.setAmount(event.getAmount() * 0.5F);
        }

        boolean skipUniversalDefense = false;

        boolean fromThrownBlade = event.getSource().getDirectEntity() instanceof EntityThrownBlade;

        if (event.getSource().getEntity() instanceof Player attacker) {
            CompoundTag attackerData = attacker.getPersistentData();
            boolean daggerExtraHit = attackerData.getBoolean(TAG_DAGGER_EXTRA_HIT);
            boolean precomputedSkillDamage = attackerData.getBoolean(TAG_PRECOMPUTED_SKILL_DAMAGE);

            if (fromThrownBlade || daggerExtraHit || precomputedSkillDamage) {
                skipUniversalDefense = true;
            } else if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow
                    && attacker.getMainHandItem().getItem() instanceof com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalBow bow) {
                event.setAmount((float) computePlayerBowDamage(attacker, victim, statsFor(attacker), bow));
                skipUniversalDefense = true;
                if (attacker instanceof ServerPlayer serverPlayer) {
                    bow.onProjectileHit(serverPlayer, victim, attacker.getMainHandItem());
                }
            } else if (event.getSource().is(DamageTypes.INDIRECT_MAGIC)) {
                skipUniversalDefense = true;
            } else if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                skipUniversalDefense = applyPlayerDirectAttackDamage(attacker, victim, event);
            } else {
                handlePlayerAttackVariance(attacker, event);
            }

            applyConditionalWeaponDamageBonus(attacker, victim, event);

            if (!fromThrownBlade && !daggerExtraHit && attacker != victim) {
                applyPlayerOnHitStatusEffects(attacker, victim);
            }
            double criticalEvasionChance = Math.min(1.0D,
                    (getBaubleCount(victim, BlackSouls.WINTER_KNIGHT_ARMOR.get())
                            + getBaubleCount(victim, BlackSouls.WINTER_KNIGHT_HELMET.get())) * 0.50D);
            if (attackerData.getBoolean("bs2_is_crit")
                    && criticalEvasionChance > 0.0D
                    && victim.getRandom().nextDouble() < criticalEvasionChance) {
                event.setAmount(event.getAmount() / 3.0F);
                attackerData.remove("bs2_is_crit");
            }
        }

        if (!skipUniversalDefense) {
            handleUniversalDefense(victim, event);
        }

        if (BlackSouls.BUFF_DEFENSELESS.isPresent() && victim.hasEffect(BlackSouls.BUFF_DEFENSELESS.get())) {
            MobEffectInstance defenseless = victim.getEffect(BlackSouls.BUFF_DEFENSELESS.get());
            event.setAmount(event.getAmount() * (defenseless != null && defenseless.getAmplifier() > 0 ? 3.0F : 2.0F));
        }
        if (BlackSouls.BUFF_FRAGILE.isPresent() && victim.hasEffect(BlackSouls.BUFF_FRAGILE.get())) {
            event.setAmount(event.getAmount() * 1.5F);
        }

    }

    private static void applyConditionalWeaponDamageBonus(Player attacker, LivingEntity victim, LivingHurtEvent event) {
        Item item = attacker.getMainHandItem().getItem();
        boolean cleaverAxe = item == BlackSouls.MEAT_CLEAVER_GREATAXE.get()
                || item == BlackSouls.SLAUGHTERER_GREATAXE.get();
        boolean ragnarokRoute = item == BlackSouls.DOUBLE_EDGED_GREATSWORD.get()
                || item == BlackSouls.RAGNAROK.get();
        if (cleaverAxe && BlackSouls.BUFF_BLEEDING.isPresent() && victim.hasEffect(BlackSouls.BUFF_BLEEDING.get())) {
            event.setAmount(event.getAmount() * 2.0F);
        } else if (ragnarokRoute && BlackSouls.BUFF_STUN.isPresent() && victim.hasEffect(BlackSouls.BUFF_STUN.get())) {
            event.setAmount(event.getAmount() * 2.0F);
        } else if (item == BlackSouls.EUNICES_RAPIER.get()
                && BlackSouls.BUFF_EXPOSED_WEAKNESS.isPresent()
                && victim.hasEffect(BlackSouls.BUFF_EXPOSED_WEAKNESS.get())) {
            event.setAmount(event.getAmount() * 1.5F);
        }
    }

    private static void applyManagedMobAttackOverride(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player || !(event.getSource().getEntity() instanceof LivingEntity mobAttacker)) {
            return;
        }
        if (!com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.hasManagedStats(mobAttacker)) {
            return;
        }

        double mobAttack = resolveManagedMobAttack(mobAttacker, event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR));
        if (mobAttack > 0.0D) {
            event.setAmount((float) Math.max(event.getAmount(), mobAttack));
        }
    }

    private static boolean applyPlayerDirectAttackDamage(Player attacker, LivingEntity victim, LivingHurtEvent event) {
        BSPlayerStats stats = attacker.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            return false;
        }

        event.setAmount((float) computePlayerDirectAttackDamage(attacker, victim, stats));
        return true;
    }

    private static double computePlayerDirectAttackDamage(Player attacker, LivingEntity victim, BSPlayerStats stats) {
        Item mainHandItem = attacker.getMainHandItem().getItem();
        boolean isDagger = mainHandItem == BlackSouls.THIEFS_DAGGER.get()
                || mainHandItem == BlackSouls.GREAT_THIEFS_DAGGER.get();
        boolean isFortress = mainHandItem == BlackSouls.SHIELD_GUARD_FORTRESS.get()
                || mainHandItem == BlackSouls.GUARDIAN_FORTRESS.get();
        double rawDamage;
        if (mainHandItem == BlackSouls.HANS_MACHINE_GUN.get()) {
            rawDamage = stats.attack * 2.0D - resolveVictimDirectDefense(victim);
        } else if (mainHandItem == BlackSouls.CORRUPT_JABBERWOCK_SCYTHE.get()) {
            rawDamage = 10.0D * (0.8D + Math.random() * 0.4D)
                    + victim.getMaxHealth() * 0.01D * getPercentageDamageMultiplier(victim);
        } else if (mainHandItem == BlackSouls.MAD_BOW_JUBJUB.get()) {
            rawDamage = stats.attack * 3.0D - resolveVictimDirectDefense(victim) * 2.0D;
        } else if (mainHandItem == BlackSouls.LOST_SWORD.get()) {
            rawDamage = stats.magicAttack * 4.0D - getRpgMagicDefense(victim) * 2.0D;
        } else if (mainHandItem == BlackSouls.GLACHID.get()) {
            rawDamage = stats.attack * 4.0D;
        } else if (mainHandItem == BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get()) {
            rawDamage = stats.attack * 3.0D - resolveVictimDirectDefense(victim) * 2.0D;
        } else if (mainHandItem == BlackSouls.MARY_SUES_BRANCH_STAFF.get()) {
            rawDamage = stats.attack * 2.0D - resolveVictimDirectDefense(victim) * 2.0D;
        } else if (mainHandItem == BlackSouls.RAIDENS_DUAL_AXES.get()) {
            rawDamage = attacker.getHealth() * 0.1D - resolveVictimDirectDefense(victim) * 2.0D;
        } else {
            rawDamage = stats.attack * (isDagger ? 3.0D : 4.0D)
                    + (isFortress ? stats.defense * 2.0D : 0.0D)
                    - resolveVictimDirectDefense(victim) * 2.0D;
        }
        boolean isWeapon = mainHandItem instanceof net.minecraft.world.item.TieredItem
                || mainHandItem instanceof net.minecraft.world.item.ProjectileWeaponItem;

        if (!isWeapon) {
            rawDamage = 1.0D;
        } else {
            double attackStrength = attacker.getAttackStrengthScale(0.5F);
            double vanillaChargeMultiplier = 0.2D + attackStrength * attackStrength * 0.8D;
            rawDamage = Math.max(1.0D, rawDamage) * vanillaChargeMultiplier;
        }

        if (mainHandItem != BlackSouls.CORRUPT_JABBERWOCK_SCYTHE.get()
                && mainHandItem != BlackSouls.GLACHID.get()) {
            List<String> attackAttrs = buildPlayerAttackAttributes(attacker, stats);
            rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(victim, attackAttrs);
            rawDamage *= 0.8D + Math.random() * 0.4D;
        } else if (mainHandItem == BlackSouls.GLACHID.get()) {
            rawDamage *= 0.8D + Math.random() * 0.4D;
        }

        if (attacker.getPersistentData().getBoolean("bs2_melee_crit")) {
            rawDamage *= 3.0D;
            attacker.getPersistentData().remove("bs2_melee_crit");
        }
        return rawDamage;
    }

    private static BSPlayerStats statsFor(Player player) {
        return player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
    }

    private static double computePlayerBowDamage(Player attacker, LivingEntity victim, BSPlayerStats stats,
                                                 com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalBow bow) {
        if (stats == null) return 1.0D;
        double rawDamage = stats.attack * bow.getAttackMultiplier()
                - resolveVictimDirectDefense(victim) * bow.getDefenseMultiplier();
        rawDamage = Math.max(1.0D, rawDamage);
        rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(
                victim,
                buildPlayerAttackAttributes(attacker, stats)
        );
        rawDamage *= 0.8D + Math.random() * 0.4D;
        return rollSkillCrit(attacker, (float) rawDamage);
    }

    private static double resolveVictimDirectDefense(LivingEntity victim) {
        if (victim instanceof Player victimPlayer) {
            BSPlayerStats victimStats = victimPlayer.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            return victimStats != null ? victimStats.defense : victim.getArmorValue();
        }
        return victim.getArmorValue() + getRpgPhysicalDefense(victim);
    }

    private static List<String> buildPlayerAttackAttributes(Player attacker, BSPlayerStats stats) {
        List<String> attackAttrs = new java.util.ArrayList<>();
        attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.PHYSICAL);
        attackAttrs.addAll(stats.weaponEnchantments);

        ItemStack currentWeapon = attacker.getMainHandItem();
        if (!currentWeapon.isEmpty() && (currentWeapon.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()
                || currentWeapon.getItem() == BlackSouls.VORPAL_SWORD.get())) {
            attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.JABBERWOCK_KILLER);
        }
        if (!currentWeapon.isEmpty() && (currentWeapon.getItem() == BlackSouls.BEAST_HUNTER_SAW.get()
                || currentWeapon.getItem() == BlackSouls.BEAST_SLAYING_SAW_SWORD.get())) {
            attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.BEAST_KILLER);
        }
        if (!currentWeapon.isEmpty() && (currentWeapon.getItem() == BlackSouls.DARK_SWORD.get()
                || currentWeapon.getItem() == BlackSouls.DARK_BLADE.get())) {
            attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.DARK);
        }
        return attackAttrs;
    }

    private static void applyPlayerOnHitStatusEffects(Player attacker, LivingEntity victim) {
        BSPlayerStats stats = attacker.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            return;
        }

        ItemStack mainHand = attacker.getMainHandItem();
        double finalStunRate = stats.stunRate;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_SWORD.get()) finalStunRate += 20.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DRAKE_SWORD.get()) finalStunRate += 10.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get()) finalStunRate += 20.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.THIEFS_DAGGER.get()
                || mainHand.getItem() == BlackSouls.GREAT_THIEFS_DAGGER.get())) finalStunRate += 5.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.GREAT_SWORD.get()
                || mainHand.getItem() == BlackSouls.GIANT_SWORD.get())) finalStunRate += 80.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.BROAD_SPEAR.get()
                || mainHand.getItem() == BlackSouls.GUNGNIR.get())) finalStunRate += 10.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BANDERSNATCH_SWORD.get()) finalStunRate += 30.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.CLUB.get()) finalStunRate += 25.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KING_CLUB.get()) finalStunRate += 60.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.MAGIC_BLADE.get()
                || mainHand.getItem() == BlackSouls.DEMON_GOD_BLADE.get())) finalStunRate += 20.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.MEAT_CLEAVER_GREATAXE.get()
                || mainHand.getItem() == BlackSouls.SLAUGHTERER_GREATAXE.get())) finalStunRate += 30.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DOUBLE_EDGED_GREATSWORD.get()) finalStunRate += 30.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.RAGNAROK.get()) finalStunRate += 40.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.MACE.get()
                || mainHand.getItem() == BlackSouls.DIVINE_PUNISHMENT_MACE.get())) finalStunRate += 30.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.HALBERD.get()) finalStunRate += 25.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BAHAMUT.get()) finalStunRate += 45.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BEAST_HUNTER_SAW.get()) finalStunRate += 10.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BEAST_SLAYING_SAW_SWORD.get()) finalStunRate += 30.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.SHIELD_GUARD_FORTRESS.get()
                || mainHand.getItem() == BlackSouls.GUARDIAN_FORTRESS.get())) finalStunRate += 15.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.DARK_SWORD.get()
                || mainHand.getItem() == BlackSouls.DARK_BLADE.get())) finalStunRate += 20.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.BROKEN_SWORD.get()
                || mainHand.getItem() == BlackSouls.GRUDGE_SWORD.get())) finalStunRate += 5.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.WARHAMMER.get()
                || mainHand.getItem() == BlackSouls.ABERRANT_WARHAMMER.get())) finalStunRate += 50.0;
        if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.KNUCKLE_DUSTER.get()
                || mainHand.getItem() == BlackSouls.KAISER_GAUNTLET.get())) finalStunRate += 5.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.UCHIGATANA.get()) finalStunRate += 20.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KISHIN_BLADE.get()) finalStunRate += 25.0;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GREAT_IRON_BALL.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(5, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            finalStunRate += 10.0D + level * 10.0D;
        }
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.JUDGMENT_SCYTHE.get()) finalStunRate += 6.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.STORM_RULER.get()) finalStunRate += 10.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MOONLIGHT_GREATSWORD.get()) finalStunRate += 5.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DEEP_SEA_KNIGHTS_ANCHOR.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(5, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            finalStunRate += level >= 5 ? 80.0D : 60.0D;
        }
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.LOST_SWORD.get()) finalStunRate += 20.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GLACHID.get()) finalStunRate += 15.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.SLAUGHTERERS_CHAINSAW.get()) finalStunRate += 5.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MOCK_TURTLE_SOUP_LADLE.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(5, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            finalStunRate += level >= 5 ? 5.0D : 3.0D;
        }
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(5, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            finalStunRate += level >= 5 ? 5.0D : 3.0D;
        }
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.RAIDENS_DUAL_AXES.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(5, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            finalStunRate += level >= 5 ? 40.0D : 30.0D;
        }

        if (finalStunRate > 0 && Math.random() * 100.0 < finalStunRate && BlackSouls.BUFF_STUN.isPresent()) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_STUN.get(), 40, 0));
        }
        if (stats.fearRate > 0 && Math.random() * 100.0 < stats.fearRate && BlackSouls.BUFF_FEAR.isPresent()) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 100, 0));
        }
        double bleedRate = 0.0D;
        if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MEAT_CLEAVER_GREATAXE.get()) {
            int level = mainHand.hasTag() ? Math.max(0, Math.min(9, mainHand.getTag().getInt("bs2_upgrade_level"))) : 0;
            bleedRate = 30.0D + level * 5.0D;
        } else if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.SLAUGHTERER_GREATAXE.get()) {
            bleedRate = 100.0D;
        }
        if (hasPorcupineShield(attacker)) bleedRate += 30.0D;
        if (bleedRate > 0.0D && Math.random() * 100.0D < bleedRate && BlackSouls.BUFF_BLEEDING.isPresent()) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
        }
        int appleRingCount = getBaubleCount(attacker, BlackSouls.RING_APPLE.get());
        if (appleRingCount > 0
                && Math.random() * 100.0D < Math.min(100.0D, appleRingCount * 50.0D)
                && BlackSouls.BUFF_SLEEP.isPresent()) {
            victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_SLEEP.get(), 600 + victim.getRandom().nextInt(401), 0));
        }
        int bankerRingCount = getBaubleCount(attacker, BlackSouls.RING_BANKER.get());
        if (bankerRingCount > 0 && BlackSouls.BUFF_OILY.isPresent()) {
            victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_OILY.get(), 600 + victim.getRandom().nextInt(201), 0));
        }
        int mosquitoRingCount = getBaubleCount(attacker, BlackSouls.RING_MOSQUITO.get());
        double mosquitoRate = Math.min(100.0D, mosquitoRingCount * 30.0D);
        if (mosquitoRate > 0.0D) {
            if (BlackSouls.BUFF_POISON.isPresent() && Math.random() * 100.0D < mosquitoRate) {
                victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_POISON.get(), 2000, 0));
            }
            if (BlackSouls.BUFF_SEVERE_POISON.isPresent() && Math.random() * 100.0D < mosquitoRate) {
                victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 2000, 0));
            }
            if (BlackSouls.BUFF_BLEEDING.isPresent() && Math.random() * 100.0D < mosquitoRate) {
                victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
            }
        }
        int ghoulCount = getOriginalRingCount(attacker, ItemOriginalRing.Profile.GHOUL);
        if (ghoulCount > 0 && BlackSouls.BUFF_WEAKNESS.isPresent()) {
            victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_WEAKNESS.get(), 600, 0));
        }
        int unicornCount = getOriginalRingCount(attacker, ItemOriginalRing.Profile.UNICORN);
        if (unicornCount > 0
                && Math.random() < 1.0D - Math.pow(0.50D, unicornCount)
                && BlackSouls.BUFF_FROSTBITE.isPresent()) {
            victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_FROSTBITE.get(), 800, 0));
        }
        int lionCount = getOriginalRingCount(attacker, ItemOriginalRing.Profile.LION);
        if (lionCount > 0
                && Math.random() < 1.0D - Math.pow(0.50D, lionCount)
                && BlackSouls.BUFF_LACERATION.isPresent()) {
            victim.addEffect(new MobEffectInstance(BlackSouls.BUFF_LACERATION.get(), 800, 0));
        }
    }

    private static double resolveManagedMobAttack(LivingEntity attacker, boolean magicLikeDamage) {
        AttributeInstance attackAttribute = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        if (!magicLikeDamage && attackAttribute != null) {
            return attackAttribute.getValue() * getAttackShiftMultiplier(attacker);
        }

        com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.MobStats stats =
                com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.getStats(attacker);
        if (magicLikeDamage && stats.magicAttack > 0.0D) {
            return com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(attacker.level(), stats.magicAttack * getMagicAttackShiftMultiplier(attacker));
        }
        return com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(attacker.level(), stats.attack * getAttackShiftMultiplier(attacker));
    }

    private static void handleUniversalDefense(LivingEntity victim, LivingHurtEvent event) {
        float incomingDamage = event.getAmount();
        double defenseValue = victim.getArmorValue();

        if (victim instanceof Player player) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                defenseValue = event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) ? stats.magicDefense : stats.defense;
            }
        } else if (com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.hasManagedStats(victim)) {
            com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.MobStats mobStats =
                    com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.getStats(victim);
            defenseValue = event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                    ? com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(victim.level(), mobStats.magicDefense * getMagicDefenseShiftMultiplier(victim))
                    : (victim.getArmorValue() + com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(victim.level(), mobStats.defense * getDefenseShiftMultiplier(victim)));
        }

        double enemyAtk = Math.max(1.0, incomingDamage);
        double reductionFactor = enemyAtk / (enemyAtk + defenseValue);
        float finalDamage = Math.max(1.0f, (float) (incomingDamage * reductionFactor));
        if (victim instanceof Player player) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                boolean isMagicLike = event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR);
                finalDamage *= isMagicLike ? (float) stats.magicDamageRate : (float) stats.physicalDamageRate;
            }
        }
        event.setAmount(finalDamage);
    }

    private static void handlePlayerAttackVariance(Player attacker, LivingHurtEvent event) {
        attacker.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            float rawDamage = event.getAmount();
            double minRoll = 0.80;
            double maxRoll = 1.20;
            double luckBonus = (stats.luck / 2000.0) * 0.01;
            double currentMin = Math.min(maxRoll, minRoll + luckBonus);
            double multiplier = currentMin + (Math.random() * (maxRoll - currentMin));
            event.setAmount((float) (rawDamage * multiplier));
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            safeSyncPlayerAndWorld(player);
            grantFirstJoinItems(player);
            syncUnlockedAvatars(player);
        }
    }

    private static void syncUnlockedAvatars(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList("bs2_unlocked_dlc_avatars", 8);

        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncUnlockedAvatars(list)
        );
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            safeSyncPlayerAndWorld(player);
        }
    }

    private static void safeSyncPlayerAndWorld(ServerPlayer player) {
        applyStats(player);
        syncToClient(player);

        ServerLevel overworld = player.server.overworld();
        com.BlackSouls.BlackSoulsMod.capability.BSWorldData data = com.BlackSouls.BlackSoulsMod.capability.BSWorldData.get(overworld);

        com.BlackSouls.BlackSoulsMod.util.DifficultyManager.currentDifficulty = data.difficulty;

        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncDifficulty(data));
    }

    private static void grantFirstJoinItems(ServerPlayer player) {
        CompoundTag persisted = SkillUtils.getPersistedData(player);

        if (!persisted.getBoolean(TAG_FIRST_JOIN_BLACK_ASH)) {
            giveItem(player, new ItemStack(BlackSouls.BLACK_ASH.get()));
            persisted.putBoolean(TAG_FIRST_JOIN_BLACK_ASH, true);
        }

        if (BSConfig.ALLOW_PLAYER_EXTRA_MODES.get() && !persisted.getBoolean(TAG_FIRST_JOIN_DEV_MODE_ITEMS)) {
            giveItem(player, new ItemStack(BlackSouls.DEV_REVENGE_MODE.get()));
            giveItem(player, new ItemStack(BlackSouls.DEV_DEATH_MODE.get()));
            giveItem(player, new ItemStack(BlackSouls.DEV_LEGENDARY_MODE.get()));
            giveItem(player, new ItemStack(BlackSouls.DEV_MALICE_MODE.get()));
            giveItem(player, new ItemStack(BlackSouls.DEV_ETERNITY_MODE.get()));
            persisted.putBoolean(TAG_FIRST_JOIN_DEV_MODE_ITEMS, true);
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats.CAPABILITY).ifPresent(oldStats -> event.getEntity().getCapability(BSPlayerStats.CAPABILITY).ifPresent(newStats -> {
            newStats.deserializeNBT(oldStats.serializeNBT());
            if (event.isWasDeath()) {
                newStats.souls = 0;
            }
        }));
        event.getOriginal().invalidateCaps();
    }
    @SubscribeEvent
    public static void onPlayerAttackCrit(CriticalHitEvent event) {
        if (event.getTarget() instanceof LivingEntity target) {
            if (getBaubleCount(target, BlackSouls.RING_RESURRECTOR.get()) > 0) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }

        Player player = event.getEntity();
        if (player == null) return;

        if (player.getMainHandItem().getItem() == BlackSouls.RAIDENS_DUAL_AXES.get()) {
            event.setResult(Event.Result.DENY);
            event.setDamageModifier(1.0F);
            player.getPersistentData().remove("bs2_melee_crit");
            return;
        }

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        double currentCritRate = stats != null ? stats.critRate : 5.0;

        boolean isCrit = player.level().random.nextDouble() * 100.0 < currentCritRate;

        if (isCrit) {
            event.setResult(Event.Result.ALLOW);
            event.setDamageModifier(3.0F);

            if (!player.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.combat.crit").withStyle(ChatFormatting.DARK_RED));
                player.getPersistentData().putBoolean("bs2_is_crit", true);
                player.getPersistentData().putBoolean("bs2_melee_crit", true);
            }
        } else {
            event.setResult(Event.Result.DENY);
            event.setDamageModifier(1.0F);
            player.getPersistentData().remove("bs2_melee_crit");
        }
    }
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) return;

        if (entity.tickCount % 20 == 0
                && BlackSouls.BUFF_INNER_POTENTIAL.isPresent()
                && entity.hasEffect(BlackSouls.BUFF_INNER_POTENTIAL.get())) {
            entity.hurt(entity.damageSources().magic(), Math.max(1.0F, entity.getMaxHealth() * 0.05F));
        }

        if (BlackSouls.BUFF_BURN.isPresent() && entity.hasEffect(BlackSouls.BUFF_BURN.get())) {
            if (entity.isInWater()) {
                entity.removeEffect(BlackSouls.BUFF_BURN.get());
                entity.clearFire();
                return;
            }

            if (entity.tickCount % 20 == 0) {
                float maxHp = entity.getMaxHealth();
                float burnDmg = (float) (maxHp * 0.05F * getPercentageDamageMultiplier(entity));

                entity.hurt(entity.damageSources().magic(), burnDmg);
                entity.setSecondsOnFire(1);

                ((ServerLevel)entity.level()).sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight()/2, entity.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    public static void applyStats(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            applyStats(player, new BaubleCounter(player), stats);
        }
    }

    private static void applyStats(Player player, BaubleCounter counts, BSPlayerStats stats) {
        if (stats != null) {
            stats.recalculateStats();
            resetDerivedStats(stats);
            applyCovenantBonuses(stats);

            // Accessory and armor section.
            int bladesCount = counts.count(BlackSouls.RING_BLADES.get());
            if (bladesCount > 0) stats.attack += (bladesCount * 50.0);

            if (counts.has(BlackSouls.RING_FRAGILE.get())) {
                stats.hp = Math.max(1, stats.hp * 0.01);
                stats.maxMp = Math.max(1, stats.maxMp * 0.01);
                stats.attack = Math.max(1, stats.attack * 0.01);
                stats.defense = Math.max(0, stats.defense * 0.01);
            }

            int rabbitCount = counts.count(BlackSouls.RING_WHITE_RABBIT.get());
            if (rabbitCount > 0) {
                double multiplier = 1.0 + (rabbitCount * 0.20);
                stats.hp *= multiplier;
                stats.maxMp *= multiplier;
                stats.attack *= multiplier;
                stats.defense *= multiplier;
                stats.magicDefense *= multiplier;
                stats.luck *= multiplier;
                multiplyMagicAttack(stats, multiplier);

                stats.speed *= 1.20;
            }

            int lifeRingCount = counts.count(BlackSouls.RING_LIFE.get());
            if (lifeRingCount > 0) stats.hp *= (1.0 + lifeRingCount * 0.05);

            int evilEyeCount = counts.count(BlackSouls.RING_EVIL_EYE.get());
            if (evilEyeCount > 0) stats.hpRegenRate += evilEyeCount * 0.03;

            int poisonBiteCount = counts.count(BlackSouls.RING_POISON_BITE.get());
            if (poisonBiteCount > 0) {
                stats.poisonResistRate = 1.0;
                stats.severePoisonResistRate = 1.0;
            }

            int bloodBiteCount = counts.count(BlackSouls.RING_BLOOD_BITE.get());
            if (bloodBiteCount > 0) {
                stats.bleedResistRate = 1.0;
                stats.hpRegenRate += bloodBiteCount * 0.02;
            }

            int guardCount = counts.count(BlackSouls.RING_GUARD.get());
            if (guardCount > 0) stats.defense += guardCount * 50.0;

            int terrorCount = counts.count(BlackSouls.RING_TERROR.get());
            if (terrorCount > 0) {
                stats.fearRate += terrorCount * 50.0;
                stats.fearResistRate = 1.0;
            }

            int fairyCount = counts.count(BlackSouls.RING_FAIRY.get());
            if (fairyCount > 0) {
                stats.evasion += fairyCount * 5.0;
                stats.sleepResistRate = 1.0;
            }

            int windGodCount = counts.count(BlackSouls.RING_WIND_GOD.get());
            if (windGodCount > 0) stats.speed += windGodCount * 50.0;

            int spellCount = counts.count(BlackSouls.RING_SPELL.get());
            if (spellCount > 0) stats.magicAttack += spellCount * 50.0;

            int masochistCount = counts.count(BlackSouls.RING_MASOCHIST.get());
            if (masochistCount > 0) stats.targetingRate *= Math.pow(4.0, masochistCount);

            int midnightCrownCount = counts.count(BlackSouls.RING_MIDNIGHT_CROWN.get());
            if (midnightCrownCount > 0) {
                stats.hp *= Math.pow(0.5, midnightCrownCount);
                stats.maxMp *= Math.pow(0.5, midnightCrownCount);
                stats.magicAttack *= Math.pow(1.5, midnightCrownCount);
            }

            int godFishCount = counts.count(BlackSouls.RING_GOD_FISH.get());
            if (godFishCount > 0) stats.defense *= (1.0 + godFishCount * 1.0);

            int waspCount = counts.count(BlackSouls.RING_WASP.get());
            if (waspCount > 0) stats.critRate += waspCount * 20.0;

            int puyoCount = counts.count(BlackSouls.RING_PUYO.get());
            if (puyoCount > 0) stats.hp *= (1.0 + puyoCount * 0.15);

            int hunyaCount = counts.count(BlackSouls.RING_HUNYA.get());
            if (hunyaCount > 0) stats.maxMp *= (1.0 + hunyaCount * 0.05);

            int goddessCount = counts.count(BlackSouls.RING_GODDESS.get());
            if (goddessCount > 0) stats.mpRegenRate += goddessCount * 0.05;

            int angelCount = counts.count(BlackSouls.RING_ANGEL.get());
            if (angelCount > 0) stats.mpRegenRate += angelCount * 0.03;

            int knightRingCount = counts.count(BlackSouls.RING_KNIGHT.get());
            if (knightRingCount > 0) stats.stunRate += knightRingCount * 10.0;

            int ironMaidenCount = counts.count(BlackSouls.RING_IRON_MAIDEN.get());
            if (ironMaidenCount > 0) stats.hpRegenRate -= ironMaidenCount * 0.20;

            int ironProtectionCount = counts.count(BlackSouls.RING_IRON_PROTECTION.get());
            if (ironProtectionCount > 0) stats.physicalDamageRate *= Math.pow(0.8, ironProtectionCount);

            int magicStoneCount = counts.count(BlackSouls.RING_MAGIC_STONE.get());
            if (magicStoneCount > 0) {
                stats.magicDamageRate *= Math.pow(0.8, magicStoneCount);
                stats.magicDefense += magicStoneCount * 50.0;
            }

            int murderClownCount = counts.count(BlackSouls.RING_MURDER_CLOWN.get());
            if (murderClownCount > 0) {
                stats.attack += murderClownCount * 200.0D;
                stats.magicAttack += murderClownCount * 200.0D;
                stats.physicalDamageRate *= Math.pow(2.0D, murderClownCount);
                stats.magicDamageRate *= Math.pow(2.0D, murderClownCount);
            }

            int blackGoatCount = counts.count(BlackSouls.RING_BLACK_GOAT.get());
            if (blackGoatCount > 0) {
                stats.hp += blackGoatCount * 5000.0D;
                stats.attack += blackGoatCount * 500.0D;
                stats.defense += blackGoatCount * 500.0D;
                stats.magicAttack += blackGoatCount * 500.0D;
                stats.magicDefense += blackGoatCount * 500.0D;
                stats.speed += blackGoatCount * 500.0D;
                stats.luck += blackGoatCount * 500.0D;
                double multiplier = Math.pow(1.20D, blackGoatCount);
                stats.hp *= multiplier;
                stats.attack *= multiplier;
                stats.defense *= multiplier;
                stats.magicAttack *= multiplier;
                stats.magicDefense *= multiplier;
                stats.speed *= multiplier;
                stats.luck *= multiplier;
                stats.maxMp = 0.0D;
                stats.mp = 0.0D;
            }

            int vanityCount = counts.count(BlackSouls.RING_VANITY.get());
            if (vanityCount > 0) {
                stats.hp *= Math.pow(2.0D, vanityCount);
                stats.maxMp *= Math.pow(2.0D, vanityCount);
                stats.defense *= Math.pow(0.5D, vanityCount);
                stats.magicDefense *= Math.pow(0.5D, vanityCount);
            }

            int lundinianCount = counts.count(BlackSouls.RING_LUNDINIAN.get());
            if (lundinianCount > 0) {
                double multiplier = Math.pow(1.05D, lundinianCount);
                stats.hp *= multiplier;
                stats.maxMp *= multiplier;
                stats.attack *= multiplier;
                stats.defense *= multiplier;
                stats.magicAttack *= multiplier;
                stats.magicDefense *= multiplier;
                stats.speed *= multiplier;
                stats.luck *= multiplier;
                stats.evasion -= lundinianCount * 5.0D;
            }

            stats.mpRegenRate += counts.count(BlackSouls.RING_DEEP_ONE.get()) * 0.10D;
            stats.speed += counts.count(BlackSouls.RING_WHITE_RAVEN.get()) * 100.0D;
            stats.luck += counts.count(BlackSouls.RING_FOUR_LEAF_CLOVER.get()) * 200.0D;

            int recklessHeroCount = counts.count(BlackSouls.RING_RECKLESS_HERO.get());
            if (recklessHeroCount > 0) {
                stats.critRate += recklessHeroCount * 60.0D;
                stats.defense *= Math.pow(0.01D, recklessHeroCount);
                stats.magicDefense *= Math.pow(0.01D, recklessHeroCount);
            }
            stats.speed *= Math.pow(1.15D, counts.count(BlackSouls.RING_BOOTBLACK.get()));
            stats.extraActionRate += counts.count(BlackSouls.RING_PROSTITUTE.get()) * 0.10D;

            int trollCount = counts.count(BlackSouls.RING_TROLL.get());
            if (trollCount > 0) {
                stats.attack *= Math.pow(1.50D, trollCount);
                stats.critRate -= trollCount * 50.0D;
            }

            int redTearstoneCount = counts.count(BlackSouls.RING_RED_TEARSTONE.get());
            if (redTearstoneCount > 0) {
                stats.attack *= Math.pow(1.20D, redTearstoneCount);
                stats.magicAttack *= Math.pow(1.20D, redTearstoneCount);
            }

            int walrusCount = counts.count(BlackSouls.RING_WALRUS.get());
            if (walrusCount > 0) {
                stats.hp *= Math.pow(0.50D, walrusCount);
                stats.hpRegenRate += walrusCount * 0.50D;
            }

            int hellDestructionCount = counts.count(BlackSouls.RING_HELL_DESTRUCTION.get());
            if (hellDestructionCount > 0) {
                stats.attack *= Math.pow(1.50D, hellDestructionCount);
                stats.speed *= Math.pow(0.50D, hellDestructionCount);
            }

            int heartKnightCount = counts.count(BlackSouls.RING_HEART_KNIGHT.get());
            stats.hpRegenRate += heartKnightCount * 0.10D;
            stats.mpRegenRate += heartKnightCount * 0.10D;
            stats.speed *= Math.pow(1.20D, counts.count(BlackSouls.RING_SPADE_KNIGHT.get()));
            stats.defense *= Math.pow(1.10D, counts.count(BlackSouls.RING_CLUB_KNIGHT.get()));

            int sinCount = counts.count(BlackSouls.RING_SIN.get());
            if (sinCount > 0) {
                double multiplier = Math.pow(0.70D, sinCount);
                stats.hp *= multiplier;
                stats.maxMp *= multiplier;
                stats.attack *= multiplier;
                stats.defense *= multiplier;
                stats.magicAttack *= multiplier;
                stats.magicDefense *= multiplier;
                stats.speed *= multiplier;
                stats.luck *= multiplier;
                stats.critRate += sinCount * 50.0D;
                stats.evasion += sinCount * 50.0D;
            }

            int starCount = counts.count(BlackSouls.RING_STAR.get());
            if (starCount > 0) {
                stats.attack *= Math.pow(2.0D, starCount);
                stats.magicAttack = 0.0D;
            }

            int ogreCount = counts.count(BlackSouls.RING_OGRE.get());
            if (ogreCount > 0) {
                stats.defense *= Math.pow(1.50D, ogreCount);
                stats.evasion -= ogreCount * 50.0D;
            }
            stats.speed *= Math.pow(1.50D, counts.count(BlackSouls.RING_IDATEN.get()));
            stats.attack *= Math.pow(0.01D, counts.count(BlackSouls.RING_ADULTERY.get()));

            stats.evasion += counts.count(BlackSouls.RING_LIEF.get()) * 20.0;
            stats.evasion += counts.count(BlackSouls.RING_VOID.get()) * 10.0;
            stats.evasion -= counts.count(BlackSouls.RING_DEATH.get()) * 50.0;
            applyExpandedOriginalRingStats(counts, stats);

            if (counts.count(BlackSouls.NOBLE_CLOTHES.get()) > 0) {
                stats.evasion += 5.0; stats.magicDefense *= 1.05; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.LAWYER_MASK.get()) > 0) {
                stats.magicDefense *= 1.10; stats.defense *= 1.05; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.VIOLENT_CLOAK.get()) > 0) {
                stats.attack *= 1.05; try { stats.magicAttack *= 1.05; } catch (Exception ignored) {}
                stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.FRENZIED_KING_CLOAK.get()) > 0) {
                try { stats.magicAttack *= 1.15; } catch (Exception ignored) {}
                stats.magicDefense *= 1.15; stats.speed *= 0.90;
            }
            if (counts.count(BlackSouls.ANGEL_RAIMENT.get()) > 0) {
                stats.hp *= 1.50; stats.speed *= 0.96;
            }
            if (counts.count(BlackSouls.LEATHER_ARMOR.get()) > 0) {
                stats.attack *= 1.08; stats.defense *= 1.05; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.MATCH_GIRL_CLOTHES.get()) > 0) {
                stats.evasion += 5.0;
            }
            if (counts.count(BlackSouls.GENTLEMAN_COAT.get()) > 0) {
                stats.evasion += 5.0; stats.defense *= 1.03;
            }
            if (counts.count(BlackSouls.PROSTITUTE_DRESS.get()) > 0) {
                stats.evasion += 5.0;
            }
            if (counts.count(BlackSouls.PLATE_ARMOR.get()) > 0) {
                stats.defense *= 1.10; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.MILTON_ARMOR.get()) > 0) {
                stats.defense *= 1.10; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.MILTON_HELMET.get()) > 0) {
                stats.defense *= 1.06; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.HUNTERS_ATTIRE.get()) > 0) {
                stats.evasion += 10.0; stats.speed *= 0.98;
            }
            if (counts.count(BlackSouls.DEEP_SEA_KNIGHT_HELMET.get()) > 0) {
                stats.defense *= 1.40; stats.speed *= 0.80;
            }
            if (counts.count(BlackSouls.DEEP_SEA_KNIGHT_ARMOR.get()) > 0) {
                stats.defense *= 1.50; stats.speed *= 0.70;
            }
            if (counts.count(BlackSouls.CREW_HEADSCARF.get()) > 0) {
                stats.critRate += 5.0; stats.speed *= 0.99;
            }
            if (counts.count(BlackSouls.ONI_WARRIOR_HELMET.get()) > 0) {
                stats.critRate += 15.0; stats.defense *= 1.15; stats.speed *= 0.90;
            }
            if (counts.count(BlackSouls.ONI_WARRIOR_ARMOR.get()) > 0) {
                stats.critRate += 20.0; stats.defense *= 1.20; stats.speed *= 0.80;
            }
            if (counts.count(BlackSouls.SAILOR_SUIT.get()) > 0) {
                stats.critRate += 5.0; stats.evasion += 5.0;
            }
            if (counts.count(BlackSouls.SNAKE_DRESS.get()) > 0) {
                stats.evasion += 5.0; stats.speed *= 0.98;
            }
            if (counts.count(BlackSouls.DISCIPLINARIAN_ROBE.get()) > 0) {
                stats.magicDefense *= 1.25; stats.mpRegenRate += 0.05; stats.speed *= 0.94;
            }
            if (counts.count(BlackSouls.OMINOUS_CLOTHES.get()) > 0) {
                stats.evasion += 30.0;
            }
            if (counts.count(BlackSouls.BUTETSU_ARMOR.get()) > 0) {
                stats.critRate += 15.0; stats.defense *= 1.15; stats.speed *= 0.90;
            }
            int workClothesCount = counts.count(BlackSouls.WORK_CLOTHES.get());
            stats.evasion += workClothesCount * 5.0D;

            int abyssArmorCount = counts.count(BlackSouls.ABYSS_ARMOR.get());
            stats.defense *= Math.pow(1.10D, abyssArmorCount);
            stats.speed *= Math.pow(0.90D, abyssArmorCount);

            int abyssHelmetCount = counts.count(BlackSouls.ABYSS_HELMET.get());
            stats.defense *= Math.pow(1.08D, abyssHelmetCount);
            stats.speed *= Math.pow(0.94D, abyssHelmetCount);

            stats.speed *= Math.pow(2.0D, counts.count(BlackSouls.YELLOW_CLOTH.get()));
            stats.speed += counts.count(BlackSouls.PLAYWRIGHT_HEADSCARF.get()) * 2.0D;

            int falseAngelCrownCount = counts.count(BlackSouls.FALSE_ANGEL_CROWN.get());
            if (falseAngelCrownCount > 0) {
                stats.hp *= Math.pow(1.30D, falseAngelCrownCount);
                stats.speed += falseAngelCrownCount * 500.0D;
                stats.speed *= Math.pow(1.50D, falseAngelCrownCount);
            }

            int winterMageCoatCount = counts.count(BlackSouls.WINTER_MAGE_COAT.get());
            stats.magicDefense *= Math.pow(1.10D, winterMageCoatCount);
            stats.speed *= Math.pow(0.95D, winterMageCoatCount);

            int winterKnightArmorCount = counts.count(BlackSouls.WINTER_KNIGHT_ARMOR.get());
            stats.defense *= Math.pow(1.15D, winterKnightArmorCount);
            stats.speed *= Math.pow(0.90D, winterKnightArmorCount);

            int winterKnightHelmetCount = counts.count(BlackSouls.WINTER_KNIGHT_HELMET.get());
            stats.defense *= Math.pow(1.08D, winterKnightHelmetCount);
            stats.speed *= Math.pow(0.94D, winterKnightHelmetCount);

            int miracleGarbCount = counts.count(BlackSouls.MIRACLE_SHRINE_MAIDEN_GARB.get());
            if (miracleGarbCount > 0) {
                stats.magicAttack *= Math.pow(1.50D, miracleGarbCount);
                stats.magicDefense *= Math.pow(1.50D, miracleGarbCount);
                stats.maxMp *= Math.pow(0.01D, miracleGarbCount);
            }
            if (counts.hasChronoClock()) {
                stats.extraActionRate += 1.0;
            }

            if (counts.count(BlackSouls.ARMOR_OF_THE_SUN.get()) > 0) {
                stats.attack *= 1.15; stats.defense *= 1.15; stats.speed *= 0.90;
            }
            if (counts.count(BlackSouls.CLERIC_VESTMENT.get()) > 0) {
                stats.defense *= 1.05; stats.magicDefense *= 1.07; stats.evasion += 5.0; stats.speed *= 0.96;
            }
            if (counts.count(BlackSouls.MAGICIAN_COAT.get()) > 0) {
                try { stats.magicAttack *= 1.05; } catch (Exception ignored) {}
                stats.magicDefense *= 1.10; stats.evasion += 5.0; stats.speed *= 0.96;
            }
            if (counts.count(BlackSouls.SHADOW_ATTIRE.get()) > 0) {
                stats.defense *= 1.08; stats.evasion += 5.0; stats.speed *= 0.98;
            }
            if (counts.count(BlackSouls.KNIGHT_ARMOR.get()) > 0) {
                stats.defense *= 1.15; stats.speed *= 0.95;
            }
            if (counts.count(BlackSouls.WARRIOR_ARMOR.get()) > 0) {
                stats.attack *= 1.05; stats.defense *= 1.10; stats.speed *= 0.95;
            }
            if (counts.count(BlackSouls.BABEL_TOWER_ARMOR.get()) > 0) {
                stats.defense *= 1.30; stats.speed *= 0.80;
            }
            if (counts.count(BlackSouls.PHANTOM_THIEF_CLOAK.get()) > 0) {
                stats.evasion += 15.0; stats.defense *= 0.90;
            }
            if (counts.count(BlackSouls.CLERIC_CIRCLET.get()) > 0) {
                stats.defense *= 1.02; stats.magicDefense *= 1.03; stats.speed *= 0.99;
            }
            if (counts.count(BlackSouls.MAGICIAN_HAT.get()) > 0) {
                try { stats.magicAttack *= 1.03; } catch (Exception ignored) {}
                stats.magicDefense *= 1.05; stats.speed *= 0.98;
            }
            if (counts.count(BlackSouls.THIEF_MASK.get()) > 0) {
                stats.defense *= 1.04; stats.speed *= 0.99;
            }
            if (counts.count(BlackSouls.KNIGHT_HELMET.get()) > 0) {
                stats.defense *= 1.08; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.VIKING_HELMET.get()) > 0) {
                stats.attack *= 1.04; stats.defense *= 1.04; stats.speed *= 0.97;
            }
            if (counts.count(BlackSouls.RABBIT_EARS.get()) > 0) {
                stats.speed *= 1.05;
            }
            if (counts.count(BlackSouls.WHITE_HAIRBAND.get()) > 0) {
                stats.magicDefense *= 1.20; stats.speed *= 0.98;
            }
            if (counts.count(BlackSouls.BABEL_TOWER_HELMET.get()) > 0) {
                stats.defense *= 1.20; stats.speed *= 0.90;
            }
            if (counts.count(BlackSouls.NINJA_HEADBAND.get()) > 0) {
                stats.attack *= 1.02; stats.defense *= 1.02;
            }
            if (counts.count(BlackSouls.MYSTERIOUS_HAT.get()) > 0) {
                try { stats.magicAttack *= 1.08; } catch (Exception ignored) {}
                stats.speed *= 0.96;
            }
            if (counts.count(BlackSouls.HATTER_HAT.get()) > 0) {
                try { stats.magicAttack *= 1.15; } catch (Exception ignored) {}
                stats.speed *= 0.98;
            }
            if (counts.count(BlackSouls.SKY_KNIGHT_HAT.get()) > 0) {
                stats.evasion += 5.0;
            }
            if (counts.count(BlackSouls.IGOR_MASK.get()) > 0) {
                stats.attack *= 1.20; try { stats.magicAttack *= 1.20; } catch (Exception ignored) {}
                stats.speed *= 0.85;
            }
            if (counts.count(BlackSouls.BUNNY_GIRL_UNIFORM.get()) > 0) {
                stats.speed *= 1.05;
            }

            if (counts.count(BlackSouls.GUARDIAN_ANGEL.get()) > 0) {
                stats.defense *= 0.70;
                stats.magicDefense *= 0.70;
            }

            int abyssCount = counts.count(BlackSouls.RING_ABYSS.get());
            if (abyssCount > 0) stats.critRate += abyssCount * 40.0;
            int blackbeardCount = counts.count(BlackSouls.RING_BLACKBEARD.get());
            if (blackbeardCount > 0) {
                stats.instantDeathRate += blackbeardCount * 20.0;
            }
            ItemStack mainHand = player.getMainHandItem();

            int upgradeLevel = 0;
            if (!mainHand.isEmpty() && mainHand.hasTag() && mainHand.getTag().contains("bs2_upgrade_level")) {
                upgradeLevel = mainHand.getTag().getInt("bs2_upgrade_level");
            }

            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()) {
                stats.attack += 180.0;
                stats.instantDeathRate += 15.0;
            }

            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.ANDOR_SWORD.get()) {

                stats.mpCostRate *= 2.0;

                double addAtk = 10.0, addDef = 10.0, addMatk = 10.0, addMdef = 10.0;
                double addExtraAction = 0.30;

                if (upgradeLevel == 1) { addAtk = 32.0; addDef = 21.0; addMatk = 32.0; addMdef = 21.0; addExtraAction = 0.30; }
                else if (upgradeLevel == 2) { addAtk = 50.0; addDef = 40.0; addMatk = 50.0; addMdef = 40.0; addExtraAction = 0.50; }
                else if (upgradeLevel == 3) { addAtk = 70.0; addDef = 60.0; addMatk = 70.0; addMdef = 60.0; addExtraAction = 0.70; }
                else if (upgradeLevel == 4) { addAtk = 90.0; addDef = 80.0; addMatk = 90.0; addMdef = 80.0; addExtraAction = 0.90; }
                else if (upgradeLevel >= 5) { addAtk = 100.0; addDef = 90.0; addMatk = 100.0; addMdef = 90.0; addExtraAction = 1.00; }

                stats.attack += addAtk;
                stats.defense += addDef;
                stats.magicAttack += addMatk;
                stats.magicDefense += addMdef;
                stats.extraActionRate += addExtraAction;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.VORPAL_BLADE.get()) {
                double[] attackByLevel = {21.0D, 33.0D, 46.0D, 55.0D, 64.0D, 70.0D, 82.0D, 91.0D, 98.0D, 108.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }

            if (BlackSouls.BUFF_KNIGHTS_GLORY.isPresent() && player.hasEffect(BlackSouls.BUFF_KNIGHTS_GLORY.get())) {
                stats.hp *= 2.0;
            }

            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_SWORD.get()) {
                double addAtk = 18.0, addDef = 10.0, addMdef = 10.0;
                if (upgradeLevel == 1) { addAtk = 25.0; addDef = 15.0; addMdef = 15.0; }
                else if (upgradeLevel == 2) { addAtk = 34.0; addDef = 20.0; addMdef = 20.0; }
                else if (upgradeLevel == 3) { addAtk = 46.0; addDef = 25.0; addMdef = 25.0; }
                else if (upgradeLevel == 4) { addAtk = 55.0; addDef = 30.0; addMdef = 30.0; }
                else if (upgradeLevel == 5) { addAtk = 63.0; addDef = 35.0; addMdef = 35.0; }
                else if (upgradeLevel == 6) { addAtk = 72.0; addDef = 40.0; addMdef = 40.0; }
                else if (upgradeLevel == 7) { addAtk = 84.0; addDef = 45.0; addMdef = 45.0; }
                else if (upgradeLevel == 8) { addAtk = 95.0; addDef = 50.0; addMdef = 50.0; }
                else if (upgradeLevel >= 9) { addAtk = 105.0; addDef = 55.0; addMdef = 55.0; }

                stats.attack += addAtk;
                stats.defense += addDef;
                stats.magicDefense += addMdef;
            }

            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get()) {
                stats.attack += 120.0;
                stats.defense += 60.0;
                stats.magicDefense += 60.0;
                stats.defense *= 1.5;
                stats.magicDefense *= 1.5;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DRAKE_SWORD.get()) {
                double addAtk = 26.0;
                if (upgradeLevel == 1) addAtk = 50.0;
                else if (upgradeLevel == 2) addAtk = 70.0;
                else if (upgradeLevel == 3) addAtk = 110.0;
                else if (upgradeLevel == 4) addAtk = 130.0;
                else if (upgradeLevel >= 5) addAtk = 160.0;
                stats.attack += addAtk;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.THIEFS_DAGGER.get()) {
                double addAtk = 8.0;
                double addSpeed = 20.0;
                if (upgradeLevel == 1) { addAtk = 13.0; addSpeed = 30.0; }
                else if (upgradeLevel == 2) { addAtk = 18.0; addSpeed = 40.0; }
                else if (upgradeLevel == 3) { addAtk = 26.0; addSpeed = 50.0; }
                else if (upgradeLevel == 4) { addAtk = 32.0; addSpeed = 60.0; }
                else if (upgradeLevel == 5) { addAtk = 42.0; addSpeed = 70.0; }
                else if (upgradeLevel == 6) { addAtk = 49.0; addSpeed = 80.0; }
                else if (upgradeLevel == 7) { addAtk = 56.0; addSpeed = 90.0; }
                else if (upgradeLevel == 8) { addAtk = 62.0; addSpeed = 100.0; }
                else if (upgradeLevel >= 9) { addAtk = 68.0; addSpeed = 110.0; }
                stats.attack += addAtk;
                stats.speed += addSpeed;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GREAT_THIEFS_DAGGER.get()) {
                stats.attack += 72.0;
                stats.speed += 120.0;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GREAT_SWORD.get()) {
                double[] attackByLevel = {60.0D, 80.0D, 100.0D, 120.0D, 140.0D, 160.0D, 180.0D, 200.0D, 220.0D, 240.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
                stats.speed *= 0.5D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GIANT_SWORD.get()) {
                stats.attack += 280.0D;
                stats.attack *= 2.0D;
                stats.speed *= 0.5D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BROAD_SPEAR.get()) {
                double[] attackByLevel = {30.0D, 42.0D, 50.0D, 62.0D, 71.0D, 85.0D, 93.0D, 104.0D, 115.0D, 124.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GUNGNIR.get()) {
                stats.attack += 150.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.VORPAL_SWORD.get()) {
                stats.attack += 160.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BANDERSNATCH_SWORD.get()) {
                double[] attackByLevel = {50.0D, 70.0D, 90.0D, 110.0D, 135.0D, 180.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                if (level >= 5) {
                    stats.critRate += 30.0D;
                }
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.CLUB.get()) {
                double[] attackByLevel = {22.0D, 30.0D, 38.0D, 46.0D, 55.0D, 65.0D, 71.0D, 80.0D, 95.0D, 123.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KING_CLUB.get()) {
                stats.attack += 150.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MAGIC_BLADE.get()) {
                double[] attackByLevel = {50.0D, 65.0D, 75.0D, 85.0D, 94.0D, 102.0D, 114.0D, 127.0D, 144.0D, 160.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
                stats.hp *= 0.8D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DEMON_GOD_BLADE.get()) {
                stats.attack += 190.0D;
                stats.hp *= 0.9D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MAGICIANS_STAFF.get()) {
                double[] attackByLevel = {2.0D, 4.0D, 7.0D, 9.0D, 11.0D, 12.0D, 15.0D, 15.0D, 17.0D, 19.0D};
                double[] magicByLevel = {10.0D, 15.0D, 20.0D, 28.0D, 35.0D, 42.0D, 49.0D, 56.0D, 66.0D, 77.0D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.magicAttack += magicByLevel[level];
                stats.mpCostRate *= 0.5D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.ALL_CREATION_STAFF.get()) {
                stats.attack += 21.0D;
                stats.magicAttack += 100.0D;
                stats.mpCostRate = 0.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MEAT_CLEAVER_GREATAXE.get()) {
                double[] attackByLevel = {40.0D, 50.0D, 69.0D, 78.0D, 85.0D, 93.0D, 100.0D, 115.0D, 128.0D, 135.0D};
                double[] speedByLevel = {-10.0D, -15.0D, -20.0D, -25.0D, -30.0D, -35.0D, -40.0D, -45.0D, -50.0D, -55.0D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed += speedByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.SLAUGHTERER_GREATAXE.get()) {
                stats.attack += 150.0D;
                stats.speed -= 60.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DOUBLE_EDGED_GREATSWORD.get()) {
                double[] attackByLevel = {45.0D, 54.0D, 67.0D, 78.0D, 86.0D, 95.0D, 106.0D, 115.0D, 129.0D, 140.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.RAGNAROK.get()) {
                stats.attack += 200.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.HUNTING_BOW.get()) {
                double[] attackByLevel = {10.0D, 19.0D, 22.0D, 28.0D, 34.0D, 40.0D, 48.0D, 52.0D, 55.0D, 61.0D};
                double[] speedByLevel = {5.0D, 10.0D, 15.0D, 22.0D, 30.0D, 40.0D, 48.0D, 53.0D, 62.0D, 70.0D};
                double[] criticalByLevel = {5.0D, 10.0D, 15.0D, 20.0D, 25.0D, 30.0D, 35.0D, 45.0D, 55.0D, 65.0D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed += speedByLevel[level];
                stats.critRate += criticalByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BRAVE_BOW.get()) {
                stats.attack += 70.0D;
                stats.speed += 80.0D;
                stats.critRate += 70.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MACE.get()) {
                double[] attackByLevel = {28.0D, 34.0D, 40.0D, 50.0D, 60.0D, 70.0D, 80.0D, 90.0D, 100.0D, 110.0D};
                double[] magicByLevel = {5.0D, 10.0D, 15.0D, 20.0D, 25.0D, 30.0D, 40.0D, 50.0D, 60.0D, 70.0D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.magicAttack += magicByLevel[level];
                stats.hpRegenRate += level <= 3 ? 0.20D : level <= 6 ? 0.30D : 0.40D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DIVINE_PUNISHMENT_MACE.get()) {
                stats.attack += 130.0D;
                stats.magicAttack += 90.0D;
                stats.hpRegenRate += 0.50D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.HALBERD.get()) {
                double[] attackByLevel = {26.0D, 41.0D, 48.0D, 53.0D, 65.0D, 70.0D, 80.0D, 88.0D, 100.0D, 125.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BAHAMUT.get()) {
                stats.attack += 170.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BEAST_HUNTER_SAW.get()) {
                double[] attackByLevel = {19.0D, 33.0D, 47.0D, 58.0D, 68.0D, 75.0D, 84.0D, 90.0D, 100.0D, 115.0D};
                double[] speedByLevel = {5.0D, 12.0D, 18.0D, 22.0D, 28.0D, 36.0D, 42.0D, 47.0D, 54.0D, 60.0D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed += speedByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BEAST_SLAYING_SAW_SWORD.get()) {
                stats.attack += 144.0D;
                stats.speed += 90.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.SHIELD_GUARD_FORTRESS.get()) {
                double[] attackByLevel = {10.0D, 15.0D, 22.0D, 30.0D, 36.0D, 48.0D, 51.0D, 58.0D, 65.0D, 80.0D};
                double[] physicalRateByLevel = {0.95D, 0.90D, 0.85D, 0.80D, 0.75D, 0.70D, 0.65D, 0.60D, 0.55D, 0.50D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.physicalDamageRate *= physicalRateByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GUARDIAN_FORTRESS.get()) {
                stats.attack += 100.0D;
                stats.physicalDamageRate *= 0.45D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DARK_SWORD.get()) {
                double[] attackByLevel = {24.0D, 34.0D, 48.0D, 55.0D, 64.0D, 77.0D, 83.0D, 90.0D, 104.0D, 110.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DARK_BLADE.get()) {
                stats.attack += 175.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BROKEN_SWORD.get()) {
                double[] attackByLevel = {7.0D, 14.0D, 21.0D, 28.0D, 35.0D, 43.0D, 50.0D, 57.0D, 64.0D, 70.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GRUDGE_SWORD.get()) {
                stats.attack += 77.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.WARHAMMER.get()) {
                double[] attackByLevel = {50.0D, 80.0D, 92.0D, 107.0D, 118.0D, 126.0D, 139.0D, 150.0D, 166.0D, 180.0D};
                double[] magicByLevel = {-50.0D, -80.0D, -92.0D, -107.0D, -118.0D, -126.0D, -117.0D, -106.0D, -90.0D, -76.0D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.magicAttack += magicByLevel[level];
                stats.critRate += 50.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.ABERRANT_WARHAMMER.get()) {
                stats.attack += 300.0D;
                stats.magicAttack -= 500.0D;
                stats.critRate += 100.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNUCKLE_DUSTER.get()) {
                double[] attackByLevel = {20.0D, 28.0D, 39.0D, 48.0D, 56.0D, 66.0D, 73.0D, 79.0D, 85.0D, 90.0D};
                double[] speedRateByLevel = {1.05D, 1.10D, 1.15D, 1.20D, 1.25D, 1.30D, 1.35D, 1.40D, 1.45D, 1.50D};
                int level = Math.max(0, Math.min(9, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed *= speedRateByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KAISER_GAUNTLET.get()) {
                stats.attack += 100.0D;
                stats.speed *= 1.60D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.UCHIGATANA.get()) {
                double[] attackByLevel = {40.0D, 53.0D, 65.0D, 77.0D, 84.0D, 92.0D, 100.0D, 111.0D, 124.0D, 136.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(9, upgradeLevel))];
                stats.magicAttack *= 0.01D;
                stats.critRate += 50.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KISHIN_BLADE.get()) {
                stats.attack += 170.0D;
                stats.magicAttack *= 0.01D;
                stats.critRate += 100.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GREAT_IRON_BALL.get()) {
                double[] attackByLevel = {18.0D, 36.0D, 58.0D, 72.0D, 92.0D, 130.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(5, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.HANS_MACHINE_GUN.get()) {
                double[] attackByLevel = {20.0D, 30.0D, 40.0D, 50.0D, 60.0D, 70.0D};
                double[] speedByLevel = {-30.0D, -40.0D, -50.0D, -60.0D, -70.0D, -80.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed += speedByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.JUDGMENT_SCYTHE.get()) {
                double bonus = upgradeLevel >= 5 ? 66.0D : 6.0D;
                stats.hp += bonus;
                stats.maxMp += bonus;
                stats.attack += bonus;
                stats.defense += bonus;
                stats.magicAttack += bonus;
                stats.magicDefense += bonus;
                stats.speed += bonus;
                stats.luck += bonus;
                stats.hpRegenRate += 0.06D;
                stats.mpRegenRate += 0.06D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.STORM_RULER.get()) {
                double[] attackByLevel = {24.0D, 42.0D, 63.0D, 84.0D, 111.0D, 120.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                if (level >= 5) {
                    stats.maxMp += 300.0D;
                    stats.speed += 100.0D;
                }
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DEMON_STAFF.get()) {
                double[] magicByLevel = {20.0D, 30.0D, 40.0D, 50.0D, 60.0D, 70.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.magicAttack += magicByLevel[level];
                stats.magicAttack *= level >= 5 ? 2.0D : 1.5D;
                stats.mpCostRate *= level >= 5 ? 3.0D : 2.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MOONLIGHT_GREATSWORD.get()) {
                double[] attackByLevel = {35.0D, 50.0D, 66.0D, 70.0D, 82.0D, 100.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.magicAttack += attackByLevel[level];
                stats.hpRegenRate += level >= 5 ? 0.15D : 0.05D;
                stats.mpRegenRate += level >= 5 ? 0.15D : 0.05D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.CORRUPT_JABBERWOCK_SCYTHE.get()) {
                double[] attackByLevel = {5.0D, 10.0D, 15.0D, 20.0D, 25.0D, 30.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.magicAttack += attackByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MAD_BOW_JUBJUB.get()) {
                double[] attackByLevel = {25.0D, 35.0D, 44.0D, 54.0D, 65.0D, 70.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(5, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MIRANDA_AXE.get()) {
                double[] attackByLevel = {50.0D, 100.0D, 200.0D, 300.0D, 400.0D, 500.0D};
                double[] speedByLevel = {-50.0D, -100.0D, -106.0D, -56.0D, -6.0D, -300.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed += speedByLevel[level];
                stats.attack *= level >= 5 ? 3.0D : 2.0D;
                stats.critRate += level >= 5 ? 40.0D : 30.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.RLYEH_STAFF.get()) {
                double[] magicByLevel = {25.0D, 45.0D, 65.0D, 80.0D, 95.0D, 125.0D};
                stats.magicAttack += magicByLevel[Math.max(0, Math.min(5, upgradeLevel))];
                stats.magicEvasion += 30.0D;
                stats.mpRegenRate += 0.50D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DEEP_SEA_KNIGHTS_ANCHOR.get()) {
                double[] attackByLevel = {45.0D, 95.0D, 170.0D, 240.0D, 300.0D, 450.0D};
                double[] speedByLevel = {-100.0D, -120.0D, -106.0D, -76.0D, -56.0D, -6.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.speed += speedByLevel[level];
                stats.attack *= level >= 5 ? 3.0D : 2.0D;
                stats.critRate -= 100.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.LOST_SWORD.get()) {
                double[] magicByLevel = {30.0D, 50.0D, 80.0D, 120.0D, 160.0D, 220.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.magicAttack += magicByLevel[level];
                stats.magicDefense += magicByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.GLACHID.get()) {
                double[] attackByLevel = {30.0D, 60.0D, 90.0D, 120.0D, 140.0D, 160.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(5, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.SLAUGHTERERS_CHAINSAW.get()) {
                double[] attackByLevel = {25.0D, 45.0D, 60.0D, 80.0D, 92.0D, 108.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(5, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MOCK_TURTLE_SOUP_LADLE.get()) {
                double[] attackByLevel = {1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 6.0D};
                double[] luckByLevel = {10.0D, 15.0D, 20.0D, 30.0D, 40.0D, 50.0D};
                double[] luckRateByLevel = {1.05D, 1.10D, 1.20D, 1.30D, 1.40D, 1.50D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.luck += luckByLevel[level];
                stats.luck *= luckRateByLevel[level];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get()) {
                double[] attackByLevel = {30.0D, 40.0D, 50.0D, 60.0D, 70.0D, 80.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(5, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.HOLY_GUNBLADE.get()) {
                double[] attackByLevel = {60.0D, 75.0D, 83.0D, 95.0D, 110.0D, 130.0D};
                stats.attack += attackByLevel[Math.max(0, Math.min(5, upgradeLevel))];
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.MARY_SUES_BRANCH_STAFF.get()) {
                stats.magicAttack += 500.0D;
                stats.speed += 500.0D;
                stats.speed *= 2.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.EUNICES_RAPIER.get()) {
                double[] attackByLevel = {40.0D, 65.0D, 85.0D, 105.0D, 125.0D, 145.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.attack += attackByLevel[level];
                stats.critRate += level >= 5 ? 20.0D : 15.0D;
            }
            if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.RAIDENS_DUAL_AXES.get()) {
                double[] hpByLevel = {500.0D, 1000.0D, 1500.0D, 2000.0D, 2500.0D, 3000.0D};
                int level = Math.max(0, Math.min(5, upgradeLevel));
                stats.hp += hpByLevel[level];
                stats.hp *= level >= 5 ? 1.40D : 1.30D;
                stats.critRate -= 100.0D;
            }
            if (BlackSouls.BUFF_MANA_REGEN.isPresent() && player.hasEffect(BlackSouls.BUFF_MANA_REGEN.get())) {
                stats.mpRegenRate += 0.10D;
            }
            if (BlackSouls.BUFF_OILY.isPresent() && player.hasEffect(BlackSouls.BUFF_OILY.get())) {
                stats.speed *= 0.80D;
            }
            if (BlackSouls.BUFF_STRUGGLE.isPresent() && player.hasEffect(BlackSouls.BUFF_STRUGGLE.get())) {
                stats.critRate += 30.0D;
            }
            if (BlackSouls.BUFF_BERSERK.isPresent() && player.hasEffect(BlackSouls.BUFF_BERSERK.get())) {
                stats.attack *= 1.5D;
                stats.speed *= 1.5D;
            }
            if (BlackSouls.BUFF_SELF_HARM.isPresent() && player.hasEffect(BlackSouls.BUFF_SELF_HARM.get())) {
                stats.attack *= 1.5D;
                stats.critRate += 50.0D;
            }
            if (BlackSouls.BUFF_HAKI.isPresent() && player.hasEffect(BlackSouls.BUFF_HAKI.get())) {
                stats.hp *= 1.25D;
                stats.maxMp *= 1.25D;
                stats.attack *= 1.25D;
                stats.defense *= 1.25D;
                stats.magicAttack *= 1.25D;
                stats.magicDefense *= 1.25D;
                stats.speed *= 1.25D;
                stats.luck *= 1.25D;
            }
            if (BlackSouls.BUFF_QUICK_RELOAD.isPresent() && player.hasEffect(BlackSouls.BUFF_QUICK_RELOAD.get())) {
                stats.evasion += 70.0D;
                stats.speed *= 1.5D;
            }
            if (BlackSouls.BUFF_QUICK_RELOAD_CRIT.isPresent() && player.hasEffect(BlackSouls.BUFF_QUICK_RELOAD_CRIT.get())) {
                stats.critRate += 30.0D;
            }
            if (BlackSouls.BUFF_MAD_BIRD_CALL.isPresent() && player.hasEffect(BlackSouls.BUFF_MAD_BIRD_CALL.get())) {
                stats.attack *= 2.0D;
                stats.magicAttack *= 2.0D;
                stats.hpRegenRate += 0.50D;
            }
            if (BlackSouls.BUFF_ECLIPSE.isPresent() && player.hasEffect(BlackSouls.BUFF_ECLIPSE.get())) {
                stats.defense *= 1.25D;
                stats.magicDefense *= 1.25D;
                stats.physicalDamageRate *= 0.50D;
                stats.magicDamageRate *= 0.50D;
            }
            if (BlackSouls.BUFF_HIGH_MOBILITY.isPresent() && player.hasEffect(BlackSouls.BUFF_HIGH_MOBILITY.get())) {
                stats.speed *= 2.0D;
                stats.extraActionRate += 1.0D;
            }
            if (BlackSouls.BUFF_DUAL_SWORD_AURA.isPresent() && player.hasEffect(BlackSouls.BUFF_DUAL_SWORD_AURA.get())) {
                MobEffectInstance aura = player.getEffect(BlackSouls.BUFF_DUAL_SWORD_AURA.get());
                int stacks = aura == null ? 0 : Math.min(7, aura.getAmplifier() + 1);
                stats.attack *= 1.0D + stacks * 0.10D;
            }
            if (BlackSouls.BUFF_NATURAL_RECOVERY.isPresent() && player.hasEffect(BlackSouls.BUFF_NATURAL_RECOVERY.get())) {
                stats.hpRegenRate += 0.50D;
            }
            if (BlackSouls.BUFF_INNER_POTENTIAL.isPresent() && player.hasEffect(BlackSouls.BUFF_INNER_POTENTIAL.get())) {
                stats.magicAttack *= 2.0D;
            }
            if (BlackSouls.BUFF_AWAKENING.isPresent() && player.hasEffect(BlackSouls.BUFF_AWAKENING.get())) {
                stats.attack *= 0.50D;
                stats.magicAttack *= 2.0D;
                stats.critRate += 100.0D;
                stats.extraActionRate += 1.0D;
            }
            if (BlackSouls.BUFF_HP_REGEN.isPresent() && player.hasEffect(BlackSouls.BUFF_HP_REGEN.get())) {
                stats.hpRegenRate += 0.10D;
            }
            if (BlackSouls.BUFF_HP_MP_UP.isPresent() && player.hasEffect(BlackSouls.BUFF_HP_MP_UP.get())) {
                stats.hp *= 1.25D;
                stats.maxMp *= 1.25D;
            }
            if (BlackSouls.BUFF_JUGGLING_EVASION.isPresent() && player.hasEffect(BlackSouls.BUFF_JUGGLING_EVASION.get())) {
                stats.speed *= 1.50D;
                stats.evasion += 70.0D;
            }
            if (BlackSouls.BUFF_NECRONOMICON.isPresent() && player.hasEffect(BlackSouls.BUFF_NECRONOMICON.get())) {
                stats.mpCostRate = 0.0D;
            }
            if (BlackSouls.BUFF_FROSTBITE.isPresent() && player.hasEffect(BlackSouls.BUFF_FROSTBITE.get())) {
                stats.magicDefense *= 0.80D;
            }
            if (BlackSouls.BUFF_LACERATION.isPresent() && player.hasEffect(BlackSouls.BUFF_LACERATION.get())) {
                stats.attack *= 0.85D;
                stats.magicAttack *= 0.85D;
                stats.hpRegenRate -= 0.02D;
            }
            if (BlackSouls.BUFF_DEFENSE_KING.isPresent() && player.hasEffect(BlackSouls.BUFF_DEFENSE_KING.get())) {
                stats.defense *= 9.0D;
                stats.magicDefense *= 9.0D;
                stats.hpRegenRate += 1.0D;
            }
            if (BlackSouls.BUFF_PLAYWRIGHT.isPresent() && player.hasEffect(BlackSouls.BUFF_PLAYWRIGHT.get())) {
                stats.attack *= 2.0D;
                stats.magicAttack *= 2.0D;
                stats.critRate += 100.0D;
            }

            stats.attack *= getAttackShiftMultiplier(player);
            stats.defense *= getDefenseShiftMultiplier(player);
            stats.magicAttack *= getMagicAttackShiftMultiplier(player);
            stats.magicDefense *= getMagicDefenseShiftMultiplier(player);
            stats.luck *= getLuckShiftMultiplier(player);
            stats.speed *= getSpeedShiftMultiplier(player);

            ItemStack offHand = player.getOffhandItem();
            if (!offHand.isEmpty() && offHand.getItem() == BlackSouls.MURDERERS_SHOTGUN.get()) {
                stats.attack += 50.0; stats.speed *= 0.95;
            }
            if (hasPorcupineShield(player)) stats.speed *= 0.97D;

            stats.weaponEnchantments.clear();

            if (BlackSouls.BUFF_FIRE_POWER.isPresent() && player.hasEffect(BlackSouls.BUFF_FIRE_POWER.get())) {
                stats.weaponEnchantments.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.FIRE);
                stats.attack *= 1.05;
            }
            if (BlackSouls.BUFF_ICE_POWER.isPresent() && player.hasEffect(BlackSouls.BUFF_ICE_POWER.get())) {
                stats.weaponEnchantments.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.ICE);
                stats.attack *= 1.05;
            }
            if (BlackSouls.BUFF_THUNDER_POWER.isPresent() && player.hasEffect(BlackSouls.BUFF_THUNDER_POWER.get())) {
                stats.weaponEnchantments.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.THUNDER);
                stats.attack *= 1.05;
            }
            if (BlackSouls.BUFF_DARK_POWER.isPresent() && player.hasEffect(BlackSouls.BUFF_DARK_POWER.get())) {
                stats.weaponEnchantments.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.DARK);
                stats.attack *= 1.05;
            }
            if (BlackSouls.BUFF_DAGGER_EVASION.isPresent() && player.hasEffect(BlackSouls.BUFF_DAGGER_EVASION.get())) {
                stats.evasion += 70.0D;
            }
            if (BlackSouls.BUFF_FEAR.isPresent() && player.hasEffect(BlackSouls.BUFF_FEAR.get())) {
                stats.evasion -= 100.0;
            }
            if (BlackSouls.BUFF_SEVERED_LEG.isPresent() && player.hasEffect(BlackSouls.BUFF_SEVERED_LEG.get())) {
                stats.speed = 0.0;
                stats.evasion = 0.0;
                stats.defense *= 0.01;
            }
            MinecraftForge.EVENT_BUS.post(new BSStatsRecalcEvent(player, stats));
            stats.magicEvasion += stats.evasion;
            clampCalculatedStats(player, stats);
            syncVanillaAttributes(player, stats);
        }
    }

    private static void applyExpandedOriginalRingStats(BaubleCounter counts, BSPlayerStats stats) {
        int count;

        stats.hp *= Math.pow(1.10D, counts.count(ItemOriginalRing.Profile.LIFE_PLUS_1));
        stats.hp *= Math.pow(1.25D, counts.count(ItemOriginalRing.Profile.LIFE_PLUS_2));
        stats.hp *= Math.pow(1.50D, counts.count(ItemOriginalRing.Profile.LIFE_PLUS_3));

        count = counts.count(ItemOriginalRing.Profile.PUYO_PLUS_1);
        stats.hp = (stats.hp + count * 4000.0D) * Math.pow(1.30D, count);
        count = counts.count(ItemOriginalRing.Profile.PUYO_PLUS_2);
        stats.hp = (stats.hp + count * 4500.0D) * Math.pow(1.40D, count);
        count = counts.count(ItemOriginalRing.Profile.PUYO_PLUS_3);
        stats.hp = (stats.hp + count * 5000.0D) * Math.pow(1.50D, count);

        count = counts.count(ItemOriginalRing.Profile.HUNYA_PLUS_1);
        stats.maxMp = (stats.maxMp + count * 300.0D) * Math.pow(1.10D, count);
        count = counts.count(ItemOriginalRing.Profile.HUNYA_PLUS_2);
        stats.maxMp = (stats.maxMp + count * 400.0D) * Math.pow(1.25D, count);
        count = counts.count(ItemOriginalRing.Profile.HUNYA_PLUS_3);
        stats.maxMp = (stats.maxMp + count * 500.0D) * Math.pow(1.40D, count);

        stats.evasion += counts.count(ItemOriginalRing.Profile.VOID_PLUS_1) * 15.0D;
        stats.evasion += counts.count(ItemOriginalRing.Profile.VOID_PLUS_2) * 20.0D;
        stats.evasion += counts.count(ItemOriginalRing.Profile.VOID_PLUS_3) * 25.0D;
        stats.hpRegenRate += counts.count(ItemOriginalRing.Profile.EVIL_EYE_PLUS_1) * 0.08D;
        stats.hpRegenRate += counts.count(ItemOriginalRing.Profile.EVIL_EYE_PLUS_2) * 0.15D;
        stats.hpRegenRate += counts.count(ItemOriginalRing.Profile.EVIL_EYE_PLUS_3) * 0.20D;
        stats.mpRegenRate += counts.count(ItemOriginalRing.Profile.GODDESS_PLUS_1) * 0.10D;
        stats.mpRegenRate += counts.count(ItemOriginalRing.Profile.GODDESS_PLUS_2) * 0.15D;
        stats.mpRegenRate += counts.count(ItemOriginalRing.Profile.GODDESS_PLUS_3) * 0.20D;

        stats.physicalDamageRate *= Math.pow(0.75D, counts.count(ItemOriginalRing.Profile.IRON_PROTECTION_PLUS_1));
        stats.physicalDamageRate *= Math.pow(0.70D, counts.count(ItemOriginalRing.Profile.IRON_PROTECTION_PLUS_2));
        stats.physicalDamageRate *= Math.pow(0.65D, counts.count(ItemOriginalRing.Profile.IRON_PROTECTION_PLUS_3));
        count = counts.count(ItemOriginalRing.Profile.MAGIC_STONE_PLUS_1);
        stats.magicDefense += count * 50.0D;
        stats.magicDamageRate *= Math.pow(0.75D, count);
        count = counts.count(ItemOriginalRing.Profile.MAGIC_STONE_PLUS_2);
        stats.magicDefense += count * 50.0D;
        stats.magicDamageRate *= Math.pow(0.70D, count);
        count = counts.count(ItemOriginalRing.Profile.MAGIC_STONE_PLUS_3);
        stats.magicDefense += count * 50.0D;
        stats.magicDamageRate *= Math.pow(0.65D, count);

        stats.critRate += counts.count(ItemOriginalRing.Profile.WASP_PLUS_1) * 30.0D;
        stats.critRate += counts.count(ItemOriginalRing.Profile.WASP_PLUS_2) * 40.0D;
        stats.critRate += counts.count(ItemOriginalRing.Profile.WASP_PLUS_3) * 50.0D;
        stats.attack += counts.count(ItemOriginalRing.Profile.BLADES_PLUS_1) * 100.0D;
        stats.attack += counts.count(ItemOriginalRing.Profile.BLADES_PLUS_2) * 150.0D;
        stats.attack += counts.count(ItemOriginalRing.Profile.BLADES_PLUS_3) * 200.0D;
        stats.defense += counts.count(ItemOriginalRing.Profile.GUARD_PLUS_1) * 100.0D;
        stats.defense += counts.count(ItemOriginalRing.Profile.GUARD_PLUS_2) * 150.0D;
        stats.defense += counts.count(ItemOriginalRing.Profile.GUARD_PLUS_3) * 200.0D;
        stats.speed += counts.count(ItemOriginalRing.Profile.WIND_GOD_PLUS_1) * 100.0D;
        stats.speed += counts.count(ItemOriginalRing.Profile.WIND_GOD_PLUS_2) * 150.0D;
        stats.speed += counts.count(ItemOriginalRing.Profile.WIND_GOD_PLUS_3) * 200.0D;
        stats.magicAttack += counts.count(ItemOriginalRing.Profile.SPELL_PLUS_1) * 100.0D;
        stats.magicAttack += counts.count(ItemOriginalRing.Profile.SPELL_PLUS_2) * 150.0D;
        stats.magicAttack += counts.count(ItemOriginalRing.Profile.SPELL_PLUS_3) * 200.0D;

        applyAllParameterRing(stats, counts.count(ItemOriginalRing.Profile.LUNDINIAN_PLUS_1), 1.10D);
        stats.evasion -= counts.count(ItemOriginalRing.Profile.LUNDINIAN_PLUS_1) * 10.0D;
        applyAllParameterRing(stats, counts.count(ItemOriginalRing.Profile.LUNDINIAN_PLUS_2), 1.20D);
        stats.evasion -= counts.count(ItemOriginalRing.Profile.LUNDINIAN_PLUS_2) * 15.0D;
        applyAllParameterRing(stats, counts.count(ItemOriginalRing.Profile.LUNDINIAN_PLUS_3), 1.30D);
        stats.evasion -= counts.count(ItemOriginalRing.Profile.LUNDINIAN_PLUS_3) * 20.0D;

        count = counts.count(ItemOriginalRing.Profile.ALMIGHTY);
        stats.hp += count * 5000.0D;
        stats.maxMp += count * 100.0D;
        stats.attack += count * 500.0D;
        stats.defense += count * 500.0D;
        stats.magicAttack += count * 500.0D;
        stats.magicDefense += count * 500.0D;
        stats.speed += count * 500.0D;
        stats.luck += count * 500.0D;

        applySinUpgrade(stats, counts.count(ItemOriginalRing.Profile.SIN_PLUS_1), 0.65D, 55.0D);
        applySinUpgrade(stats, counts.count(ItemOriginalRing.Profile.SIN_PLUS_2), 0.60D, 60.0D);
        applySinUpgrade(stats, counts.count(ItemOriginalRing.Profile.SIN_PLUS_3), 0.55D, 70.0D);

        if (counts.count(ItemOriginalRing.Profile.TIGER_FOX) > 0) {
            stats.targetingRate = 0.0D;
        }
        count = counts.count(ItemOriginalRing.Profile.OLD_KING);
        if (count > 0) {
            stats.attack = 0.0D;
            stats.magicAttack *= Math.pow(2.0D, count);
        }
        count = counts.count(ItemOriginalRing.Profile.POLAR_BEAR);
        if (count > 0) {
            stats.hp *= Math.pow(0.10D, count);
            stats.critRate += count * 100.0D;
        }
        count = counts.count(ItemOriginalRing.Profile.COUNTERATTACK);
        stats.attack += count * 500.0D;
        count = counts.count(ItemOriginalRing.Profile.MOLASSES);
        stats.attack += count * 500.0D;
    }

    private static void applyAllParameterRing(BSPlayerStats stats, int count, double multiplier) {
        if (count <= 0) return;
        double combined = Math.pow(multiplier, count);
        stats.hp *= combined;
        stats.maxMp *= combined;
        stats.attack *= combined;
        stats.defense *= combined;
        stats.magicAttack *= combined;
        stats.magicDefense *= combined;
        stats.speed *= combined;
        stats.luck *= combined;
    }

    private static void applySinUpgrade(BSPlayerStats stats, int count, double multiplier, double probabilityBonus) {
        if (count <= 0) return;
        applyAllParameterRing(stats, count, multiplier);
        stats.critRate += count * probabilityBonus;
        stats.evasion += count * probabilityBonus;
    }

    private static void resetDerivedStats(BSPlayerStats stats) {
        stats.evasion = 0.0;
        stats.magicEvasion = 0.0;
        stats.critRate = 5.0;
        stats.burnRate = 0.0;
        stats.hpRegenRate = 0.0;
        stats.instantDeathRate = 0.0;
        stats.stunRate = 0.0;
        stats.mpRegenRate = 0.0;
        stats.mpCostRate = 1.0;
        stats.extraActionRate = 0.0;
        stats.physicalDamageRate = 1.0;
        stats.magicDamageRate = 1.0;
        stats.poisonResistRate = 0.0;
        stats.severePoisonResistRate = 0.0;
        stats.bleedResistRate = 0.0;
        stats.sleepResistRate = 0.0;
        stats.fearRate = 0.0;
        stats.fearResistRate = 0.0;
        stats.targetingRate = 1.0;
    }

    private static void applyCovenantBonuses(BSPlayerStats stats) {
        if ("noden".equals(stats.activeCovenant)) {
            stats.hp *= 1.50;
        } else if ("tweedle".equals(stats.activeCovenant)) {
            stats.evasion += 20.0;
        }
    }

    private static void clampCalculatedStats(Player player, BSPlayerStats stats) {
        stats.evasion = Math.max(0.0, Math.min(100.0, stats.evasion));
        stats.magicEvasion = Math.max(0.0, Math.min(100.0, stats.magicEvasion));
        if (!stats.developerLimitBreak) {
            stats.hp = Math.min(CAP_HP, stats.hp);
            stats.maxMp = Math.min(CAP_MP, stats.maxMp);
            stats.attack = Math.min(CAP_ATK, stats.attack);
            stats.defense = Math.min(CAP_DEF, stats.defense);
            stats.magicAttack = Math.min(CAP_MATK, stats.magicAttack);
            stats.magicDefense = Math.min(CAP_MDEF, stats.magicDefense);
            stats.luck = Math.min(CAP_LUCK, stats.luck);
            stats.speed = Math.min(CAP_SPEED, stats.speed);
        }
        if (stats.mp > stats.maxMp) {
            stats.mp = stats.maxMp;
        }
        stats.clampActionPoints(SkillUtils.getMaxActionPoints(player));
    }

    private static void syncVanillaAttributes(Player player, BSPlayerStats stats) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(stats.hp);
        }

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(stats.defense);
        }

        AttributeInstance atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null && atkAttr.getBaseValue() != stats.attack) {
            atkAttr.setBaseValue(stats.attack);
        }

        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null && speedAttr.getBaseValue() != 4.0D) {
            speedAttr.setBaseValue(4.0D);
        }

        float actualMaxHp = player.getMaxHealth();
        if (player.getHealth() > actualMaxHp) {
            player.setHealth(actualMaxHp);
        }
        if (!player.level().isClientSide() && player.tickCount < 100 && player.getHealth() < actualMaxHp) {
            player.setHealth(actualMaxHp);
        }
    }

    private static void multiplyMagicAttack(BSPlayerStats stats, double multiplier) {
        try {
            stats.magicAttack *= multiplier;
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getMsgId().equals("bs2_extra_hit")) return;

        LivingEntity victim = event.getEntity();
        float damage = event.getAmount();

        if (event.getSource().getEntity() instanceof Player player) {
            CompoundTag attackerData = player.getPersistentData();
            if (attackerData.getBoolean(TAG_DAGGER_EXTRA_HIT) || attackerData.getBoolean(TAG_RING_EXTRA_HIT)) {
                if (!player.level().isClientSide() && damage > 0.1F) {
                    showDamageFeedback(player, victim, damage);
                }
                return;
            }

            if (!player.level().isClientSide() && damage > 0.1F) {
                showDamageFeedback(player, victim, damage);
            }

            Item heldItem = player.getMainHandItem().getItem();
            if (!player.level().isClientSide() && damage > 0.0F
                    && (heldItem == BlackSouls.MAGIC_BLADE.get() || heldItem == BlackSouls.DEMON_GOD_BLADE.get())) {
                player.heal(damage);
            }

            if (player instanceof ServerPlayer serverPlayer
                    && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                    && !attackerData.getBoolean(TAG_PRECOMPUTED_SKILL_DAMAGE)) {
                int pumpkinRingCount = getBaubleCount(player, BlackSouls.RING_PUMPKIN_KNIGHT.get());
                if (pumpkinRingCount > 0) {
                    schedulePumpkinRingExtraAttacks(serverPlayer, victim, pumpkinRingCount);
                }
            }

            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            double skillInstantDeathRate = attackerData.getDouble(TAG_SKILL_INSTANT_DEATH_RATE);
            boolean skillInstantDeathRolled = skillInstantDeathRate > 0.0D
                    && Math.random() * 100.0D < skillInstantDeathRate;
            if (skillInstantDeathRolled) {
                tryTriggerInstantDeath(player, victim, event, skillInstantDeathRate);
            } else if (stats != null && stats.instantDeathRate > 0 && Math.random() * 100.0 < stats.instantDeathRate) {
                tryTriggerInstantDeath(player, victim, event, stats.instantDeathRate);
            }
            if (!player.level().isClientSide() && getBaubleCount(player, BlackSouls.SNAKE_DRESS.get()) > 0 && BlackSouls.BUFF_SEVERE_POISON.isPresent()) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 1000, 0));
            }
        }
    }

    private static void schedulePumpkinRingExtraAttacks(ServerPlayer player, LivingEntity target, int count) {
        net.minecraft.server.MinecraftServer server = player.serverLevel().getServer();
        for (int hit = 0; hit < count; hit++) {
            int executeTick = server.getTickCount() + 2 + hit * 2;
            server.tell(new net.minecraft.server.TickTask(executeTick, () -> {
                if (!player.isAlive() || !target.isAlive() || target.isRemoved()) {
                    return;
                }
                CompoundTag data = player.getPersistentData();
                data.putBoolean(TAG_RING_EXTRA_HIT, true);
                target.invulnerableTime = 0;
                try {
                    player.attack(target);
                } finally {
                    data.remove(TAG_RING_EXTRA_HIT);
                }
            }));
        }
    }

    private static void activateBattleStartRings(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int beeCount = getBaubleCount(player, BlackSouls.RING_BEE.get());
        int frenziedKingCount = getBaubleCount(player, BlackSouls.RING_FRENZIED_KING.get());
        int myStruggleCount = getBaubleCount(player, BlackSouls.RING_MY_STRUGGLE.get());
        int defenseKingCount = getOriginalRingCount(player, ItemOriginalRing.Profile.DEFENSE_KING);
        int playwrightCount = getBaubleCount(player, BlackSouls.PLAYWRIGHT_HEADSCARF.get());
        if (beeCount + frenziedKingCount + myStruggleCount + defenseKingCount + playwrightCount <= 0) {
            return;
        }

        CompoundTag data = SkillUtils.getPersistedData(player);
        long gameTime = player.level().getGameTime();
        boolean enteringCombat = gameTime >= data.getLong(TAG_RING_COMBAT_UNTIL);
        data.putLong(TAG_RING_COMBAT_UNTIL, gameTime + 200L);
        if (!enteringCombat) {
            return;
        }

        if (beeCount > 0 && BlackSouls.BUFF_DAGGER_EVASION.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DAGGER_EVASION.get(), 400, 0));
        }
        if (frenziedKingCount > 0 && BlackSouls.BUFF_BERSERK.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_BERSERK.get(), 2000, 0));
        }
        if (myStruggleCount > 0) {
            applyAttackUp(player, 2000);
            applyDefenseUp(player, 2000);
            applyMagicAttackUp(player, 2000);
            applyMagicDefenseUp(player, 2000);
            applySpeedUp(player, 2000);
            applyLuckUp(player, 2000);
        }
        if (defenseKingCount > 0 && BlackSouls.BUFF_DEFENSE_KING.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSE_KING.get(), 400, 0));
        }
        if (playwrightCount > 0 && BlackSouls.BUFF_PLAYWRIGHT.isPresent()) {
            player.addEffect(new MobEffectInstance(BlackSouls.BUFF_PLAYWRIGHT.get(), 200, 0));
        }
        applyStats(player);
        syncToClient(serverPlayer);
    }

    private static void showDamageFeedback(Player player, LivingEntity victim, float damage) {
        long displayedDamage = Math.max(0L, Math.round((double) damage));
        if (BSConfig.SHOW_COMBAT_DAMAGE_CHAT.get()) {
            String damageStr = Long.toString(displayedDamage);
            Component msg = Component.translatable("message.blacksouls.combat.damage", player.getName().getString(), damageStr)
                    .withStyle(ChatFormatting.WHITE);
            player.sendSystemMessage(msg);
        }

        boolean isCrit = player.getPersistentData().getBoolean("bs2_is_crit");
        if (isCrit) {
            player.getPersistentData().remove("bs2_is_crit");
        }

        PacketDistributor.TargetPoint p = new PacketDistributor.TargetPoint(victim.getX(), victim.getY(), victim.getZ(), 64, player.level().dimension());
        try {
            NetworkHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> p),
                    new PacketSpawnDamageText(victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(), displayedDamage, isCrit));
        } catch (Exception ignored) {
        }
    }

    private static void tryTriggerInstantDeath(Player player, LivingEntity victim, LivingDamageEvent event, double instantDeathRate) {
        if (isVictimImmuneToInstantDeath(victim)) {
            if (!player.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.blacksouls.skill.instant_death_immune").withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        event.setAmount(Math.max(event.getAmount(), victim.getHealth() + 1.0F));
        if (player.level().isClientSide()) {
            return;
        }

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.instant_death", (int) instantDeathRate).withStyle(ChatFormatting.DARK_RED));
        PacketDistributor.TargetPoint p = new PacketDistributor.TargetPoint(victim.getX(), victim.getY(), victim.getZ(), 64, player.level().dimension());
        try {
            NetworkHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> p),
                    new PacketPlayAnim(56, victim.getX(), victim.getY() + victim.getBbHeight() / 2.0F, victim.getZ()));
        } catch (Exception ignored) {
        }

        double vX = victim.getX();
        double vY = victim.getY() + victim.getBbHeight() / 2.0;
        double vZ = victim.getZ();
        ServerLevel serverLevel = (ServerLevel) player.level();

        net.minecraft.server.MinecraftServer server = serverLevel.getServer();
        server.tell(new net.minecraft.server.TickTask(0, () -> serverLevel.playSound(null, vX, vY, vZ, BlackSouls.DARKNESS5_EVENT.get(), SoundSource.PLAYERS, 1.0f, 1.0f)));
        server.tell(new net.minecraft.server.TickTask(Math.max(1, (int) Math.round(396 / 50.0)), () -> serverLevel.playSound(null, vX, vY, vZ, BlackSouls.THUNDER1_EVENT.get(), SoundSource.PLAYERS, 1.0f, 1.0f)));
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (BlackSouls.BUFF_WEAKNESS.isPresent() && event.getEntity().hasEffect(BlackSouls.BUFF_WEAKNESS.get())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Player player) {
            if (getBaubleCount(player, BlackSouls.RING_GOD_FISH.get()) > 0 || getBaubleCount(player, BlackSouls.OMINOUS_CLOTHES.get()) > 0) {
                event.setCanceled(true);
                return;
            }
            if (getBaubleCount(player, BlackSouls.MYSTERY_OF_NIGHT_SKY.get()) > 0) {
                event.setAmount(event.getAmount() * 1.30F);
            }
            int miracleCount = getBaubleCount(player, BlackSouls.RING_MIRACLE.get());
            if (miracleCount > 0) {
                event.setAmount((float) (event.getAmount() * Math.pow(2.0D, miracleCount)));
            }
            int cutDownCount = getOriginalRingCount(player, ItemOriginalRing.Profile.CUT_DOWN);
            if (cutDownCount > 0) {
                event.setAmount((float) (event.getAmount() * Math.pow(0.8D, cutDownCount)));
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (event.getEffectInstance() == null) {
            return;
        }

        if (BlackSouls.BUFF_JUGGLING_EVASION.isPresent()
                && event.getEntity().hasEffect(BlackSouls.BUFF_JUGGLING_EVASION.get())
                && event.getEffectInstance().getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (event.getEntity() instanceof Player player) {
            net.minecraft.world.effect.MobEffect incomingEffect = event.getEffectInstance().getEffect();
            if (getBaubleCount(player, BlackSouls.RING_BARBER.get()) > 0 && isParameterDebuff(incomingEffect)) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (getBaubleCount(player, BlackSouls.RING_APPLE.get()) > 0
                    && BlackSouls.BUFF_SLEEP.isPresent()
                    && incomingEffect == BlackSouls.BUFF_SLEEP.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (getBaubleCount(player, BlackSouls.RING_BUTCHER.get()) > 0
                    && BlackSouls.BUFF_MADNESS.isPresent()
                    && incomingEffect == BlackSouls.BUFF_MADNESS.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (getBaubleCount(player, BlackSouls.RING_PROSTITUTE.get()) > 0
                    && BlackSouls.BUFF_WEAKNESS.isPresent()
                    && incomingEffect == BlackSouls.BUFF_WEAKNESS.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (getBaubleCount(player, BlackSouls.WORK_CLOTHES.get()) > 0
                    && BlackSouls.BUFF_OILY.isPresent()
                    && incomingEffect == BlackSouls.BUFF_OILY.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            int fighterCount = getBaubleCount(player, BlackSouls.RING_FIGHTER.get());
            int clubKnightCount = getBaubleCount(player, BlackSouls.RING_CLUB_KNIGHT.get());
            double stunResistance = 1.0D - Math.pow(0.50D, fighterCount + clubKnightCount);
            if (stunResistance > 0.0D
                    && BlackSouls.BUFF_STUN.isPresent()
                    && incomingEffect == BlackSouls.BUFF_STUN.get()
                    && Math.random() < stunResistance) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (player.getMainHandItem().getItem() == BlackSouls.MARY_SUES_BRANCH_STAFF.get()
                    && event.getEffectInstance().getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                event.setResult(Event.Result.DENY);
                return;
            }
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                if (BlackSouls.BUFF_POISON.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_POISON.get() && stats.poisonResistRate >= 1.0) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (BlackSouls.BUFF_SEVERE_POISON.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_SEVERE_POISON.get() && stats.severePoisonResistRate >= 1.0) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (BlackSouls.BUFF_BLEEDING.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_BLEEDING.get() && stats.bleedResistRate >= 1.0) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (BlackSouls.BUFF_SLEEP.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_SLEEP.get() && stats.sleepResistRate >= 1.0) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (BlackSouls.BUFF_FEAR.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_FEAR.get() && stats.fearResistRate >= 1.0) {
                    event.setResult(Event.Result.DENY); return;
                }
            }

            if (getOriginalRingCount(player, ItemOriginalRing.Profile.UNICORN) > 0
                    && BlackSouls.BUFF_FROSTBITE.isPresent()
                    && incomingEffect == BlackSouls.BUFF_FROSTBITE.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (getOriginalRingCount(player, ItemOriginalRing.Profile.LION) > 0
                    && BlackSouls.BUFF_LACERATION.isPresent()
                    && incomingEffect == BlackSouls.BUFF_LACERATION.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            if (getOriginalRingCount(player, ItemOriginalRing.Profile.BREAK_RESISTANCE) > 0
                    && BlackSouls.BUFF_DEFENSELESS.isPresent()
                    && incomingEffect == BlackSouls.BUFF_DEFENSELESS.get()) {
                event.setResult(Event.Result.DENY);
                return;
            }
            int holyForestCount = getOriginalRingCount(player, ItemOriginalRing.Profile.HOLY_FOREST);
            int abyssArmorCount = getBaubleCount(player, BlackSouls.ABYSS_ARMOR.get());
            int abyssHelmetCount = getBaubleCount(player, BlackSouls.ABYSS_HELMET.get());
            double ailmentPassRate = Math.pow(0.70D, holyForestCount + abyssArmorCount)
                    * Math.pow(0.80D, abyssHelmetCount);
            if (incomingEffect.getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL
                    && Math.random() >= ailmentPassRate) {
                event.setResult(Event.Result.DENY);
                return;
            }

            if (BlackSouls.BUFF_DEFENSELESS.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_DEFENSELESS.get()) {
                if (BlackSouls.BUFF_KNIGHTS_GLORY.isPresent() && player.hasEffect(BlackSouls.BUFF_KNIGHTS_GLORY.get())) {
                    event.setResult(Event.Result.DENY); return;
                }
            }
            if (BlackSouls.BUFF_STUN.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_STUN.get()) {
                if (getBaubleCount(player, BlackSouls.KNIGHT_ARMOR.get()) > 0 && Math.random() < 0.30) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (getBaubleCount(player, BlackSouls.KNIGHT_HELMET.get()) > 0 && Math.random() < 0.20) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (getBaubleCount(player, BlackSouls.RING_KNIGHT.get()) > 0 && Math.random() < 0.50) {
                    event.setResult(Event.Result.DENY); return;
                }
            }
            if (BlackSouls.BUFF_MADNESS.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_MADNESS.get()) {
                int resistanceSources = getBaubleCount(player, BlackSouls.RABBIT_EARS.get())
                        + getBaubleCount(player, BlackSouls.BUNNY_GIRL_UNIFORM.get())
                        + getBaubleCount(player, BlackSouls.PROSTITUTE_DRESS.get());
                int yellowClothCount = getBaubleCount(player, BlackSouls.YELLOW_CLOTH.get());
                double madnessPassRate = Math.min(1.0D,
                        Math.pow(0.50D, resistanceSources) * Math.pow(2.0D, yellowClothCount));
                if (Math.random() >= madnessPassRate) {
                    event.setResult(Event.Result.DENY); return;
                }
            }
            if (BlackSouls.BUFF_BURN.isPresent() && event.getEffectInstance().getEffect() == BlackSouls.BUFF_BURN.get()) {
                if (getBaubleCount(player, BlackSouls.MATCH_GIRL_CLOTHES.get()) > 0 && Math.random() < 0.80) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (getBaubleCount(player, BlackSouls.MILTON_ARMOR.get()) > 0 && Math.random() < 0.25) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (getBaubleCount(player, BlackSouls.MILTON_HELMET.get()) > 0 && Math.random() < 0.25) {
                    event.setResult(Event.Result.DENY); return;
                }
            }
        }
    }

    private static boolean isParameterDebuff(net.minecraft.world.effect.MobEffect effect) {
        return effect == BlackSouls.BUFF_ATK_DOWN.get()
                || effect == BlackSouls.BUFF_ATK_DOWN_2.get()
                || effect == BlackSouls.BUFF_DEF_DOWN.get()
                || effect == BlackSouls.BUFF_DEF_DOWN_2.get()
                || effect == BlackSouls.BUFF_MAGIC_ATK_DOWN.get()
                || effect == BlackSouls.BUFF_MAGIC_ATK_DOWN_2.get()
                || effect == BlackSouls.BUFF_MAGIC_DEF_DOWN.get()
                || effect == BlackSouls.BUFF_MAGIC_DEF_DOWN_2.get()
                || effect == BlackSouls.BUFF_LUCK_DOWN.get()
                || effect == BlackSouls.BUFF_LUCK_DOWN_2.get()
                || effect == BlackSouls.BUFF_SPEED_DOWN.get()
                || effect == BlackSouls.BUFF_SPEED_DOWN_2.get();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            safeSyncPlayerAndWorld(player);
            if (com.BlackSouls.BlackSoulsMod.BlackSouls.BUFF_HOLLOWED.isPresent()) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.BlackSouls.BlackSoulsMod.BlackSouls.BUFF_HOLLOWED.get(),
                        -1, 0, false, false, true
                ));
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.blacksouls.status.hollowed").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            net.minecraft.world.item.ItemStack darkSoulStack = new net.minecraft.world.item.ItemStack(
                    com.BlackSouls.BlackSoulsMod.BlackSouls.SOUL_BLACK.get(), 1
            );
            if (!player.getInventory().add(darkSoulStack)) {
                player.drop(darkSoulStack, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player instanceof ServerPlayer serverPlayer) {
            com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalBow.tickHansMachineGunBursts(serverPlayer);
            if (player.getMainHandItem().getItem() == BlackSouls.HOLY_GUNBLADE.get()) {
                com.BlackSouls.BlackSoulsMod.util.skill.SkillHolyGunbladeArt.ensureAmmoState(serverPlayer);
            } else {
                player.removeEffect(BlackSouls.BUFF_GUNBLADE_AMMO_I.get());
                player.removeEffect(BlackSouls.BUFF_GUNBLADE_AMMO_II.get());
                player.removeEffect(BlackSouls.BUFF_GUNBLADE_AMMO_III.get());
            }
            if (player.getMainHandItem().getItem() != BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get()) {
                player.removeEffect(BlackSouls.BUFF_DUAL_SWORD_AURA.get());
            }
            if (player.getMainHandItem().getItem() == BlackSouls.MARY_SUES_BRANCH_STAFF.get()) {
                for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                    if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                        player.removeEffect(effect.getEffect());
                    }
                }
            }
        }
        BaubleCounter counts = new BaubleCounter(player);
        applyPassiveSnakeDressPoison(player, counts);
        updateChronoClockState(player, counts.hasChronoClock());
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            applyStats(player, counts, stats);
        }

        if (stats != null && !player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            tickServerPlayerResources(serverPlayer, player, stats, counts);
        }
    }

    private static void applyPassiveSnakeDressPoison(Player player, BaubleCounter counts) {
        if (!player.level().isClientSide()
                && counts.has(BlackSouls.SNAKE_DRESS.get())
                && BlackSouls.BUFF_SEVERE_POISON.isPresent()
                && !player.hasEffect(BlackSouls.BUFF_SEVERE_POISON.get())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 200, 0, false, true, true));
        }
    }

    private static void updateChronoClockState(Player player, boolean chronoClockEquipped) {
        if (player.level().isClientSide()) {
            return;
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            boolean shouldHaveWatchSlot = SkillUtils.hasChronoClockAvailable(player, chronoClockEquipped);
            boolean hasWatchSlotModifier = false;
            for (Map.Entry<String, AttributeModifier> entry : handler.getModifiers().entries()) {
                if (entry.getKey().equals("watch") && CHRONO_WATCH_SLOT_UUID.equals(entry.getValue().getId())) {
                    hasWatchSlotModifier = true;
                    break;
                }
            }

            if (shouldHaveWatchSlot && !hasWatchSlotModifier) {
                HashMultimap<String, AttributeModifier> modifiers = createChronoWatchSlotModifier();
                handler.addTransientSlotModifiers(modifiers);
            } else if (!shouldHaveWatchSlot && hasWatchSlotModifier) {
                HashMultimap<String, AttributeModifier> modifiers = createChronoWatchSlotModifier();
                handler.removeSlotModifiers(modifiers);
            }
        });
    }

    private static HashMultimap<String, AttributeModifier> createChronoWatchSlotModifier() {
        HashMultimap<String, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put("watch", new AttributeModifier(CHRONO_WATCH_SLOT_UUID, "chrono_clock_watch_slot", 1.0D, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    private static void tickServerPlayerResources(ServerPlayer serverPlayer, Player player, BSPlayerStats stats, BaubleCounter counts) {
        tickPuppetRing(serverPlayer, stats, counts);
        boolean syncNeeded = clearChronoClockBindingsIfNeeded(stats, counts.hasChronoClock());
        double previousActionPoints = stats.getCurrentActionPoints();
        double maxActionPoints = SkillUtils.getActionCount(
                serverPlayer,
                stats,
                counts.count(BlackSouls.RING_WHITE_RABBIT.get()),
                counts.count(BlackSouls.MYSTERY_OF_NIGHT_SKY.get()),
                counts.count(BlackSouls.RING_BLACK_RABBIT.get())
        );
        if (previousActionPoints < maxActionPoints) {
            stats.restoreActionPoints(maxActionPoints / SkillUtils.ACTION_TURN_TICKS, maxActionPoints);
            stats.clampActionPoints(maxActionPoints);
            if (serverPlayer.tickCount % 5 == 0
                    && Math.abs(stats.getCurrentActionPoints() - previousActionPoints) > 1.0E-4) {
                syncNeeded = true;
            }
        }

        if (serverPlayer.tickCount % 20 == 0
                && !TurnBattleManager.isInBattle(serverPlayer)) {
            if (stats.mp < stats.maxMp) {
                double regenAmount = 1.0 + (stats.maxMp * stats.mpRegenRate);
                stats.mp += regenAmount;
                if (stats.mp >= stats.maxMp || (stats.maxMp - stats.mp) < 0.5) {
                    stats.mp = stats.maxMp;
                }
            }
            syncNeeded = true;
        }
        if (syncNeeded) {
            syncToClient(serverPlayer, stats);
        }
        if (serverPlayer.tickCount % 200 == 0 && stats.hpRegenRate > 0.0 && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
            serverPlayer.heal((float) (serverPlayer.getMaxHealth() * stats.hpRegenRate));
        }
        if (serverPlayer.tickCount % 200 == 0 && stats.hpRegenRate < 0.0 && serverPlayer.getHealth() > 1.0F) {
            float regenLoss = (float) (serverPlayer.getMaxHealth() * Math.abs(stats.hpRegenRate));
            serverPlayer.setHealth(Math.max(1.0F, serverPlayer.getHealth() - regenLoss));
        }
        if (stats.lostSouls > 0 && player.isAlive()) {
            tryRecoverLostSouls(serverPlayer, stats);
        }
    }

    private static void tickPuppetRing(ServerPlayer player, BSPlayerStats stats, BaubleCounter counts) {
        if (player.tickCount % 5 != 0
                || counts.count(ItemOriginalRing.Profile.PUPPET) <= 0
                || !player.isAlive()
                || player.isSpectator()
                || player.isCrouching()
                || player.containerMenu != player.inventoryMenu) {
            return;
        }

        LivingEntity target = player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(12.0D),
                        entity -> entity != player
                                && entity.isAlive()
                                && !entity.isSpectator()
                                && (entity instanceof net.minecraft.world.entity.monster.Enemy
                                || com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.hasManagedStats(entity))
                                && player.hasLineOfSight(entity))
                .stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return;
        }

        if (com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry.tryCastPuppetSkill(player, stats, target)) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        if (weapon.getItem() instanceof com.BlackSouls.BlackSoulsMod.item.weapon.ItemOriginalBow) {
            tryPuppetRangedAttack(player, target, weapon);
        } else if (player.distanceToSqr(target) <= 20.25D && player.getAttackStrengthScale(0.5F) >= 0.90F) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            player.attack(target);
        }
    }

    private static void tryPuppetRangedAttack(ServerPlayer player, LivingEntity target, ItemStack weapon) {
        CompoundTag data = player.getPersistentData();
        long gameTime = player.level().getGameTime();
        if (gameTime < data.getLong(TAG_PUPPET_RANGED_COOLDOWN)) {
            return;
        }

        ItemStack ammo = player.getProjectile(weapon);
        boolean virtualArrow = ammo.isEmpty() && player.getAbilities().instabuild;
        if (virtualArrow) {
            ammo = new ItemStack(net.minecraft.world.item.Items.ARROW);
        }
        if (!(ammo.getItem() instanceof net.minecraft.world.item.ArrowItem arrowItem)) {
            return;
        }

        net.minecraft.world.entity.projectile.AbstractArrow arrow =
                arrowItem.createArrow(player.level(), ammo, player);
        arrow.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY(0.5D) - arrow.getY() + horizontalDistance * 0.05D;
        arrow.shoot(dx, dy, dz, 2.5F, 1.0F);
        if (virtualArrow) {
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        } else if (!player.getAbilities().instabuild) {
            ammo.shrink(1);
        }
        player.level().addFreshEntity(arrow);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        data.putLong(TAG_PUPPET_RANGED_COOLDOWN, gameTime + 20L);
    }

    private static boolean clearChronoClockBindingsIfNeeded(BSPlayerStats stats, boolean chronoClockEquipped) {
        if (chronoClockEquipped) {
            return false;
        }
        boolean changed = false;
        if ("bs2_skill_chrono_clock".equals(stats.skillZ)) {
            stats.skillZ = "";
            changed = true;
        }
        if ("bs2_skill_chrono_clock".equals(stats.skillX)) {
            stats.skillX = "";
            changed = true;
        }
        if ("bs2_skill_chrono_clock".equals(stats.skillC)) {
            stats.skillC = "";
            changed = true;
        }
        if ("bs2_skill_chrono_clock".equals(stats.skillV)) {
            stats.skillV = "";
            changed = true;
        }
        return changed;
    }

    private static void tryRecoverLostSouls(ServerPlayer serverPlayer, BSPlayerStats stats) {
        if (!serverPlayer.level().dimension().location().toString().equals(stats.lostDim)) {
            return;
        }

        double distSq = serverPlayer.distanceToSqr(stats.lostX, stats.lostY, stats.lostZ);
        if (distSq > 4.0) {
            return;
        }

        long recoveredAmount = stats.lostSouls;
        stats.souls += recoveredAmount;
        clearLostSoulRecord(stats);
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

        NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                new com.BlackSouls.BlackSoulsMod.network.packets.ClientboundSimpleActionPacket(
                        com.BlackSouls.BlackSoulsMod.network.packets.ClientboundSimpleActionPacket.Action.SHOW_RETRIEVAL_BANNER
                )
        );
        serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.blacksouls.death.recovered", recoveredAmount).withStyle(net.minecraft.ChatFormatting.GOLD), true
        );
        syncToClient(serverPlayer, stats);
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {

            if (com.BlackSouls.BlackSoulsMod.BlackSouls.PLAYER_DEATH_EVENT.isPresent()) {
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(), com.BlackSouls.BlackSoulsMod.BlackSouls.PLAYER_DEATH_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats stats = serverPlayer.getCapability(com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats.CAPABILITY).resolve().orElse(null);
            if (stats != null) {
                clearLostSoulRecord(stats);
                stats.lostSouls = stats.souls;
                stats.lostX = serverPlayer.getX();
                stats.lostY = serverPlayer.getY();
                stats.lostZ = serverPlayer.getZ();
                stats.lostDim = serverPlayer.level().dimension().location().toString();

                stats.souls = 0;
                syncToClient(serverPlayer, stats);
            }
        }
    }

    @SubscribeEvent
    public static void onFriendlyMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        int senLoss = 0;

        if (victim instanceof AbstractVillager) {
            senLoss = 5;
        } else if (victim instanceof Animal || victim instanceof WaterAnimal || victim instanceof AmbientCreature || victim instanceof TamableAnimal) {
            senLoss = 1;
        }

        if (senLoss <= 0) {
            return;
        }

        final int finalSenLoss = senLoss;
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            stats.sen = Math.max(0, stats.sen - finalSenLoss);
            syncToClient(player);
            NetworkHandler.sendToPlayer(new ClientboundBannerPacket(ClientboundBannerPacket.Type.SEEK_SERVICE, -finalSenLoss), player);
        });
    }

    @SubscribeEvent
    public static void onPurgeMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) {
            return;
        }

        if (ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()) == null
                || !"minecraft".equals(ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()).getNamespace())) {
            return;
        }

        String targetId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()).getPath();
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> stats.purgeMobKills++);
        progressPurgeTasks(player, "mob", targetId);
        syncToClient(player);
    }

    private static void clearLostSoulRecord(BSPlayerStats stats) {
        stats.lostSouls = 0;
        stats.lostX = 0.0;
        stats.lostY = 0.0;
        stats.lostZ = 0.0;
        stats.lostDim = "";
    }

    @SubscribeEvent
    public static void onBonfireBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        net.minecraft.world.level.LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos pos = event.getPos();

            if (serverLevel.getBlockState(pos).is(net.minecraft.tags.BlockTags.CAMPFIRES)) {
                com.BlackSouls.BlackSoulsMod.capability.BSWorldData data = com.BlackSouls.BlackSoulsMod.capability.BSWorldData.get(serverLevel.getServer().overworld());

                if (data.removeBonfire(serverLevel, pos)) {
                    event.getPlayer().sendSystemMessage(net.minecraft.network.chat.Component.literal("Bonfire destroyed.").withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPurgeOreBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()) == null
                || !"minecraft".equals(ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).getNamespace())) {
            return;
        }

        if (!event.getState().is(net.minecraftforge.common.Tags.Blocks.ORES)) {
            return;
        }

        String oreTarget = resolveOreTarget(event.getState());
        if (oreTarget.isEmpty()) {
            return;
        }

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> stats.purgeOreBreaks++);
        progressPurgeTasks(player, "ore", oreTarget);
        syncToClient(player);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            LivingEntity victim = event.getEntity();
            if (shouldDropTinySoul(victim) && victim.level().random.nextDouble() < getTinySoulDropChance(victim)) {
                event.getDrops().add(new ItemEntity(
                        victim.level(),
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        new ItemStack(BlackSouls.SOUL_FADING.get())
                ));
            }
            int serpentCount = getBaubleCount(player, BlackSouls.RING_GOLD_SERPENT.get());
            if (serpentCount > 0) {
                int multiplier = 1 + serpentCount;
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack stack = drop.getItem();
                    if (!stack.isEmpty()) {
                        stack.setCount(stack.getCount() * multiplier);
                    }
                }
            }
        }
    }

    private static boolean shouldDropTinySoul(LivingEntity entity) {
        if (entity instanceof Player || entity instanceof net.minecraft.world.entity.monster.Enemy) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        if (entity instanceof Animal animal && animal.isBaby()) {
            return false;
        }
        return entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature;
    }

    private static double getTinySoulDropChance(LivingEntity entity) {
        if (entity instanceof AmbientCreature) {
            return 0.005D;
        }
        if (entity instanceof WaterAnimal) {
            return 0.015D;
        }
        return 0.02D;
    }

    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                if ("noden".equals(stats.activeCovenant)) {
                    event.setAmount(event.getAmount() * 2.0F);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            net.minecraft.world.entity.LivingEntity target = event.getEntity();
            if (BlackSouls.BUFF_OILY.isPresent()
                    && target.hasEffect(BlackSouls.BUFF_OILY.get())
                    && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                event.setAmount(event.getAmount() * 1.5F);
            }
            if (target.hasEffect(BlackSouls.BUFF_SLEEP.get())) {

                float originalDamage = event.getAmount();
                event.setAmount(originalDamage * 9.0F);
                target.removeEffect(BlackSouls.BUFF_SLEEP.get());
            }
        }
    }

}
