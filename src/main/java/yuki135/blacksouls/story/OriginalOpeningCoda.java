package yuki135.blacksouls.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import yuki135.blacksouls.story.OriginalOpeningProfile.Cursor;

public final class OriginalOpeningCoda {
    public static final int BASE=20000;
    public static final int ORIGIN_ANIMATION=BASE+100;
    public static final int ORIGIN_FADE_OUT=BASE+101;
    public static final int ORIGIN_TRANSFER=BASE+102;
    public static final int ORIGIN_FADE_IN=BASE+103;
    public static final int OP_SKIP=BASE+200;
    public static final int SKIP_ALICE=BASE+201;
    public static final int STORY_ALICE=BASE+214;
    public static final int EFFECT_PLAYED=1<<19;
    private static final int TONE_MASK=255;
    private static final int FRAME_MASK=511;
    private static final Map<Integer,Cue> CUES=load();

    public record Cue(int command,int map,int event,int page,int sourceIndex,int code,String kind,
            String text,String face,int faceIndex,int style,int position,List<String> choices,List<Integer> targets,
            int next,int durationFrames,int toneTarget,int toneFrames,String picture,double pictureFrom,double pictureTo,
            int pictureFrames,boolean pictureBlack,float blackFrom,float blackTo,int blackFrames,boolean fog,
            boolean sunshine,JsonArray parameters) { }

    private OriginalOpeningCoda() { }

    private static Map<Integer,Cue> load() {
        try(var input=OriginalOpeningCoda.class.getResourceAsStream("/data/blacksouls/original_opening_coda.json")) {
            if(input==null)throw new IllegalStateException("Missing original opening coda");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)throw new IllegalArgumentException("Invalid opening coda version");
            var result=new LinkedHashMap<Integer,Cue>();
            for(var value:root.getAsJsonArray("cues")) {
                var row=value.getAsJsonObject();int command=row.get("command").getAsInt();
                var choices=new ArrayList<String>();row.getAsJsonArray("choices").forEach(item->choices.add(item.getAsString()));
                var targets=new ArrayList<Integer>();row.getAsJsonArray("targets").forEach(item->targets.add(item.getAsInt()));
                var cue=new Cue(command,row.get("map").getAsInt(),row.get("event").getAsInt(),row.get("page").getAsInt(),row.get("sourceIndex").getAsInt(),
                        row.get("code").getAsInt(),row.get("kind").getAsString(),row.get("text").getAsString(),row.get("face").getAsString(),row.get("faceIndex").getAsInt(),
                        row.get("style").getAsInt(),row.get("position").getAsInt(),List.copyOf(choices),List.copyOf(targets),row.get("next").getAsInt(),
                        row.get("durationFrames").getAsInt(),row.get("toneTarget").getAsInt(),row.get("toneFrames").getAsInt(),row.get("picture").getAsString(),
                        row.get("pictureFrom").getAsDouble(),row.get("pictureTo").getAsDouble(),row.get("pictureFrames").getAsInt(),row.get("pictureBlack").getAsBoolean(),
                        row.get("blackFrom").getAsFloat(),row.get("blackTo").getAsFloat(),row.get("blackFrames").getAsInt(),row.get("fog").getAsBoolean(),
                        row.get("sunshine").getAsBoolean(),row.getAsJsonArray("parameters"));
                if(command<BASE||command>25000||cue.durationFrames()<0||cue.durationFrames()>3600||cue.toneTarget()<0||cue.toneTarget()>255
                        ||cue.toneFrames()<0||cue.toneFrames()>511||choices.size()!=targets.size()||result.put(command,cue)!=null)throw new IllegalArgumentException("Invalid opening coda cue");
            }
            for(var cue:result.values()) {
                if(cue.next()!=0&&!result.containsKey(cue.next()))throw new IllegalArgumentException("Missing opening coda successor");
                for(int target:cue.targets())if(target!=0&&!result.containsKey(target))throw new IllegalArgumentException("Missing opening choice successor");
            }
            return Map.copyOf(result);
        } catch(java.io.IOException failure) { throw new IllegalStateException("Cannot load original opening coda",failure); }
    }

    public static boolean active(int command) { return CUES.containsKey(command); }
    public static Cue cue(int command) {
        var result=CUES.get(command);if(result==null)throw new IllegalArgumentException("Unknown opening coda command: "+command);return result;
    }
    public static String message(int command) { return active(command)?cue(command).text():""; }
    public static String message(int command,String name) { return message(command).replace("\\n[1]",name==null?"":name); }
    public static List<String> choices(int command) { return active(command)?cue(command).choices():List.of(); }
    public static boolean acceptsInput(int command) { return active(command)&&(cue(command).kind().equals("message")||cue(command).kind().equals("choice")); }
    public static int messageStyle(int command) { return active(command)?cue(command).style():1; }
    public static int messagePosition(int command) { return active(command)?cue(command).position():1; }
    public static String faceName(int command) { return active(command)?cue(command).face():""; }
    public static int faceIndex(int command) { return active(command)?cue(command).faceIndex():0; }
    public static String pictureName(int command) { return active(command)?cue(command).picture():""; }
    public static boolean pictureBackgroundBlack(int command) { return active(command)&&cue(command).pictureBlack(); }
    public static boolean fogVisible(int command) { return active(command)&&cue(command).fog(); }
    public static boolean sunshineVisible(int command) { return active(command)&&cue(command).sunshine(); }
    public static boolean isBalloon(int command) { return active(command)&&cue(command).code()==213; }
    public static boolean memory(int command) { return active(command)&&cue(command).map()==110&&cue(command).page()>0; }
    public static int memoryStart(int relationship) { if(relationship<1||relationship>3)throw new IllegalArgumentException("Invalid Alice relationship");return BASE+relationship*1000; }
    public static boolean aliceChoice(int command) { return command==SKIP_ALICE||command==STORY_ALICE; }
    public static boolean professionsPresent(int command) { return command==ORIGIN_ANIMATION||command==ORIGIN_FADE_OUT; }
    public static int durationTicks(int command) { return active(command)?(cue(command).durationFrames()+2)/3:0; }
    public static int elapsedFrames(int command,int remainingTicks) { return Math.min(cue(command).durationFrames(),Math.max(0,durationTicks(command)-remainingTicks)*3); }
    public static int next(int command,int value) {
        var cue=cue(command);
        if(cue.choices().isEmpty())return value==0?cue.next():-1;
        return value>=1&&value<=cue.targets().size()?cue.targets().get(value-1):-1;
    }
    public static Cursor cursor(int command,int visualState) {
        var cue=cue(command);return new Cursor(cue.map(),cue.event(),cue.page(),command,visualState,durationTicks(command));
    }
    public static int enterVisualState(int command,int previousCommand,int previousState) {
        int state=previousState&~EFFECT_PLAYED;
        var cue=cue(command);
        if(cue.code()==223) {
            int from=active(previousCommand)?Math.round(-sceneTone(previousCommand,0,previousState)):0;
            return cue.toneFrames()==0?cue.toneTarget():from|(cue.toneFrames()<<8);
        }
        return state;
    }
    public static int advanceVisualState(int command,int state,int elapsedSourceFrames) {
        if(!active(command))return 0;
        int frames=(state>>8)&FRAME_MASK;
        if(frames==0)return state;
        int remaining=Math.max(0,frames-Math.max(0,elapsedSourceFrames));
        return (state&EFFECT_PLAYED)|(remaining==0?cue(command).toneTarget():(state&TONE_MASK)|(remaining<<8));
    }
    public static float sceneTone(int command,int remainingTicks,int visualState) {
        if(!active(command))return 0;
        var cue=cue(command);int frames=(visualState>>8)&FRAME_MASK;int from=visualState&TONE_MASK;
        if(frames==0||cue.toneFrames()==0)return -from;
        float fraction=1-Math.min(1F,frames/(float)cue.toneFrames());
        return -(from+(cue.toneTarget()-from)*fraction);
    }
    public static float blackAlpha(int command,int remainingTicks) {
        if(!active(command))return 0;
        var cue=cue(command);
        if(cue.blackFrames()==0)return cue.blackTo();
        float fraction=elapsedFrames(command,remainingTicks)/(float)cue.blackFrames();
        return cue.blackFrom()+(cue.blackTo()-cue.blackFrom())*Math.max(0,Math.min(1,fraction));
    }
    public static double pictureOpacity(int command,int remainingTicks) {
        if(!active(command))return 0;
        var cue=cue(command);
        if(cue.pictureFrames()==0||cue.code()!=232)return cue.pictureTo();
        double fraction=elapsedFrames(command,remainingTicks)/(double)cue.pictureFrames();
        return cue.pictureFrom()+(cue.pictureTo()-cue.pictureFrom())*Math.max(0,Math.min(1,fraction));
    }
    public static int migrateOrigin(int command) {
        if(active(command))return command;
        return switch(command) { case 27->BASE+26;case 31->BASE+30;case 35->BASE+34;case 40->BASE+39;case 45->BASE+44;default->ORIGIN_ANIMATION; };
    }

    public static int aliceDirection(int command) {
        int direction=4;
        if(!memory(command))return direction;
        var current=cue(command);
        for(int index=0;index<current.sourceIndex();index++) {
            var previous=CUES.get(BASE+current.page()*1000+index);
            if(previous==null||previous.code()!=205||previous.parameters().get(0).getAsInt()!=6)continue;
            for(var value:previous.parameters().get(1).getAsJsonObject().getAsJsonArray("list")) {
                int code=value.getAsJsonObject().get("code").getAsInt();
                if(code>=16&&code<=19)direction=switch(code) {case 16->2;case 17->4;case 18->6;default->8;};
            }
        }
        return direction;
    }

    public static boolean aliceVisible(int command) {
        if(!memory(command))return true;
        var current=cue(command);
        for(int index=0;index<=current.sourceIndex();index++) {
            var previous=CUES.get(BASE+current.page()*1000+index);
            if(previous!=null&&previous.code()==121) {
                var p=previous.parameters();
                if(p.get(0).getAsInt()<=103&&p.get(1).getAsInt()>=103&&p.get(2).getAsInt()==0)return false;
            }
        }
        return true;
    }
}
