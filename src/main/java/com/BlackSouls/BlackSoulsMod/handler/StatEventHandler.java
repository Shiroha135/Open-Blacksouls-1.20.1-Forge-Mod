package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.api.event.BSStatsRecalcEvent;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.google.common.collect.HashMultimap;
import com.BlackSouls.BlackSoulsMod.entity.EntityThrownBlade;
import com.BlackSouls.BlackSoulsMod.entity.InstantDeathImmuneEntity;
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
        private final LivingEntity entity;
        private final Map<Item, Integer> cache = new IdentityHashMap<>();

        private BaubleCounter(LivingEntity entity) {
            this.entity = entity;
        }

        private int count(Item item) {
            return cache.computeIfAbsent(item, ignored -> getBaubleCount(entity, item));
        }

        private boolean has(Item item) {
            return count(item) > 0;
        }
    }

    public static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PacketSyncStats(stats.serializeNBT())));
        }
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
            return com.BlackSouls.BlackSoulsMod.util.DifficultyManager.scaleManagedStat(
                    target.level(),
                    com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.getStats(target).magicDefense * getMagicDefenseShiftMultiplier(target)
            );
        }
        return 0.0D;
    }

    private static boolean isVictimImmuneToInstantDeath(LivingEntity victim) {
        return getBaubleCount(victim, BlackSouls.RING_RESURRECTOR.get()) > 0
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            DamageSource source = event.getSource();

            if (EntityHellPrince.isOpeningComboDamage(source)) {
                return;
            }

            if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL) || "bs2_sure_hit".equals(source.getMsgId())) {
                return;
            }

            if (source.getDirectEntity() instanceof EntityThrownBlade thrownBlade && thrownBlade.isSureHit()) {
                return;
            }

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                if (stats.evasion > 0 && Math.random() * 100 < stats.evasion) {
                    event.setCanceled(true);
                    if (!player.level().isClientSide()) {
                        ((ServerLevel) player.level()).sendParticles(
                                ParticleTypes.CLOUD,
                                player.getX(), player.getY() + 1, player.getZ(),
                                3, 0.2, 0.2, 0.2, 0.05
                        );
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1.0F, 2.0F);
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
                    serverPlayer.setRespawnPosition(level.dimension(), pos, player.getYRot(), true, false);
                    com.BlackSouls.BlackSoulsMod.util.SkillUtils.setMana(serverPlayer, com.BlackSouls.BlackSoulsMod.util.SkillUtils.getMaxMana(serverPlayer));
                    serverPlayer.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                        stats.restoreActionPoints(SkillUtils.getMaxActionPoints(serverPlayer), SkillUtils.getMaxActionPoints(serverPlayer));
                        syncToClient(serverPlayer);
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
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;

        LivingEntity victim = event.getEntity();

        if (EntityHellPrince.isOpeningComboDamage(event.getSource())) {
            return;
        }

        if (BlackSouls.BUFF_KNIGHTS_GLORY.isPresent() && victim.hasEffect(BlackSouls.BUFF_KNIGHTS_GLORY.get())) {
            event.setAmount(event.getAmount() * 0.5F);
        }

        applyManagedMobAttackOverride(event);

        boolean skipUniversalDefense = false;

        boolean fromThrownBlade = event.getSource().getDirectEntity() instanceof EntityThrownBlade;

        if (event.getSource().getEntity() instanceof Player attacker) {
            if (fromThrownBlade) {
                skipUniversalDefense = true;
            } else if (event.getSource().is(DamageTypes.INDIRECT_MAGIC)) {
                skipUniversalDefense = true;
            } else if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                skipUniversalDefense = applyPlayerDirectAttackDamage(attacker, victim, event);
            } else {
                handlePlayerAttackVariance(attacker, event);
            }

            if (!fromThrownBlade) {
                applyPlayerOnHitStatusEffects(attacker, victim);
            }
        }

        if (!skipUniversalDefense) {
            handleUniversalDefense(victim, event);
        }

        if (BlackSouls.BUFF_DEFENSELESS.isPresent() && victim.hasEffect(BlackSouls.BUFF_DEFENSELESS.get())) {
            event.setAmount(event.getAmount() * 2.0F);
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
        double rawDamage = stats.attack * 4.0 - resolveVictimDirectDefense(victim) * 2.0;
        Item mainHandItem = attacker.getMainHandItem().getItem();
        boolean isWeapon = mainHandItem instanceof net.minecraft.world.item.TieredItem
                || mainHandItem instanceof net.minecraft.world.item.ProjectileWeaponItem;

        if (!isWeapon) {
            rawDamage = 1.0D;
        } else {
            double attackStrength = attacker.getAttackStrengthScale(0.5F);
            double vanillaChargeMultiplier = 0.2D + attackStrength * attackStrength * 0.8D;
            rawDamage = Math.max(1.0D, rawDamage) * vanillaChargeMultiplier;
        }

        List<String> attackAttrs = buildPlayerAttackAttributes(attacker, stats);
        rawDamage *= com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.getBestMultiplier(victim, attackAttrs);
        rawDamage *= (0.8D + Math.random() * 0.4D);

        if (attacker.getPersistentData().getBoolean("bs2_melee_crit")) {
            rawDamage *= 3.0D;
            attacker.getPersistentData().remove("bs2_melee_crit");
        }
        return rawDamage;
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
        if (!currentWeapon.isEmpty() && currentWeapon.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()) {
            attackAttrs.add(com.BlackSouls.BlackSoulsMod.util.BSAttributeManager.JABBERWOCK_KILLER);
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

        if (finalStunRate > 0 && Math.random() * 100.0 < finalStunRate && BlackSouls.BUFF_STUN.isPresent()) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_STUN.get(), 40, 0));
        }
        if (stats.fearRate > 0 && Math.random() * 100.0 < stats.fearRate && BlackSouls.BUFF_FEAR.isPresent()) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 100, 0));
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

        if (BlackSouls.BUFF_BURN.isPresent() && entity.hasEffect(BlackSouls.BUFF_BURN.get())) {
            if (entity.isInWater()) {
                entity.removeEffect(BlackSouls.BUFF_BURN.get());
                entity.clearFire();
                return;
            }

            if (entity.tickCount % 20 == 0) {
                float maxHp = entity.getMaxHealth();
                float burnDmg = maxHp * 0.05F;

                entity.hurt(entity.damageSources().magic(), burnDmg);
                entity.setSecondsOnFire(1);

                ((ServerLevel)entity.level()).sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight()/2, entity.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    public static void applyStats(Player player) {
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);
        if (stats != null) {
            BaubleCounter counts = new BaubleCounter(player);
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

            int lifeRingCount = getBaubleCount(player, BlackSouls.RING_LIFE.get());
            if (lifeRingCount > 0) stats.hp *= (1.0 + lifeRingCount * 0.05);

            int evilEyeCount = getBaubleCount(player, BlackSouls.RING_EVIL_EYE.get());
            if (evilEyeCount > 0) stats.hpRegenRate += evilEyeCount * 0.03;

            int poisonBiteCount = getBaubleCount(player, BlackSouls.RING_POISON_BITE.get());
            if (poisonBiteCount > 0) {
                stats.poisonResistRate = 1.0;
                stats.severePoisonResistRate = 1.0;
            }

            int bloodBiteCount = getBaubleCount(player, BlackSouls.RING_BLOOD_BITE.get());
            if (bloodBiteCount > 0) {
                stats.bleedResistRate = 1.0;
                stats.hpRegenRate += bloodBiteCount * 0.02;
            }

            int guardCount = getBaubleCount(player, BlackSouls.RING_GUARD.get());
            if (guardCount > 0) stats.defense += guardCount * 50.0;

            int terrorCount = getBaubleCount(player, BlackSouls.RING_TERROR.get());
            if (terrorCount > 0) {
                stats.fearRate += terrorCount * 50.0;
                stats.fearResistRate = 1.0;
            }

            int fairyCount = getBaubleCount(player, BlackSouls.RING_FAIRY.get());
            if (fairyCount > 0) {
                stats.evasion += fairyCount * 5.0;
                stats.sleepResistRate = 1.0;
            }

            int windGodCount = getBaubleCount(player, BlackSouls.RING_WIND_GOD.get());
            if (windGodCount > 0) stats.speed += windGodCount * 50.0;

            int spellCount = getBaubleCount(player, BlackSouls.RING_SPELL.get());
            if (spellCount > 0) stats.magicAttack += spellCount * 50.0;

            int masochistCount = getBaubleCount(player, BlackSouls.RING_MASOCHIST.get());
            if (masochistCount > 0) stats.targetingRate *= Math.pow(4.0, masochistCount);

            int midnightCrownCount = getBaubleCount(player, BlackSouls.RING_MIDNIGHT_CROWN.get());
            if (midnightCrownCount > 0) {
                stats.hp *= Math.pow(0.5, midnightCrownCount);
                stats.maxMp *= Math.pow(0.5, midnightCrownCount);
                stats.magicAttack *= Math.pow(1.5, midnightCrownCount);
            }

            int godFishCount = getBaubleCount(player, BlackSouls.RING_GOD_FISH.get());
            if (godFishCount > 0) stats.defense *= (1.0 + godFishCount * 1.0);

            int waspCount = getBaubleCount(player, BlackSouls.RING_WASP.get());
            if (waspCount > 0) stats.critRate += waspCount * 20.0;

            int puyoCount = getBaubleCount(player, BlackSouls.RING_PUYO.get());
            if (puyoCount > 0) stats.hp *= (1.0 + puyoCount * 0.15);

            int hunyaCount = getBaubleCount(player, BlackSouls.RING_HUNYA.get());
            if (hunyaCount > 0) stats.maxMp *= (1.0 + hunyaCount * 0.05);

            int goddessCount = getBaubleCount(player, BlackSouls.RING_GODDESS.get());
            if (goddessCount > 0) stats.mpRegenRate += goddessCount * 0.05;

            int angelCount = getBaubleCount(player, BlackSouls.RING_ANGEL.get());
            if (angelCount > 0) stats.mpRegenRate += angelCount * 0.03;

            int knightRingCount = getBaubleCount(player, BlackSouls.RING_KNIGHT.get());
            if (knightRingCount > 0) stats.stunRate += knightRingCount * 10.0;

            int ironMaidenCount = getBaubleCount(player, BlackSouls.RING_IRON_MAIDEN.get());
            if (ironMaidenCount > 0) stats.hpRegenRate -= ironMaidenCount * 0.20;

            int ironProtectionCount = getBaubleCount(player, BlackSouls.RING_IRON_PROTECTION.get());
            if (ironProtectionCount > 0) stats.physicalDamageRate *= Math.pow(0.8, ironProtectionCount);

            int magicStoneCount = getBaubleCount(player, BlackSouls.RING_MAGIC_STONE.get());
            if (magicStoneCount > 0) {
                stats.magicDamageRate *= Math.pow(0.8, magicStoneCount);
                stats.magicDefense += magicStoneCount * 50.0;
            }

            stats.evasion += getBaubleCount(player, BlackSouls.RING_LIEF.get()) * 20.0;
            stats.evasion += getBaubleCount(player, BlackSouls.RING_VOID.get()) * 10.0;
            stats.evasion -= getBaubleCount(player, BlackSouls.RING_DEATH.get()) * 50.0;

            if (getBaubleCount(player, BlackSouls.NOBLE_CLOTHES.get()) > 0) {
                stats.evasion += 5.0; stats.magicDefense *= 1.05; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.LAWYER_MASK.get()) > 0) {
                stats.magicDefense *= 1.10; stats.defense *= 1.05; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.VIOLENT_CLOAK.get()) > 0) {
                stats.attack *= 1.05; try { stats.magicAttack *= 1.05; } catch (Exception ignored) {}
                stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.FRENZIED_KING_CLOAK.get()) > 0) {
                try { stats.magicAttack *= 1.15; } catch (Exception ignored) {}
                stats.magicDefense *= 1.15; stats.speed *= 0.90;
            }
            if (getBaubleCount(player, BlackSouls.ANGEL_RAIMENT.get()) > 0) {
                stats.hp *= 1.50; stats.speed *= 0.96;
            }
            if (getBaubleCount(player, BlackSouls.LEATHER_ARMOR.get()) > 0) {
                stats.attack *= 1.08; stats.defense *= 1.05; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.MATCH_GIRL_CLOTHES.get()) > 0) {
                stats.evasion += 5.0;
            }
            if (getBaubleCount(player, BlackSouls.GENTLEMAN_COAT.get()) > 0) {
                stats.evasion += 5.0; stats.defense *= 1.03;
            }
            if (getBaubleCount(player, BlackSouls.PROSTITUTE_DRESS.get()) > 0) {
                stats.evasion += 5.0;
            }
            if (getBaubleCount(player, BlackSouls.PLATE_ARMOR.get()) > 0) {
                stats.defense *= 1.10; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.MILTON_ARMOR.get()) > 0) {
                stats.defense *= 1.10; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.MILTON_HELMET.get()) > 0) {
                stats.defense *= 1.06; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.HUNTERS_ATTIRE.get()) > 0) {
                stats.evasion += 10.0; stats.speed *= 0.98;
            }
            if (getBaubleCount(player, BlackSouls.DEEP_SEA_KNIGHT_HELMET.get()) > 0) {
                stats.defense *= 1.40; stats.speed *= 0.80;
            }
            if (getBaubleCount(player, BlackSouls.DEEP_SEA_KNIGHT_ARMOR.get()) > 0) {
                stats.defense *= 1.50; stats.speed *= 0.70;
            }
            if (getBaubleCount(player, BlackSouls.CREW_HEADSCARF.get()) > 0) {
                stats.critRate += 5.0; stats.speed *= 0.99;
            }
            if (getBaubleCount(player, BlackSouls.ONI_WARRIOR_HELMET.get()) > 0) {
                stats.critRate += 15.0; stats.defense *= 1.15; stats.speed *= 0.90;
            }
            if (getBaubleCount(player, BlackSouls.ONI_WARRIOR_ARMOR.get()) > 0) {
                stats.critRate += 20.0; stats.defense *= 1.20; stats.speed *= 0.80;
            }
            if (getBaubleCount(player, BlackSouls.SAILOR_SUIT.get()) > 0) {
                stats.critRate += 5.0; stats.evasion += 5.0;
            }
            if (getBaubleCount(player, BlackSouls.SNAKE_DRESS.get()) > 0) {
                stats.evasion += 5.0; stats.speed *= 0.98;
            }
            if (getBaubleCount(player, BlackSouls.DISCIPLINARIAN_ROBE.get()) > 0) {
                stats.magicDefense *= 1.25; stats.mpRegenRate += 0.05; stats.speed *= 0.94;
            }
            if (getBaubleCount(player, BlackSouls.OMINOUS_CLOTHES.get()) > 0) {
                stats.evasion += 30.0;
            }
            if (getBaubleCount(player, BlackSouls.BUTETSU_ARMOR.get()) > 0) {
                stats.critRate += 15.0; stats.defense *= 1.15; stats.speed *= 0.90;
            }
            if (SkillUtils.hasChronoClockEquipped(player)) {
                stats.extraActionRate += 1.0;
            }

            if (getBaubleCount(player, BlackSouls.ARMOR_OF_THE_SUN.get()) > 0) {
                stats.attack *= 1.15; stats.defense *= 1.15; stats.speed *= 0.90;
            }
            if (getBaubleCount(player, BlackSouls.CLERIC_VESTMENT.get()) > 0) {
                stats.defense *= 1.05; stats.magicDefense *= 1.07; stats.evasion += 5.0; stats.speed *= 0.96;
            }
            if (getBaubleCount(player, BlackSouls.MAGICIAN_COAT.get()) > 0) {
                try { stats.magicAttack *= 1.05; } catch (Exception ignored) {}
                stats.magicDefense *= 1.10; stats.evasion += 5.0; stats.speed *= 0.96;
            }
            if (getBaubleCount(player, BlackSouls.SHADOW_ATTIRE.get()) > 0) {
                stats.defense *= 1.08; stats.evasion += 5.0; stats.speed *= 0.98;
            }
            if (getBaubleCount(player, BlackSouls.KNIGHT_ARMOR.get()) > 0) {
                stats.defense *= 1.15; stats.speed *= 0.95;
            }
            if (getBaubleCount(player, BlackSouls.WARRIOR_ARMOR.get()) > 0) {
                stats.attack *= 1.05; stats.defense *= 1.10; stats.speed *= 0.95;
            }
            if (getBaubleCount(player, BlackSouls.BABEL_TOWER_ARMOR.get()) > 0) {
                stats.defense *= 1.30; stats.speed *= 0.80;
            }
            if (getBaubleCount(player, BlackSouls.PHANTOM_THIEF_CLOAK.get()) > 0) {
                stats.evasion += 15.0; stats.defense *= 0.90;
            }
            if (getBaubleCount(player, BlackSouls.CLERIC_CIRCLET.get()) > 0) {
                stats.defense *= 1.02; stats.magicDefense *= 1.03; stats.speed *= 0.99;
            }
            if (getBaubleCount(player, BlackSouls.MAGICIAN_HAT.get()) > 0) {
                try { stats.magicAttack *= 1.03; } catch (Exception ignored) {}
                stats.magicDefense *= 1.05; stats.speed *= 0.98;
            }
            if (getBaubleCount(player, BlackSouls.THIEF_MASK.get()) > 0) {
                stats.defense *= 1.04; stats.speed *= 0.99;
            }
            if (getBaubleCount(player, BlackSouls.KNIGHT_HELMET.get()) > 0) {
                stats.defense *= 1.08; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.VIKING_HELMET.get()) > 0) {
                stats.attack *= 1.04; stats.defense *= 1.04; stats.speed *= 0.97;
            }
            if (getBaubleCount(player, BlackSouls.RABBIT_EARS.get()) > 0) {
                stats.speed *= 1.05;
            }
            if (getBaubleCount(player, BlackSouls.WHITE_HAIRBAND.get()) > 0) {
                stats.magicDefense *= 1.20; stats.speed *= 0.98;
            }
            if (getBaubleCount(player, BlackSouls.BABEL_TOWER_HELMET.get()) > 0) {
                stats.defense *= 1.20; stats.speed *= 0.90;
            }
            if (getBaubleCount(player, BlackSouls.NINJA_HEADBAND.get()) > 0) {
                stats.attack *= 1.02; stats.defense *= 1.02;
            }
            if (getBaubleCount(player, BlackSouls.MYSTERIOUS_HAT.get()) > 0) {
                try { stats.magicAttack *= 1.08; } catch (Exception ignored) {}
                stats.speed *= 0.96;
            }
            if (getBaubleCount(player, BlackSouls.HATTER_HAT.get()) > 0) {
                try { stats.magicAttack *= 1.15; } catch (Exception ignored) {}
                stats.speed *= 0.98;
            }
            if (getBaubleCount(player, BlackSouls.SKY_KNIGHT_HAT.get()) > 0) {
                stats.evasion += 5.0;
            }
            if (getBaubleCount(player, BlackSouls.IGOR_MASK.get()) > 0) {
                stats.attack *= 1.20; try { stats.magicAttack *= 1.20; } catch (Exception ignored) {}
                stats.speed *= 0.85;
            }
            if (getBaubleCount(player, BlackSouls.BUNNY_GIRL_UNIFORM.get()) > 0) {
                stats.speed *= 1.05;
            }

            if (getBaubleCount(player, BlackSouls.GUARDIAN_ANGEL.get()) > 0) {
                stats.defense *= 0.70;
                stats.magicDefense *= 0.70;
            }

            int abyssCount = getBaubleCount(player, BlackSouls.RING_ABYSS.get());
            if (abyssCount > 0) stats.critRate += abyssCount * 40.0;
            int blackbeardCount = getBaubleCount(player, BlackSouls.RING_BLACKBEARD.get());
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
                stats.attack += 21.0;
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
            if (BlackSouls.BUFF_FEAR.isPresent() && player.hasEffect(BlackSouls.BUFF_FEAR.get())) {
                stats.evasion -= 100.0;
            }
            if (BlackSouls.BUFF_SEVERED_LEG.isPresent() && player.hasEffect(BlackSouls.BUFF_SEVERED_LEG.get())) {
                stats.speed = 0.0;
                stats.evasion = 0.0;
                stats.defense *= 0.01;
            }
            MinecraftForge.EVENT_BUS.post(new BSStatsRecalcEvent(player, stats));
            clampCalculatedStats(player, stats);
            syncVanillaAttributes(player, stats);
        }
    }

    private static void resetDerivedStats(BSPlayerStats stats) {
        stats.evasion = 0.0;
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
        stats.hp = Math.min(CAP_HP, stats.hp);
        stats.maxMp = Math.min(CAP_MP, stats.maxMp);
        stats.attack = Math.min(CAP_ATK, stats.attack);
        stats.defense = Math.min(CAP_DEF, stats.defense);
        stats.magicAttack = Math.min(CAP_MATK, stats.magicAttack);
        stats.magicDefense = Math.min(CAP_MDEF, stats.magicDefense);
        stats.luck = Math.min(CAP_LUCK, stats.luck);
        stats.speed = Math.min(CAP_SPEED, stats.speed);
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
            if (!player.level().isClientSide() && damage > 0.1F) {
                showDamageFeedback(player, victim, damage);
            }

            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null && stats.instantDeathRate > 0 && Math.random() * 100.0 < stats.instantDeathRate) {
                tryTriggerInstantDeath(player, victim, event, stats);
            }
            if (!player.level().isClientSide() && getBaubleCount(player, BlackSouls.SNAKE_DRESS.get()) > 0 && BlackSouls.BUFF_SEVERE_POISON.isPresent()) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 1000, 0));
            }
        }
    }

    private static void showDamageFeedback(Player player, LivingEntity victim, float damage) {
        if (BSConfig.SHOW_COMBAT_DAMAGE_CHAT.get()) {
            String damageStr = String.format("%.0f", damage);
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
                    new PacketSpawnDamageText(victim.getX(), victim.getY() + victim.getBbHeight() / 2.0, victim.getZ(), damage, isCrit));
        } catch (Exception ignored) {
        }
    }

    private static void tryTriggerInstantDeath(Player player, LivingEntity victim, LivingDamageEvent event, BSPlayerStats stats) {
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

        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.instant_death", (int) stats.instantDeathRate).withStyle(ChatFormatting.DARK_RED));
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
        if (event.getEntity() instanceof Player player) {
            if (getBaubleCount(player, BlackSouls.RING_GOD_FISH.get()) > 0 || getBaubleCount(player, BlackSouls.OMINOUS_CLOTHES.get()) > 0) {
                event.setCanceled(true);
                return;
            }
            if (getBaubleCount(player, BlackSouls.MYSTERY_OF_NIGHT_SKY.get()) > 0) {
                event.setAmount(event.getAmount() * 1.30F);
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (event.getEffectInstance() == null) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
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
                if (getBaubleCount(player, BlackSouls.RABBIT_EARS.get()) > 0 && Math.random() < 0.50) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (getBaubleCount(player, BlackSouls.BUNNY_GIRL_UNIFORM.get()) > 0 && Math.random() < 0.50) {
                    event.setResult(Event.Result.DENY); return;
                }
                if (getBaubleCount(player, BlackSouls.PROSTITUTE_DRESS.get()) > 0 && Math.random() < 0.50) {
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
        applyPassiveSnakeDressPoison(player);
        updateChronoClockState(player);
        applyStats(player);

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            tickServerPlayerResources(serverPlayer, player);
        }
    }

    private static void applyPassiveSnakeDressPoison(Player player) {
        if (!player.level().isClientSide()
                && getBaubleCount(player, BlackSouls.SNAKE_DRESS.get()) > 0
                && BlackSouls.BUFF_SEVERE_POISON.isPresent()
                && !player.hasEffect(BlackSouls.BUFF_SEVERE_POISON.get())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 200, 0, false, true, true));
        }
    }

    private static void updateChronoClockState(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            HashMultimap<String, AttributeModifier> modifiers = HashMultimap.create();
            AttributeModifier modifier = new AttributeModifier(CHRONO_WATCH_SLOT_UUID, "chrono_clock_watch_slot", 1.0D, AttributeModifier.Operation.ADDITION);
            modifiers.put("watch", modifier);

            boolean shouldHaveWatchSlot = SkillUtils.hasChronoClockAvailable(player);
            boolean hasWatchSlotModifier = handler.getModifiers().entries().stream()
                    .anyMatch(entry -> entry.getKey().equals("watch") && CHRONO_WATCH_SLOT_UUID.equals(entry.getValue().getId()));

            if (shouldHaveWatchSlot && !hasWatchSlotModifier) {
                handler.addTransientSlotModifiers(modifiers);
            } else if (!shouldHaveWatchSlot && hasWatchSlotModifier) {
                handler.removeSlotModifiers(modifiers);
            }
        });
    }

    private static void tickServerPlayerResources(ServerPlayer serverPlayer, Player player) {
        serverPlayer.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            boolean chronoSkillBindingChanged = clearChronoClockBindingsIfNeeded(serverPlayer, stats);
            double previousActionPoints = stats.getCurrentActionPoints();
            double maxActionPoints = SkillUtils.getMaxActionPoints(serverPlayer);
            if (previousActionPoints < maxActionPoints) {
                stats.restoreActionPoints(SkillUtils.getActionRegenPerTick(serverPlayer), maxActionPoints);
                stats.clampActionPoints(maxActionPoints);
                if (serverPlayer.tickCount % 5 == 0
                        && Math.abs(stats.getCurrentActionPoints() - previousActionPoints) > 1.0E-4) {
                    syncToClient(serverPlayer);
                }
            }

              if (serverPlayer.tickCount % 20 == 0) {
                  if (stats.mp < stats.maxMp) {
                      double regenAmount = 1.0 + (stats.maxMp * stats.mpRegenRate);
                      stats.mp += regenAmount;
                      if (stats.mp >= stats.maxMp || (stats.maxMp - stats.mp) < 0.5) {
                          stats.mp = stats.maxMp;
                      }
                  }
                  syncToClient(serverPlayer);
              }
              if (chronoSkillBindingChanged) {
                  syncToClient(serverPlayer);
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
          });
      }

    private static boolean clearChronoClockBindingsIfNeeded(ServerPlayer player, BSPlayerStats stats) {
        if (SkillUtils.hasChronoClockEquipped(player)) {
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
        syncToClient(serverPlayer);
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
                com.BlackSouls.BlackSoulsMod.handler.StatEventHandler.syncToClient(serverPlayer);
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
            if (target.hasEffect(BlackSouls.BUFF_SLEEP.get())) {

                float originalDamage = event.getAmount();
                event.setAmount(originalDamage * 9.0F);
                target.removeEffect(BlackSouls.BUFF_SLEEP.get());
            }
        }
    }

}
