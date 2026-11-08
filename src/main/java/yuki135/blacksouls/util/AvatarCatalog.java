package yuki135.blacksouls.util;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.story.OriginalOpeningProfile.ReadState;

public final class AvatarCatalog {
    private static final List<String> PROFESSIONS;
    private static final List<String> AVATARS;

    static {
        try(var input=AvatarCatalog.class.getResourceAsStream("/data/blacksouls/avatars.json")) {
            if(input==null)throw new IllegalStateException("Missing avatar catalog");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)throw new IllegalArgumentException("Invalid avatar catalog");
            var professions=new ArrayList<String>();root.getAsJsonArray("professions").forEach(v->professions.add(v.getAsString()));
            var avatars=new ArrayList<String>();root.getAsJsonArray("avatars").forEach(v->avatars.add(v.getAsString()));
            if(professions.size()!=9||avatars.size()>512||avatars.stream().distinct().count()!=avatars.size()
                    ||!avatars.containsAll(professions)||avatars.stream().anyMatch(v->!v.matches("[a-z0-9_]{1,64}")))throw new IllegalArgumentException("Invalid avatar identities");
            PROFESSIONS=List.copyOf(professions);AVATARS=List.copyOf(avatars);
        }catch(Exception failure){throw new IllegalStateException("Cannot load avatar catalog",failure);}
    }

    private AvatarCatalog() { }

    public static List<String> all() {return AVATARS;}
    public static String profession(int origin) {return origin>=1&&origin<=9?PROFESSIONS.get(origin-1):"";}
    public static String initial(BSPlayerStats stats) {
        if(stats==null||stats.openingProfile().hasRejectedData())return "";
        String avatar=profession(stats.openingProfile().realOriginId());
        if(!avatar.isEmpty())return avatar;
        return stats.openingProfile().readState()==ReadState.ABSENT_LEGACY||stats.openingProfile().readState()==ReadState.LEGACY?PROFESSIONS.get(0):"";
    }
    public static List<String> unlocked(BSPlayerStats stats) {
        if(stats==null)return List.of();
        if(stats.allAvatarsUnlocked)return AVATARS;
        String avatar=initial(stats);return avatar.isEmpty()?List.of():List.of(avatar);
    }
    public static String selected(BSPlayerStats stats) {
        var allowed=unlocked(stats);
        if(stats!=null&&allowed.contains(stats.avatarId))return stats.avatarId;
        String initial=initial(stats);
        return allowed.contains(initial)?initial:allowed.isEmpty()?"unknown":allowed.get(0);
    }
}
