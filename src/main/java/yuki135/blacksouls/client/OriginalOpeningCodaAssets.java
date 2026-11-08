package yuki135.blacksouls.client;

import java.util.Map;
import java.util.LinkedHashMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import yuki135.blacksouls.BlackSouls;

public final class OriginalOpeningCodaAssets {
    private static final Map<String,ResourceLocation> PICTURES=Map.of(
            "1",picture("memory_1"),"2",picture("memory_2"),"3",picture("memory_3"));
    private static final Map<String,String> BGM=loadMusic();
    private static final Map<String,Portrait> FAMILY_PORTRAITS=Map.of(
            "娘アリス",avatar("alice_daughter"),"妹アリス",avatar("alice_sister"),"母アリス",avatar("alice_mother"));

    public record Portrait(ResourceLocation texture,int width,int height,int columns,int rows) {
        public int frameWidth() { return width/columns; }
        public int frameHeight() { return height/rows; }
    }

    private OriginalOpeningCodaAssets() { }

    private static ResourceLocation picture(String name) {
        return ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/gui/opening_coda/"+name+".png");
    }

    public static ResourceLocation pictureTexture(String name) { return PICTURES.get(name); }
    public static String music(String name) { return BGM.getOrDefault(name,""); }

    private static Portrait avatar(String name) {
        return new Portrait(ResourceLocation.fromNamespaceAndPath(BlackSouls.MODID,"textures/gui/avatars/"+name+".png"),96,96,1,1);
    }

    public static Portrait portrait(String name,int index) {
        var portrait=FAMILY_PORTRAITS.get(name);
        if(portrait==null) {
            var entry=yuki135.blacksouls.util.OriginalNpcTextures.portrait(name);
            if(entry!=null)portrait=new Portrait(yuki135.blacksouls.util.OriginalNpcTextures.texture(entry),entry.get("width").getAsInt(),
                    entry.get("height").getAsInt(),entry.get("columns").getAsInt(),entry.get("rows").getAsInt());
        }
        return portrait!=null&&index>=0&&index<portrait.columns()*portrait.rows()?portrait:null;
    }

    private static Map<String,String> loadMusic() {
        try(var input=OriginalOpeningCodaAssets.class.getResourceAsStream("/data/blacksouls/original_npc_audio.json")) {
            if(input==null)throw new IllegalStateException("Missing original opening music index");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("bgm");
            var result=new LinkedHashMap<String,String>();
            root.entrySet().forEach(entry->result.put(entry.getKey(),entry.getValue().getAsJsonObject().get("sound").getAsString()));
            return Map.copyOf(result);
        } catch(java.io.IOException failure) { throw new IllegalStateException("Cannot read original opening music index",failure); }
    }

    public static String speaker(String faceName) {
        if(faceName.equals("娘アリス")||faceName.equals("妹アリス")||faceName.equals("母アリス"))return "爱丽丝";
        return yuki135.blacksouls.util.OriginalNpcTextures.displayName(faceName);
    }
}
