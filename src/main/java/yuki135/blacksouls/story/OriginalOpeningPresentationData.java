package yuki135.blacksouls.story;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OriginalOpeningPresentationData {
    public static final String RESOURCE="/data/blacksouls/original_opening_presentation.json";
    public static final int SOURCE_FPS=60;
    public static final int SOURCE_WIDTH=640;
    public static final int SOURCE_HEIGHT=480;
    public static final int SOURCE_FONT_SIZE=22;
    private static final Map<String,Program> PROGRAMS;
    private static final List<Integer> PALETTE;
    public static final String HASH;

    static {
        try(var stream=OriginalOpeningPresentationData.class.getResourceAsStream(RESOURCE)) {
            if(stream==null)throw new IllegalStateException("Missing opening presentation data");
            byte[] bytes=stream.readAllBytes();
            HASH=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            var root=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!root.keySet().equals(Set.of("version","sourceFramesPerSecond","sourceCanvas","sourceFontSize","palette","programs"))
                    || integer(root,"version",1,1)!=1 || integer(root,"sourceFramesPerSecond",60,60)!=SOURCE_FPS
                    || integer(root,"sourceFontSize",22,22)!=SOURCE_FONT_SIZE
                    || !root.getAsJsonArray("sourceCanvas").toString().equals("[640,480]"))throw new IllegalArgumentException("Invalid opening presentation schema");
            var palette=new ArrayList<Integer>();
            for(var value:root.getAsJsonArray("palette"))palette.add((int)value.getAsLong());
            if(palette.size()!=32)throw new IllegalArgumentException("Invalid source message palette");
            PALETTE=List.copyOf(palette);
            var programs=new LinkedHashMap<String,Program>();
            for(var value:root.getAsJsonArray("programs")) {
                var row=value.getAsJsonObject();
                if(!row.keySet().equals(Set.of("id","map","event","page","cues")))throw new IllegalArgumentException("Invalid source presentation program");
                String id=row.get("id").getAsString();
                if(!id.matches("[a-z][a-z0-9_]{0,31}"))throw new IllegalArgumentException("Invalid source presentation identity");
                var cues=new ArrayList<Cue>();int previous=-1;
                for(var cueValue:row.getAsJsonArray("cues")) {
                    var cue=cueValue.getAsJsonObject();
                    if(!cue.keySet().equals(Set.of("command","code","args","sound","lines","style","blockingFrames","holdForInput","sourceIndices")))throw new IllegalArgumentException("Invalid source presentation cue");
                    int command=integer(cue,"command",0,10000);
                    if(command<=previous)throw new IllegalArgumentException("Unordered presentation cue");
                    previous=command;
                    int code=integer(cue,"code",0,999);
                    if(!Set.of(101,201,213,221,222,223,224,225,230,245,246,250,355).contains(code))throw new IllegalArgumentException("Unsupported presentation command");
                    var args=new ArrayList<Double>();
                    for(var argument:cue.getAsJsonArray("args")) {
                        double number=argument.getAsDouble();
                        if(!Double.isFinite(number)||Math.abs(number)>1000000)throw new IllegalArgumentException("Invalid presentation argument");
                        args.add(number);
                    }
                    if(args.size()>8)throw new IllegalArgumentException("Too many presentation arguments");
                    String sound=cue.get("sound").getAsString();
                    if(!sound.isEmpty()&&!sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid presentation sound identity");
                    var lines=new ArrayList<String>();
                    for(var line:cue.getAsJsonArray("lines")) {
                        String text=line.getAsString();
                        if(text.length()>2048)throw new IllegalArgumentException("Oversized presentation text");
                        lines.add(text);
                    }
                    String style=cue.get("style").getAsString();
                    if(!Set.of("none","poem","title").contains(style)||code==101&&lines.isEmpty()||code!=101&&!lines.isEmpty())throw new IllegalArgumentException("Invalid presentation message");
                    int blocking=integer(cue,"blockingFrames",0,36000);
                    if(!cue.get("holdForInput").isJsonPrimitive()||!cue.get("holdForInput").getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Invalid presentation hold flag");
                    boolean hold=cue.get("holdForInput").getAsBoolean();
                    if(hold!=(code==101))throw new IllegalArgumentException("Only source message commands hold for input");
                    validateArguments(code,args,sound);
                    cues.add(new Cue(command,code,List.copyOf(args),sound,List.copyOf(lines),style,blocking,hold));
                }
                var program=new Program(id,integer(row,"map",1,9999),integer(row,"event",0,9999),integer(row,"page",0,99),List.copyOf(cues));
                if(cues.isEmpty()||programs.putIfAbsent(id,program)!=null)throw new IllegalArgumentException("Empty or duplicate presentation program");
            }
            if(!programs.keySet().equals(Set.of("collapse110","collapse97","collapse98","finale99","arrival")))throw new IllegalArgumentException("Incomplete opening presentation programs");
            PROGRAMS=Map.copyOf(programs);
        } catch(Exception failure) { throw new IllegalStateException("Cannot load original opening presentation",failure); }
    }

    private OriginalOpeningPresentationData() {
    }

    public record Cue(int command,int code,List<Double> args,String sound,List<String> lines,String style,int blockingFrames,boolean holdForInput) { }
    public record Program(String id,int map,int event,int page,List<Cue> cues) {
        public Cue cue(int command) { return cues.stream().filter(cue->cue.command()==command).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown source presentation command: "+id+":"+command)); }
    }
    public static Program program(String id) { var result=PROGRAMS.get(id);if(result==null)throw new IllegalArgumentException("Unknown opening presentation program");return result; }
    public static Map<String,Program> programs() { return PROGRAMS; }
    public static int color(int index) { if(index<0||index>=PALETTE.size())throw new IllegalArgumentException("Unknown source message color");return PALETTE.get(index); }

    private static void validateArguments(int code,List<Double> values,String sound) {
        int expected=switch(code){case 101,245,250->2;case 201,223,224->5;case 213,246->1;case 225->3;default->0;};
        if((code==221||code==222)&&values.size()==2) {
            if(values.get(0)!=2D)throw new IllegalArgumentException("Invalid cinematic transfer fade marker");
            whole(values.get(1),0,30);return;
        }
        if(values.size()!=expected)throw new IllegalArgumentException("Invalid cinematic command argument count");
        switch(code) {
            case 101 -> { whole(values.get(0),0,2);whole(values.get(1),0,2); }
            case 201 -> { for(int index=0;index<3;index++)whole(values.get(index),index==0?1:0,9999);whole(values.get(3),0,8);whole(values.get(4),0,2); }
            case 213 -> whole(values.get(0),1,10);
            case 223,224 -> {
                for(int index=0;index<4;index++) {
                    double value=values.get(index);
                    if(value<(code==223&&index<3?-255:0)||value>255)throw new IllegalArgumentException("Invalid cinematic tone or color channel");
                }
                whole(values.get(4),0,36000);
            }
            case 225 -> { whole(values.get(0),0,9);whole(values.get(1),0,9);whole(values.get(2),0,36000); }
            case 245,250 -> { whole(values.get(0),0,100);whole(values.get(1),0,200);if(code==250&&sound.isEmpty())throw new IllegalArgumentException("Missing cinematic one-shot sound"); }
            case 246 -> whole(values.get(0),0,120);
            default -> { }
        }
    }

    private static void whole(double value,int minimum,int maximum) { if(value!=Math.rint(value)||value<minimum||value>maximum)throw new IllegalArgumentException("Invalid whole cinematic argument"); }

    private static int integer(JsonObject row,String field,int minimum,int maximum) {
        var value=row.get(field);
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid presentation integer: "+field);
        double number=value.getAsDouble();
        if(!Double.isFinite(number)||number!=Math.rint(number)||number<minimum||number>maximum)throw new IllegalArgumentException("Presentation integer outside bound: "+field);
        return (int)number;
    }
}
