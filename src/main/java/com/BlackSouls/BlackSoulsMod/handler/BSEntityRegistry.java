package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BSEntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlackSouls.MODID);

    public static final RegistryObject<EntityType<EntityNoden>> NODEN = ENTITY_TYPES.register("noden",
            () -> EntityType.Builder.<EntityNoden>of(EntityNoden::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("noden"));

    public static final RegistryObject<EntityType<EntityHailCaesar>> HAIL_CAESAR = ENTITY_TYPES.register("hail_caesar",
            () -> EntityType.Builder.of(EntityHailCaesar::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.8F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("hail_caesar"));

    public static final RegistryObject<EntityType<EntityTestDummy>> TEST_DUMMY = ENTITY_TYPES.register("test_dummy",
            () -> EntityType.Builder.of(EntityTestDummy::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .build("test_dummy"));

    public static final RegistryObject<EntityType<EntityThrownBlade>> THROWN_BLADE = ENTITY_TYPES.register("thrown_blade",
            () -> EntityType.Builder.<EntityThrownBlade>of(EntityThrownBlade::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("thrown_blade"));
    public static final RegistryObject<EntityType<EntityHellPrince>> HELL_PRINCE =
            ENTITY_TYPES.register("hell_prince", () ->
                    EntityType.Builder.of(EntityHellPrince::new, MobCategory.MONSTER)
                            .sized(1.2F, 3.5F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("hell_prince"));

    public static final RegistryObject<EntityType<EntityMeatWall>> MEAT_WALL =
            ENTITY_TYPES.register("meat_wall", () ->
                    EntityType.Builder.of(EntityMeatWall::new, MobCategory.CREATURE)
                            .sized(1.5F, 2.0F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("meat_wall"));

    public static final RegistryObject<EntityType<EntityCorpseEatingRabbit>> CORPSE_EATING_RABBIT =
            ENTITY_TYPES.register("corpse_eating_rabbit", () ->
                    EntityType.Builder.of(EntityCorpseEatingRabbit::new, MobCategory.MONSTER)
                            .sized(0.72F, 1.15F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("corpse_eating_rabbit"));

    public static final RegistryObject<EntityType<EntityOriginalDatabaseEnemy>> ORIGINAL_ENEMY =
            ENTITY_TYPES.register("original_enemy", () ->
                    EntityType.Builder.of(EntityOriginalDatabaseEnemy::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.8F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("original_enemy"));

    public static final RegistryObject<EntityType<EntityOriginalTurnBattleEnemy>> HEADLESS_UNDEAD =
            ENTITY_TYPES.register("headless_undead", () ->
                    EntityType.Builder.<EntityOriginalTurnBattleEnemy>of(
                                    (type, level) -> new EntityOriginalTurnBattleEnemy(
                                            type, level, EntityOriginalTurnBattleEnemy.Profile.HEADLESS_UNDEAD),
                                    MobCategory.MONSTER)
                            .sized(0.62F, 1.8F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("headless_undead"));

    public static final RegistryObject<EntityType<EntityOriginalTurnBattleEnemy>> CORRUPT_DOG =
            ENTITY_TYPES.register("corrupt_dog", () ->
                    EntityType.Builder.<EntityOriginalTurnBattleEnemy>of(
                                    (type, level) -> new EntityOriginalTurnBattleEnemy(
                                            type, level, EntityOriginalTurnBattleEnemy.Profile.CORRUPT_DOG),
                                    MobCategory.MONSTER)
                            .sized(0.9F, 1.05F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("corrupt_dog"));

    public static final RegistryObject<EntityType<EntityOriginalTurnBattleEnemy>> WEREWOLF =
            ENTITY_TYPES.register("werewolf", () ->
                    EntityType.Builder.<EntityOriginalTurnBattleEnemy>of(
                                    (type, level) -> new EntityOriginalTurnBattleEnemy(
                                            type, level, EntityOriginalTurnBattleEnemy.Profile.WEREWOLF),
                                    MobCategory.MONSTER)
                            .sized(1.1F, 1.9F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("werewolf"));
}
