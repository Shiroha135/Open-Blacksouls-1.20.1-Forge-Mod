package com.BlackSouls.BlackSoulsMod;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.item.ItemMaxHpMeat;
import com.BlackSouls.BlackSoulsMod.item.ItemMaxStatsFood;
import com.BlackSouls.BlackSoulsMod.item.material.ItemAbandonedTrash;
import com.BlackSouls.BlackSoulsMod.item.material.ItemCandy;
import com.BlackSouls.BlackSoulsMod.item.material.ItemFairyTaleBook;
import com.BlackSouls.BlackSoulsMod.item.material.ItemUpgradeMaterial;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.item.accessories.*;
import com.BlackSouls.BlackSoulsMod.item.consumables.*;
import com.BlackSouls.BlackSoulsMod.item.dev.*;
import com.BlackSouls.BlackSoulsMod.item.rings.*;
import com.BlackSouls.BlackSoulsMod.item.skillbook.*;
import com.BlackSouls.BlackSoulsMod.item.soul.*;
import com.BlackSouls.BlackSoulsMod.item.weapon.*;
import com.BlackSouls.BlackSoulsMod.item.ItemBaubleBase;
import com.BlackSouls.BlackSoulsMod.item.ItemDevTool;
import com.BlackSouls.BlackSoulsMod.item.ItemNodenSpawn;
import com.BlackSouls.BlackSoulsMod.potion.*;
import com.BlackSouls.BlackSoulsMod.sound.BSSoundRegistry;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@Mod(BlackSouls.MODID)
public class BlackSouls {
    public static final String MODID = "blacksouls";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    // ============================================================================================================================================
    // 注册表声明
    //=============================================================================================================================================
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // ==================================================================================================================================================
    // 状态效果(Buff/Debuff)
    // ==================================================================================================================================================
    public static final RegistryObject<MobEffect> BUFF_ATK_UP = MOB_EFFECTS.register("atk_up", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD98CFF));
    public static final RegistryObject<MobEffect> BUFF_ATK_UP_2 = MOB_EFFECTS.register("atk_up_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xFFB3FF));
    public static final RegistryObject<MobEffect> BUFF_ATK_DOWN = MOB_EFFECTS.register("atk_down", PotionAtkDown::new);
    public static final RegistryObject<MobEffect> BUFF_ATK_DOWN_2 = MOB_EFFECTS.register("atk_down_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x3F3F3F));
    public static final RegistryObject<MobEffect> BUFF_BURN = MOB_EFFECTS.register("burn", PotionBurn::new);
    public static final RegistryObject<MobEffect> BUFF_DEF_UP = MOB_EFFECTS.register("def_up", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xB0D9FF));
    public static final RegistryObject<MobEffect> BUFF_DEF_UP_2 = MOB_EFFECTS.register("def_up_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xCFE8FF));
    public static final RegistryObject<MobEffect> BUFF_DEF_DOWN = MOB_EFFECTS.register("def_down", PotionDefDown::new);
    public static final RegistryObject<MobEffect> BUFF_DEF_DOWN_2 = MOB_EFFECTS.register("def_down_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x5C2F12));
    public static final RegistryObject<MobEffect> BUFF_DEFENSELESS = MOB_EFFECTS.register("defenseless", PotionDefenseless::new);
    public static final RegistryObject<MobEffect> BUFF_DODO_RUN = MOB_EFFECTS.register("dodo_run", PotionDodoRun::new);
    public static final RegistryObject<MobEffect> BUFF_GRIT = MOB_EFFECTS.register("grit", PotionGrit::new);
    public static final RegistryObject<MobEffect> BUFF_HELANRITH_WINE = MOB_EFFECTS.register("helanrith_wine", PotionHelanrithWine::new);
    public static final RegistryObject<MobEffect> BUFF_HOLLOWED = MOB_EFFECTS.register("hollowed", PotionHollowed::new);
    public static final RegistryObject<MobEffect> BUFF_INVISIBLE_BODY = MOB_EFFECTS.register("invisible_body", PotionInvisibleBody::new);
    public static final RegistryObject<MobEffect> BUFF_MADNESS = MOB_EFFECTS.register("madness", PotionMadness::new);
    public static final RegistryObject<MobEffect> BUFF_REQUIEM = MOB_EFFECTS.register("requiem", PotionRequiem::new);
    public static final RegistryObject<MobEffect> BUFF_SILENCE = MOB_EFFECTS.register("silence", PotionSilence::new);
    public static final RegistryObject<MobEffect> BUFF_STUN = MOB_EFFECTS.register("stun", PotionStun::new);
    public static final RegistryObject<MobEffect> BUFF_BLEEDING = MOB_EFFECTS.register("bleeding", PotionBleeding::new);
    public static final RegistryObject<MobEffect> BUFF_POISON = MOB_EFFECTS.register("poison", PotionPoison::new);
    public static final RegistryObject<MobEffect> BUFF_SEVERE_POISON = MOB_EFFECTS.register("severe_poison", PotionSeverePoison::new);
    public static final RegistryObject<MobEffect> BUFF_FEAR = MOB_EFFECTS.register("fear", PotionFear::new);
    public static final RegistryObject<MobEffect> BUFF_SLEEP = MOB_EFFECTS.register("sleep", PotionSleep::new);
    public static final RegistryObject<MobEffect> BUFF_KNIGHTS_GLORY = MOB_EFFECTS.register("knights_glory", PotionKnightsGlory::new);
    public static final RegistryObject<MobEffect> BUFF_DAGGER_EVASION = MOB_EFFECTS.register("dagger_evasion", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD8E4F2));
    public static final RegistryObject<MobEffect> BUFF_DAGGER_GUARD = MOB_EFFECTS.register("dagger_guard", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x9CA8B8));
    public static final RegistryObject<MobEffect> BUFF_BERSERK = MOB_EFFECTS.register("berserk", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xC63B2D));
    public static final RegistryObject<MobEffect> BUFF_STRUGGLE = MOB_EFFECTS.register("struggle", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xE6B94A));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_ATK_UP = MOB_EFFECTS.register("magic_atk_up", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xFF8ED8));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_ATK_UP_2 = MOB_EFFECTS.register("magic_atk_up_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xFFC5F1));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_ATK_DOWN = MOB_EFFECTS.register("magic_atk_down", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x8C3A72));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_ATK_DOWN_2 = MOB_EFFECTS.register("magic_atk_down_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x5B2048));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_DEF_UP = MOB_EFFECTS.register("magic_def_up", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x7ED8FF));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_DEF_UP_2 = MOB_EFFECTS.register("magic_def_up_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xB5F1FF));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_DEF_DOWN = MOB_EFFECTS.register("magic_def_down", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x2E5D76));
    public static final RegistryObject<MobEffect> BUFF_MAGIC_DEF_DOWN_2 = MOB_EFFECTS.register("magic_def_down_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x1F3E50));
    public static final RegistryObject<MobEffect> BUFF_LUCK_UP = MOB_EFFECTS.register("luck_up", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xFFF59A));
    public static final RegistryObject<MobEffect> BUFF_LUCK_UP_2 = MOB_EFFECTS.register("luck_up_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xFFFAD1));
    public static final RegistryObject<MobEffect> BUFF_LUCK_DOWN = MOB_EFFECTS.register("luck_down", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x8A7A2C));
    public static final RegistryObject<MobEffect> BUFF_LUCK_DOWN_2 = MOB_EFFECTS.register("luck_down_2", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x5E521B));
    public static final RegistryObject<MobEffect> BUFF_SPEED_UP = MOB_EFFECTS.register("speed_up", () -> new PotionSpeedShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x93FFB8, "26ec83dc-4156-4124-9012-7b987c2e9801", 0.25D));
    public static final RegistryObject<MobEffect> BUFF_SPEED_UP_2 = MOB_EFFECTS.register("speed_up_2", () -> new PotionSpeedShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD2FFE0, "dc93f8d2-d77d-4daf-b7f9-88bd34a6ad11", 0.50D));
    public static final RegistryObject<MobEffect> BUFF_SPEED_DOWN = MOB_EFFECTS.register("speed_down", () -> new PotionSpeedShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x3D8450, "4a588b8e-79ec-4e42-ad7f-baa5ee8e7c41", -0.25D));
    public static final RegistryObject<MobEffect> BUFF_SPEED_DOWN_2 = MOB_EFFECTS.register("speed_down_2", () -> new PotionSpeedShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x23492D, "8d74a82a-49cd-4478-92ad-a13898bcb8fc", -0.50D));
    public static final RegistryObject<MobEffect> BUFF_FIRE_POWER = MOB_EFFECTS.register("fire_power", PotionFirePower::new);
    public static final RegistryObject<MobEffect> BUFF_ICE_POWER = MOB_EFFECTS.register("ice_power", PotionIcePower::new);
    public static final RegistryObject<MobEffect> BUFF_THUNDER_POWER = MOB_EFFECTS.register("thunder_power", PotionThunderPower::new);
    public static final RegistryObject<MobEffect> BUFF_DARK_POWER = MOB_EFFECTS.register("dark_power", PotionDarkPower::new);
    public static final RegistryObject<MobEffect> BUFF_SEVERED_LEG = MOB_EFFECTS.register("severed_leg", PotionSeveredLeg::new);
    public static final RegistryObject<MobEffect> BUFF_FROSTBITE = MOB_EFFECTS.register("frostbite", PotionFrostbite::new);
    // ===========================================================================================================
    // 音效注册
    // ===========================================================================================================
    public static final RegistryObject<SoundEvent> ACID_EVENT = registerSound("acid");
    public static final RegistryObject<SoundEvent> ATTACK3_EVENT = registerSound("attack3");
    public static final RegistryObject<SoundEvent> BATTLE3_EVENT =  registerSound("battle3");
    public static final RegistryObject<SoundEvent> BLIND_EVENT = registerSound("blind");
    public static final RegistryObject<SoundEvent> BLOW6_EVENT = registerSound("blow6");
    public static final RegistryObject<SoundEvent> BLOW7_EVENT = registerSound("blow7");
    public static final RegistryObject<SoundEvent> BREAK_EVENT = registerSound("break");
    public static final RegistryObject<SoundEvent> CRASH_EVENT = registerSound("crash");
    public static final RegistryObject<SoundEvent> CURSOR1_EVENT = registerSound("cursor1");
    public static final RegistryObject<SoundEvent> DAO_EVENT = registerSound("dao");
    public static final RegistryObject<SoundEvent> DARKNESS3_EVENT = registerSound("darkness3");
    public static final RegistryObject<SoundEvent> DARKNESS5_EVENT = registerSound("darkness5");
    public static final RegistryObject<SoundEvent> DARKNESS7_EVENT = registerSound("darkness7");
    public static final RegistryObject<SoundEvent> DAMAGE4_EVENT = registerSound("damage4");
    public static final RegistryObject<SoundEvent> DOWN2_EVENT = registerSound("down2");
    public static final RegistryObject<SoundEvent> EARTH6_EVENT = registerSound("earth6");
    public static final RegistryObject<SoundEvent> EARTH1_EVENT = registerSound("earth1");
    public static final RegistryObject<SoundEvent> EARTH5_EVENT = registerSound("earth5");
    public static final RegistryObject<SoundEvent> EVASION1_EVENT  = registerSound("evasion1");
    public static final RegistryObject<SoundEvent> EXPLOSION3_EVENT =registerSound("explosion3");
    public static final RegistryObject<SoundEvent> FIRE2_EVENT = registerSound("fire2");
    public static final RegistryObject<SoundEvent> FIRE1_EVENT = registerSound("fire1");
    public static final RegistryObject<SoundEvent> FIRE3_EVENT = registerSound("fire3");
    public static final RegistryObject<SoundEvent> FIRE4_EVENT = registerSound("fire4");
    public static final RegistryObject<SoundEvent> FIRE6_EVENT = registerSound("fire6");
    public static final RegistryObject<SoundEvent> FIRE7_EVENT = registerSound("fire7");
    public static final RegistryObject<SoundEvent> FIRE8_EVENT = registerSound("fire8");
    public static final RegistryObject<SoundEvent> FLASH1_EVENT = registerSound("flash1");
    public static final RegistryObject<SoundEvent> FLASH3_EVENT = registerSound("flash3");
    public static final RegistryObject<SoundEvent> FOG1_EVENT = registerSound("fog1");
    public static final RegistryObject<SoundEvent> FOG2_EVENT = registerSound("fog2");
    public static final RegistryObject<SoundEvent> GUCHA004A_EVENT = registerSound("gucha004a");
    public static final RegistryObject<SoundEvent> GUN1_EVENT = registerSound("gun1");
    public static final RegistryObject<SoundEvent> ICE1_EVENT = registerSound("ice1");
    public static final RegistryObject<SoundEvent> ICE2_EVENT = registerSound("ice2");
    public static final RegistryObject<SoundEvent> ICE4_EVENT = registerSound("ice4");
    public static final RegistryObject<SoundEvent> ICE7_EVENT = registerSound("ice7");
    public static final RegistryObject<SoundEvent> ICE8_EVENT = registerSound("ice8");
    public static final RegistryObject<SoundEvent> ICE11_EVENT = registerSound("ice11");
    public static final RegistryObject<SoundEvent> ITEM1_EVENT = registerSound("item1");
    public static final RegistryObject<SoundEvent> KEY_EVENT = registerSound("key");
    public static final RegistryObject<SoundEvent> MAGIC1_EVENT = registerSound("magic1");
    public static final RegistryObject<SoundEvent> MBJH_ME03_EVENT = registerSound("mbjh_me03");
    public static final RegistryObject<SoundEvent> MONSTER4_EVENT = registerSound("monster4");
    public static final RegistryObject<SoundEvent> MONSTER1_EVENT = registerSound("monster1");
    public static final RegistryObject<SoundEvent> HEARTBEAT_EVENT = registerSound("heartbeat");
    public static final RegistryObject<SoundEvent> PLAYER_DEATH_EVENT = registerSound("player_death");
    public static final RegistryObject<SoundEvent> RABBIT_WATCH_WIND_EVENT = registerSound("rabbit_watch_wind");
    public static final RegistryObject<SoundEvent> RAISE3_EVENT = registerSound("raise3");
    public static final RegistryObject<SoundEvent> RAISE1_EVENT = registerSound("raise1");
    public static final RegistryObject<SoundEvent> SAINT6_EVENT = registerSound("saint6");
    public static final RegistryObject<SoundEvent> SAINT7_EVENT = registerSound("saint7");
    public static final RegistryObject<SoundEvent> SAINT9_EVENT = registerSound("saint9");
    public static final RegistryObject<SoundEvent> SAND_EVENT = registerSound("sand");
    public static final RegistryObject<SoundEvent> PUSH_EVENT = registerSound("push");
    public static final RegistryObject<SoundEvent> SHOTGUN_FIRE_EVENT = registerSound("shotgun_fire");
    public static final RegistryObject<SoundEvent> SKILL1_EVENT =  registerSound("skill1");
    public static final RegistryObject<SoundEvent> SKILL3_EVENT =  registerSound("skill3");
    public static final RegistryObject<SoundEvent> SLASH1_EVENT = registerSound("slash1");
    public static final RegistryObject<SoundEvent> SLASH2_EVENT = registerSound("slash2");
    public static final RegistryObject<SoundEvent> SLASH3_EVENT = registerSound("slash3");
    public static final RegistryObject<SoundEvent> SLASH4_EVENT = registerSound("slash4");
    public static final RegistryObject<SoundEvent> SLASH8_EVENT = registerSound("slash8");
    public static final RegistryObject<SoundEvent> SLASH9_EVENT = registerSound("slash9");
    public static final RegistryObject<SoundEvent> SLASH10_EVENT = registerSound("slash10");
    public static final RegistryObject<SoundEvent> SLASH11_EVENT = registerSound("slash11");
    public static final RegistryObject<SoundEvent> SLASH12_EVENT = registerSound("slash12");
    public static final RegistryObject<SoundEvent> SLEEP_EVENT = registerSound("sleep");
    public static final RegistryObject<SoundEvent> SONG_EVENT = registerSound("song");
    public static final RegistryObject<SoundEvent> SWORD1_EVENT = registerSound("sword1");
    public static final RegistryObject<SoundEvent> SWORD3_EVENT = registerSound("sword3");
    public static final RegistryObject<SoundEvent> SWORD4_EVENT = registerSound("sword4");
    public static final RegistryObject<SoundEvent> SWORD5_EVENT = registerSound("sword5");
    public static final RegistryObject<SoundEvent> THUNDER1_EVENT = registerSound("thunder1");
    public static final RegistryObject<SoundEvent> THUNDER7_EVENT = registerSound("thunder7");
    public static final RegistryObject<SoundEvent> THUNDER5_EVENT = registerSound("thunder5");
    public static final RegistryObject<SoundEvent> THUNDER8_EVENT = registerSound("thunder8");
    public static final RegistryObject<SoundEvent> THUNDER10_EVENT = registerSound("thunder10");
    public static final RegistryObject<SoundEvent> TITLE_BGM_EVENT = registerSound("title_bgm");
    public static final RegistryObject<SoundEvent> TWINE_EVENT = registerSound("twine");
    public static final RegistryObject<SoundEvent> UP4_EVENT = registerSound("up4");
    public static final RegistryObject<SoundEvent> WATER1_EVENT = registerSound("water1");
    public static final RegistryObject<SoundEvent> WIND1_EVENT = registerSound("wind1");
    public static final RegistryObject<SoundEvent> WIND7_EVENT = registerSound("wind7");
    public static final RegistryObject<SoundEvent> WIND10_EVENT = registerSound("wind10");

    //boss战洛德
    public static final RegistryObject<SoundEvent> HELL_PRINCE_BGM_EVENT = registerSound("hell_prince_bgm");

    //图书馆维度专用BGM
    public static final RegistryObject<SoundEvent> LIBRARY_BGM_EVENT = SOUND_EVENTS.register("music.library",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "music.library")));

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(MODID, name))
        );
    }
    // =========================================================================================================================================================================
    // 卖钱用/杂项
    // =========================================================================================================================================================================
    // 废弃垃圾
    public static final RegistryObject<Item> ABANDONED_TRASH = ITEMS.register("abandoned_trash", ItemAbandonedTrash::new);
    // 糖果
    public static final RegistryObject<Item> CANDY = ITEMS.register("candy", () -> new ItemCandy(new Item.Properties()));
    // =========================================================================================================================================================================
    // 武器强化素材
    // =========================================================================================================================================================================
    // 强化石的碎片
    public static final RegistryObject<Item> UPGRADE_SHARD = ITEMS.register("upgrade_shard", () -> new ItemUpgradeMaterial(new Item.Properties(), "item.blacksouls.upgrade_material.lore"));
    // 强化石大碎片
    public static final RegistryObject<Item> UPGRADE_LARGE_SHARD = ITEMS.register("upgrade_large_shard", () -> new ItemUpgradeMaterial(new Item.Properties(), "item.blacksouls.upgrade_material.lore"));
    // 强化石块
    public static final RegistryObject<Item> UPGRADE_CHUNK = ITEMS.register("upgrade_chunk", () -> new ItemUpgradeMaterial(new Item.Properties(), "item.blacksouls.upgrade_material.lore"));
    // 强化石原盘
    public static final RegistryObject<Item> UPGRADE_SLAB = ITEMS.register("upgrade_slab", () -> new ItemUpgradeMaterial(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC), "item.blacksouls.upgrade_material.lore"));
    // 神秘的碎片
    public static final RegistryObject<Item> MYSTERIOUS_SHARD = ITEMS.register("mysterious_shard", () -> new ItemUpgradeMaterial(new Item.Properties(), "item.blacksouls.mysterious_shard.lore"));
    // =========================================================================================================================================================================
    // 消耗品
    // =========================================================================================================================================================================
    // 输血药
    public static final RegistryObject<Item> BLOOD_VIAL = ITEMS.register("blood_vial", () -> new ItemBloodVial(new Item.Properties()));
    // 水
    public static final RegistryObject<Item> PURE_WATER = ITEMS.register("pure_water", () -> new ItemPureWater(new Item.Properties()));
    // 镇静剂
    public static final RegistryObject<Item> SEDATIVE = ITEMS.register("sedative", () -> new ItemSedative(new Item.Properties()));
    // 兔子的怀表
    public static final RegistryObject<Item> RABBIT_WATCH = ITEMS.register("rabbit_watch", () -> new ItemRabbitWatch(new Item.Properties()));
    // 布莱克威尔的输血药
    public static final RegistryObject<Item> BLACKWELL_BLOOD_VIAL = ITEMS.register("blackwell_blood_vial", () -> new ItemBlackwellBloodVial(new Item.Properties()));
    // 人鱼的歌声
    public static final RegistryObject<Item> MERMAIDSONG = ITEMS.register("mermaid_song", () -> new ItemMermaidSong(new Item.Properties()));
    // 少女之香
    public static final RegistryObject<Item> MAIDENSFRAGRANCE = ITEMS.register("maidens_fragrance", () -> new ItemMaidensFragrance(new Item.Properties()));
    // 黄金的蜂蜜酒
    public static final RegistryObject<Item> GOLDENMEAD = ITEMS.register("golden_mead", () -> new ItemGoldenMead(new Item.Properties()));
    // 海兰里斯酒
    public static final RegistryObject<Item> HELANRITHWINE = ITEMS.register("helanrith_wine", () -> new ItemHelanrithWine(new Item.Properties()));
    // 药草瓶
    public static final RegistryObject<Item> HERB_BOTTLE = ITEMS.register("herb_bottle", () -> new ItemHerbBottle(new Item.Properties().stacksTo(1).durability(10)));
    // 药草瓶M
    public static final RegistryObject<Item> HERB_BOTTLE_M = ITEMS.register("herb_bottle_m", () -> new ItemHerbBottleM(new Item.Properties().stacksTo(1).durability(10)));
    // 鸽子蛋
    public static final RegistryObject<Item> PIGEON_EGG = ITEMS.register("pigeon_egg", () -> new ItemPigeonEgg(new Item.Properties()));
    // 女神之血
    public static final RegistryObject<Item> GODDESS_BLOOD = ITEMS.register("goddess_blood", () -> new ItemGoddessBlood(new Item.Properties()));
    // 山羊的肉
    public static final RegistryObject<Item> GOAT_MEAT = ITEMS.register("goat_meat", () -> new ItemMaxHpMeat(new Item.Properties(), 100.0));
    // 孕妇蛋糕之肉
    public static final RegistryObject<Item> PREGNANT_CAKE_MEAT = ITEMS.register("pregnant_cake_meat", () -> new ItemMaxHpMeat(new Item.Properties(), 100.0));
    // 梦之魂
    public static final RegistryObject<Item> DREAM_SOUL = ITEMS.register("dream_soul", () -> new ItemDreamSoul(new Item.Properties()));
    // 蛇神的血
    public static final RegistryObject<Item> SNAKE_GOD_BLOOD = ITEMS.register("snake_god_blood", () -> new ItemSnakeGodBlood(new Item.Properties()));
    // 来路不明的肉
    public static final RegistryObject<Item> MYSTERIOUS_MEAT = ITEMS.register("mysterious_meat", () -> new ItemMaxHpMeat(new Item.Properties(), 50.0));
    // 圣诞鸡肉
    public static final RegistryObject<Item> CHRISTMAS_CHICKEN = ITEMS.register("christmas_chicken", () -> new ItemMaxHpMeat(new Item.Properties(), 1000.0));
    // 鸡肉
    public static final RegistryObject<Item> CHICKEN = ITEMS.register("chicken", () -> new ItemMaxHpMeat(new Item.Properties(), 100.0));
    // 火柴药
    public static final RegistryObject<Item> MATCH_MEDICINE = ITEMS.register("match_medicine", () -> new ItemMatchMedicine(new Item.Properties()));
    // 比尔的便当
    public static final RegistryObject<Item> BILLS_BENTO = ITEMS.register("bills_bento", () -> new ItemBillsBento(new Item.Properties()));
    // 女王的蛋挞
    public static final RegistryObject<Item> QUEEN_EGG_TART = ITEMS.register("queen_egg_tart", () -> new ItemMaxHpMeat(new Item.Properties(), 300.0));
    // 烤起司
    public static final RegistryObject<Item> ROASTED_CHEESE = ITEMS.register("roasted_cheese", () -> new ItemMaxStatsFood(new Item.Properties(), 10.0, 5.0));
    // 海龟汤
    public static final RegistryObject<Item> TURTLE_SOUP = ITEMS.register("turtle_soup", () -> new ItemTurtleSoup(new Item.Properties()));
    // 再思的扑克
    public static final RegistryObject<Item> RETRIEVAL_POKER = ITEMS.register("retrieval_poker", () -> new ItemRetrievalPoker(new Item.Properties()));
    // 处方药
    public static final RegistryObject<Item> PRESCRIPTION_MEDICINE = ITEMS.register("prescription_medicine", () -> new ItemPrescriptionMedicine(new Item.Properties()));
    // 少女的写真
    public static final RegistryObject<Item> GIRLS_PHOTO = ITEMS.register("girls_photo", () -> new ItemGirlsPhoto(new Item.Properties()));
    // 黑之灰
    public static final RegistryObject <Item> BLACK_ASH = ITEMS.register("black_ash", () -> new ItemBlackAsh(new Item.Properties()));
    // 归还的蛇骨
    public static final RegistryObject <Item> SNAKE_BONE_RETURN = ITEMS.register("snake_bone_return", () -> new ItemSnakeBoneReturn(new Item.Properties()));
    // 归还骨粉
    public static final RegistryObject <Item> HOMEWARD_BONE_DUST = ITEMS.register("homeward_bone_dust", () -> new ItemHomewardBoneDust(new Item.Properties()));
    // 止血布
    public static final RegistryObject <Item> HEMOSTATIC_CLOTH = ITEMS.register("hemostatic_cloth", () -> new ItemHemostaticCloth(new Item.Properties()));
    // 解毒草
    public static final RegistryObject <Item> ANTIDOTE_HERB = ITEMS.register("antidote_herb", () -> new ItemAntidoteHerb(new Item.Properties()));
    // 看不见的胡椒
    public static final RegistryObject <Item> INVISIBLE_PEPPER = ITEMS.register("invisible_pepper", () -> new ItemInvisiblePepper(new Item.Properties()));
    // 妖精的鳞粉
    public static final RegistryObject <Item> FAIRY_SCALE_POWDER = ITEMS.register("fairy_scale_powder", () -> new ItemFairyScalePowder(new Item.Properties()));
    // 橘子果酱
    public static final RegistryObject <Item> ORANGE_MARMALADE = ITEMS.register("orange_marmalade",()-> new ItemOrangeMarmalade(new Item.Properties()));
    // 星水
    public static final RegistryObject <Item> STAR_WATER = ITEMS.register("star_water",() -> new ItemStarWater(new Item.Properties()));
    // 投掷小刀
    public static final RegistryObject<Item> THROWING_KNIFE = ITEMS.register("throwing_knife", () -> new ItemThrowingKnife(new Item.Properties()));
    // 手术刀
    public static final RegistryObject<Item> SCALPEL = ITEMS.register("scalpel", () -> new ItemScalpel(new Item.Properties()));
    // 炭松脂
    public static final RegistryObject<Item> CHARCOAL_PINE_RESIN = ITEMS.register("charcoal_pine_resin", () -> new ItemWeaponResin(new Item.Properties(), BUFF_FIRE_POWER, 2000, 57, FIRE4_EVENT, null, 0, true, -1.1F));
    // 黄金松脂
    public static final RegistryObject<Item> GOLD_PINE_RESIN = ITEMS.register("gold_pine_resin", () -> new ItemWeaponResin(new Item.Properties(), BUFF_THUNDER_POWER, 2000, 66, THUNDER10_EVENT, THUNDER8_EVENT, 3, false, 0.0F));
    // 暗松脂
    public static final RegistryObject<Item> DARK_PINE_RESIN = ITEMS.register("dark_pine_resin", () -> new ItemWeaponResin(new Item.Properties(), BUFF_DARK_POWER, 2000, 77, DARKNESS3_EVENT, null, 0, false, 0.0F));
    // 冰松脂
    public static final RegistryObject<Item> ICE_PINE_RESIN = ITEMS.register("ice_pine_resin", () -> new ItemWeaponResin(new Item.Properties(), BUFF_ICE_POWER, 2000, 61, ICE4_EVENT, THUNDER1_EVENT, 3, false, -1.2F));
    // 蜡烛的余烬
    public static final RegistryObject <Item> CANDLE_EMBER = ITEMS.register("candle_ember",() -> new ItemCandleEmber(new Item.Properties()));
    // 浑浊之鱼
    public static final RegistryObject <Item> MUDDY_FISH = ITEMS.register("muddy_fish",() -> new ItemMuddyFish(new Item.Properties()));
    // 又白又黏的那啥
    public static final RegistryObject<Item> WHITE_STICKY_THING = ITEMS.register("white_sticky_thing", () -> new ItemWhiteStickyThing(new Item.Properties()));
    // 铁渣点心
    public static final RegistryObject<Item> IRON_SCRAP_SNACK = ITEMS.register("iron_scrap_snack", () -> new ItemIronScrapSnack(new Item.Properties()));
    // 大工的钉子
    public static final RegistryObject<Item> CARPENTER_NAIL = ITEMS.register("carpenter_nail", () -> new ItemCarpenterNail(new Item.Properties()));
    // 咒骂之花
    public static final RegistryObject<Item> CURSING_FLOWER = ITEMS.register("cursing_flower", () -> new ItemCursingFlower(new Item.Properties()));
    // =========================================================================================================================================================================
    // 胸饰、头饰
    // =========================================================================================================================================================================
    public static final RegistryObject<Item> ANGEL_RAIMENT = ITEMS.register("angel_raiment", () -> new ItemAngelRaiment(new Item.Properties()));
    public static final RegistryObject<Item> ARMOR_OF_THE_SUN = ITEMS.register("armor_of_the_sun", () -> new ItemArmorOfTheSun(new Item.Properties()));
    public static final RegistryObject<Item> BABEL_TOWER_ARMOR = ITEMS.register("babel_tower_armor", () -> new ItemBabelTowerArmor(new Item.Properties()));
    public static final RegistryObject<Item> BABEL_TOWER_HELMET = ITEMS.register("babel_tower_helmet", () -> new ItemBabelTowerHelmet(new Item.Properties()));
    public static final RegistryObject<Item> BUNNY_GIRL_UNIFORM = ITEMS.register("bunny_girl_uniform", () -> new ItemBunnyGirlUniform(new Item.Properties()));
    public static final RegistryObject<Item> BUTETSU_ARMOR = ITEMS.register("butetsu_armor", () -> new ItemButetsuArmor(new Item.Properties()));
    public static final RegistryObject<Item> CLERIC_CIRCLET = ITEMS.register("cleric_circlet", () -> new ItemClericCirclet(new Item.Properties()));
    public static final RegistryObject<Item> CLERIC_VESTMENT = ITEMS.register("cleric_vestment", () -> new ItemClericVestment(new Item.Properties()));
    public static final RegistryObject<Item> CREW_HEADSCARF = ITEMS.register("crew_headscarf", () -> new ItemCrewHeadscarf(new Item.Properties()));
    public static final RegistryObject<Item> DEEP_SEA_KNIGHT_ARMOR = ITEMS.register("deep_sea_knight_armor", () -> new ItemDeepSeaKnightArmor(new Item.Properties()));
    public static final RegistryObject<Item> DEEP_SEA_KNIGHT_HELMET = ITEMS.register("deep_sea_knight_helmet", () -> new ItemDeepSeaKnightHelmet(new Item.Properties()));
    public static final RegistryObject<Item> DISCIPLINARIAN_ROBE = ITEMS.register("disciplinarian_robe", () -> new ItemDisciplinarianRobe(new Item.Properties()));
    public static final RegistryObject<Item> GUARDIAN_ANGEL = ITEMS.register("guardian_angel", () -> new ItemGuardianAngel(new Item.Properties()));
    public static final RegistryObject<Item> FRENZIED_KING_CLOAK = ITEMS.register("frenzied_king_cloak", () -> new ItemFrenziedKingCloak(new Item.Properties()));
    public static final RegistryObject<Item> HATTER_HAT = ITEMS.register("hatter_hat", () -> new ItemHatterHat(new Item.Properties()));
    public static final RegistryObject<Item> HUNTERS_ATTIRE = ITEMS.register("hunters_attire", () -> new ItemHuntersAttire(new Item.Properties()));
    public static final RegistryObject<Item> IGOR_MASK = ITEMS.register("igor_mask", () -> new ItemIgorMask(new Item.Properties()));
    public static final RegistryObject<Item> KNIGHT_ARMOR = ITEMS.register("knight_armor", () -> new ItemKnightArmor(new Item.Properties()));
    public static final RegistryObject<Item> KNIGHT_HELMET = ITEMS.register("knight_helmet", () -> new ItemKnightHelmet(new Item.Properties()));
    public static final RegistryObject<Item> LAWYER_MASK = ITEMS.register("lawyer_mask", () -> new ItemLawyerMask(new Item.Properties()));
    public static final RegistryObject<Item> LEATHER_ARMOR = ITEMS.register("leather_armor", () -> new ItemLeatherArmor(new Item.Properties()));
    public static final RegistryObject<Item> MATCH_GIRL_CLOTHES = ITEMS.register("match_girl_clothes", () -> new ItemMatchGirlClothes(new Item.Properties()));
    public static final RegistryObject<Item> MAGICIAN_COAT = ITEMS.register("magician_coat", () -> new ItemMagicianCoat(new Item.Properties()));
    public static final RegistryObject<Item> MAGICIAN_HAT = ITEMS.register("magician_hat", () -> new ItemMagicianHat(new Item.Properties()));
    public static final RegistryObject<Item> MILTON_ARMOR = ITEMS.register("milton_armor", () -> new ItemMiltonArmor(new Item.Properties()));
    public static final RegistryObject<Item> MILTON_HELMET = ITEMS.register("milton_helmet", () -> new ItemMiltonHelmet(new Item.Properties()));
    public static final RegistryObject<Item> MYSTERIOUS_HAT = ITEMS.register("mysterious_hat", () -> new ItemMysteriousHat(new Item.Properties()));
    public static final RegistryObject<Item> MYSTERY_OF_NIGHT_SKY = ITEMS.register("mystery_of_night_sky", () -> new ItemMysteryOfNightSky(new Item.Properties()));
    public static final RegistryObject<Item> NINJA_HEADBAND = ITEMS.register("ninja_headband", () -> new ItemNinjaHeadband(new Item.Properties()));
    public static final RegistryObject<Item> NOBLE_CLOTHES = ITEMS.register("noble_clothes", () -> new ItemNobleClothes(new Item.Properties()));
    public static final RegistryObject<Item> OMINOUS_CLOTHES = ITEMS.register("ominous_clothes", () -> new ItemOminousClothes(new Item.Properties()));
    public static final RegistryObject<Item> ONI_WARRIOR_ARMOR = ITEMS.register("oni_warrior_armor", () -> new ItemOniWarriorArmor(new Item.Properties()));
    public static final RegistryObject<Item> ONI_WARRIOR_HELMET = ITEMS.register("oni_warrior_helmet", () -> new ItemOniWarriorHelmet(new Item.Properties()));
    public static final RegistryObject<Item> PHANTOM_THIEF_CLOAK = ITEMS.register("phantom_thief_cloak", () -> new ItemPhantomThiefCloak(new Item.Properties()));
    public static final RegistryObject<Item> PLATE_ARMOR = ITEMS.register("plate_armor", () -> new ItemPlateArmor(new Item.Properties()));
    public static final RegistryObject<Item> PROSTITUTE_DRESS = ITEMS.register("prostitute_dress", () -> new ItemProstituteDress(new Item.Properties()));
    public static final RegistryObject<Item> RABBIT_EARS = ITEMS.register("rabbit_ears", () -> new ItemRabbitEars(new Item.Properties()));
    public static final RegistryObject<Item> SAILOR_SUIT = ITEMS.register("sailor_suit", () -> new ItemSailorSuit(new Item.Properties()));
    public static final RegistryObject<Item> SHADOW_ATTIRE = ITEMS.register("shadow_attire", () -> new ItemShadowAttire(new Item.Properties()));
    public static final RegistryObject<Item> SKY_KNIGHT_HAT = ITEMS.register("sky_knight_hat", () -> new ItemSkyKnightHat(new Item.Properties()));
    public static final RegistryObject<Item> SNAKE_DRESS = ITEMS.register("snake_dress", () -> new ItemSnakeDress(new Item.Properties()));
    public static final RegistryObject<Item> THIEF_MASK = ITEMS.register("thief_mask", () -> new ItemThiefMask(new Item.Properties()));
    public static final RegistryObject<Item> VIKING_HELMET = ITEMS.register("viking_helmet", () -> new ItemVikingHelmet(new Item.Properties()));
    public static final RegistryObject<Item> VIOLENT_CLOAK = ITEMS.register("violent_cloak", () -> new ItemViolentCloak(new Item.Properties()));
    public static final RegistryObject<Item> WARRIOR_ARMOR = ITEMS.register("warrior_armor", () -> new ItemWarriorArmor(new Item.Properties()));
    public static final RegistryObject<Item> WHITE_HAIRBAND = ITEMS.register("white_hairband", () -> new ItemWhiteHairband(new Item.Properties()));
    public static final RegistryObject<Item> GENTLEMAN_COAT = ITEMS.register("gentleman_coat", () -> new ItemGentlemanCoat(new Item.Properties()));
    // =========================================================================================================================================================================
    // 戒指
    // =========================================================================================================================================================================
    public static final RegistryObject<Item> RING_ABYSS = ITEMS.register("ring_abyss", () -> new ItemRingAbyss(new Item.Properties()));
    public static final RegistryObject<Item> RING_BLACKBEARD = ITEMS.register("ring_blackbeard", () -> new ItemRingBlackbeard(new Item.Properties()));
    public static final RegistryObject<Item> RING_BLACK_RABBIT = ITEMS.register("ring_black_rabbit", () -> new ItemRingBlackRabbit(new Item.Properties()));
    public static final RegistryObject<Item> RING_BLADES = ITEMS.register("ring_blades", () -> new ItemRingBlades(new Item.Properties()));
    public static final RegistryObject<Item> RING_CAT = ITEMS.register("ring_cat", () -> new ItemRingCat(new Item.Properties()));
    public static final RegistryObject<Item> RING_DEATH = ITEMS.register("ring_death", () -> new ItemRingDeath(new Item.Properties()));
    public static final RegistryObject<Item> RING_FRAGILE = ITEMS.register("ring_fragile", () -> new ItemRingFragile(new Item.Properties()));
    public static final RegistryObject<Item> RING_GOD_FISH = ITEMS.register("ring_god_fish", () -> new ItemRingGodFish(new Item.Properties()));
    public static final RegistryObject<Item> RING_GOLD_SERPENT = ITEMS.register("ring_gold_serpent", () -> new ItemRingGoldSerpent(new Item.Properties()));
    public static final RegistryObject<Item> RING_SILVER_SERPENT = ITEMS.register("ring_silver_serpent", () -> new ItemRingSilverSerpent(new Item.Properties()));
    public static final RegistryObject<Item> RING_EVIL_EYE = ITEMS.register("ring_evil_eye", () -> new ItemRingEvilEye(new Item.Properties()));
    public static final RegistryObject<Item> RING_IRON_MAIDEN = ITEMS.register("ring_iron_maiden", () -> new ItemRingIronMaiden(new Item.Properties()));
    public static final RegistryObject<Item> RING_IRON_PROTECTION = ITEMS.register("ring_iron_protection", () -> new ItemRingIronProtection(new Item.Properties()));
    public static final RegistryObject<Item> RING_GUARD = ITEMS.register("ring_guard", () -> new ItemRingGuard(new Item.Properties()));
    public static final RegistryObject<Item> RING_TERROR = ITEMS.register("ring_terror", () -> new ItemRingTerror(new Item.Properties()));
    public static final RegistryObject<Item> RING_FAIRY = ITEMS.register("ring_fairy", () -> new ItemRingFairy(new Item.Properties()));
    public static final RegistryObject<Item> RING_LIEF = ITEMS.register("ring_lief", () -> new ItemRingLief(new Item.Properties()));
    public static final RegistryObject<Item> RING_LIFE = ITEMS.register("life_ring", () -> new ItemRingLife(new Item.Properties()));
    public static final RegistryObject<Item> RING_POISON_BITE = ITEMS.register("ring_poison_bite", () -> new ItemRingPoisonBite(new Item.Properties()));
    public static final RegistryObject<Item> RING_BLOOD_BITE = ITEMS.register("ring_blood_bite", () -> new ItemRingBloodBite(new Item.Properties()));
    public static final RegistryObject<Item> RING_FIRE_STONE = ITEMS.register("ring_fire_stone", () -> new ItemRingFireStone(new Item.Properties()));
    public static final RegistryObject<Item> RING_THUNDER_STONE = ITEMS.register("ring_thunder_stone", () -> new ItemRingThunderStone(new Item.Properties()));
    public static final RegistryObject<Item> RING_DARK_STONE = ITEMS.register("ring_dark_stone", () -> new ItemRingDarkStone(new Item.Properties()));
    public static final RegistryObject<Item> RING_MAGIC_STONE = ITEMS.register("ring_magic_stone", () -> new ItemRingMagicStone(new Item.Properties()));
    public static final RegistryObject<Item> RING_WIND_GOD = ITEMS.register("ring_wind_god", () -> new ItemRingWindGod(new Item.Properties()));
    public static final RegistryObject<Item> RING_SPELL = ITEMS.register("ring_spell", () -> new ItemRingSpell(new Item.Properties()));
    public static final RegistryObject<Item> RING_MASOCHIST = ITEMS.register("ring_masochist", () -> new ItemRingMasochist(new Item.Properties()));
    public static final RegistryObject<Item> RING_DRAGON_GUARD = ITEMS.register("ring_dragon_guard", () -> new ItemRingDragonGuard(new Item.Properties()));
    public static final RegistryObject<Item> RING_MIDNIGHT_CROWN = ITEMS.register("ring_midnight_crown", () -> new ItemRingMidnightCrown(new Item.Properties()));
    public static final RegistryObject<Item> RING_VOID = ITEMS.register("ring_void", () -> new ItemRingVoid(new Item.Properties()));
    public static final RegistryObject<Item> RING_WHITE_RABBIT = ITEMS.register("ring_white_rabbit", () -> new ItemRingWhiteRabbit(new Item.Properties()));
    public static final RegistryObject<Item> RING_RESURRECTOR = ITEMS.register("ring_resurrector", () -> new ItemRingResurrector(new Item.Properties()));
    public static final RegistryObject<Item> RING_WASP = ITEMS.register("ring_wasp", () -> new ItemRingWasp(new Item.Properties()));
    public static final RegistryObject<Item> RING_GODDESS = ITEMS.register("ring_goddess", () -> new ItemRingGoddess(new Item.Properties()));
    public static final RegistryObject<Item> RING_PUYO = ITEMS.register("ring_puyo", () -> new ItemRingPuyo(new Item.Properties()));
    public static final RegistryObject<Item> RING_HUNYA = ITEMS.register("ring_hunya", () -> new ItemRingHunya(new Item.Properties()));
    public static final RegistryObject<Item> RING_KNIGHT = ITEMS.register("ring_knight", () -> new ItemRingKnight(new Item.Properties()));
    public static final RegistryObject<Item> RING_ANGEL = ITEMS.register("ring_angel", () -> new ItemRingAngel(new Item.Properties()));
    // =========================================================================================================================================================================
    // 武器/盾
    // =========================================================================================================================================================================
    public static final RegistryObject<Item> ANDOR_SWORD = ITEMS.register("andor_sword", () -> new ItemAndorSword(new Item.Properties()));
    public static final RegistryObject<Item> DRAKE_SWORD = ITEMS.register("drake_sword", () -> new ItemDrakeSword(new Item.Properties()));
    public static final RegistryObject<Item> KNIGHT_SWORD = ITEMS.register("knight_sword", () -> new ItemKnightSword(new Item.Properties()));
    public static final RegistryObject<Item> MURDERERS_SHOTGUN = ITEMS.register("murderers_shotgun", () -> new ItemMurderersShotgun(new Item.Properties()));
    public static final RegistryObject<Item> VORPAL_BLADE = ITEMS.register("vorpal_blade", () -> new ItemVorpalBlade(new Item.Properties()));
    public static final RegistryObject<Item> BRAVE_SWORD_VORPAL = ITEMS.register("brave_sword_vorpal", () -> new ItemBraveSwordVorpal(new Item.Properties()));
    public static final RegistryObject<Item> KNIGHT_KING_SWORD = ITEMS.register("knight_king_sword", () -> new ItemKnightKingSword(new Item.Properties()));
    public static final RegistryObject<Item> THIEFS_DAGGER = ITEMS.register("thiefs_dagger", () -> new ItemThiefsDagger(new Item.Properties()));
    public static final RegistryObject<Item> GREAT_THIEFS_DAGGER = ITEMS.register("great_thiefs_dagger", () -> new ItemGreatThiefsDagger(new Item.Properties()));
    public static final RegistryObject<Item> GREAT_SWORD = ITEMS.register("great_sword", () -> new ItemGreatSword(new Item.Properties()));
    public static final RegistryObject<Item> GIANT_SWORD = ITEMS.register("giant_sword", () -> new ItemGiantSword(new Item.Properties()));
    public static final RegistryObject<Item> BROAD_SPEAR = ITEMS.register("broad_spear", () -> new ItemBroadSpear(new Item.Properties()));
    public static final RegistryObject<Item> GUNGNIR = ITEMS.register("gungnir", () -> new ItemGungnir(new Item.Properties()));
    public static final RegistryObject<Item> BANDERSNATCH_SWORD = ITEMS.register("bandersnatch_sword", () -> new ItemBandersnatchSword(new Item.Properties()));
    public static final RegistryObject<Item> VORPAL_SWORD = ITEMS.register("vorpal_sword", () -> new ItemVorpalSword(new Item.Properties()));
    public static final RegistryObject<Item> CLUB = ITEMS.register("club", () -> new ItemClub(new Item.Properties()));
    public static final RegistryObject<Item> KING_CLUB = ITEMS.register("king_club", () -> new ItemKingClub(new Item.Properties()));
    // =========================================================================================================================================================================
    // 技能书
    // =========================================================================================================================================================================
    public static final RegistryObject<Item> SKILL_BOOK_GRIT = ITEMS.register("skill_book_grit", () -> new ItemSkillBookGrit(new Item.Properties()));
    public static final RegistryObject<Item> SKILL_BOOK_INVISIBLE = ITEMS.register("skill_book_invisible", () -> new ItemSkillBookInvisible(new Item.Properties()));
    public static final RegistryObject<Item> SKILL_BOOK_REQUIEM = ITEMS.register("skill_book_requiem", () -> new ItemSkillBookRequiem(new Item.Properties()));
    public static final RegistryObject<Item> SKILL_BOOK_SOUL_ARROW = ITEMS.register("skill_book_soul_arrow", () -> new ItemSkillBookSoulArrow(new Item.Properties()));
    public static final RegistryObject<Item> SKILL_BOOK_SOUL_LIGHT = ITEMS.register("skill_book_soul_light", () -> new ItemSkillBookSoulLight(new Item.Properties()));
    public static final RegistryObject<Item> SKILL_BOOK_SOUL_RADIATION = ITEMS.register("skill_book_soul_radiation", () -> new ItemSkillBookSoulRadiation(new Item.Properties()));
    public static final RegistryObject<Item> SKILL_BOOK_CARRHUS_BLOOD_CURSE = ITEMS.register("skill_book_carthus_blood_curse", () -> new ItemSkillBookCarthusBloodCurse(new Item.Properties()));
    // =============================================================================================================================
    // 经验魂(共13种)
    // =============================================================================================================================
    public static final RegistryObject<Item> SOUL_FADING = ITEMS.register("soul_fading",
            () -> new ItemSoul(new Item.Properties(), 50, "lore.blacksouls.soul_fading"));

    public static final RegistryObject<Item> SOUL_LOST_UNDEAD = ITEMS.register("soul_lost_undead",
            () -> new ItemSoul(new Item.Properties(), 200, "lore.blacksouls.soul_lost_undead"));

    public static final RegistryObject<Item> SOUL_LOST_UNDEAD_LARGE = ITEMS.register("soul_lost_undead_large",
            () -> new ItemSoul(new Item.Properties(), 400, "lore.blacksouls.soul_lost_undead_large"));

    public static final RegistryObject<Item> SOUL_NAMELESS_TRAVELER = ITEMS.register("soul_nameless_traveler",
            () -> new ItemSoul(new Item.Properties(), 800, "lore.blacksouls.soul_nameless_traveler"));

    public static final RegistryObject<Item> SOUL_NAMELESS_TRAVELER_LARGE = ITEMS.register("soul_nameless_traveler_large",
            () -> new ItemSoul(new Item.Properties(), 1000, "lore.blacksouls.soul_nameless_traveler_large"));

    public static final RegistryObject<Item> SOUL_NAMELESS_SOLDIER = ITEMS.register("soul_nameless_soldier",
            () -> new ItemSoul(new Item.Properties(), 2000, "lore.blacksouls.soul_nameless_soldier"));

    public static final RegistryObject<Item> SOUL_NAMELESS_SOLDIER_LARGE = ITEMS.register("soul_nameless_soldier_large",
            () -> new ItemSoul(new Item.Properties(), 3000, "lore.blacksouls.soul_nameless_soldier_large"));

    public static final RegistryObject<Item> SOUL_EXHAUSTED_WARRIOR = ITEMS.register("soul_exhausted_warrior",
            () -> new ItemSoul(new Item.Properties(), 5000, "lore.blacksouls.soul_exhausted_warrior"));

    public static final RegistryObject<Item> SOUL_EXHAUSTED_WARRIOR_LARGE = ITEMS.register("soul_exhausted_warrior_large",
            () -> new ItemSoul(new Item.Properties(), 8000, "lore.blacksouls.soul_exhausted_warrior_large"));

    public static final RegistryObject<Item> SOUL_CRESTFALLEN_KNIGHT = ITEMS.register("soul_crestfallen_knight",
            () -> new ItemSoul(new Item.Properties(), 10000, "lore.blacksouls.soul_crestfallen_knight"));

    public static final RegistryObject<Item> SOUL_CRESTFALLEN_KNIGHT_LARGE = ITEMS.register("soul_crestfallen_knight_large",
            () -> new ItemSoul(new Item.Properties(), 20000, "lore.blacksouls.soul_crestfallen_knight_large"));

    public static final RegistryObject<Item> SOUL_HERO = ITEMS.register("soul_hero",
            () -> new ItemSoul(new Item.Properties(), 25000, "lore.blacksouls.soul_hero"));

    public static final RegistryObject<Item> SOUL_GREAT_HERO = ITEMS.register("soul_great_hero",
            () -> new ItemSoul(new Item.Properties(), 50000, "lore.blacksouls.soul_great_hero"));
    // ==========================================================================================================================================================================================================================================================================
    // 属性魂(共11种)
    // ==========================================================================================================================================================================================================================================================================
    public static final RegistryObject<Item> SOUL_GREEN = ITEMS.register("soul_green",
            () -> new ItemStatSoul(new Item.Properties(), 20, 0, 0, 0, 0,0, 0, 0));
    public static final RegistryObject<Item> SOUL_PURPLE = ITEMS.register("soul_purple",
            () -> new ItemStatSoul(new Item.Properties(), 0, 10, 0, 0, 0, 0, 0,0));
    public static final RegistryObject<Item> SOUL_RED = ITEMS.register("soul_red",
            () -> new ItemStatSoul(new Item.Properties(), 0, 0, 2, 0, 0, 0, 0,0));
    public static final RegistryObject<Item> SOUL_BLUE = ITEMS.register("soul_blue",
            () -> new ItemStatSoul(new Item.Properties(), 0, 0, 0, 2, 0, 0, 0,0));
    public static final RegistryObject<Item> SOUL_YELLOW = ITEMS.register("soul_yellow",
            () -> new ItemStatSoul(new Item.Properties(), 0, 0, 0, 0, 0, 0, 0,2));
    public static final RegistryObject<Item> SOUL_GRAY = ITEMS.register("soul_gray",
            () -> new ItemStatSoul(new Item.Properties(), 0, 0, 0, 0, 2, 0, 0,0));
    public static final RegistryObject<Item> SOUL_WHITE = ITEMS.register("soul_white",
            () -> new ItemStatSoul(new Item.Properties(), 0, 0, 0, 0, 0, 2, 0,0));
    public static final RegistryObject<Item> SOUL_FOUR_LEAF_CLOVER = ITEMS.register("soul_four_leaf_clover",
            () -> new ItemStatSoul(new Item.Properties(), 0, 0, 0, 0, 0, 0, 2,0));
    public static final RegistryObject<Item> SOUL_BLACK = ITEMS.register("soul_black",
            () -> new ItemStatSoul(new Item.Properties(), 10, 5, 1, 1, 1, 1, 1,1));
    public static final RegistryObject<Item> SOUL_BLACK_DEFILED = ITEMS.register("soul_black_defiled",
            () -> new ItemStatSoul(new Item.Properties(), 30, 15, 3, 3, 3, 3, 3,3));
    public static final RegistryObject<Item> SOUL_OUTSIDER = ITEMS.register("soul_outsider",
            () -> new ItemStatSoul(new Item.Properties(), 300, 150, 30, 30, 30, 30,30,30));
    // ==========================================================================================================================================================================================================================================================================
    // 童话书系列 (共32本)
    // ==========================================================================================================================================================================================================================================================================
    public static final RegistryObject<Item> BOOK_RASCAL = ITEMS.register("book_rascal", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_rascal.1", "lore.blacksouls.book_rascal.2"));
    public static final RegistryObject<Item> BOOK_FOX_AND_GRAPES = ITEMS.register("book_fox_and_grapes", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_fox_and_grapes.1", "lore.blacksouls.book_fox_and_grapes.2"));
    public static final RegistryObject<Item> BOOK_UGLY_DUCKLING = ITEMS.register("book_ugly_duckling", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_ugly_duckling.1", "lore.blacksouls.book_ugly_duckling.2"));
    public static final RegistryObject<Item> BOOK_HIGH_JUMPER = ITEMS.register("book_high_jumper", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_high_jumper.1", "lore.blacksouls.book_high_jumper.2"));
    public static final RegistryObject<Item> BOOK_WOLF_AND_GOATS = ITEMS.register("book_wolf_and_goats", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_wolf_and_goats.1", "lore.blacksouls.book_wolf_and_goats.2"));
    public static final RegistryObject<Item> BOOK_HANSEL_AND_GRETEL = ITEMS.register("book_hansel_and_gretel", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_hansel_and_gretel.1", "lore.blacksouls.book_hansel_and_gretel.2"));
    public static final RegistryObject<Item> BOOK_SINBAD = ITEMS.register("book_sinbad", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_sinbad.1", "lore.blacksouls.book_sinbad.2"));
    public static final RegistryObject<Item> BOOK_BREMEN_MUSICIANS = ITEMS.register("book_bremen_musicians", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_bremen_musicians.1", "lore.blacksouls.book_bremen_musicians.2"));
    public static final RegistryObject<Item> BOOK_IRON_HANS = ITEMS.register("book_iron_hans", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_iron_hans.1", "lore.blacksouls.book_iron_hans.2"));
    public static final RegistryObject<Item> BOOK_DOG_OF_FLANDERS = ITEMS.register("book_dog_of_flanders", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_dog_of_flanders.1", "lore.blacksouls.book_dog_of_flanders.2"));
    public static final RegistryObject<Item> BOOK_LITTLE_PRINCE = ITEMS.register("book_little_prince", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_little_prince.1", "lore.blacksouls.book_little_prince.2"));
    public static final RegistryObject<Item> BOOK_ARMORED_KNIGHT = ITEMS.register("book_armored_knight", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_armored_knight.1", "lore.blacksouls.book_armored_knight.2"));
    public static final RegistryObject<Item> BOOK_DONKEY_EARS_KING = ITEMS.register("book_donkey_ears_king", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_donkey_ears_king.1", "lore.blacksouls.book_donkey_ears_king.2"));
    public static final RegistryObject<Item> BOOK_PETER_PAN = ITEMS.register("book_peter_pan", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_peter_pan.1", "lore.blacksouls.book_peter_pan.2"));
    public static final RegistryObject<Item> BOOK_MONKEY_AND_CRAB = ITEMS.register("book_monkey_and_crab", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_monkey_and_crab.1", "lore.blacksouls.book_monkey_and_crab.2"));
    public static final RegistryObject<Item> BOOK_WIZARD_OF_OZ = ITEMS.register("book_wizard_of_oz", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_wizard_of_oz.1", "lore.blacksouls.book_wizard_of_oz.2"));
    public static final RegistryObject<Item> BOOK_MATCH_GIRL = ITEMS.register("book_match_girl", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_match_girl.1", "lore.blacksouls.book_match_girl.2"));
    public static final RegistryObject<Item> BOOK_GOLDEN_GOOSE = ITEMS.register("book_golden_goose", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_golden_goose.1", "lore.blacksouls.book_golden_goose.2"));
    public static final RegistryObject<Item> BOOK_GREEDY_DOG = ITEMS.register("book_greedy_dog", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_greedy_dog.1", "lore.blacksouls.book_greedy_dog.2"));
    public static final RegistryObject<Item> BOOK_PULL_TURNIP = ITEMS.register("book_pull_turnip", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_pull_turnip.1", "lore.blacksouls.book_pull_turnip.2"));
    public static final RegistryObject<Item> BOOK_KACHI_KACHI_YAMA = ITEMS.register("book_kachi_kachi_yama", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_kachi_kachi_yama.1", "lore.blacksouls.book_kachi_kachi_yama.2"));
    public static final RegistryObject<Item> BOOK_INABA_BLACK_RABBIT = ITEMS.register("book_inaba_black_rabbit", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_inaba_black_rabbit.1", "lore.blacksouls.book_inaba_black_rabbit.2"));
    public static final RegistryObject<Item> BOOK_ROBIN_HOOD = ITEMS.register("book_robin_hood", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_robin_hood.1", "lore.blacksouls.book_robin_hood.2"));
    public static final RegistryObject<Item> BOOK_BLUEBEARD = ITEMS.register("book_bluebeard", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_bluebeard.1", "lore.blacksouls.book_bluebeard.2"));
    public static final RegistryObject<Item> BOOK_DADDY_LONG_LEGS = ITEMS.register("book_daddy_long_legs", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_daddy_long_legs.1", "lore.blacksouls.book_daddy_long_legs.2"));
    public static final RegistryObject<Item> BOOK_BOY_WHO_CRIED_WOLF = ITEMS.register("book_boy_who_cried_wolf", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_boy_who_cried_wolf.1", "lore.blacksouls.book_boy_who_cried_wolf.2"));
    public static final RegistryObject<Item> BOOK_WINNIE_THE_POOH = ITEMS.register("book_winnie_the_pooh", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_winnie_the_pooh.1", "lore.blacksouls.book_winnie_the_pooh.2"));
    public static final RegistryObject<Item> BOOK_PINOCCHIO = ITEMS.register("book_pinocchio", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_pinocchio.1", "lore.blacksouls.book_pinocchio.2"));
    public static final RegistryObject<Item> BOOK_NIGHTINGALE = ITEMS.register("book_nightingale", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_nightingale.1", "lore.blacksouls.book_nightingale.2"));
    public static final RegistryObject<Item> BOOK_BLANK = ITEMS.register("book_blank", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_blank.1"));
    public static final RegistryObject<Item> BOOK_SNOW_QUEEN = ITEMS.register("book_snow_queen", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_snow_queen.1", "lore.blacksouls.book_snow_queen.2"));
    public static final RegistryObject<Item> BOOK_SNOW_MAIDEN = ITEMS.register("book_snow_maiden", () -> new ItemFairyTaleBook(new Item.Properties(), "lore.blacksouls.book_snow_maiden.1"));
    // ===============================================================================================================================================================================
    // Boss灵魂系列(共29种)- 使用双行构造器
    // ===============================================================================================================================================================================
    public static final RegistryObject<Item> SOUL_SKULL_BEAST = ITEMS.register("soul_skull_beast",
            () -> new ItemSoul(new Item.Properties(), 1000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_skull_beast"));
    public static final RegistryObject<Item> SOUL_KNIGHT_OF_HEARTS = ITEMS.register("soul_knight_of_hearts",
            () -> new ItemSoul(new Item.Properties(), 2000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_knight_of_hearts"));
    public static final RegistryObject<Item> SOUL_KNIGHT_OF_SPADES = ITEMS.register("soul_knight_of_spades",
            () -> new ItemSoul(new Item.Properties(), 2000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_knight_of_spades"));
    public static final RegistryObject<Item> SOUL_KNIGHT_OF_CLUBS = ITEMS.register("soul_knight_of_clubs",
            () -> new ItemSoul(new Item.Properties(), 2000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_knight_of_clubs"));
    public static final RegistryObject<Item> SOUL_BOREDOM = ITEMS.register("soul_boredom",
            () -> new ItemSoul(new Item.Properties(), 2000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_boredom"));
    public static final RegistryObject<Item> SOUL_PREGNANT_WOMAN = ITEMS.register("soul_pregnant_woman",
            () -> new ItemSoul(new Item.Properties(), 3000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_pregnant_woman"));
    public static final RegistryObject<Item> SOUL_OLD_KNIGHT = ITEMS.register("soul_old_knight",
            () -> new ItemSoul(new Item.Properties(), 3000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_old_knight"));
    public static final RegistryObject<Item> SOUL_BELL_CALLER = ITEMS.register("soul_bell_caller",
            () -> new ItemSoul(new Item.Properties(), 5000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_bell_caller"));
    public static final RegistryObject<Item> SOUL_BEAST_PELT = ITEMS.register("soul_beast_pelt",
            () -> new ItemSoul(new Item.Properties(), 5000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_beast_pelt"));
    public static final RegistryObject<Item> SOUL_GREAT_EAGLE = ITEMS.register("soul_great_eagle",
            () -> new ItemSoul(new Item.Properties(), 5000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_great_eagle"));
    public static final RegistryObject<Item> SOUL_JACK = ITEMS.register("soul_jack",
            () -> new ItemSoul(new Item.Properties(), 8000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_jack"));
    public static final RegistryObject<Item> SOUL_DORM_HEAD = ITEMS.register("soul_dorm_head",
            () -> new ItemSoul(new Item.Properties(), 8000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_dorm_head"));
    public static final RegistryObject<Item> SOUL_NARCISSIST = ITEMS.register("soul_narcissist",
            () -> new ItemSoul(new Item.Properties(), 10000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_narcissist"));
    public static final RegistryObject<Item> SOUL_SHINING_STAR = ITEMS.register("soul_shining_star",
            () -> new ItemSoul(new Item.Properties(), 10000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_shining_star"));
    public static final RegistryObject<Item> SOUL_GIANT_HOUSE = ITEMS.register("soul_giant_house",
            () -> new ItemSoul(new Item.Properties(), 20000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_giant_house"));
    public static final RegistryObject<Item> SOUL_SLAVE_EMPEROR = ITEMS.register("soul_slave_emperor",
            () -> new ItemSoul(new Item.Properties(), 20000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_slave_emperor"));
    public static final RegistryObject<Item> SOUL_SLAVE_QUEEN = ITEMS.register("soul_slave_queen",
            () -> new ItemSoul(new Item.Properties(), 20000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_slave_queen"));
    public static final RegistryObject<Item> SOUL_TORTURE_QUEEN = ITEMS.register("soul_torture_queen",
            () -> new ItemSoul(new Item.Properties(), 10000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_torture_queen"));
    public static final RegistryObject<Item> SOUL_BANDERSNATCH = ITEMS.register("soul_bandersnatch",
            () -> new ItemSoul(new Item.Properties(), 20000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_bandersnatch"));
    public static final RegistryObject<Item> SOUL_JUBJUB = ITEMS.register("soul_jubjub",
            () -> new ItemSoul(new Item.Properties(), 20000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_jubjub"));
    public static final RegistryObject<Item> SOUL_JABBERWOCK = ITEMS.register("soul_jabberwock",
            () -> new ItemSoul(new Item.Properties(), 20000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_jabberwock"));
    public static final RegistryObject<Item> SOUL_DIVINE_FISH = ITEMS.register("soul_divine_fish",
            () -> new ItemSoul(new Item.Properties(), 50000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_divine_fish"));
    public static final RegistryObject<Item> SOUL_DEEP_SEA_KNIGHT = ITEMS.register("soul_deep_sea_knight",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_deep_sea_knight"));
    public static final RegistryObject<Item> SOUL_EVIL_DRAGON_HUNTER = ITEMS.register("soul_evil_dragon_hunter",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_evil_dragon_hunter"));
    public static final RegistryObject<Item> SOUL_APPOINTED_WET_NURSE = ITEMS.register("soul_appointed_wet_nurse",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_appointed_wet_nurse"));
    public static final RegistryObject<Item> SOUL_FLORENCE = ITEMS.register("soul_florence",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_florence"));
    public static final RegistryObject<Item> SOUL_WINTER_BELL_WIND = ITEMS.register("soul_winter_bell_wind",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_winter_bell_wind"));
    public static final RegistryObject<Item> SOUL_WHITE_UNICORN = ITEMS.register("soul_white_unicorn",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_white_unicorn"));
    public static final RegistryObject<Item> SOUL_WHITE_LION = ITEMS.register("soul_white_lion",
            () -> new ItemSoul(new Item.Properties(), 25000, "tooltip.blacksouls.soul_effect", "lore.blacksouls.soul_white_lion"));
    // ===============================================================================================================================================================================
    // 开发者物品
    // ===============================================================================================================================================================================
    public static final RegistryObject<Item> DEV_STAT_TOOL = ITEMS.register("dev_stat_tool", () -> new ItemDevTool(new Item.Properties()));
    public static final RegistryObject<Item> DEV_TIME_RING = ITEMS.register("dev_time_ring", () -> new ItemBaubleBase(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> DEV_REVENGE_MODE = ITEMS.register("dev_revenge_mode", () -> new ItemDevDifficultyMode(new Item.Properties(), ItemDevDifficultyMode.ModeType.REVENGE));
    public static final RegistryObject<Item> DEV_DEATH_MODE = ITEMS.register("dev_death_mode", () -> new ItemDevDifficultyMode(new Item.Properties(), ItemDevDifficultyMode.ModeType.DEATH));
    public static final RegistryObject<Item> DEV_LEGENDARY_MODE = ITEMS.register("dev_legendary_mode", () -> new ItemDevDifficultyMode(new Item.Properties(), ItemDevDifficultyMode.ModeType.LEGENDARY));
    public static final RegistryObject<Item> DEV_MALICE_MODE = ITEMS.register("dev_malice_mode", () -> new ItemDevDifficultyMode(new Item.Properties(), ItemDevDifficultyMode.ModeType.MALICE));
    public static final RegistryObject<Item> DEV_ETERNITY_MODE = ITEMS.register("dev_eternity_mode", () -> new ItemDevDifficultyMode(new Item.Properties(), ItemDevDifficultyMode.ModeType.ETERNITY));
    //public static final RegistryObject<Item> ULTIMATE_NANA_SWORD = ITEMS.register("ultimate_nana_sword", () -> new ItemUltimateNanaSword(new Item.Properties()));
    public static final RegistryObject<Item> KING_EXIT_AVATAR_PACK = ITEMS.register(
            "king_exit_avatar_pack",
            () -> new ItemKingExitAvatarPack(
                    new Item.Properties(),
                    List.of(
                            "guine_sheet",
                            "guine_crest_sheet",
                            "guine_prisoner_sheet",
                            "guine_king_sheet",
                            "georuise_sheet",
                            "georuise_2_sheet",
                            "stiara_sheet",
                            "stiara_2_sheet",
                            "stiara_3_sheet",
                            "stiara_4_sheet",
                            "senpai_sheet",
                            "samidare_nin_sheet",
                            "samidare_spider_sheet"
                    )
            )
    );

    @SuppressWarnings("unused")
    public static final RegistryObject<Item> DEMON_ROOTS_AVATAR_PACK = ITEMS.register(
            "demon_roots_avatar_pack",
            () -> new ItemDemonRootsAvatarPack(
                    new Item.Properties(),
                    List.of(
                            "anju_mz_sheet",
                            "dai_mz_sheet",
                            "hime_mz_sheet",
                            "karin_mz_sheet",
                            "liliy_mz_sheet",
                            "naje_mz_sheet",
                            "poryu_mz_sheet",
                            "sara_mz_sheet",
                            "syoujo2_mz_sheet"
                    )
            )
    );
    // =================================================================================================
    // 实体刷怪蛋
    // =================================================================================================
    public static final RegistryObject<Item> NODEN_SPAWN_EGG = ITEMS.register("noden_spawn_egg",
            () -> new ItemNodenSpawn(new Item.Properties().stacksTo(16)));
    //创造模式专属物品栏

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> BLACK_SOULS_TAB = CREATIVE_MODE_TABS.register("blacksouls_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(SOUL_BLACK.get())) // 用黑之魂作为图标
                    .title(Component.translatable("itemGroup.blacksouls_entity"))
                    .displayItems((parameters, output) -> {
                        output.accept(NODEN_SPAWN_EGG.get());              // 诺登召唤
                    })
                    .build());

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> BLACK_SOULS_WEAPON_TAB = CREATIVE_MODE_TABS.register("blacksouls_weapon",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ANDOR_SWORD.get()))
                    .title(Component.translatable("itemGroup.blacksouls_weapon"))
                    .displayItems((parameters, output) -> {
                        // ===========================================================
                        // 武器/盾
                        // ===========================================================
                        output.accept(ANDOR_SWORD.get());                  // 安多鲁之剑
                        output.accept(DRAKE_SWORD.get());                  // 飞龙剑
                        output.accept(KNIGHT_SWORD.get());                 // 骑士之剑
                        output.accept(KNIGHT_KING_SWORD.get());            // 骑士王之剑
                        output.accept(VORPAL_BLADE.get());                 // 沃柏尔之刃
                        output.accept(MURDERERS_SHOTGUN.get());            // 杀人魔霰弹枪
                        output.accept(BRAVE_SWORD_VORPAL.get());           // 勇剑沃柏尔
                        output.accept(THIEFS_DAGGER.get());                // 盗贼短刀
                        output.accept(GREAT_THIEFS_DAGGER.get());          // 大盗贼的短刀
                        output.accept(GREAT_SWORD.get());                  // 大剑
                        output.accept(GIANT_SWORD.get());                  // 巨人剑
                        output.accept(BROAD_SPEAR.get());                  // 阔头枪
                        output.accept(GUNGNIR.get());                      // 冈格尼尔
                        output.accept(BANDERSNATCH_SWORD.get());           // 暴剑班达斯奈奇
                        output.accept(VORPAL_SWORD.get());                 // 沃柏尔之剑
                        output.accept(CLUB.get());                         // 棍棒
                        output.accept(KING_CLUB.get());                    // 王棒
                    })
                    .build());

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> BLACK_SOULS_ACCESSORY_TAB = CREATIVE_MODE_TABS.register("blacksouls_accessory",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(CLERIC_CIRCLET.get()))
                    .title(Component.translatable("itemGroup.blacksouls_accessory"))
                    .displayItems((parameters, output) -> {
                        // ==========================================================================
                        // 头饰+胸饰
                        // ==========================================================================
                        output.accept(NOBLE_CLOTHES.get());                // 贵族之服
                        output.accept(VIOLENT_CLOAK.get());                // 暴力的披风
                        output.accept(FRENZIED_KING_CLOAK.get());          // 狂乱王的披风
                        output.accept(ANGEL_RAIMENT.get());                // 天使的羽衣
                        output.accept(LEATHER_ARMOR.get());                // 皮甲
                        output.accept(HUNTERS_ATTIRE.get());               // 猎人的装束
                        output.accept(MATCH_GIRL_CLOTHES.get());           // 卖火柴的衣服
                        // 作业服
                        output.accept(GENTLEMAN_COAT.get());               // 绅士外套
                        output.accept(PROSTITUTE_DRESS.get());             // 娼妇之服
                        output.accept(PLATE_ARMOR.get());                  // 板甲
                        // 深渊之铠
                        output.accept(ARMOR_OF_THE_SUN.get());             // 太阳之铠
                        output.accept(CLERIC_VESTMENT.get());              // 圣职者的装衣
                        output.accept(MAGICIAN_COAT.get());                // 魔术师的外套
                        output.accept(SHADOW_ATTIRE.get());                // 影之装衣
                        output.accept(KNIGHT_ARMOR.get());                 // 骑士之铠
                        output.accept(WARRIOR_ARMOR.get());                // 战士之铠
                        output.accept(MILTON_ARMOR.get());                 // 弥尔顿之铠
                        output.accept(BABEL_TOWER_ARMOR.get());            // 巴别塔之铠
                        output.accept(PHANTOM_THIEF_CLOAK.get());          // 怪盗的披风
                        output.accept(CLERIC_CIRCLET.get());               // 圣职者的头环
                        output.accept(MAGICIAN_HAT.get());                 // 魔术师的帽子
                        output.accept(THIEF_MASK.get());                   // 盗贼面具
                        output.accept(KNIGHT_HELMET.get());                // 骑士头盔
                        output.accept(VIKING_HELMET.get());                // 维京头盔
                        output.accept(MILTON_HELMET.get());                // 弥尔顿之盔
                        output.accept(RABBIT_EARS.get());                  // 兔耳
                        output.accept(WHITE_HAIRBAND.get());               // 白发带
                        output.accept(BABEL_TOWER_HELMET.get());           // 巴别塔头盔
                        output.accept(NINJA_HEADBAND.get());               // 忍者护额
                        output.accept(MYSTERIOUS_HAT.get());               // 不可思议的帽子
                        output.accept(HATTER_HAT.get());                   // 帽子屋的帽子
                        output.accept(SKY_KNIGHT_HAT.get());               // 空骑士的帽子
                        output.accept(LAWYER_MASK.get());                  // 弁护士的面具
                        output.accept(IGOR_MASK.get());                    // 伊戈尔的面具
                        output.accept(BUNNY_GIRL_UNIFORM.get());           // 兔女郎制服
                        output.accept(DEEP_SEA_KNIGHT_HELMET.get());       // 深海骑士之盔
                        output.accept(DEEP_SEA_KNIGHT_ARMOR.get());        // 深海骑士之铠
                        output.accept(CREW_HEADSCARF.get());               // 船员的包头巾
                        output.accept(ONI_WARRIOR_HELMET.get());           // 鬼武者之盔
                        output.accept(ONI_WARRIOR_ARMOR.get());            // 鬼武者之铠
                        output.accept(SAILOR_SUIT.get());                  // 船员服
                        output.accept(SNAKE_DRESS.get());                  // 毒蛇之服
                        output.accept(DISCIPLINARIAN_ROBE.get());          // 教戒师的礼袍
                        output.accept(OMINOUS_CLOTHES.get());              // 不吉的上衣
                        output.accept(BUTETSU_ARMOR.get());                // 武铁之铠
                        // 黄之布
                        output.accept(GUARDIAN_ANGEL.get());               // 守护天使
                        // 剧作家的头巾
                        // 伪天使的花冠
                        output.accept(MYSTERY_OF_NIGHT_SKY.get());         // 夜空的神秘
                        // 冬魔导士的外套
                        // 冬骑士之铠
                        // 冬骑士之盔
                        // 无风之衣
                        // 奇迹的巫女装束
                    })
                    .build());

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> BLACK_SOULS_RING_TAB = CREATIVE_MODE_TABS.register("blacksouls_ring",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(RING_LIFE.get()))
                    .title(Component.translatable("itemGroup.blacksouls_ring"))
                    .displayItems((parameters, output) -> {
                        // ==========================================================================
                        // 戒指类型饰品
                        // ==========================================================================
                        output.accept(RING_FRAGILE.get());                 // 脆弱生命的戒指
                        output.accept(RING_LIFE.get());                    // 生命戒指
                        // 坚韧者的戒指
                        output.accept(RING_EVIL_EYE.get());                // 邪眼戒指
                        output.accept(RING_IRON_PROTECTION.get());         // 铁之加护戒指
                        output.accept(RING_FIRE_STONE.get());              // 炎方石戒指
                        output.accept(RING_THUNDER_STONE.get());           // 雷方石戒指
                        output.accept(RING_DARK_STONE.get());              // 暗方石戒指
                        output.accept(RING_MAGIC_STONE.get());             // 魔法方石戒指
                        output.accept(RING_SILVER_SERPENT.get());          // 贪婪银蛇戒指
                        output.accept(RING_GOLD_SERPENT.get());            // 贪婪金蛇戒指
                        output.accept(RING_POISON_BITE.get());             // 咬毒戒指
                        output.accept(RING_BLOOD_BITE.get());              // 咬血戒指
                        output.accept(RING_BLADES.get());                  // 刃之戒指
                        output.accept(RING_RESURRECTOR.get());             // 复活业者戒指
                        output.accept(RING_WASP.get());                    // 黄蜂戒指
                        output.accept(RING_GUARD.get());                   // 守护戒指
                        output.accept(RING_GODDESS.get());                 // 女神的戒指
                        output.accept(RING_PUYO.get());                    // 噗哟噗哟
                        output.accept(RING_HUNYA.get());                   // 呼扭呼扭
                        output.accept(RING_WIND_GOD.get());                // 风神戒指
                        output.accept(RING_SPELL.get());                   // 咒术戒指
                        output.accept(RING_MASOCHIST.get());               // 受虐戒指
                        output.accept(RING_CAT.get());                     // 柴郡猫戒指
                        // 叛逆戒指
                        output.accept(RING_TERROR.get());                  // 恐怖戒指
                        output.accept(RING_IRON_MAIDEN.get());             // 铁处女戒指
                        output.accept(RING_KNIGHT.get());                  // 骑士戒指
                        output.accept(RING_ANGEL.get());                   // 天使戒指
                        output.accept(RING_FAIRY.get());                   // 叮当仙子的戒指
                        output.accept(RING_VOID.get());                    // 空虚戒指
                        output.accept(RING_DRAGON_GUARD.get());            // 龙之守护戒指
                        output.accept(RING_MIDNIGHT_CROWN.get());          // 宵暗的指冠
                        // 奇迹戒指
                        // 杀人小丑戒指
                        // 黑山羊戒指
                        output.accept(RING_DEATH.get());                   // 死神戒指
                        // 理发师戒指
                        // 虚饰戒指
                        // 苹果戒指
                        // 伦蒂尼恩的戒指
                        // 南瓜骑士戒指
                        // 狙击手戒指
                        // 深者戒指
                        // 白鸦戒指
                        // 暗沉木纹戒指
                        // 托托的戒指
                        // 四叶草戒指
                        // 傀儡戒指
                        // 无谋勇者戒指
                        // 银行家戒指
                        // 天之戒指
                        // 擦靴人的戒指
                        output.accept(RING_LIEF.get());                    // 莉耶芙的戒指
                        // 屠夫戒指
                        // 娼妇戒指
                        // 退魔戒指
                        output.accept(RING_ABYSS.get());                   // 深渊戒指
                        // 斗士戒指
                        output.accept(RING_BLACK_RABBIT.get());            // 黑兔戒指
                        // 巨魔戒指
                        // 蚊之戒指
                        // 红泪石戒指
                        // 伊迪斯的戒指
                        // 海象的戒指
                        // 狱灭戒指
                        // 红心骑士戒指
                        // 黑桃骑士戒指
                        // 草花骑士戒指
                        output.accept(RING_WHITE_RABBIT.get());            // 白兔戒指
                        output.accept(RING_GOD_FISH.get());                // 神鱼戒指
                        // 罪恶戒指
                        // 星辰戒指
                        output.accept(RING_BLACKBEARD.get());              // 黑胡子戒指
                        // 普利凯特的戒指
                        // 食人魔戒指
                        // 蜜蜂戒指
                        // 狂乱王的戒指
                        // 韦陀天戒指
                        // 我的奋斗戒指
                        // 奸淫戒指
                    })
                    .build());

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> BLACK_SOULS_CONSUMABLE_TAB = CREATIVE_MODE_TABS.register("blacksouls_consumable",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BLOOD_VIAL.get()))
                    .title(Component.translatable("itemGroup.blacksouls_consumable"))
                    .withSearchBar(30)
                    .displayItems((parameters, output) -> {
                        // ==========================================================================
                        // 一般道具
                        // ==========================================================================
                        output.accept(HERB_BOTTLE.get());                  // 药草瓶
                        output.accept(HERB_BOTTLE_M.get());                // 药草瓶M
                        output.accept(BLOOD_VIAL.get());                   // 输血药
                        output.accept(ANTIDOTE_HERB.get());                // 解毒草
                        output.accept(HEMOSTATIC_CLOTH.get());             // 止血布
                        output.accept(SEDATIVE.get());                     // 镇静剂
                        output.accept(PIGEON_EGG.get());                   // 鸽子蛋
                        output.accept(GODDESS_BLOOD.get());                // 女神之血
                        output.accept(SOUL_GREEN.get());                   // 绿之魂
                        output.accept(SOUL_PURPLE.get());                  // 紫之魂
                        output.accept(SOUL_RED.get());                     // 赤之魂
                        output.accept(SOUL_BLUE.get());                    // 青之魂
                        output.accept(SOUL_YELLOW.get());                  // 黄之魂
                        output.accept(SOUL_GRAY.get());                    // 灰之魂
                        output.accept(SOUL_WHITE.get());                   // 白之魂
                        output.accept(SOUL_FOUR_LEAF_CLOVER.get());        // 四叶之魂
                        output.accept(SOUL_BLACK.get());                   // 黑之魂
                        output.accept(HOMEWARD_BONE_DUST.get());           // 归还骨粉
                        output.accept(RABBIT_WATCH.get());                 // 兔子的怀表
                        output.accept(INVISIBLE_PEPPER.get());             // 看不见的胡椒
                        output.accept(ABANDONED_TRASH.get());              // 废弃垃圾
                        // 魔石
                        output.accept(MAIDENSFRAGRANCE.get());             // 少女之香
                        output.accept(FAIRY_SCALE_POWDER.get());           // 妖精的鳞粉
                        output.accept(MYSTERIOUS_SHARD.get());             // 神秘的碎片
                        output.accept(UPGRADE_SHARD.get());                // 强化石的碎片
                        output.accept(UPGRADE_LARGE_SHARD.get());          // 强化石大碎片
                        output.accept(UPGRADE_CHUNK.get());                // 强化石块
                        output.accept(UPGRADE_SLAB.get());                 // 强化石圆盘
                        // 火焰壶
                        // 屎块
                        output.accept(CHARCOAL_PINE_RESIN.get());          // 炭松脂
                        output.accept(GOLD_PINE_RESIN.get());              // 黄金松脂
                        output.accept(DARK_PINE_RESIN.get());              // 暗松脂
                        output.accept(SOUL_FADING.get());                  // 即将消逝的灵魂
                        output.accept(SOUL_LOST_UNDEAD.get());             // 被遗弃遗体的灵魂
                        output.accept(SOUL_LOST_UNDEAD_LARGE.get());       // 被抛弃的遗体的大块灵魂
                        output.accept(SOUL_NAMELESS_TRAVELER.get());       // 来历不明的旅人的灵魂
                        output.accept(SOUL_NAMELESS_TRAVELER_LARGE.get()); // 来历不明的旅人的大块灵魂
                        output.accept(SOUL_NAMELESS_SOLDIER.get());        // 无名士兵的灵魂
                        output.accept(SOUL_NAMELESS_SOLDIER_LARGE.get());  // 无名士兵的大块灵魂
                        output.accept(SOUL_EXHAUSTED_WARRIOR.get());       // 力竭的战士的灵魂
                        output.accept(SOUL_EXHAUSTED_WARRIOR_LARGE.get()); // 力竭的战士的大块灵魂
                        output.accept(SOUL_CRESTFALLEN_KNIGHT.get());      // 灰心的骑士的灵魂
                        output.accept(SOUL_CRESTFALLEN_KNIGHT_LARGE.get());// 灰心的骑士的大块灵魂
                        output.accept(ORANGE_MARMALADE.get());             // 橘子果酱
                        // 万能钥匙
                        output.accept(BLACKWELL_BLOOD_VIAL.get());         // 布莱克威尔的输血药
                        output.accept(CANDY.get());                        // 糖果
                        // 油壶
                        output.accept(THROWING_KNIFE.get());               // 投掷小刀
                        // 不死者杀手菇
                        output.accept(PURE_WATER.get());                   // 水
                        // 精力剂
                        output.accept(SNAKE_BONE_RETURN.get());            // 归还蛇骨
                        output.accept(MUDDY_FISH.get());                   // 浑浊之鱼
                        output.accept(WHITE_STICKY_THING.get());           // 又白又黏的那啥
                        output.accept(IRON_SCRAP_SNACK.get());             // 铁渣点心
                        // 妖精之羽
                        output.accept(GOLDENMEAD.get());                   // 黄金的蜂蜜酒
                        output.accept(CARPENTER_NAIL.get());               // 大工的钉子
                        output.accept(PRESCRIPTION_MEDICINE.get());        // 处方药
                        output.accept(GIRLS_PHOTO.get());                  // 少女的写真
                        output.accept(RETRIEVAL_POKER.get());              // 再思的扑克
                        output.accept(GOAT_MEAT.get());                    // 山羊的肉
                        output.accept(PREGNANT_CAKE_MEAT.get());           // 孕妇蛋糕之肉
                        output.accept(BLACK_ASH.get());                    // 黑之灰
                        // 染血的钥匙
                        // 喝了我吧
                        // 吃了我吧
                        // 兔之键
                        // 黄金之卵
                        // 列车票
                        // 通行证
                        output.accept(QUEEN_EGG_TART.get());               // 女王的蛋挞
                        output.accept(CANDLE_EMBER.get());                 // 蜡烛的余烬
                        output.accept(ROASTED_CHEESE.get());               // 烤起司
                        output.accept(TURTLE_SOUP.get());                  // 海龟汤
                        output.accept(SOUL_BLACK_DEFILED.get());           // 污秽的黑之魂
                        output.accept(DREAM_SOUL.get());                   // 梦之魂
                        output.accept(SNAKE_GOD_BLOOD.get());              // 蛇神的血
                        // 爱丽丝
                        output.accept(BILLS_BENTO.get());                  // 比尔的便当
                        output.accept(SOUL_OUTSIDER.get());                // 外来者之魂
                        output.accept(SOUL_HERO.get());                    // 英雄的灵魂
                        output.accept(SOUL_GREAT_HERO.get());              // 伟大英雄的灵魂
                        output.accept(MATCH_MEDICINE.get());               // 火柴药
                        // 疯狂的齿轮
                        // 噩梦提灯
                        output.accept(CHICKEN.get());                      // 鸡肉
                        output.accept(CHRISTMAS_CHICKEN.get());            // 圣诞鸡肉
                        output.accept(MYSTERIOUS_MEAT.get());              // 来路不明的肉
                        // 色情魔的那玩意
                        output.accept(MERMAIDSONG.get());                  // 人鱼的歌声
                        // 古王的骨粉
                        // 松鼠的毛
                        output.accept(ICE_PINE_RESIN.get());               // 冰松脂
                        output.accept(SCALPEL.get());                      // 手术刀
                        output.accept(STAR_WATER.get());                   // 星水
                        // 脏液
                        // 青鸟的羽毛
                        // 叮当仙子的鳞粉
                        // 威加盘
                        // 洛德的万年钢笔
                        output.accept(CURSING_FLOWER.get());               // 咒骂之花
                        // 冷谷的气息
                        output.accept(HELANRITHWINE.get());                // 海兰里斯酒
                        // 死灵之书
                        // ==========================================================================
                        // 童话书系列(共32本)
                        // ==========================================================================
                        output.accept(BOOK_RASCAL.get());                  // 童话【我昔日的拉斯卡尔】
                        output.accept(BOOK_FOX_AND_GRAPES.get());          // 童话【狐狸与酸葡萄】
                        output.accept(BOOK_UGLY_DUCKLING.get());           // 童话【丑小鸭】
                        output.accept(BOOK_HIGH_JUMPER.get());             // 童话【跳高者】
                        output.accept(BOOK_WOLF_AND_GOATS.get());          // 童话【狼和X只小山羊】
                        output.accept(BOOK_HANSEL_AND_GRETEL.get());       // 童话【糖果屋】
                        output.accept(BOOK_SINBAD.get());                  // 童话【辛巴达航海记】
                        output.accept(BOOK_BREMEN_MUSICIANS.get());        // 童话【不来梅的乐队】
                        output.accept(BOOK_IRON_HANS.get());               // 童话【铁汉斯】
                        output.accept(BOOK_DOG_OF_FLANDERS.get());         // 童话【弗兰德斯的狗】
                        output.accept(BOOK_LITTLE_PRINCE.get());           // 童话【小王子】
                        output.accept(BOOK_ARMORED_KNIGHT.get());          // 童话【穿着铠甲的骑士】
                        output.accept(BOOK_DONKEY_EARS_KING.get());        // 童话【驴耳朵的国王】
                        output.accept(BOOK_PETER_PAN.get());               // 童话【彼得・潘】
                        output.accept(BOOK_MONKEY_AND_CRAB.get());         // 童话【猿蟹合战】
                        output.accept(BOOK_WIZARD_OF_OZ.get());            // 童话【绿野仙踪】
                        output.accept(BOOK_MATCH_GIRL.get());              // 童话【卖火柴的小女孩】
                        output.accept(BOOK_GOLDEN_GOOSE.get());            // 童话【下金蛋的鹅】
                        output.accept(BOOK_GREEDY_DOG.get());              // 童话【贪心的狗】
                        output.accept(BOOK_PULL_TURNIP.get());             // 童话【拔萝卜】
                        output.accept(BOOK_KACHI_KACHI_YAMA.get());        // 童话【咔擦咔擦山】
                        output.accept(BOOK_INABA_BLACK_RABBIT.get());      // 童话【因幡的黑兔】
                        output.accept(BOOK_ROBIN_HOOD.get());              // 童话【罗宾汉】
                        output.accept(BOOK_BLUEBEARD.get());               // 童话【蓝胡子】
                        output.accept(BOOK_DADDY_LONG_LEGS.get());         // 童话【长腿叔叔】
                        output.accept(BOOK_BOY_WHO_CRIED_WOLF.get());      // 童话【狼来了】
                        output.accept(BOOK_WINNIE_THE_POOH.get());         // 童话【小熊维尼】
                        output.accept(BOOK_PINOCCHIO.get());               // 童话【匹诺曹】
                        output.accept(BOOK_NIGHTINGALE.get());             // 童话【夜莺】
                        output.accept(BOOK_BLANK.get());                   // 童话【　　　　】
                        output.accept(BOOK_SNOW_QUEEN.get());              // 童话【冰雪女王】
                        output.accept(BOOK_SNOW_MAIDEN.get());             // 童话【雪之少女】
                        // ==========================================================================
                        // Boss灵魂
                        // ==========================================================================
                        output.accept(SOUL_SKULL_BEAST.get());             // 猎颅兽的灵魂
                        output.accept(SOUL_BOREDOM.get());                 // 解闷的灵魂
                        output.accept(SOUL_PREGNANT_WOMAN.get());          // 孕妇的灵魂
                        output.accept(SOUL_BELL_CALLER.get());             // 唤铃的灵魂
                        output.accept(SOUL_BEAST_PELT.get());              // 披兽皮的灵魂
                        output.accept(SOUL_GREAT_EAGLE.get());             // 大鹫的灵魂
                        output.accept(SOUL_NARCISSIST.get());              // 自恋的灵魂
                        output.accept(SOUL_JACK.get());                    // 杰克的灵魂
                        output.accept(SOUL_DORM_HEAD.get());               // 学寮长的灵魂
                        output.accept(SOUL_SHINING_STAR.get());            // 辉星的灵魂
                        output.accept(SOUL_OLD_KNIGHT.get());              // 老骑士的灵魂
                        output.accept(SOUL_GIANT_HOUSE.get());             // 巨人之家的灵魂
                        output.accept(SOUL_KNIGHT_OF_HEARTS.get());        // 红桃骑士的灵魂
                        output.accept(SOUL_KNIGHT_OF_SPADES.get());        // 黑桃骑士的灵魂
                        output.accept(SOUL_KNIGHT_OF_CLUBS.get());         // 草花骑士的灵魂
                        output.accept(SOUL_SLAVE_EMPEROR.get());           // 奴隶帝的灵魂
                        output.accept(SOUL_SLAVE_QUEEN.get());             // 奴隶后的灵魂
                        output.accept(SOUL_TORTURE_QUEEN.get());           // 拷问具女王的灵魂
                        output.accept(SOUL_BANDERSNATCH.get());            // 班达斯奈奇的灵魂
                        output.accept(SOUL_JUBJUB.get());                  // 贾布加布的灵魂
                        output.accept(SOUL_JABBERWOCK.get());              // 贾巴沃克的灵魂
                        output.accept(SOUL_DIVINE_FISH.get());             // 神之异鱼的灵魂
                        output.accept(SOUL_DEEP_SEA_KNIGHT.get());         // 深海骑士的灵魂
                        output.accept(SOUL_EVIL_DRAGON_HUNTER.get());      // 邪龙狩猎者的灵魂
                        output.accept(SOUL_APPOINTED_WET_NURSE.get());     // 被任命的乳娘们的灵魂
                        output.accept(SOUL_FLORENCE.get());                // 弗洛伦斯的灵魂
                        output.accept(SOUL_WINTER_BELL_WIND.get());        // 冬钟之风的灵魂
                        output.accept(SOUL_WHITE_UNICORN.get());           // 白之独角兽的灵魂
                        output.accept(SOUL_WHITE_LION.get());              // 白狮子的灵魂
                        // ==========================================================================
                        // 技能书
                        // ==========================================================================
                        output.accept(SKILL_BOOK_SOUL_ARROW.get());         // 魔书【魂之矢】
                        // 魔书【魂之连射】
                        output.accept(SKILL_BOOK_SOUL_LIGHT.get());         // 魔书【魂之光】
                        output.accept(SKILL_BOOK_SOUL_RADIATION.get());     // 魔书【魂之放射】
                        // 魔书【驱散】
                        // 魔书【识破攻击】
                        output.accept(SKILL_BOOK_CARRHUS_BLOOD_CURSE.get());// 魔书【卡萨斯血咒】
                        // 魔书【毒素】
                        // 魔书【毒素Ⅱ】
                        // 魔书【催眠】
                        // 圣书【治愈】
                        // 魔书【魔力祝福】
                        // 魔书【横冲直撞】
                        // 魔书【全面祝福】
                        // 圣书【还魂】
                        // 魔书【魔力吸收】
                        // 圣书【擦除】
                        // 魔书【王之号令】
                        output.accept(SKILL_BOOK_REQUIEM.get());            // 魔书【镇魂歌】
                        output.accept(SKILL_BOOK_GRIT.get());               // 魔书【咬紧牙关】
                        // 魔书【火炎】
                        // 魔书【沉溺之泡】
                        // 魔书【月之暗面】
                        // 魔书【冰结之魔弹】
                        // 魔书【业火】
                        // 魔书【破灭风暴】
                        // 魔书【内在潜力】
                        // 魔书【魂之巨矢】
                        // 魔书【新绿之力】
                        // 魔书【岩之体】
                        // 魔书【暗之球】
                        // 魔书【暗之乱舞】
                        // 魔书【暗之群来】
                        // 魔书【神雷】
                        // 魔书【神兽之雷鸣】
                        // 魔书【流星群】
                        // 魔书【全面诅咒】
                        // 魔书【魂之巨矢连射】
                        output.accept(SKILL_BOOK_INVISIBLE.get());          // 魔书【看不见的身体】
                        // 圣书【致命守护】
                        // 魔书【幽火】
                        // 魔书【法拉克斯】
                        // 魔书【绝对必中】
                        // 魔书【混沌爆炎】
                        // 魔书【会心一击】
                        // 圣书【灵魂盾】
                        // 魔书【密螺旋体】
                        // 圣书【肉壁召唤】
                        // 魔书【撕裂的遗恨】
                        // 魔书【贯穿冰柱】
                        // 魔书【灭亡的箭雨】
                        // 魔书【阴暗之沼】
                        // 魔书【酸雨】
                        // 魔书【皇家红茶】
                        // 魔书【神速之舞】
                        // 圣书【卡塔丽娜的车轮】
                        // 圣书【圣骑士的御旗】
                        // 魔书【黑之波动】
                        // 魔书【黑之斩击】
                        // 魔书【觉醒】
                        // 魔书【毒蛇的拥抱】
                        // 魔书【魂之奔流】
                    })
                    .build());
    // 注册方法
    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(BSPlayerStats.class);
    }
    // ==========================================
    //                 生命周期事件
    // ==========================================
    public BlackSouls() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册核心组件
        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        BSEntityRegistry.ENTITY_TYPES.register(modEventBus);
        BSSoundRegistry.register(modEventBus);
        // 注册能力
        modEventBus.addListener(this::onRegisterCapabilities);
        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BSConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BSConfig.CLIENT_SPEC);
        // 绑定生命周期
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 发包
            com.BlackSouls.BlackSoulsMod.network.BSItemSellRegistry.init();
            com.BlackSouls.BlackSoulsMod.network.BSItemBuyRegistry.init();
            NetworkHandler.register();
            // 启动时技能塞进服务端的字典里
            SkillRegistry.init();
            LOGGER.info("Core systems and network bus initialized successfully");
        });
    }
}
