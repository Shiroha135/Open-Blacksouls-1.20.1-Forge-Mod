package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.capability.BSWorldData;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public class SkillEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        if (!SkillUtils.hasLearnedSkill(player, "bs2_skill_difficulty")) {
            SkillUtils.learnSkill(player, "bs2_skill_difficulty");
        }

        syncData(player);

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int savedLevel = BSWorldData.get((ServerLevel) player.level()).difficulty;
            DifficultyManager.currentDifficulty = savedLevel;
        }

    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncData(event.getEntity());
    }

    private static void syncData(Player player) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            /*
            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_invisible_body")) NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncSkill("bs2_skill_invisible_body"));
            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_requiem")) NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncSkill("bs2_skill_requiem"));
            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_grit")) NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncSkill("bs2_skill_grit"));
            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_difficulty")) NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncSkill("bs2_skill_difficulty"));

            NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncMana(SkillUtils.getMana(player)));
            */

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            });
        }
    }
    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (BlackSouls.BUFF_INVISIBLE_BODY.isPresent() && attacker.hasEffect(BlackSouls.BUFF_INVISIBLE_BODY.get())) {
                attacker.removeEffect(BlackSouls.BUFF_INVISIBLE_BODY.get());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player player = event.player;

            if (SkillUtils.hasLearnedSkill(player, "bs2_skill_invisible_body")) {
                if (player.tickCount % 20 == 0) {
                    float current = SkillUtils.getMana(player);
                    float max = SkillUtils.getMaxMana(player);
                    if (current < max) {
                        SkillUtils.setMana(player, current + 2.0F);
                    }
                }
            }
            if (player.tickCount % 20 == 0) {
                player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                    if (stats.mp < stats.maxMp) {
                        stats.restoreMP(1.0);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerBeingAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (BlackSouls.BUFF_INVISIBLE_BODY.isPresent() && player.hasEffect(BlackSouls.BUFF_INVISIBLE_BODY.get())) {
                if (event.getSource().getEntity() instanceof LivingEntity) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemStack groundStack = event.getItem().getItem();
        Item item = groundStack.getItem();
        if (item == BlackSouls.BLOOD_VIAL.get() || item == BlackSouls.RABBIT_WATCH.get()) {
            checkLimit(event, item, 99);
        }
    }

    private static void checkLimit(EntityItemPickupEvent event, Item targetItem, int maxLimit) {
        Player player = event.getEntity();
        ItemStack groundStack = event.getItem().getItem();
        int currentCount = 0;
        
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == targetItem) currentCount += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == targetItem) currentCount += stack.getCount();
        }

        int pickupAmount = groundStack.getCount();
        
        if (currentCount >= maxLimit) {
            event.getItem().discard(); 
            event.setCanceled(true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            sendTranslatedMsg(player, "message.blacksouls.pickup.limit", ChatFormatting.RED, maxLimit);
        }
        else if (currentCount + pickupAmount > maxLimit) {
            int canPick = maxLimit - currentCount;
            ItemStack newStack = groundStack.copy();
            newStack.setCount(canPick);
            player.getInventory().add(newStack); 
            event.getItem().discard();
            event.setCanceled(true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            sendTranslatedMsg(player, "message.blacksouls.pickup.overflow", ChatFormatting.RED);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeathPrevent(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean hasGrit = BlackSouls.BUFF_GRIT.isPresent() && player.hasEffect(BlackSouls.BUFF_GRIT.get());
        boolean hasRequiem = BlackSouls.BUFF_REQUIEM.isPresent() && player.hasEffect(BlackSouls.BUFF_REQUIEM.get());

        if (hasGrit || hasRequiem) {
            float damage = event.getAmount();
            float currentHealth = player.getHealth();

            if (damage >= currentHealth) {
                event.setCanceled(true);
                player.setHealth(1.0F);

                if (hasGrit && !hasRequiem) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 0.5F);
                    sendTranslatedMsg(player, "message.blacksouls.skill.grit_trigger", ChatFormatting.RED);
                } else {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    sendTranslatedMsg(player, "message.blacksouls.skill.requiem_trigger", ChatFormatting.GOLD);
                }

                player.invulnerableTime = 10; 
            }
        }

        if (event.isCanceled()) {
            return;
        }

        float damage = event.getAmount();
        float currentHealth = player.getHealth();

        if (damage >= currentHealth) {
            boolean canGuardianAngelTrigger = !event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                    && !event.getSource().is(DamageTypes.GENERIC_KILL)
                    && !"bs2_sure_hit".equals(event.getSource().getMsgId());

            if (canGuardianAngelTrigger && getBaubleCount(player, BlackSouls.GUARDIAN_ANGEL.get()) > 0 && player.getRandom().nextDouble() < 0.80D) {
                event.setCanceled(true);
                player.setHealth(1.0F);
                player.invulnerableTime = 20;
                player.hurtMarked = true;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                sendTranslatedMsg(player, "message.blacksouls.guardian_angel.trigger", ChatFormatting.GOLD);
                return;
            }

            if (getBaubleCount(player, BlackSouls.RING_DRAGON_GUARD.get()) > 0) {
                event.setCanceled(true);
                player.setHealth(1.0F);
                player.invulnerableTime = 10;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);

                if (player.level().random.nextDouble() < 0.75D) {
                    breakEquippedRing(player, BlackSouls.RING_DRAGON_GUARD.get());
                    sendTranslatedMsg(player, "message.blacksouls.ring_dragon_guard.break", ChatFormatting.RED);
                } else {
                    sendTranslatedMsg(player, "message.blacksouls.ring_dragon_guard.saved", ChatFormatting.GOLD);
                }
            }
        }
    }

    private static int getBaubleCount(LivingEntity entity, Item item) {
        if (entity == null || item == null) {
            return 0;
        }
        return CuriosApi.getCuriosInventory(entity).map(handler -> handler.findCurios(item).size()).orElse(0);
    }

    private static void breakEquippedRing(Player player, Item item) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            java.util.List<top.theillusivec4.curios.api.SlotResult> results = handler.findCurios(item);
            if (!results.isEmpty()) {
                results.get(0).stack().shrink(1);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        if (oldPlayer.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) {
            newPlayer.getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    oldPlayer.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }

    @SubscribeEvent
    public static void onPlayerAttackCheck(AttackEntityEvent event) {
        if (BlackSouls.BUFF_STUN.isPresent() && event.getEntity().hasEffect(BlackSouls.BUFF_STUN.get())) {
            event.setCanceled(true);
            return;
        }

        double attackStrength = event.getEntity().getAttackStrengthScale(0.5F);
        double basicAttackCost = SkillUtils.BASIC_ATTACK_ACTION_COST * Math.max(0.25D, Math.min(1.0D, attackStrength));
        if (!event.getEntity().level().isClientSide()
                && event.getTarget() instanceof LivingEntity
                && !SkillUtils.consumeActionPoints(event.getEntity(), basicAttackCost)) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.literal(String.format(
                    "message.blacksouls.ap_insufficient",
                    SkillUtils.getCurrentActionPoints(event.getEntity()),
                    SkillUtils.getMaxActionPoints(event.getEntity())
            )).withStyle(ChatFormatting.GREEN), true);
            return;
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (BlackSouls.BUFF_STUN.isPresent() && event.getEntity().hasEffect(BlackSouls.BUFF_STUN.get())) {
            event.getEntity().setDeltaMovement(Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (BlackSouls.BUFF_STUN.isPresent() && entity.hasEffect(BlackSouls.BUFF_STUN.get())) {
            Vec3 currentMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(0, Math.min(0, currentMotion.y), 0);
        }

        if (BlackSouls.BUFF_INVISIBLE_BODY.isPresent() && entity.hasEffect(BlackSouls.BUFF_INVISIBLE_BODY.get())) {
            entity.setInvisible(true);
        }

        if (!entity.level().isClientSide() && entity instanceof Mob mob) {
            if (mob.getTarget() instanceof Player player) {
                if (BlackSouls.BUFF_INVISIBLE_BODY.isPresent() && player.hasEffect(BlackSouls.BUFF_INVISIBLE_BODY.get())) {
                    mob.setTarget(null);
                    mob.setLastHurtByMob(null);
                    mob.getNavigation().stop();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (BlackSouls.BUFF_STUN.isPresent() && event.getEntity().hasEffect(BlackSouls.BUFF_STUN.get())) {
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    private static void sendTranslatedMsg(Player player, String key, ChatFormatting color, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(color), false);
    }

    @SubscribeEvent
    public static void onMonsterSpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof LivingEntity monster
                && com.BlackSouls.BlackSoulsMod.util.BSMobStatManager.hasManagedStats(monster)) {
            DifficultyManager.applyModifierToSingleMonster(monster);
        }
    }
}
