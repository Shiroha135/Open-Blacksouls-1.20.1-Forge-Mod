package com.BlackSouls.BlackSoulsMod;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.entity.EntityThrownBlade;
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
import com.BlackSouls.BlackSoulsMod.util.skill.SkillOriginalMagic;
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
    public static final RegistryObject<MobEffect> BUFF_HASSO = MOB_EFFECTS.register("hasso", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x8B2035));
    public static final RegistryObject<MobEffect> BUFF_MANA_REGEN = MOB_EFFECTS.register("mana_regen", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x79C8FF));
    public static final RegistryObject<MobEffect> BUFF_OILY = MOB_EFFECTS.register("oily", () -> new PotionSpeedShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x4A3723, "2d564afc-d614-4871-8d67-4bf98ac4d811", -0.20D));
    public static final RegistryObject<MobEffect> BUFF_WEAKNESS = MOB_EFFECTS.register("weakness", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x6F5842));
    public static final RegistryObject<MobEffect> BUFF_HP_REGEN = MOB_EFFECTS.register("hp_regen", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x8FD486));
    public static final RegistryObject<MobEffect> BUFF_HP_MP_UP = MOB_EFFECTS.register("hp_mp_up", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD58BE8));
    public static final RegistryObject<MobEffect> BUFF_JUGGLING_EVASION = MOB_EFFECTS.register("juggling_evasion", () -> new PotionSpeedShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xB5F3D8, "c3456a45-308c-46f4-b1ce-06f84e59ac44", 0.50D));
    public static final RegistryObject<MobEffect> BUFF_NECRONOMICON = MOB_EFFECTS.register("necronomicon", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x38204F));
    public static final RegistryObject<MobEffect> BUFF_COUNTER_STANCE = MOB_EFFECTS.register("counter_stance", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD7B46A));
    public static final RegistryObject<MobEffect> BUFF_FRAGILE = MOB_EFFECTS.register("fragile", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x8B6C78));
    public static final RegistryObject<MobEffect> BUFF_SELF_HARM = MOB_EFFECTS.register("self_harm", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x9E2535));
    public static final RegistryObject<MobEffect> BUFF_AIM = MOB_EFFECTS.register("aim", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD8C99C));
    public static final RegistryObject<MobEffect> BUFF_HAKI = MOB_EFFECTS.register("haki", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xE4A745));
    public static final RegistryObject<MobEffect> BUFF_QUICK_RELOAD = MOB_EFFECTS.register("quick_reload", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xCAD7E8));
    public static final RegistryObject<MobEffect> BUFF_QUICK_RELOAD_CRIT = MOB_EFFECTS.register("quick_reload_crit", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xF2C35A));
    public static final RegistryObject<MobEffect> BUFF_MAD_BIRD_CALL = MOB_EFFECTS.register("mad_bird_call", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xA7324A));
    public static final RegistryObject<MobEffect> BUFF_ECLIPSE = MOB_EFFECTS.register("eclipse", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD7C98B));
    public static final RegistryObject<MobEffect> BUFF_HIGH_MOBILITY = MOB_EFFECTS.register("high_mobility", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x55CFE8));
    public static final RegistryObject<MobEffect> BUFF_SLAUGHTER_MODE = MOB_EFFECTS.register("slaughter_mode", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xA91E28));
    public static final RegistryObject<MobEffect> BUFF_DUAL_SWORD_AURA = MOB_EFFECTS.register("dual_sword_aura", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xE8F5FF));
    public static final RegistryObject<MobEffect> BUFF_GUNBLADE_AMMO_I = MOB_EFFECTS.register("gunblade_ammo_i", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xF2D6A2));
    public static final RegistryObject<MobEffect> BUFF_GUNBLADE_AMMO_II = MOB_EFFECTS.register("gunblade_ammo_ii", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xF2A65A));
    public static final RegistryObject<MobEffect> BUFF_GUNBLADE_AMMO_III = MOB_EFFECTS.register("gunblade_ammo_iii", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xE04B3F));
    public static final RegistryObject<MobEffect> BUFF_EXPOSED_WEAKNESS = MOB_EFFECTS.register("exposed_weakness", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0xF0E5C0));
    public static final RegistryObject<MobEffect> BUFF_MIND_EYE = MOB_EFFECTS.register("mind_eye", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xCFEFFF));
    public static final RegistryObject<MobEffect> BUFF_NATURAL_RECOVERY = MOB_EFFECTS.register("natural_recovery", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x88E6A5));
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
    public static final RegistryObject<MobEffect> BUFF_INNER_POTENTIAL = MOB_EFFECTS.register("inner_potential", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD42A47));
    public static final RegistryObject<MobEffect> BUFF_AWAKENING = MOB_EFFECTS.register("awakening", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x8E44AD));
    public static final RegistryObject<MobEffect> BUFF_LACERATION = MOB_EFFECTS.register("laceration", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x8A2030));
    public static final RegistryObject<MobEffect> BUFF_DEFENSE_KING = MOB_EFFECTS.register("defense_king", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xD8C36A));
    public static final RegistryObject<MobEffect> BUFF_PLAYWRIGHT = MOB_EFFECTS.register("playwright_effect", () -> new PotionStatShift(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x8B1A1A));
    // ===========================================================================================================
    // 音效注册
    // ===========================================================================================================
    public static final RegistryObject<SoundEvent> ACID_EVENT = registerSound("acid");
    public static final RegistryObject<SoundEvent> ABSORB1_EVENT = registerSound("absorb1");
    public static final RegistryObject<SoundEvent> ATTACK3_EVENT = registerSound("attack3");
    public static final RegistryObject<SoundEvent> BATTLE3_EVENT =  registerSound("battle3");
    public static final RegistryObject<SoundEvent> BLIND_EVENT = registerSound("blind");
    public static final RegistryObject<SoundEvent> BLOW2_EVENT = registerSound("blow2");
    public static final RegistryObject<SoundEvent> BLOW1_EVENT = registerSound("blow1");
    public static final RegistryObject<SoundEvent> BLOW4_EVENT = registerSound("blow4");
    public static final RegistryObject<SoundEvent> BLOW6_EVENT = registerSound("blow6");
    public static final RegistryObject<SoundEvent> BLOW7_EVENT = registerSound("blow7");
    public static final RegistryObject<SoundEvent> BLOW3_EVENT = registerSound("blow3");
    public static final RegistryObject<SoundEvent> BLOW5_EVENT = registerSound("blow5");
    public static final RegistryObject<SoundEvent> BREAK_EVENT = registerSound("break");
    public static final RegistryObject<SoundEvent> BOW1_EVENT = registerSound("bow1");
    public static final RegistryObject<SoundEvent> BOW2_EVENT = registerSound("bow2");
    public static final RegistryObject<SoundEvent> BOW4_EVENT = registerSound("bow4");
    public static final RegistryObject<SoundEvent> CRASH_EVENT = registerSound("crash");
    public static final RegistryObject<SoundEvent> CURSOR1_EVENT = registerSound("cursor1");
    public static final RegistryObject<SoundEvent> DAO_EVENT = registerSound("dao");
    public static final RegistryObject<SoundEvent> BLOOD_SPLATTER_EVENT = registerSound("blood_splatter");
    public static final RegistryObject<SoundEvent> DARKNESS3_EVENT = registerSound("darkness3");
    public static final RegistryObject<SoundEvent> DARKNESS1_EVENT = registerSound("darkness1");
    public static final RegistryObject<SoundEvent> DARKNESS4_EVENT = registerSound("darkness4");
    public static final RegistryObject<SoundEvent> DARKNESS5_EVENT = registerSound("darkness5");
    public static final RegistryObject<SoundEvent> DARKNESS6_EVENT = registerSound("darkness6");
    public static final RegistryObject<SoundEvent> DARKNESS7_EVENT = registerSound("darkness7");
    public static final RegistryObject<SoundEvent> DARKNESS8_EVENT = registerSound("darkness8");
    public static final RegistryObject<SoundEvent> DAMAGE4_EVENT = registerSound("damage4");
    public static final RegistryObject<SoundEvent> DAMAGE2_EVENT = registerSound("damage2");
    public static final RegistryObject<SoundEvent> DOWN2_EVENT = registerSound("down2");
    public static final RegistryObject<SoundEvent> EARTH6_EVENT = registerSound("earth6");
    public static final RegistryObject<SoundEvent> EARTH1_EVENT = registerSound("earth1");
    public static final RegistryObject<SoundEvent> EARTH2_EVENT = registerSound("earth2");
    public static final RegistryObject<SoundEvent> EARTH5_EVENT = registerSound("earth5");
    public static final RegistryObject<SoundEvent> EVASION1_EVENT  = registerSound("evasion1");
    public static final RegistryObject<SoundEvent> EXPLOSION3_EVENT =registerSound("explosion3");
    public static final RegistryObject<SoundEvent> EXPLOSION2_EVENT = registerSound("explosion2");
    public static final RegistryObject<SoundEvent> EQUIP1_EVENT = registerSound("equip1");
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
    public static final RegistryObject<SoundEvent> GUN2_EVENT = registerSound("gun2");
    public static final RegistryObject<SoundEvent> ICE1_EVENT = registerSound("ice1");
    public static final RegistryObject<SoundEvent> ICE2_EVENT = registerSound("ice2");
    public static final RegistryObject<SoundEvent> ICE4_EVENT = registerSound("ice4");
    public static final RegistryObject<SoundEvent> ICE7_EVENT = registerSound("ice7");
    public static final RegistryObject<SoundEvent> ICE8_EVENT = registerSound("ice8");
    public static final RegistryObject<SoundEvent> ICE11_EVENT = registerSound("ice11");
    public static final RegistryObject<SoundEvent> ITEM1_EVENT = registerSound("item1");
    public static final RegistryObject<SoundEvent> KEY_EVENT = registerSound("key");
    public static final RegistryObject<SoundEvent> MAGIC1_EVENT = registerSound("magic1");
    public static final RegistryObject<SoundEvent> MAGIC2_EVENT = registerSound("magic2");
    public static final RegistryObject<SoundEvent> MAGIC4_EVENT = registerSound("magic4");
    public static final RegistryObject<SoundEvent> MAGIC7_EVENT = registerSound("magic7");
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
    public static final RegistryObject<SoundEvent> SAINT8_EVENT = registerSound("saint8");
    public static final RegistryObject<SoundEvent> SAINT9_EVENT = registerSound("saint9");
    public static final RegistryObject<SoundEvent> SAINT3_EVENT = registerSound("saint3");
    public static final RegistryObject<SoundEvent> SAND_EVENT = registerSound("sand");
    public static final RegistryObject<SoundEvent> PUSH_EVENT = registerSound("push");
    public static final RegistryObject<SoundEvent> SHOTGUN_FIRE_EVENT = registerSound("shotgun_fire");
    public static final RegistryObject<SoundEvent> SKILL1_EVENT =  registerSound("skill1");
    public static final RegistryObject<SoundEvent> SKILL3_EVENT =  registerSound("skill3");
    public static final RegistryObject<SoundEvent> SKILL2_EVENT = registerSound("skill2");
    public static final RegistryObject<SoundEvent> SLASH1_EVENT = registerSound("slash1");
    public static final RegistryObject<SoundEvent> SLASH2_EVENT = registerSound("slash2");
    public static final RegistryObject<SoundEvent> SLASH3_EVENT = registerSound("slash3");
    public static final RegistryObject<SoundEvent> SLASH4_EVENT = registerSound("slash4");
    public static final RegistryObject<SoundEvent> SLASH5_EVENT = registerSound("slash5");
    public static final RegistryObject<SoundEvent> SLASH6_EVENT = registerSound("slash6");
    public static final RegistryObject<SoundEvent> SLASH7_EVENT = registerSound("slash7");
    public static final RegistryObject<SoundEvent> SLASH8_EVENT = registerSound("slash8");
    public static final RegistryObject<SoundEvent> SLASH9_EVENT = registerSound("slash9");
    public static final RegistryObject<SoundEvent> SLASH10_EVENT = registerSound("slash10");
    public static final RegistryObject<SoundEvent> SLASH11_EVENT = registerSound("slash11");
    public static final RegistryObject<SoundEvent> SLASH12_EVENT = registerSound("slash12");
    public static final RegistryObject<SoundEvent> SLEEP_EVENT = registerSound("sleep");
    public static final RegistryObject<SoundEvent> SILENCE_EVENT = registerSound("silence");
    public static final RegistryObject<SoundEvent> SONG_EVENT = registerSound("song");
    public static final RegistryObject<SoundEvent> SWORD1_EVENT = registerSound("sword1");
    public static final RegistryObject<SoundEvent> SWORD3_EVENT = registerSound("sword3");
    public static final RegistryObject<SoundEvent> SWORD4_EVENT = registerSound("sword4");
    public static final RegistryObject<SoundEvent> SWORD5_EVENT = registerSound("sword5");
    public static final RegistryObject<SoundEvent> DAO2_EVENT = registerSound("dao2");
    public static final RegistryObject<SoundEvent> DAO3_EVENT = registerSound("dao3");
    public static final RegistryObject<SoundEvent> THUNDER1_EVENT = registerSound("thunder1");
    public static final RegistryObject<SoundEvent> TELEPORT_EVENT = registerSound("teleport");
    public static final RegistryObject<SoundEvent> SWORD_STAB_EVENT = registerSound("sword_stab");
    public static final RegistryObject<SoundEvent> THUNDER7_EVENT = registerSound("thunder7");
    public static final RegistryObject<SoundEvent> THUNDER5_EVENT = registerSound("thunder5");
    public static final RegistryObject<SoundEvent> THUNDER8_EVENT = registerSound("thunder8");
    public static final RegistryObject<SoundEvent> THUNDER10_EVENT = registerSound("thunder10");
    public static final RegistryObject<SoundEvent> TITLE_BGM_EVENT = registerSound("title_bgm");
    public static final RegistryObject<SoundEvent> TWINE_EVENT = registerSound("twine");
    public static final RegistryObject<SoundEvent> UP4_EVENT = registerSound("up4");
    public static final RegistryObject<SoundEvent> WATER1_EVENT = registerSound("water1");
    public static final RegistryObject<SoundEvent> WIND1_EVENT = registerSound("wind1");
    public static final RegistryObject<SoundEvent> WIND5_EVENT = registerSound("wind5");
    public static final RegistryObject<SoundEvent> WIND8_EVENT = registerSound("wind8");
    public static final RegistryObject<SoundEvent> WIND7_EVENT = registerSound("wind7");
    public static final RegistryObject<SoundEvent> WIND10_EVENT = registerSound("wind10");
    public static final RegistryObject<SoundEvent> WIND6_EVENT = registerSound("wind6");
    public static final RegistryObject<SoundEvent> UP1_EVENT = registerSound("up1");
    public static final RegistryObject<SoundEvent> COLLAPSE2_EVENT = registerSound("collapse2");
    public static final RegistryObject<SoundEvent> POLLEN_EVENT = registerSound("pollen");
    public static final RegistryObject<SoundEvent> BIRD_CRY_EVENT = registerSound("bird_cry");
    public static final RegistryObject<SoundEvent> WIND2_EVENT = registerSound("wind2");
    public static final RegistryObject<SoundEvent> ITEM3_EVENT = registerSound("item3");
    public static final RegistryObject<SoundEvent> DIVE_EVENT = registerSound("dive");
    public static final RegistryObject<SoundEvent> POISON_EVENT = registerSound("poison");
    public static final RegistryObject<SoundEvent> WATER6_EVENT = registerSound("water6");
    public static final RegistryObject<SoundEvent> HEAL4_EVENT = registerSound("heal4");
    public static final RegistryObject<SoundEvent> METAL1_EVENT = registerSound("metal1");
    public static final RegistryObject<SoundEvent> METAL2_EVENT = registerSound("metal2");
    public static final RegistryObject<SoundEvent> CHAINSAW_REV_EVENT = registerSound("chainsaw_rev");
    public static final RegistryObject<SoundEvent> MAGIC5_EVENT = registerSound("magic5");
    public static final RegistryObject<SoundEvent> SWITCH2_EVENT = registerSound("switch2");
    public static final RegistryObject<SoundEvent> THUNDER6_EVENT = registerSound("thunder6");
    public static final RegistryObject<SoundEvent> SNIPER_RIFLE_EVENT = registerSound("sniper_rifle");
    public static final RegistryObject<SoundEvent> GUN_GIRD1_EVENT = registerSound("gun_gird1");

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

    private static RegistryObject<Item> registerOriginalSkillBook(SkillOriginalMagic.Profile profile) {
        return ITEMS.register(profile.getBookId(), () -> new ItemOriginalSkillBook(new Item.Properties(), profile));
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
    // 魔石
    public static final RegistryObject<Item> MAGIC_STONE = ITEMS.register("magic_stone", () -> new ItemMagicStone(new Item.Properties()));
    // 火焰壶
    public static final RegistryObject<Item> FIRE_BOMB = ITEMS.register("fire_bomb", () -> new ItemThrownConsumable(new Item.Properties(), EntityThrownBlade.MODE_FIRE_BOMB, false, 204, EVASION1_EVENT));
    // 屎块
    public static final RegistryObject<Item> DUNG_PIE = ITEMS.register("dung_pie", () -> new ItemThrownConsumable(new Item.Properties(), EntityThrownBlade.MODE_DUNG_PIE, false, 205, EVASION1_EVENT));
    // 万能钥匙
    public static final RegistryObject<Item> MASTER_KEY = ITEMS.register("master_key", () -> new ItemMasterKey(new Item.Properties()));
    // 油壶
    public static final RegistryObject<Item> OIL_URN = ITEMS.register("oil_urn", () -> new ItemThrownConsumable(new Item.Properties(), EntityThrownBlade.MODE_OIL_URN, true, 206, EVASION1_EVENT));
    // 不死者杀手菇
    public static final RegistryObject<Item> UNDEAD_KILLER_MUSHROOM = ITEMS.register("undead_killer_mushroom", () -> new ItemThrownConsumable(new Item.Properties(), EntityThrownBlade.MODE_UNDEAD_KILLER_MUSHROOM, true, 210, EVASION1_EVENT));
    // 精力剂
    public static final RegistryObject<Item> STAMINA_TONIC = ITEMS.register("stamina_tonic", () -> new ItemStaminaTonic(new Item.Properties()));
    public static final RegistryObject<Item> FAIRY_FEATHER = ITEMS.register("fairy_feather", () -> new ItemParameterBoost(new Item.Properties(), ItemParameterBoost.Mode.SPEED));
    public static final RegistryObject<Item> BLOODSTAINED_KEY = ITEMS.register("bloodstained_key", () -> new ItemSimpleLore(new Item.Properties().stacksTo(1), 1));
    public static final RegistryObject<Item> DRINK_ME = ITEMS.register("drink_me", () -> new ItemUnavailableStoryConsumable(new Item.Properties().stacksTo(1), "message.blacksouls.story_map_only"));
    public static final RegistryObject<Item> EAT_ME = ITEMS.register("eat_me", () -> new ItemUnavailableStoryConsumable(new Item.Properties().stacksTo(1), "message.blacksouls.story_map_only"));
    public static final RegistryObject<Item> RABBIT_KEY = ITEMS.register("rabbit_key", () -> new ItemSimpleLore(new Item.Properties().stacksTo(1), 2));
    public static final RegistryObject<Item> GOLDEN_EGG = ITEMS.register("golden_egg", () -> new ItemSoul(new Item.Properties(), 250000, "item.blacksouls.golden_egg.lore.1", "item.blacksouls.golden_egg.lore.2"));
    public static final RegistryObject<Item> TRAIN_TICKET = ITEMS.register("train_ticket", () -> new ItemSimpleLore(new Item.Properties().stacksTo(1), 1, true));
    public static final RegistryObject<Item> ENTRY_PASS = ITEMS.register("entry_pass", () -> new ItemSimpleLore(new Item.Properties().stacksTo(1), 1, true));
    public static final RegistryObject<Item> ALICE_ITEM = ITEMS.register("alice", () -> new ItemSimpleLore(new Item.Properties().stacksTo(1), 1, true));
    public static final RegistryObject<Item> MAD_GEAR = ITEMS.register("mad_gear", () -> new ItemMadGear(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NIGHTMARE_LANTERN = ITEMS.register("nightmare_lantern", () -> new ItemThrownConsumable(new Item.Properties(), EntityThrownBlade.MODE_NIGHTMARE_LANTERN, false, 204, EVASION1_EVENT));
    public static final RegistryObject<Item> SATYRS_THING = ITEMS.register("satyrs_thing", () -> new ItemGrantWhiteStickyThing(new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_KINGS_BONE_DUST = ITEMS.register("ancient_kings_bone_dust", () -> new ItemReturnToBonfire(new Item.Properties()));
    public static final RegistryObject<Item> SQUIRREL_FUR = ITEMS.register("squirrel_fur", () -> new ItemUnavailableStoryConsumable(new Item.Properties(), "message.blacksouls.chaos_dungeon_only"));
    public static final RegistryObject<Item> FILTHY_LIQUID = ITEMS.register("filthy_liquid", () -> new ItemFilthyLiquid(new Item.Properties()));
    public static final RegistryObject<Item> BLUEBIRD_FEATHER = ITEMS.register("bluebird_feather", () -> new ItemParameterBoost(new Item.Properties(), ItemParameterBoost.Mode.LUCK));
    public static final RegistryObject<Item> TINKER_BELLS_SCALE_POWDER = ITEMS.register("tinker_bells_scale_powder", () -> new ItemTinkerBellScalePowder(new Item.Properties()));
    public static final RegistryObject<Item> OUIJA_BOARD = ITEMS.register("ouija_board", () -> new ItemAreaAttackConsumable(new Item.Properties(), ItemAreaAttackConsumable.Mode.OUIJA_BOARD));
    public static final RegistryObject<Item> ROLDS_FOUNTAIN_PEN = ITEMS.register("rolds_fountain_pen", () -> new ItemParameterBoost(new Item.Properties(), ItemParameterBoost.Mode.HP_MP));
    public static final RegistryObject<Item> COLD_VALLEY_BREATH = ITEMS.register("cold_valley_breath", () -> new ItemAreaAttackConsumable(new Item.Properties(), ItemAreaAttackConsumable.Mode.COLD_VALLEY_BREATH));
    public static final RegistryObject<Item> NECRONOMICON = ITEMS.register("necronomicon", () -> new ItemNecronomicon(new Item.Properties()));
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
    public static final RegistryObject<Item> WORK_CLOTHES = ITEMS.register("work_clothes", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.WORK_CLOTHES, new Item.Properties()));
    public static final RegistryObject<Item> ABYSS_ARMOR = ITEMS.register("abyss_armor", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.ABYSS_ARMOR, new Item.Properties()));
    public static final RegistryObject<Item> ABYSS_HELMET = ITEMS.register("abyss_helmet", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.ABYSS_HELMET, new Item.Properties()));
    public static final RegistryObject<Item> YELLOW_CLOTH = ITEMS.register("yellow_cloth", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.YELLOW_CLOTH, new Item.Properties()));
    public static final RegistryObject<Item> PLAYWRIGHT_HEADSCARF = ITEMS.register("playwright_headscarf", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.PLAYWRIGHT_HEADSCARF, new Item.Properties()));
    public static final RegistryObject<Item> FALSE_ANGEL_CROWN = ITEMS.register("false_angel_crown", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.FALSE_ANGEL_CROWN, new Item.Properties()));
    public static final RegistryObject<Item> WINTER_MAGE_COAT = ITEMS.register("winter_mage_coat", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.WINTER_MAGE_COAT, new Item.Properties()));
    public static final RegistryObject<Item> WINTER_KNIGHT_ARMOR = ITEMS.register("winter_knight_armor", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.WINTER_KNIGHT_ARMOR, new Item.Properties()));
    public static final RegistryObject<Item> WINTER_KNIGHT_HELMET = ITEMS.register("winter_knight_helmet", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.WINTER_KNIGHT_HELMET, new Item.Properties()));
    public static final RegistryObject<Item> WINDLESS_CLOTHES = ITEMS.register("windless_clothes", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.WINDLESS_CLOTHES, new Item.Properties()));
    public static final RegistryObject<Item> MIRACLE_SHRINE_MAIDEN_GARB = ITEMS.register("miracle_shrine_maiden_garb", () -> new ItemOriginalAccessory(ItemOriginalAccessory.Profile.MIRACLE_SHRINE_MAIDEN_GARB, new Item.Properties()));
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
    public static final RegistryObject<Item> RING_TENACIOUS = ITEMS.register("ring_tenacious", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TENACIOUS, new Item.Properties()));
    public static final RegistryObject<Item> RING_REBELLION = ITEMS.register("ring_rebellion", () -> new ItemOriginalRing(ItemOriginalRing.Profile.REBELLION, new Item.Properties()));
    public static final RegistryObject<Item> RING_MIRACLE = ITEMS.register("ring_miracle", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MIRACLE, new Item.Properties()));
    public static final RegistryObject<Item> RING_MURDER_CLOWN = ITEMS.register("ring_murder_clown", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MURDER_CLOWN, new Item.Properties()));
    public static final RegistryObject<Item> RING_BLACK_GOAT = ITEMS.register("ring_black_goat", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BLACK_GOAT, new Item.Properties()));
    public static final RegistryObject<Item> RING_BARBER = ITEMS.register("ring_barber", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BARBER, new Item.Properties()));
    public static final RegistryObject<Item> RING_VANITY = ITEMS.register("ring_vanity", () -> new ItemOriginalRing(ItemOriginalRing.Profile.VANITY, new Item.Properties()));
    public static final RegistryObject<Item> RING_APPLE = ITEMS.register("ring_apple", () -> new ItemOriginalRing(ItemOriginalRing.Profile.APPLE, new Item.Properties()));
    public static final RegistryObject<Item> RING_LUNDINIAN = ITEMS.register("ring_lundinian", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LUNDINIAN, new Item.Properties()));
    public static final RegistryObject<Item> RING_PUMPKIN_KNIGHT = ITEMS.register("ring_pumpkin_knight", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PUMPKIN_KNIGHT, new Item.Properties()));
    public static final RegistryObject<Item> RING_SNIPER = ITEMS.register("ring_sniper", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SNIPER, new Item.Properties()));
    public static final RegistryObject<Item> RING_DEEP_ONE = ITEMS.register("ring_deep_one", () -> new ItemOriginalRing(ItemOriginalRing.Profile.DEEP_ONE, new Item.Properties()));
    public static final RegistryObject<Item> RING_WHITE_RAVEN = ITEMS.register("ring_white_raven", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WHITE_RAVEN, new Item.Properties()));
    public static final RegistryObject<Item> RING_DULL_WOOD_GRAIN = ITEMS.register("ring_dull_wood_grain", () -> new ItemOriginalRing(ItemOriginalRing.Profile.DULL_WOOD_GRAIN, new Item.Properties()));
    public static final RegistryObject<Item> RING_TOTO = ITEMS.register("ring_toto", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TOTO, new Item.Properties()));
    public static final RegistryObject<Item> RING_FOUR_LEAF_CLOVER = ITEMS.register("ring_four_leaf_clover", () -> new ItemOriginalRing(ItemOriginalRing.Profile.FOUR_LEAF_CLOVER, new Item.Properties()));
    public static final RegistryObject<Item> RING_PUPPET = ITEMS.register("ring_puppet", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PUPPET, new Item.Properties()));
    public static final RegistryObject<Item> RING_EDITH = ITEMS.register("ring_edith", () -> new ItemOriginalRing(ItemOriginalRing.Profile.EDITH, new Item.Properties()));
    public static final RegistryObject<Item> RING_PRICKETT = ITEMS.register("ring_prickett", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PRICKETT, new Item.Properties()));
    public static final RegistryObject<Item> RING_LIFE_PLUS_1 = ITEMS.register("life_ring_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LIFE_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_LIFE_PLUS_2 = ITEMS.register("life_ring_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LIFE_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_LIFE_PLUS_3 = ITEMS.register("life_ring_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LIFE_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_TENACIOUS_PLUS_1 = ITEMS.register("ring_tenacious_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TENACIOUS_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_TENACIOUS_PLUS_2 = ITEMS.register("ring_tenacious_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TENACIOUS_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_TENACIOUS_PLUS_3 = ITEMS.register("ring_tenacious_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TENACIOUS_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_PUYO_PLUS_1 = ITEMS.register("ring_puyo_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PUYO_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_PUYO_PLUS_2 = ITEMS.register("ring_puyo_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PUYO_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_PUYO_PLUS_3 = ITEMS.register("ring_puyo_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PUYO_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_HUNYA_PLUS_1 = ITEMS.register("ring_hunya_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HUNYA_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_HUNYA_PLUS_2 = ITEMS.register("ring_hunya_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HUNYA_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_HUNYA_PLUS_3 = ITEMS.register("ring_hunya_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HUNYA_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_VOID_PLUS_1 = ITEMS.register("ring_void_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.VOID_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_VOID_PLUS_2 = ITEMS.register("ring_void_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.VOID_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_VOID_PLUS_3 = ITEMS.register("ring_void_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.VOID_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_EVIL_EYE_PLUS_1 = ITEMS.register("ring_evil_eye_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.EVIL_EYE_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_EVIL_EYE_PLUS_2 = ITEMS.register("ring_evil_eye_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.EVIL_EYE_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_EVIL_EYE_PLUS_3 = ITEMS.register("ring_evil_eye_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.EVIL_EYE_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_GODDESS_PLUS_1 = ITEMS.register("ring_goddess_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GODDESS_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_GODDESS_PLUS_2 = ITEMS.register("ring_goddess_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GODDESS_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_GODDESS_PLUS_3 = ITEMS.register("ring_goddess_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GODDESS_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_IRON_PROTECTION_PLUS_1 = ITEMS.register("ring_iron_protection_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.IRON_PROTECTION_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_IRON_PROTECTION_PLUS_2 = ITEMS.register("ring_iron_protection_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.IRON_PROTECTION_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_IRON_PROTECTION_PLUS_3 = ITEMS.register("ring_iron_protection_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.IRON_PROTECTION_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_MAGIC_STONE_PLUS_1 = ITEMS.register("ring_magic_stone_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MAGIC_STONE_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_MAGIC_STONE_PLUS_2 = ITEMS.register("ring_magic_stone_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MAGIC_STONE_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_MAGIC_STONE_PLUS_3 = ITEMS.register("ring_magic_stone_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MAGIC_STONE_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_SNIPER_PLUS_1 = ITEMS.register("ring_sniper_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SNIPER_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_SNIPER_PLUS_2 = ITEMS.register("ring_sniper_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SNIPER_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_SNIPER_PLUS_3 = ITEMS.register("ring_sniper_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SNIPER_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_WASP_PLUS_1 = ITEMS.register("ring_wasp_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WASP_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_WASP_PLUS_2 = ITEMS.register("ring_wasp_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WASP_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_WASP_PLUS_3 = ITEMS.register("ring_wasp_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WASP_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_BLADES_PLUS_1 = ITEMS.register("ring_blades_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BLADES_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_BLADES_PLUS_2 = ITEMS.register("ring_blades_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BLADES_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_BLADES_PLUS_3 = ITEMS.register("ring_blades_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BLADES_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_GUARD_PLUS_1 = ITEMS.register("ring_guard_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GUARD_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_GUARD_PLUS_2 = ITEMS.register("ring_guard_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GUARD_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_GUARD_PLUS_3 = ITEMS.register("ring_guard_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GUARD_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_WIND_GOD_PLUS_1 = ITEMS.register("ring_wind_god_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WIND_GOD_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_WIND_GOD_PLUS_2 = ITEMS.register("ring_wind_god_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WIND_GOD_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_WIND_GOD_PLUS_3 = ITEMS.register("ring_wind_god_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WIND_GOD_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_SPELL_PLUS_1 = ITEMS.register("ring_spell_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SPELL_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_SPELL_PLUS_2 = ITEMS.register("ring_spell_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SPELL_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_SPELL_PLUS_3 = ITEMS.register("ring_spell_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SPELL_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_LUNDINIAN_PLUS_1 = ITEMS.register("ring_lundinian_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LUNDINIAN_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_LUNDINIAN_PLUS_2 = ITEMS.register("ring_lundinian_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LUNDINIAN_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_LUNDINIAN_PLUS_3 = ITEMS.register("ring_lundinian_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LUNDINIAN_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_CUT_DOWN = ITEMS.register("ring_cut_down", () -> new ItemOriginalRing(ItemOriginalRing.Profile.CUT_DOWN, new Item.Properties()));
    public static final RegistryObject<Item> RING_GHOUL = ITEMS.register("ring_ghoul", () -> new ItemOriginalRing(ItemOriginalRing.Profile.GHOUL, new Item.Properties()));
    public static final RegistryObject<Item> RING_ALMIGHTY = ITEMS.register("ring_almighty", () -> new ItemOriginalRing(ItemOriginalRing.Profile.ALMIGHTY, new Item.Properties()));
    public static final RegistryObject<Item> RING_SIN_PLUS_1 = ITEMS.register("ring_sin_plus_1", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SIN_PLUS_1, new Item.Properties()));
    public static final RegistryObject<Item> RING_SIN_PLUS_2 = ITEMS.register("ring_sin_plus_2", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SIN_PLUS_2, new Item.Properties()));
    public static final RegistryObject<Item> RING_SIN_PLUS_3 = ITEMS.register("ring_sin_plus_3", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SIN_PLUS_3, new Item.Properties()));
    public static final RegistryObject<Item> RING_UNICORN = ITEMS.register("ring_unicorn", () -> new ItemOriginalRing(ItemOriginalRing.Profile.UNICORN, new Item.Properties()));
    public static final RegistryObject<Item> RING_LION = ITEMS.register("ring_lion", () -> new ItemOriginalRing(ItemOriginalRing.Profile.LION, new Item.Properties()));
    public static final RegistryObject<Item> RING_TIGER_FOX = ITEMS.register("ring_tiger_fox", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TIGER_FOX, new Item.Properties()));
    public static final RegistryObject<Item> RING_ICE_STONE = ITEMS.register("ring_ice_stone", () -> new ItemOriginalRing(ItemOriginalRing.Profile.ICE_STONE, new Item.Properties()));
    public static final RegistryObject<Item> RING_OLD_KING = ITEMS.register("ring_old_king", () -> new ItemOriginalRing(ItemOriginalRing.Profile.OLD_KING, new Item.Properties()));
    public static final RegistryObject<Item> RING_POLAR_BEAR = ITEMS.register("ring_polar_bear", () -> new ItemOriginalRing(ItemOriginalRing.Profile.POLAR_BEAR, new Item.Properties()));
    public static final RegistryObject<Item> RING_DEFENSE_KING = ITEMS.register("ring_defense_king", () -> new ItemOriginalRing(ItemOriginalRing.Profile.DEFENSE_KING, new Item.Properties()));
    public static final RegistryObject<Item> RING_BREAK_RESISTANCE = ITEMS.register("ring_break_resistance", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BREAK_RESISTANCE, new Item.Properties()));
    public static final RegistryObject<Item> RING_COUNTERATTACK = ITEMS.register("ring_counterattack", () -> new ItemOriginalRing(ItemOriginalRing.Profile.COUNTERATTACK, new Item.Properties()));
    public static final RegistryObject<Item> RING_HOLY_FOREST = ITEMS.register("ring_holy_forest", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HOLY_FOREST, new Item.Properties()));
    public static final RegistryObject<Item> RING_MOLASSES = ITEMS.register("ring_molasses", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MOLASSES, new Item.Properties()));
    public static final RegistryObject<Item> RING_RECKLESS_HERO = ITEMS.register("ring_reckless_hero", () -> new ItemOriginalRing(ItemOriginalRing.Profile.RECKLESS_HERO, new Item.Properties()));
    public static final RegistryObject<Item> RING_BANKER = ITEMS.register("ring_banker", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BANKER, new Item.Properties()));
    public static final RegistryObject<Item> RING_HEAVEN = ITEMS.register("ring_heaven", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HEAVEN, new Item.Properties()));
    public static final RegistryObject<Item> RING_BOOTBLACK = ITEMS.register("ring_bootblack", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BOOTBLACK, new Item.Properties()));
    public static final RegistryObject<Item> RING_BUTCHER = ITEMS.register("ring_butcher", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BUTCHER, new Item.Properties()));
    public static final RegistryObject<Item> RING_PROSTITUTE = ITEMS.register("ring_prostitute", () -> new ItemOriginalRing(ItemOriginalRing.Profile.PROSTITUTE, new Item.Properties()));
    public static final RegistryObject<Item> RING_EXORCISM = ITEMS.register("ring_exorcism", () -> new ItemOriginalRing(ItemOriginalRing.Profile.EXORCISM, new Item.Properties()));
    public static final RegistryObject<Item> RING_FIGHTER = ITEMS.register("ring_fighter", () -> new ItemOriginalRing(ItemOriginalRing.Profile.FIGHTER, new Item.Properties()));
    public static final RegistryObject<Item> RING_TROLL = ITEMS.register("ring_troll", () -> new ItemOriginalRing(ItemOriginalRing.Profile.TROLL, new Item.Properties()));
    public static final RegistryObject<Item> RING_MOSQUITO = ITEMS.register("ring_mosquito", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MOSQUITO, new Item.Properties()));
    public static final RegistryObject<Item> RING_RED_TEARSTONE = ITEMS.register("ring_red_tearstone", () -> new ItemOriginalRing(ItemOriginalRing.Profile.RED_TEARSTONE, new Item.Properties()));
    public static final RegistryObject<Item> RING_WALRUS = ITEMS.register("ring_walrus", () -> new ItemOriginalRing(ItemOriginalRing.Profile.WALRUS, new Item.Properties()));
    public static final RegistryObject<Item> RING_HELL_DESTRUCTION = ITEMS.register("ring_hell_destruction", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HELL_DESTRUCTION, new Item.Properties()));
    public static final RegistryObject<Item> RING_HEART_KNIGHT = ITEMS.register("ring_heart_knight", () -> new ItemOriginalRing(ItemOriginalRing.Profile.HEART_KNIGHT, new Item.Properties()));
    public static final RegistryObject<Item> RING_SPADE_KNIGHT = ITEMS.register("ring_spade_knight", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SPADE_KNIGHT, new Item.Properties()));
    public static final RegistryObject<Item> RING_CLUB_KNIGHT = ITEMS.register("ring_club_knight", () -> new ItemOriginalRing(ItemOriginalRing.Profile.CLUB_KNIGHT, new Item.Properties()));
    public static final RegistryObject<Item> RING_SIN = ITEMS.register("ring_sin", () -> new ItemOriginalRing(ItemOriginalRing.Profile.SIN, new Item.Properties()));
    public static final RegistryObject<Item> RING_STAR = ITEMS.register("ring_star", () -> new ItemOriginalRing(ItemOriginalRing.Profile.STAR, new Item.Properties()));
    public static final RegistryObject<Item> RING_OGRE = ITEMS.register("ring_ogre", () -> new ItemOriginalRing(ItemOriginalRing.Profile.OGRE, new Item.Properties()));
    public static final RegistryObject<Item> RING_BEE = ITEMS.register("ring_bee", () -> new ItemOriginalRing(ItemOriginalRing.Profile.BEE, new Item.Properties()));
    public static final RegistryObject<Item> RING_FRENZIED_KING = ITEMS.register("ring_frenzied_king", () -> new ItemOriginalRing(ItemOriginalRing.Profile.FRENZIED_KING, new Item.Properties()));
    public static final RegistryObject<Item> RING_IDATEN = ITEMS.register("ring_idaten", () -> new ItemOriginalRing(ItemOriginalRing.Profile.IDATEN, new Item.Properties()));
    public static final RegistryObject<Item> RING_MY_STRUGGLE = ITEMS.register("ring_my_struggle", () -> new ItemOriginalRing(ItemOriginalRing.Profile.MY_STRUGGLE, new Item.Properties()));
    public static final RegistryObject<Item> RING_ADULTERY = ITEMS.register("ring_adultery", () -> new ItemOriginalRing(ItemOriginalRing.Profile.ADULTERY, new Item.Properties()));
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
    public static final RegistryObject<Item> MAGIC_BLADE = ITEMS.register("magic_blade", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MAGIC_BLADE, new Item.Properties()));
    public static final RegistryObject<Item> DEMON_GOD_BLADE = ITEMS.register("demon_god_blade", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DEMON_GOD_BLADE, new Item.Properties()));
    public static final RegistryObject<Item> MAGICIANS_STAFF = ITEMS.register("magicians_staff", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MAGICIANS_STAFF, new Item.Properties()));
    public static final RegistryObject<Item> ALL_CREATION_STAFF = ITEMS.register("all_creation_staff", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.ALL_CREATION_STAFF, new Item.Properties()));
    public static final RegistryObject<Item> DOUBLE_EDGED_GREATSWORD = ITEMS.register("double_edged_greatsword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DOUBLE_EDGED_GREATSWORD, new Item.Properties()));
    public static final RegistryObject<Item> RAGNAROK = ITEMS.register("ragnarok", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.RAGNAROK, new Item.Properties()));
    public static final RegistryObject<Item> MEAT_CLEAVER_GREATAXE = ITEMS.register("meat_cleaver_greataxe", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MEAT_CLEAVER_GREATAXE, new Item.Properties()));
    public static final RegistryObject<Item> SLAUGHTERER_GREATAXE = ITEMS.register("slaughterer_greataxe", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.SLAUGHTERER_GREATAXE, new Item.Properties()));
    public static final RegistryObject<Item> HUNTING_BOW = ITEMS.register("hunting_bow", () -> new ItemOriginalBow(ItemOriginalBow.Profile.HUNTING, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BRAVE_BOW = ITEMS.register("brave_bow", () -> new ItemOriginalBow(ItemOriginalBow.Profile.BRAVE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MACE = ITEMS.register("mace", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MACE, new Item.Properties()));
    public static final RegistryObject<Item> DIVINE_PUNISHMENT_MACE = ITEMS.register("divine_punishment_mace", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DIVINE_PUNISHMENT_MACE, new Item.Properties()));
    public static final RegistryObject<Item> HALBERD = ITEMS.register("halberd", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.HALBERD, new Item.Properties()));
    public static final RegistryObject<Item> BAHAMUT = ITEMS.register("bahamut", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.BAHAMUT, new Item.Properties()));
    public static final RegistryObject<Item> BEAST_HUNTER_SAW = ITEMS.register("beast_hunter_saw", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.BEAST_HUNTER_SAW, new Item.Properties()));
    public static final RegistryObject<Item> BEAST_SLAYING_SAW_SWORD = ITEMS.register("beast_slaying_saw_sword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.BEAST_SLAYING_SAW_SWORD, new Item.Properties()));
    public static final RegistryObject<Item> SHIELD_GUARD_FORTRESS = ITEMS.register("shield_guard_fortress", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.SHIELD_GUARD_FORTRESS, new Item.Properties()));
    public static final RegistryObject<Item> GUARDIAN_FORTRESS = ITEMS.register("guardian_fortress", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.GUARDIAN_FORTRESS, new Item.Properties()));
    public static final RegistryObject<Item> DARK_SWORD = ITEMS.register("dark_sword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DARK_SWORD, new Item.Properties()));
    public static final RegistryObject<Item> DARK_BLADE = ITEMS.register("dark_blade", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DARK_BLADE, new Item.Properties()));
    public static final RegistryObject<Item> BROKEN_SWORD = ITEMS.register("broken_sword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.BROKEN_SWORD, new Item.Properties()));
    public static final RegistryObject<Item> GRUDGE_SWORD = ITEMS.register("grudge_sword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.GRUDGE_SWORD, new Item.Properties()));
    public static final RegistryObject<Item> WARHAMMER = ITEMS.register("warhammer", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.WARHAMMER, new Item.Properties()));
    public static final RegistryObject<Item> ABERRANT_WARHAMMER = ITEMS.register("aberrant_warhammer", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.ABERRANT_WARHAMMER, new Item.Properties()));
    public static final RegistryObject<Item> KNUCKLE_DUSTER = ITEMS.register("knuckle_duster", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.KNUCKLE_DUSTER, new Item.Properties()));
    public static final RegistryObject<Item> KAISER_GAUNTLET = ITEMS.register("kaiser_gauntlet", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.KAISER_GAUNTLET, new Item.Properties()));
    public static final RegistryObject<Item> UCHIGATANA = ITEMS.register("uchigatana", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.UCHIGATANA, new Item.Properties()));
    public static final RegistryObject<Item> KISHIN_BLADE = ITEMS.register("kishin_blade", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.KISHIN_BLADE, new Item.Properties()));
    public static final RegistryObject<Item> GREAT_IRON_BALL = ITEMS.register("great_iron_ball", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.GREAT_IRON_BALL, new Item.Properties()));
    public static final RegistryObject<Item> HANS_MACHINE_GUN = ITEMS.register("hans_machine_gun", () -> new ItemOriginalBow(ItemOriginalBow.Profile.HANS_MACHINE_GUN, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> JUDGMENT_SCYTHE = ITEMS.register("judgment_scythe", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.JUDGMENT_SCYTHE, new Item.Properties()));
    public static final RegistryObject<Item> STORM_RULER = ITEMS.register("storm_ruler", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.STORM_RULER, new Item.Properties()));
    public static final RegistryObject<Item> DEMON_STAFF = ITEMS.register("demon_staff", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DEMON_STAFF, new Item.Properties()));
    public static final RegistryObject<Item> MOONLIGHT_GREATSWORD = ITEMS.register("moonlight_greatsword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MOONLIGHT_GREATSWORD, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPT_JABBERWOCK_SCYTHE = ITEMS.register("corrupt_jabberwock_scythe", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.CORRUPT_JABBERWOCK_SCYTHE, new Item.Properties()));
    public static final RegistryObject<Item> MAD_BOW_JUBJUB = ITEMS.register("mad_bow_jubjub", () -> new ItemOriginalBow(ItemOriginalBow.Profile.MAD_BOW_JUBJUB, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MIRANDA_AXE = ITEMS.register("miranda_axe", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MIRANDA_AXE, new Item.Properties()));
    public static final RegistryObject<Item> RLYEH_STAFF = ITEMS.register("rlyeh_staff", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.RLYEH_STAFF, new Item.Properties()));
    public static final RegistryObject<Item> DEEP_SEA_KNIGHTS_ANCHOR = ITEMS.register("deep_sea_knights_anchor", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DEEP_SEA_KNIGHTS_ANCHOR, new Item.Properties()));
    public static final RegistryObject<Item> LOST_SWORD = ITEMS.register("lost_sword", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.LOST_SWORD, new Item.Properties()));
    public static final RegistryObject<Item> GLACHID = ITEMS.register("glachid", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.GLACHID, new Item.Properties()));
    public static final RegistryObject<Item> SLAUGHTERERS_CHAINSAW = ITEMS.register("slaughterers_chainsaw", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.SLAUGHTERERS_CHAINSAW, new Item.Properties()));
    public static final RegistryObject<Item> MOCK_TURTLE_SOUP_LADLE = ITEMS.register("mock_turtle_soup_ladle", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MOCK_TURTLE_SOUP_LADLE, new Item.Properties()));
    public static final RegistryObject<Item> DIVINE_ANGEL_DUAL_SWORDS = ITEMS.register("divine_angel_dual_swords", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.DIVINE_ANGEL_DUAL_SWORDS, new Item.Properties()));
    public static final RegistryObject<Item> HOLY_GUNBLADE = ITEMS.register("holy_gunblade", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.HOLY_GUNBLADE, new Item.Properties()));
    public static final RegistryObject<Item> MARY_SUES_BRANCH_STAFF = ITEMS.register("mary_sues_branch_staff", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.MARY_SUES_BRANCH_STAFF, new Item.Properties()));
    public static final RegistryObject<Item> EUNICES_RAPIER = ITEMS.register("eunices_rapier", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.EUNICES_RAPIER, new Item.Properties()));
    public static final RegistryObject<Item> RAIDENS_DUAL_AXES = ITEMS.register("raidens_dual_axes", () -> new ItemOriginalWeapon(ItemOriginalWeapon.Profile.RAIDENS_DUAL_AXES, new Item.Properties()));
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
    public static final RegistryObject<Item> SKILL_BOOK_SOUL_VOLLEY = registerOriginalSkillBook(SkillOriginalMagic.Profile.SOUL_VOLLEY);
    public static final RegistryObject<Item> SKILL_BOOK_DISPEL = registerOriginalSkillBook(SkillOriginalMagic.Profile.DISPEL);
    public static final RegistryObject<Item> SKILL_BOOK_SEE_THROUGH_ATTACK = registerOriginalSkillBook(SkillOriginalMagic.Profile.SEE_THROUGH_ATTACK);
    public static final RegistryObject<Item> SKILL_BOOK_POISON = registerOriginalSkillBook(SkillOriginalMagic.Profile.POISON);
    public static final RegistryObject<Item> SKILL_BOOK_POISON_II = registerOriginalSkillBook(SkillOriginalMagic.Profile.POISON_II);
    public static final RegistryObject<Item> SKILL_BOOK_HYPNOSIS = registerOriginalSkillBook(SkillOriginalMagic.Profile.HYPNOSIS);
    public static final RegistryObject<Item> SKILL_BOOK_CURE = registerOriginalSkillBook(SkillOriginalMagic.Profile.CURE);
    public static final RegistryObject<Item> SKILL_BOOK_MAGIC_BLESSING = registerOriginalSkillBook(SkillOriginalMagic.Profile.MAGIC_BLESSING);
    public static final RegistryObject<Item> SKILL_BOOK_RAMPAGE = registerOriginalSkillBook(SkillOriginalMagic.Profile.RAMPAGE);
    public static final RegistryObject<Item> SKILL_BOOK_FULL_BLESSING = registerOriginalSkillBook(SkillOriginalMagic.Profile.FULL_BLESSING);
    public static final RegistryObject<Item> SKILL_BOOK_RESURRECTION = registerOriginalSkillBook(SkillOriginalMagic.Profile.RESURRECTION);
    public static final RegistryObject<Item> SKILL_BOOK_MANA_ABSORPTION = registerOriginalSkillBook(SkillOriginalMagic.Profile.MANA_ABSORPTION);
    public static final RegistryObject<Item> SKILL_BOOK_ERASE = registerOriginalSkillBook(SkillOriginalMagic.Profile.ERASE);
    public static final RegistryObject<Item> SKILL_BOOK_KINGS_COMMAND = registerOriginalSkillBook(SkillOriginalMagic.Profile.KINGS_COMMAND);
    public static final RegistryObject<Item> SKILL_BOOK_FIRE = registerOriginalSkillBook(SkillOriginalMagic.Profile.FIRE);
    public static final RegistryObject<Item> SKILL_BOOK_DROWNING_BUBBLE = registerOriginalSkillBook(SkillOriginalMagic.Profile.DROWNING_BUBBLE);
    public static final RegistryObject<Item> SKILL_BOOK_DARK_SIDE_OF_MOON = registerOriginalSkillBook(SkillOriginalMagic.Profile.DARK_SIDE_OF_MOON);
    public static final RegistryObject<Item> SKILL_BOOK_FREEZING_MAGIC_BULLET = registerOriginalSkillBook(SkillOriginalMagic.Profile.FREEZING_MAGIC_BULLET);
    public static final RegistryObject<Item> SKILL_BOOK_HELLFIRE = registerOriginalSkillBook(SkillOriginalMagic.Profile.HELLFIRE);
    public static final RegistryObject<Item> SKILL_BOOK_DESTRUCTION_STORM = registerOriginalSkillBook(SkillOriginalMagic.Profile.DESTRUCTION_STORM);
    public static final RegistryObject<Item> SKILL_BOOK_INNER_POTENTIAL = registerOriginalSkillBook(SkillOriginalMagic.Profile.INNER_POTENTIAL);
    public static final RegistryObject<Item> SKILL_BOOK_GREAT_SOUL_ARROW = registerOriginalSkillBook(SkillOriginalMagic.Profile.GREAT_SOUL_ARROW);
    public static final RegistryObject<Item> SKILL_BOOK_VERDANT_POWER = registerOriginalSkillBook(SkillOriginalMagic.Profile.VERDANT_POWER);
    public static final RegistryObject<Item> SKILL_BOOK_ROCK_BODY = registerOriginalSkillBook(SkillOriginalMagic.Profile.ROCK_BODY);
    public static final RegistryObject<Item> SKILL_BOOK_DARK_ORB = registerOriginalSkillBook(SkillOriginalMagic.Profile.DARK_ORB);
    public static final RegistryObject<Item> SKILL_BOOK_DARK_DANCE = registerOriginalSkillBook(SkillOriginalMagic.Profile.DARK_DANCE);
    public static final RegistryObject<Item> SKILL_BOOK_DARK_SWARM = registerOriginalSkillBook(SkillOriginalMagic.Profile.DARK_SWARM);
    public static final RegistryObject<Item> SKILL_BOOK_DIVINE_THUNDER = registerOriginalSkillBook(SkillOriginalMagic.Profile.DIVINE_THUNDER);
    public static final RegistryObject<Item> SKILL_BOOK_DIVINE_BEAST_THUNDER = registerOriginalSkillBook(SkillOriginalMagic.Profile.DIVINE_BEAST_THUNDER);
    public static final RegistryObject<Item> SKILL_BOOK_METEOR_SWARM = registerOriginalSkillBook(SkillOriginalMagic.Profile.METEOR_SWARM);
    public static final RegistryObject<Item> SKILL_BOOK_FULL_CURSE = registerOriginalSkillBook(SkillOriginalMagic.Profile.FULL_CURSE);
    public static final RegistryObject<Item> SKILL_BOOK_GREAT_SOUL_ARROW_VOLLEY = registerOriginalSkillBook(SkillOriginalMagic.Profile.GREAT_SOUL_ARROW_VOLLEY);
    public static final RegistryObject<Item> SKILL_BOOK_FATAL_GUARD = registerOriginalSkillBook(SkillOriginalMagic.Profile.FATAL_GUARD);
    public static final RegistryObject<Item> SKILL_BOOK_GHOST_FIRE = registerOriginalSkillBook(SkillOriginalMagic.Profile.GHOST_FIRE);
    public static final RegistryObject<Item> SKILL_BOOK_PHALANX = registerOriginalSkillBook(SkillOriginalMagic.Profile.PHALANX);
    public static final RegistryObject<Item> SKILL_BOOK_ABSOLUTE_HIT = registerOriginalSkillBook(SkillOriginalMagic.Profile.ABSOLUTE_HIT);
    public static final RegistryObject<Item> SKILL_BOOK_CHAOS_EXPLOSION = registerOriginalSkillBook(SkillOriginalMagic.Profile.CHAOS_EXPLOSION);
    public static final RegistryObject<Item> SKILL_BOOK_CRITICAL_STRIKE = registerOriginalSkillBook(SkillOriginalMagic.Profile.CRITICAL_STRIKE);
    public static final RegistryObject<Item> SKILL_BOOK_SOUL_SHIELD = registerOriginalSkillBook(SkillOriginalMagic.Profile.SOUL_SHIELD);
    public static final RegistryObject<Item> SKILL_BOOK_DENSE_SPIROCHETE = registerOriginalSkillBook(SkillOriginalMagic.Profile.DENSE_SPIROCHETE);
    public static final RegistryObject<Item> SKILL_BOOK_SUMMON_MEAT_WALL = registerOriginalSkillBook(SkillOriginalMagic.Profile.SUMMON_MEAT_WALL);
    public static final RegistryObject<Item> SKILL_BOOK_TORN_GRUDGE = registerOriginalSkillBook(SkillOriginalMagic.Profile.TORN_GRUDGE);
    public static final RegistryObject<Item> SKILL_BOOK_PIERCING_ICICLE = registerOriginalSkillBook(SkillOriginalMagic.Profile.PIERCING_ICICLE);
    public static final RegistryObject<Item> SKILL_BOOK_RAIN_OF_RUIN = registerOriginalSkillBook(SkillOriginalMagic.Profile.RAIN_OF_RUIN);
    public static final RegistryObject<Item> SKILL_BOOK_GLOOMY_SWAMP = registerOriginalSkillBook(SkillOriginalMagic.Profile.GLOOMY_SWAMP);
    public static final RegistryObject<Item> SKILL_BOOK_ACID_RAIN = registerOriginalSkillBook(SkillOriginalMagic.Profile.ACID_RAIN);
    public static final RegistryObject<Item> SKILL_BOOK_ROYAL_TEA = registerOriginalSkillBook(SkillOriginalMagic.Profile.ROYAL_TEA);
    public static final RegistryObject<Item> SKILL_BOOK_GODSPEED_DANCE = registerOriginalSkillBook(SkillOriginalMagic.Profile.GODSPEED_DANCE);
    public static final RegistryObject<Item> SKILL_BOOK_KATARINA_WHEEL = registerOriginalSkillBook(SkillOriginalMagic.Profile.KATARINA_WHEEL);
    public static final RegistryObject<Item> SKILL_BOOK_PALADIN_BANNER = registerOriginalSkillBook(SkillOriginalMagic.Profile.PALADIN_BANNER);
    public static final RegistryObject<Item> SKILL_BOOK_BLACK_WAVE = registerOriginalSkillBook(SkillOriginalMagic.Profile.BLACK_WAVE);
    public static final RegistryObject<Item> SKILL_BOOK_BLACK_SLASH = registerOriginalSkillBook(SkillOriginalMagic.Profile.BLACK_SLASH);
    public static final RegistryObject<Item> SKILL_BOOK_AWAKENING = registerOriginalSkillBook(SkillOriginalMagic.Profile.AWAKENING);
    public static final RegistryObject<Item> SKILL_BOOK_SERPENT_EMBRACE = registerOriginalSkillBook(SkillOriginalMagic.Profile.SERPENT_EMBRACE);
    public static final RegistryObject<Item> SKILL_BOOK_SOUL_STREAM = registerOriginalSkillBook(SkillOriginalMagic.Profile.SOUL_STREAM);
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
                        // 普通武器
                        // ===========================================================
                        output.accept(KNIGHT_SWORD.get());                 // 骑士之剑
                        output.accept(KNIGHT_KING_SWORD.get());            // 骑士王之剑
                        output.accept(THIEFS_DAGGER.get());                // 盗贼短刀
                        output.accept(GREAT_THIEFS_DAGGER.get());          // 大盗贼的短刀
                        output.accept(GREAT_SWORD.get());                  // 大剑
                        output.accept(GIANT_SWORD.get());                  // 巨人剑
                        output.accept(MAGIC_BLADE.get());                  // 魔刀
                        output.accept(DEMON_GOD_BLADE.get());              // 魔神刀
                        output.accept(BROAD_SPEAR.get());                  // 阔头枪
                        output.accept(GUNGNIR.get());                      // 冈格尼尔
                        output.accept(CLUB.get());                         // 棍棒
                        output.accept(KING_CLUB.get());                    // 王棒
                        output.accept(MAGICIANS_STAFF.get());              // 魔术师之杖
                        output.accept(ALL_CREATION_STAFF.get());           // 万象之杖
                        output.accept(HUNTING_BOW.get());                  // 猎弓
                        output.accept(BRAVE_BOW.get());                    // 勇者之弓
                        output.accept(MEAT_CLEAVER_GREATAXE.get());        // 断肉大斧
                        output.accept(SLAUGHTERER_GREATAXE.get());         // 虐杀者大斧
                        output.accept(MACE.get());                         // 锤矛
                        output.accept(DIVINE_PUNISHMENT_MACE.get());       // 神罚的锤矛
                        output.accept(HALBERD.get());                      // 斧枪
                        output.accept(BAHAMUT.get());                      // 巴哈姆特
                        output.accept(BEAST_HUNTER_SAW.get());             // 猎兽锯
                        output.accept(BEAST_SLAYING_SAW_SWORD.get());      // 猎杀魔兽之锯剑
                        output.accept(SHIELD_GUARD_FORTRESS.get());        // 盾卫堡垒
                        output.accept(GUARDIAN_FORTRESS.get());            // 守护者堡垒
                        output.accept(DARK_SWORD.get());                   // 黑暗剑
                        output.accept(DARK_BLADE.get());                   // 黑暗之刃
                        output.accept(BROKEN_SWORD.get());                 // 断剑
                        output.accept(GRUDGE_SWORD.get());                 // 怨恨之剑
                        output.accept(WARHAMMER.get());                    // 战锤
                        output.accept(ABERRANT_WARHAMMER.get());           // 异形战锤
                        output.accept(KNUCKLE_DUSTER.get());               // 指虎
                        output.accept(KAISER_GAUNTLET.get());              // 凯撒拳套
                        output.accept(VORPAL_BLADE.get());                 // 沃柏尔之刃
                        output.accept(VORPAL_SWORD.get());                 // 沃柏尔之剑
                        output.accept(UCHIGATANA.get());                   // 打刀
                        output.accept(KISHIN_BLADE.get());                 // 鬼神刀
                        output.accept(BRAVE_SWORD_VORPAL.get());           // 勇剑沃柏尔
                        output.accept(DOUBLE_EDGED_GREATSWORD.get());      // 双刃大剑
                        output.accept(RAGNAROK.get());                     // 诸神黄昏
                        // ===========================================================
                        // 特殊武器
                        // ===========================================================
                        output.accept(GREAT_IRON_BALL.get());              // 大铁球
                        output.accept(HANS_MACHINE_GUN.get());             // 汉斯的机关枪
                        output.accept(JUDGMENT_SCYTHE.get());              // 审判者大镰
                        output.accept(STORM_RULER.get());                  // 暴风之律
                        output.accept(ANDOR_SWORD.get());                  // 安多鲁之剑
                        output.accept(DRAKE_SWORD.get());                  // 飞龙剑
                        output.accept(DEMON_STAFF.get());                  // 恶魔之杖
                        output.accept(MOONLIGHT_GREATSWORD.get());         // 月光大剑
                        output.accept(BANDERSNATCH_SWORD.get());           // 暴剑班达斯奈奇
                        output.accept(CORRUPT_JABBERWOCK_SCYTHE.get());    // 腐镰贾巴沃克
                        output.accept(MAD_BOW_JUBJUB.get());               // 狂弓贾布加布
                        output.accept(MIRANDA_AXE.get());                  // 米兰达之斧
                        output.accept(RLYEH_STAFF.get());                  // 拉莱耶之杖
                        output.accept(DEEP_SEA_KNIGHTS_ANCHOR.get());      // 深海骑士的锚
                        output.accept(LOST_SWORD.get());                   // 失落之剑
                        output.accept(GLACHID.get());                      // 格拉奇德
                        output.accept(SLAUGHTERERS_CHAINSAW.get());        // 屠宰者的电锯
                        output.accept(MOCK_TURTLE_SOUP_LADLE.get());       // 假海龟的大汤勺
                        output.accept(DIVINE_ANGEL_DUAL_SWORDS.get());     // 神天使双剑
                        output.accept(HOLY_GUNBLADE.get());                // 神圣铳剑
                        output.accept(MARY_SUES_BRANCH_STAFF.get());       // 玛丽苏的枝杖
                        output.accept(EUNICES_RAPIER.get());               // 尤妮丝的刺剑
                        output.accept(RAIDENS_DUAL_AXES.get());            // 莱登的双斧
                        // =============================================================
                        // 盾系列
                        // =============================================================
                        output.accept(MURDERERS_SHOTGUN.get());            // 杀人魔霰弹铳
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
                        output.accept(WORK_CLOTHES.get());                 // 作业服
                        output.accept(GENTLEMAN_COAT.get());               // 绅士外套
                        output.accept(PROSTITUTE_DRESS.get());             // 娼妇之服
                        output.accept(PLATE_ARMOR.get());                  // 板甲
                        output.accept(ABYSS_ARMOR.get());                  // 深渊之铠
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
                        output.accept(ABYSS_HELMET.get());                 // 深渊之盔
                        output.accept(OMINOUS_CLOTHES.get());              // 不吉的上衣
                        output.accept(BUTETSU_ARMOR.get());                // 武铁之铠
                        output.accept(YELLOW_CLOTH.get());                 // 黄之布
                        output.accept(GUARDIAN_ANGEL.get());               // 守护天使
                        output.accept(PLAYWRIGHT_HEADSCARF.get());         // 剧作家的头巾
                        output.accept(FALSE_ANGEL_CROWN.get());            // 伪天使的花冠
                        output.accept(MYSTERY_OF_NIGHT_SKY.get());         // 夜空的神秘
                        output.accept(WINTER_MAGE_COAT.get());             // 冬魔导士的外套
                        output.accept(WINTER_KNIGHT_ARMOR.get());          // 冬骑士之铠
                        output.accept(WINTER_KNIGHT_HELMET.get());         // 冬骑士之盔
                        output.accept(WINDLESS_CLOTHES.get());             // 无风之衣
                        output.accept(MIRACLE_SHRINE_MAIDEN_GARB.get());   // 奇迹的巫女装束
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
                        output.accept(RING_TENACIOUS.get());               // 坚韧者的戒指
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
                        output.accept(RING_REBELLION.get());               // 叛逆戒指
                        output.accept(RING_TERROR.get());                  // 恐怖戒指
                        output.accept(RING_IRON_MAIDEN.get());             // 铁处女戒指
                        output.accept(RING_KNIGHT.get());                  // 骑士戒指
                        output.accept(RING_ANGEL.get());                   // 天使戒指
                        output.accept(RING_FAIRY.get());                   // 叮当仙子的戒指
                        output.accept(RING_VOID.get());                    // 空虚戒指
                        output.accept(RING_DRAGON_GUARD.get());            // 龙之守护戒指
                        output.accept(RING_MIDNIGHT_CROWN.get());          // 宵暗的指冠
                        output.accept(RING_MIRACLE.get());                 // 奇迹戒指
                        output.accept(RING_MURDER_CLOWN.get());            // 杀人小丑戒指
                        output.accept(RING_BLACK_GOAT.get());              // 黑山羊戒指
                        output.accept(RING_DEATH.get());                   // 死神戒指
                        output.accept(RING_BARBER.get());                  // 理发师戒指
                        output.accept(RING_VANITY.get());                  // 虚饰戒指
                        output.accept(RING_APPLE.get());                   // 苹果戒指
                        output.accept(RING_LUNDINIAN.get());               // 伦蒂尼恩的戒指
                        output.accept(RING_PUMPKIN_KNIGHT.get());          // 南瓜骑士戒指
                        output.accept(RING_SNIPER.get());                  // 狙击手戒指
                        output.accept(RING_DEEP_ONE.get());                // 深者戒指
                        output.accept(RING_WHITE_RAVEN.get());             // 白鸦戒指
                        output.accept(RING_DULL_WOOD_GRAIN.get());         // 暗沉木纹戒指
                        output.accept(RING_TOTO.get());                    // 托托的戒指
                        output.accept(RING_FOUR_LEAF_CLOVER.get());        // 四叶草戒指
                        output.accept(RING_PUPPET.get());                  // 傀儡戒指
                        output.accept(RING_RECKLESS_HERO.get());           // 无谋勇者戒指
                        output.accept(RING_BANKER.get());                  // 银行家戒指
                        output.accept(RING_HEAVEN.get());                  // 天之戒指
                        output.accept(RING_BOOTBLACK.get());               // 擦靴人的戒指
                        output.accept(RING_LIEF.get());                    // 莉耶芙的戒指
                        output.accept(RING_BUTCHER.get());                 // 屠夫戒指
                        output.accept(RING_PROSTITUTE.get());              // 娼妇戒指
                        output.accept(RING_EXORCISM.get());                // 退魔戒指
                        output.accept(RING_ABYSS.get());                   // 深渊戒指
                        output.accept(RING_FIGHTER.get());                 // 斗士戒指
                        output.accept(RING_BLACK_RABBIT.get());            // 黑兔戒指
                        output.accept(RING_TROLL.get());                   // 巨魔戒指
                        output.accept(RING_MOSQUITO.get());                // 蚊之戒指
                        output.accept(RING_RED_TEARSTONE.get());           // 红泪石戒指
                        output.accept(RING_EDITH.get());                   // 伊迪斯的戒指
                        output.accept(RING_WALRUS.get());                  // 海象的戒指
                        output.accept(RING_HELL_DESTRUCTION.get());        // 狱灭戒指
                        output.accept(RING_HEART_KNIGHT.get());            // 红心骑士戒指
                        output.accept(RING_SPADE_KNIGHT.get());            // 黑桃骑士戒指
                        output.accept(RING_CLUB_KNIGHT.get());             // 草花骑士戒指
                        output.accept(RING_WHITE_RABBIT.get());            // 白兔戒指
                        output.accept(RING_GOD_FISH.get());                // 神鱼戒指
                        output.accept(RING_SIN.get());                     // 罪恶戒指
                        output.accept(RING_STAR.get());                    // 星辰戒指
                        output.accept(RING_BLACKBEARD.get());              // 黑胡子戒指
                        output.accept(RING_PRICKETT.get());                // 普利凯特的戒指
                        output.accept(RING_OGRE.get());                    // 食人魔戒指
                        output.accept(RING_BEE.get());                     // 蜜蜂戒指
                        output.accept(RING_FRENZIED_KING.get());           // 狂乱王的戒指
                        output.accept(RING_IDATEN.get());                  // 韦陀天戒指
                        output.accept(RING_MY_STRUGGLE.get());             // 我的奋斗戒指
                        output.accept(RING_ADULTERY.get());                // 奸淫戒指
                        output.accept(RING_LIFE_PLUS_1.get());             // 生命戒指＋1
                        output.accept(RING_LIFE_PLUS_2.get());             // 生命戒指＋2
                        output.accept(RING_LIFE_PLUS_3.get());             // 生命戒指＋3
                        output.accept(RING_TENACIOUS_PLUS_1.get());        // 坚韧者的戒指＋1
                        output.accept(RING_TENACIOUS_PLUS_2.get());        // 坚韧者的戒指＋2
                        output.accept(RING_TENACIOUS_PLUS_3.get());        // 坚韧者的戒指＋3
                        output.accept(RING_PUYO_PLUS_1.get());             // 噗哟噗哟＋1
                        output.accept(RING_PUYO_PLUS_2.get());             // 噗哟噗哟＋2
                        output.accept(RING_PUYO_PLUS_3.get());             // 噗哟噗哟＋3
                        output.accept(RING_HUNYA_PLUS_1.get());            // 呼扭呼扭＋1
                        output.accept(RING_HUNYA_PLUS_2.get());            // 呼扭呼扭＋2
                        output.accept(RING_HUNYA_PLUS_3.get());            // 呼扭呼扭＋3
                        output.accept(RING_VOID_PLUS_1.get());             // 空虚戒指＋1
                        output.accept(RING_VOID_PLUS_2.get());             // 空虚戒指＋2
                        output.accept(RING_VOID_PLUS_3.get());             // 空虚戒指＋3
                        output.accept(RING_EVIL_EYE_PLUS_1.get());         // 邪眼戒指＋1
                        output.accept(RING_EVIL_EYE_PLUS_2.get());         // 邪眼戒指＋2
                        output.accept(RING_EVIL_EYE_PLUS_3.get());         // 邪眼戒指＋3
                        output.accept(RING_GODDESS_PLUS_1.get());          // 女神的戒指＋1
                        output.accept(RING_GODDESS_PLUS_2.get());          // 女神的戒指＋2
                        output.accept(RING_GODDESS_PLUS_3.get());          // 女神的戒指＋3
                        output.accept(RING_IRON_PROTECTION_PLUS_1.get());  // 铁之加护戒指＋1
                        output.accept(RING_IRON_PROTECTION_PLUS_2.get());  // 铁之加护戒指＋2
                        output.accept(RING_IRON_PROTECTION_PLUS_3.get());  // 铁之加护戒指＋3
                        output.accept(RING_MAGIC_STONE_PLUS_1.get());      // 魔法方石戒指＋1
                        output.accept(RING_MAGIC_STONE_PLUS_2.get());      // 魔法方石戒指＋2
                        output.accept(RING_MAGIC_STONE_PLUS_3.get());      // 魔法方石戒指＋3
                        output.accept(RING_SNIPER_PLUS_1.get());           // 狙击手戒指＋1
                        output.accept(RING_SNIPER_PLUS_2.get());           // 狙击手戒指＋2
                        output.accept(RING_SNIPER_PLUS_3.get());           // 狙击手戒指＋3
                        output.accept(RING_WASP_PLUS_1.get());             // 黄蜂戒指＋1
                        output.accept(RING_WASP_PLUS_2.get());             // 黄蜂戒指＋2
                        output.accept(RING_WASP_PLUS_3.get());             // 黄蜂戒指＋3
                        output.accept(RING_BLADES_PLUS_1.get());           // 刃之戒指＋1
                        output.accept(RING_BLADES_PLUS_2.get());           // 刃之戒指＋2
                        output.accept(RING_BLADES_PLUS_3.get());           // 刃之戒指＋3
                        output.accept(RING_GUARD_PLUS_1.get());            // 守护戒指＋1
                        output.accept(RING_GUARD_PLUS_2.get());            // 守护戒指＋2
                        output.accept(RING_GUARD_PLUS_3.get());            // 守护戒指＋3
                        output.accept(RING_WIND_GOD_PLUS_1.get());         // 风神戒指＋1
                        output.accept(RING_WIND_GOD_PLUS_2.get());         // 风神戒指＋2
                        output.accept(RING_WIND_GOD_PLUS_3.get());         // 风神戒指＋3
                        output.accept(RING_SPELL_PLUS_1.get());            // 咒术戒指＋1
                        output.accept(RING_SPELL_PLUS_2.get());            // 咒术戒指＋2
                        output.accept(RING_SPELL_PLUS_3.get());            // 咒术戒指＋3
                        output.accept(RING_LUNDINIAN_PLUS_1.get());        // 伦蒂尼恩的戒指＋1
                        output.accept(RING_LUNDINIAN_PLUS_2.get());        // 伦蒂尼恩的戒指＋2
                        output.accept(RING_LUNDINIAN_PLUS_3.get());        // 伦蒂尼恩的戒指＋3
                        output.accept(RING_CUT_DOWN.get());                // 削落戒指
                        output.accept(RING_GHOUL.get());                   // 食尸鬼戒指
                        output.accept(RING_ALMIGHTY.get());                // 全能戒指
                        output.accept(RING_SIN_PLUS_1.get());              // 罪恶戒指+1
                        output.accept(RING_SIN_PLUS_2.get());              // 罪恶戒指+2
                        output.accept(RING_SIN_PLUS_3.get());              // 罪恶戒指+3
                        output.accept(RING_UNICORN.get());                 // 独角兽戒指
                        output.accept(RING_LION.get());                    // 狮子戒指
                        output.accept(RING_TIGER_FOX.get());               // 虎狐戒指
                        output.accept(RING_ICE_STONE.get());               // 冰方石戒指
                        output.accept(RING_OLD_KING.get());                // 古王戒指
                        output.accept(RING_POLAR_BEAR.get());              // 白熊戒指
                        output.accept(RING_DEFENSE_KING.get());            // 防王戒指
                        output.accept(RING_BREAK_RESISTANCE.get());        // 破耐戒指
                        output.accept(RING_COUNTERATTACK.get());           // 逆袭戒指
                        output.accept(RING_HOLY_FOREST.get());             // 圣森戒指
                        output.accept(RING_MOLASSES.get());                // 糖蜜戒指
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
                        output.accept(MAGIC_STONE.get());                  // 魔石
                        output.accept(MAIDENSFRAGRANCE.get());             // 少女之香
                        output.accept(FAIRY_SCALE_POWDER.get());           // 妖精的鳞粉
                        output.accept(MYSTERIOUS_SHARD.get());             // 神秘的碎片
                        output.accept(UPGRADE_SHARD.get());                // 强化石的碎片
                        output.accept(UPGRADE_LARGE_SHARD.get());          // 强化石大碎片
                        output.accept(UPGRADE_CHUNK.get());                // 强化石块
                        output.accept(UPGRADE_SLAB.get());                 // 强化石圆盘
                        output.accept(FIRE_BOMB.get());                    // 火焰壶
                        output.accept(DUNG_PIE.get());                     // 屎块
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
                        output.accept(MASTER_KEY.get());                   // 万能钥匙
                        output.accept(BLACKWELL_BLOOD_VIAL.get());         // 布莱克威尔的输血药
                        output.accept(CANDY.get());                        // 糖果
                        output.accept(OIL_URN.get());                      // 油壶
                        output.accept(THROWING_KNIFE.get());               // 投掷小刀
                        output.accept(UNDEAD_KILLER_MUSHROOM.get());       // 不死者杀手菇
                        output.accept(PURE_WATER.get());                   // 水
                        output.accept(STAMINA_TONIC.get());                // 精力剂
                        output.accept(SNAKE_BONE_RETURN.get());            // 归还蛇骨
                        output.accept(MUDDY_FISH.get());                   // 浑浊之鱼
                        output.accept(WHITE_STICKY_THING.get());           // 又白又黏的那啥
                        output.accept(IRON_SCRAP_SNACK.get());             // 铁渣点心
                        output.accept(FAIRY_FEATHER.get());                // 妖精之羽
                        output.accept(GOLDENMEAD.get());                   // 黄金的蜂蜜酒
                        output.accept(CARPENTER_NAIL.get());               // 大工的钉子
                        output.accept(PRESCRIPTION_MEDICINE.get());        // 处方药
                        output.accept(GIRLS_PHOTO.get());                  // 少女的写真
                        output.accept(RETRIEVAL_POKER.get());              // 再思的扑克
                        output.accept(GOAT_MEAT.get());                    // 山羊的肉
                        output.accept(PREGNANT_CAKE_MEAT.get());           // 孕妇蛋糕之肉
                        output.accept(BLACK_ASH.get());                    // 黑之灰
                        output.accept(BLOODSTAINED_KEY.get());             // 染血的钥匙
                        output.accept(DRINK_ME.get());                     // 喝了我吧
                        output.accept(EAT_ME.get());                       // 吃了我吧
                        output.accept(RABBIT_KEY.get());                   // 兔之键
                        output.accept(GOLDEN_EGG.get());                   // 黄金之卵
                        output.accept(TRAIN_TICKET.get());                 // 列车票
                        output.accept(ENTRY_PASS.get());                   // 通行证
                        output.accept(QUEEN_EGG_TART.get());               // 女王的蛋挞
                        output.accept(CANDLE_EMBER.get());                 // 蜡烛的余烬
                        output.accept(ROASTED_CHEESE.get());               // 烤起司
                        output.accept(TURTLE_SOUP.get());                  // 海龟汤
                        output.accept(SOUL_BLACK_DEFILED.get());           // 污秽的黑之魂
                        output.accept(DREAM_SOUL.get());                   // 梦之魂
                        output.accept(SNAKE_GOD_BLOOD.get());              // 蛇神的血
                        output.accept(ALICE_ITEM.get());                   // 爱丽丝
                        output.accept(BILLS_BENTO.get());                  // 比尔的便当
                        output.accept(SOUL_OUTSIDER.get());                // 外来者之魂
                        output.accept(SOUL_HERO.get());                    // 英雄的灵魂
                        output.accept(SOUL_GREAT_HERO.get());              // 伟大英雄的灵魂
                        output.accept(MATCH_MEDICINE.get());               // 火柴药
                        output.accept(MAD_GEAR.get());                     // 疯狂的齿轮
                        output.accept(NIGHTMARE_LANTERN.get());            // 噩梦提灯
                        output.accept(CHICKEN.get());                      // 鸡肉
                        output.accept(CHRISTMAS_CHICKEN.get());            // 圣诞鸡肉
                        output.accept(MYSTERIOUS_MEAT.get());              // 来路不明的肉
                        output.accept(SATYRS_THING.get());                 // 色情魔的那玩意
                        output.accept(MERMAIDSONG.get());                  // 人鱼的歌声
                        output.accept(ANCIENT_KINGS_BONE_DUST.get());      // 古王的骨粉
                        output.accept(SQUIRREL_FUR.get());                 // 松鼠的毛
                        output.accept(ICE_PINE_RESIN.get());               // 冰松脂
                        output.accept(SCALPEL.get());                      // 手术刀
                        output.accept(STAR_WATER.get());                   // 星水
                        output.accept(FILTHY_LIQUID.get());                // 脏液
                        output.accept(BLUEBIRD_FEATHER.get());             // 青鸟的羽毛
                        output.accept(TINKER_BELLS_SCALE_POWDER.get());    // 叮当仙子的鳞粉
                        output.accept(OUIJA_BOARD.get());                  // 威加盘
                        output.accept(ROLDS_FOUNTAIN_PEN.get());           // 洛德的万年钢笔
                        output.accept(CURSING_FLOWER.get());               // 咒骂之花
                        output.accept(COLD_VALLEY_BREATH.get());           // 冷谷的气息
                        output.accept(HELANRITHWINE.get());                // 海兰里斯酒
                        output.accept(NECRONOMICON.get());                 // 死灵之书
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
                        output.accept(SKILL_BOOK_SOUL_VOLLEY.get());        // 魔书【魂之连射】
                        output.accept(SKILL_BOOK_SOUL_LIGHT.get());         // 魔书【魂之光】
                        output.accept(SKILL_BOOK_SOUL_RADIATION.get());     // 魔书【魂之放射】
                        output.accept(SKILL_BOOK_DISPEL.get());             // 魔书【驱散】
                        output.accept(SKILL_BOOK_SEE_THROUGH_ATTACK.get()); // 魔书【识破攻击】
                        output.accept(SKILL_BOOK_CARRHUS_BLOOD_CURSE.get());// 魔书【卡萨斯血咒】
                        output.accept(SKILL_BOOK_POISON.get());             // 魔书【毒素】
                        output.accept(SKILL_BOOK_POISON_II.get());          // 魔书【毒素Ⅱ】
                        output.accept(SKILL_BOOK_HYPNOSIS.get());           // 魔书【催眠】
                        output.accept(SKILL_BOOK_CURE.get());               // 圣书【治愈】
                        output.accept(SKILL_BOOK_MAGIC_BLESSING.get());     // 魔书【魔力祝福】
                        output.accept(SKILL_BOOK_RAMPAGE.get());            // 魔书【横冲直撞】
                        output.accept(SKILL_BOOK_FULL_BLESSING.get());      // 魔书【全面祝福】
                        output.accept(SKILL_BOOK_RESURRECTION.get());       // 圣书【还魂】
                        output.accept(SKILL_BOOK_MANA_ABSORPTION.get());    // 魔书【魔力吸收】
                        output.accept(SKILL_BOOK_ERASE.get());              // 圣书【擦除】
                        output.accept(SKILL_BOOK_KINGS_COMMAND.get());      // 魔书【王之号令】
                        output.accept(SKILL_BOOK_REQUIEM.get());            // 魔书【镇魂歌】
                        output.accept(SKILL_BOOK_GRIT.get());               // 魔书【咬紧牙关】
                        output.accept(SKILL_BOOK_FIRE.get());               // 魔书【火炎】
                        output.accept(SKILL_BOOK_DROWNING_BUBBLE.get());    // 魔书【沉溺之泡】
                        output.accept(SKILL_BOOK_DARK_SIDE_OF_MOON.get());  // 魔书【月之暗面】
                        output.accept(SKILL_BOOK_FREEZING_MAGIC_BULLET.get());// 魔书【冰结之魔弹】
                        output.accept(SKILL_BOOK_HELLFIRE.get());           // 魔书【业火】
                        output.accept(SKILL_BOOK_DESTRUCTION_STORM.get());  // 魔书【破灭风暴】
                        output.accept(SKILL_BOOK_INNER_POTENTIAL.get());    // 魔书【内在潜力】
                        output.accept(SKILL_BOOK_GREAT_SOUL_ARROW.get());   // 魔书【魂之巨矢】
                        output.accept(SKILL_BOOK_VERDANT_POWER.get());      // 魔书【新绿之力】
                        output.accept(SKILL_BOOK_ROCK_BODY.get());          // 魔书【岩之体】
                        output.accept(SKILL_BOOK_DARK_ORB.get());           // 魔书【暗之球】
                        output.accept(SKILL_BOOK_DARK_DANCE.get());         // 魔书【暗之乱舞】
                        output.accept(SKILL_BOOK_DARK_SWARM.get());         // 魔书【暗之群来】
                        output.accept(SKILL_BOOK_DIVINE_THUNDER.get());     // 魔书【神雷】
                        output.accept(SKILL_BOOK_DIVINE_BEAST_THUNDER.get());// 魔书【神兽之雷鸣】
                        output.accept(SKILL_BOOK_METEOR_SWARM.get());       // 魔书【流星群】
                        output.accept(SKILL_BOOK_FULL_CURSE.get());         // 魔书【全面诅咒】
                        output.accept(SKILL_BOOK_GREAT_SOUL_ARROW_VOLLEY.get());// 魔书【魂之巨矢连射】
                        output.accept(SKILL_BOOK_INVISIBLE.get());          // 魔书【看不见的身体】
                        output.accept(SKILL_BOOK_FATAL_GUARD.get());        // 圣书【致命守护】
                        output.accept(SKILL_BOOK_GHOST_FIRE.get());         // 魔书【幽火】
                        output.accept(SKILL_BOOK_PHALANX.get());            // 魔书【法拉克斯】
                        output.accept(SKILL_BOOK_ABSOLUTE_HIT.get());       // 魔书【绝对必中】
                        output.accept(SKILL_BOOK_CHAOS_EXPLOSION.get());    // 魔书【混沌爆炎】
                        output.accept(SKILL_BOOK_CRITICAL_STRIKE.get());    // 魔书【会心一击】
                        output.accept(SKILL_BOOK_SOUL_SHIELD.get());        // 圣书【灵魂盾】
                        output.accept(SKILL_BOOK_DENSE_SPIROCHETE.get());   // 魔书【密螺旋体】
                        output.accept(SKILL_BOOK_SUMMON_MEAT_WALL.get());   // 圣书【肉壁召唤】
                        output.accept(SKILL_BOOK_TORN_GRUDGE.get());        // 魔书【撕裂的遗恨】
                        output.accept(SKILL_BOOK_PIERCING_ICICLE.get());    // 魔书【贯穿冰柱】
                        output.accept(SKILL_BOOK_RAIN_OF_RUIN.get());       // 魔书【灭亡的箭雨】
                        output.accept(SKILL_BOOK_GLOOMY_SWAMP.get());       // 魔书【阴暗之沼】
                        output.accept(SKILL_BOOK_ACID_RAIN.get());          // 魔书【酸雨】
                        output.accept(SKILL_BOOK_ROYAL_TEA.get());          // 魔书【皇家红茶】
                        output.accept(SKILL_BOOK_GODSPEED_DANCE.get());     // 魔书【神速之舞】
                        output.accept(SKILL_BOOK_KATARINA_WHEEL.get());     // 圣书【卡塔丽娜的车轮】
                        output.accept(SKILL_BOOK_PALADIN_BANNER.get());     // 圣书【圣骑士的御旗】
                        output.accept(SKILL_BOOK_BLACK_WAVE.get());         // 魔书【黑之波动】
                        output.accept(SKILL_BOOK_BLACK_SLASH.get());        // 魔书【黑之斩击】
                        output.accept(SKILL_BOOK_AWAKENING.get());          // 魔书【觉醒】
                        output.accept(SKILL_BOOK_SERPENT_EMBRACE.get());    // 魔书【毒蛇的拥抱】
                        output.accept(SKILL_BOOK_SOUL_STREAM.get());        // 魔书【魂之奔流】
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
