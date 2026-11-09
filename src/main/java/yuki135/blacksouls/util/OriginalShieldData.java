package yuki135.blacksouls.util;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.item.weapon.ItemOriginalShield;

public final class OriginalShieldData {
    public record Shield(int id,String registry,String name,String description,int price,int icon,int armorType,List<Integer> parameters,List<BSOriginalCombatData.Feature> features) {
        public Shield {parameters=List.copyOf(parameters);features=List.copyOf(features);}
    }
    private static final Map<Integer,Shield> SHIELDS=load();

    private OriginalShieldData() { }
    public static Shield get(int id) {return SHIELDS.get(id);}
    public static List<Shield> all() {return SHIELDS.values().stream().sorted(java.util.Comparator.comparingInt(Shield::id)).toList();}
    public static Shield equipped(LivingEntity entity) {
        return entity instanceof Player&&entity.getOffhandItem().getItem() instanceof ItemOriginalShield item?get(item.sourceArmorId()):null;
    }
    public static boolean isShield(ItemStack stack) {return !stack.isEmpty()&&stack.getItem() instanceof ItemOriginalShield;}
    public static double rate(LivingEntity entity,int code,int id) {
        var shield=equipped(entity);double result=1;
        if(shield!=null)for(var f:shield.features())if(f.code()==code&&f.dataId()==id)result*=f.value();
        return result;
    }
    public static double sum(LivingEntity entity,int code,int id) {
        var shield=equipped(entity);double result=0;
        if(shield!=null)for(var f:shield.features())if(f.code()==code&&f.dataId()==id)result+=f.value();
        return result;
    }
    public static List<BSOriginalCombatData.Feature> battleFeatures(LivingEntity entity) {
        var shield=equipped(entity);if(shield==null)return List.of();
        return shield.features().stream().filter(f->f.code()==11||f.code()==12||f.code()==13||f.code()==32||f.code()==34||f.code()==61
                ||f.code()==22&&(f.dataId()==3||f.dataId()==5)||f.code()==23&&f.dataId()==1).toList();
    }
    public static void apply(Player player,BSPlayerStats stats) {
        var shield=equipped(player);if(shield==null)return;
        double[] base={stats.hp,stats.maxMp,stats.attack,stats.defense,stats.magicAttack,stats.magicDefense,stats.speed,stats.luck};
        for(int i=0;i<8;i++)base[i]=Math.floor((base[i]+shield.parameters().get(i))*rate(player,21,i));
        stats.hp=base[0];stats.maxMp=base[1];stats.attack=base[2];stats.defense=base[3];stats.magicAttack=base[4];stats.magicDefense=base[5];stats.speed=base[6];stats.luck=base[7];
        stats.evasion+=sum(player,22,1)*100;stats.magicEvasion+=sum(player,22,4)*100;
        stats.hpRegenRate+=sum(player,22,7);stats.mpRegenRate+=sum(player,22,8);
        stats.physicalDamageRate*=rate(player,23,6);stats.magicDamageRate*=rate(player,23,7);
    }

    private static Map<Integer,Shield> load() {
        try(var input=OriginalShieldData.class.getResourceAsStream("/data/blacksouls/original_shields.json")) {
            if(input==null)throw new IllegalStateException("Missing original shield source");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1||!root.get("sourceArmorSha256").getAsString().matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid shield source");
            var result=new LinkedHashMap<Integer,Shield>();
            for(var value:root.getAsJsonArray("shields")) {
                var row=value.getAsJsonObject();int id=row.get("id").getAsInt();
                var parameters=new ArrayList<Integer>();row.getAsJsonArray("parameters").forEach(p->parameters.add(p.getAsInt()));
                var features=new ArrayList<BSOriginalCombatData.Feature>();
                for(var raw:row.getAsJsonArray("features")) {
                    var f=raw.getAsJsonObject();double number=f.get("value").getAsDouble();
                    if(!Double.isFinite(number))throw new IllegalArgumentException("Nonfinite shield feature");
                    features.add(new BSOriginalCombatData.Feature(f.get("code").getAsInt(),f.get("id").getAsInt(),number));
                }
                var source=OriginalEquipmentData.get(2,id);
                if(source==null||source.equipmentType()!=1||!source.name().equals(row.get("name").getAsString())||!source.parameters().equals(parameters)
                        ||!source.features().equals(features.stream().map(f->new OriginalEquipmentData.Feature(f.code(),f.dataId())).toList()))throw new IllegalArgumentException("Shield identity differs from source armor");
                var shield=new Shield(id,row.get("registry").getAsString(),source.name(),row.get("description").getAsString(),row.get("price").getAsInt(),row.get("icon").getAsInt(),row.get("armorType").getAsInt(),parameters,features);
                if(parameters.size()!=8||!shield.registry().matches("blacksouls:[a-z0-9_]+")||shield.armorType()<5||shield.armorType()>6||result.put(id,shield)!=null)throw new IllegalArgumentException("Invalid shield record");
            }
            if(result.size()!=31)throw new IllegalArgumentException("Incomplete original shields");
            return Map.copyOf(result);
        }catch(Exception failure){throw new IllegalStateException("Cannot load original shields",failure);}
    }
}
