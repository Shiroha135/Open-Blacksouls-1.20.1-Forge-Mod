package com.BlackSouls.BlackSoulsMod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillOriginalMagic;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

public final class OriginalShopGoods {
    private static final Map<Integer, Good> GOODS = load();

    private OriginalShopGoods() {
    }

    public static Good get(int kind, int id) {
        return kind == 0 ? GOODS.get(id) : null;
    }

    private static Map<Integer, Good> load() {
        try (var stream = OriginalShopGoods.class.getResourceAsStream("/data/blacksouls/original_shop_goods.json")) {
            if (stream == null) throw new IllegalStateException("Missing original shop goods");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("version").getAsBigDecimal().intValueExact() != 1) throw new IllegalArgumentException("Invalid original shop goods version");
            Map<Integer, Good> goods = new LinkedHashMap<>();
            Set<Integer> mirrors = new HashSet<>();
            Set<SkillOriginalMagic.Profile> profiles = EnumSet.noneOf(SkillOriginalMagic.Profile.class);
            for (JsonElement element : json.getAsJsonArray("goods")) {
                JsonObject row = element.getAsJsonObject();
                int kind = row.get("kind").getAsBigDecimal().intValueExact();
                int id = row.get("id").getAsBigDecimal().intValueExact();
                ResourceLocation registry = ResourceLocation.tryParse(row.get("registry").getAsString());
                String name = row.get("name").getAsString();
                SkillOriginalMagic.Profile profile = null;
                int sourceSkill = 0;
                if ("mirror".equals(row.get("type").getAsString())) {
                    mirrors.add(id);
                } else if ("skill_book".equals(row.get("type").getAsString())) {
                    profile = SkillOriginalMagic.Profile.valueOf(row.get("profile").getAsString());
                    sourceSkill = row.get("sourceSkillId").getAsBigDecimal().intValueExact();
                    if (!profiles.add(profile) || new SkillOriginalMagic(profile).getTurnBattleSourceSkillId() != sourceSkill
                            || registry == null || !registry.getPath().equals(profile.getBookId())) {
                        throw new IllegalArgumentException("Original skill book source identity mismatch");
                    }
                } else throw new IllegalArgumentException("Invalid original shop good type");
                if (kind != 0 || registry == null || !registry.getNamespace().equals("blacksouls") || name.isBlank()
                        || goods.putIfAbsent(id, new Good(id, name, registry, profile, sourceSkill)) != null) {
                    throw new IllegalArgumentException("Invalid original shop good");
                }
            }
            if (goods.size() != 59 || !mirrors.equals(Set.of(180, 181, 182, 183))
                    || !profiles.equals(EnumSet.allOf(SkillOriginalMagic.Profile.class))) {
                throw new IllegalArgumentException("Unexpected original shop goods scope");
            }
            return Map.copyOf(goods);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read original shop goods", exception);
        }
    }

    public record Good(int id, String name, ResourceLocation registry, SkillOriginalMagic.Profile bookProfile, int sourceSkillId) {
    }
}
