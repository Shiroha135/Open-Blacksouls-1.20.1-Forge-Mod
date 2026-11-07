package yuki135.blacksouls.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class OriginalOpeningAmbientSnapshot {
    public static final int MAX_LOOPS=8;
    public static final double MAX_SEEK_SECONDS=300;
    public record Loop(String sound,float volume,float pitch,double x,double y,double z,boolean relative,String attenuation,double seconds,boolean limited) {
        public Loop {
            if(sound==null||!sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!Float.isFinite(volume)||volume<0||volume>16
                    ||!Float.isFinite(pitch)||pitch<=0||pitch>4||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)
                    ||!Double.isFinite(seconds)||seconds<0||seconds>MAX_SEEK_SECONDS||!List.of("NONE","LINEAR").contains(attenuation))throw new IllegalArgumentException("Invalid captured opening ambient loop");
        }
        public Loop advance() {
            double next=seconds+.05D*pitch;
            return new Loop(sound,volume,pitch,x,y,z,relative,attenuation,Math.min(MAX_SEEK_SECONDS,next),limited||next>MAX_SEEK_SECONDS);
        }
        public Loop parameters(float volume,float pitch,double x,double y,double z,boolean relative,String attenuation) {
            return new Loop(sound,volume,pitch,x,y,z,relative,attenuation,seconds,limited);
        }
    }
    private final LinkedHashMap<Long,Loop> active=new LinkedHashMap<>();
    private List<Loop> preview;
    private boolean managed;
    private boolean suspended;
    private int overflow;

    public boolean observe(long identity,Loop loop) {
        if(!active.containsKey(identity)&&active.size()>=MAX_LOOPS) { overflow++;return false; }
        active.put(identity,loop);return true;
    }
    public void update(long identity,Loop parameters,boolean running,boolean paused) {
        var current=active.get(identity);if(current==null)return;
        current=current.parameters(parameters.volume(),parameters.pitch(),parameters.x(),parameters.y(),parameters.z(),parameters.relative(),parameters.attenuation());
        active.put(identity,running&&!paused&&!suspended?current.advance():current);
    }
    public void remove(long identity) { active.remove(identity); }
    public void capturePreview() { if(preview==null)preview=List.copyOf(active.values()); }
    public void capturePreview(java.util.Set<Long> streaming) { if(preview==null)preview=active.entrySet().stream().filter(entry->streaming.contains(entry.getKey())).map(java.util.Map.Entry::getValue).toList(); }
    public void begin() { begin(true); }
    public void begin(boolean preview) { managed=true;suspended=preview; }
    public void stopSource() { if(managed)suspended=true; }
    public List<Loop> finish(boolean replay) {
        List<Loop> result=replay&&preview!=null?preview:List.of();
        preview=null;managed=false;suspended=false;active.clear();return result;
    }
    public boolean managed() { return managed; }
    public boolean suspended() { return suspended; }
    public int size() { return active.size(); }
    public int overflow() { return overflow; }
    public List<Loop> snapshot() { return List.copyOf(active.values()); }
    public java.util.Map<Long,Loop> entries() { return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(active)); }
    public void clear() { active.clear();preview=null;managed=false;suspended=false;overflow=0; }
}
