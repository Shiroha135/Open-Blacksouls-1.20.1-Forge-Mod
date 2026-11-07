package yuki135.blacksouls.client;

import yuki135.blacksouls.story.OriginalOpeningPresentationData;
import yuki135.blacksouls.story.OriginalOpeningPresentationData.Cue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class OriginalOpeningPresentationEngine {
    public record Snapshot(UUID episode,long sequence,String program,int command,int elapsedFrames,boolean restoring,
                           boolean awaitingDestination,boolean finished,boolean skipped,String dimension,int originX,int originY,int originZ) {
        public Snapshot {
            Objects.requireNonNull(episode);Objects.requireNonNull(program);Objects.requireNonNull(dimension);
            if(sequence<0||elapsedFrames<0||elapsedFrames>1000000||Math.abs((long)originX)>30000000||Math.abs((long)originZ)>30000000
                    ||originY< -2048||originY>2048||!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid opening cinematic snapshot");
            OriginalOpeningPresentationData.program(program).cue(command);
        }
    }
    public record Effect(int code,List<Double> args,String sound,String key) { }
    public record Frame(int brightness,double[] tone,double[] flash,double shake,OriginalOpeningPresentationMessage.Page page,
                        int messageFrames,boolean canAdvance,int balloon,int balloonFrame,boolean fogStopped,boolean awaitingDestination,boolean active) {
        public Frame { tone=tone.clone();flash=flash.clone(); }
        @Override public double[] tone() { return tone.clone(); }
        @Override public double[] flash() { return flash.clone(); }
    }
    private Snapshot snapshot;
    private int predictedFrames;
    private final Set<String> emitted=new HashSet<>();
    private long advanceRequestSequence=-1;
    private long skipRequestSequence=-1;
    private Cue carriedAmbient;

    public List<Effect> apply(Snapshot incoming) {
        Objects.requireNonNull(incoming);
        if(snapshot!=null&&snapshot.episode().equals(incoming.episode())) {
            if(incoming.sequence()<snapshot.sequence())return List.of();
            if(incoming.sequence()==snapshot.sequence()) {
                if(!incoming.equals(snapshot))throw new IllegalArgumentException("Conflicting opening cinematic sequence");
                return List.of();
            }
            if(snapshot.program().equals(incoming.program())&&incoming.command()<snapshot.command()&&!incoming.restoring()&&!incoming.skipped())throw new IllegalArgumentException("Opening cinematic rewound without recovery");
        } else { emitted.clear();carriedAmbient=null; }
        var effects=new ArrayList<Effect>();
        var program=OriginalOpeningPresentationData.program(incoming.program());
        if(program.id().equals("arrival"))carriedAmbient=null;
        int first=0;
        if(snapshot!=null&&snapshot.episode().equals(incoming.episode())&&snapshot.program().equals(incoming.program()))first=snapshot.command();
        else for(Cue cue:program.cues()) {
            if(cue.command()>=incoming.command())break;
            if(cue.blockingFrames()>0||cue.holdForInput())first=cue.command()+1;
        }
        for(Cue cue:program.cues()) {
            if(cue.command()>incoming.command())break;
            String key=incoming.episode()+":"+program.id()+":"+cue.command();
            if(emitted.add(key)&&cue.command()>=first&&!incoming.restoring()&&!incoming.skipped()&&!incoming.finished()
                    &&(cue.code()==250||cue.code()==213||cue.code()==245||cue.code()==246||cue.code()==355||cue.code()==225))effects.add(new Effect(cue.code(),cue.args(),cue.sound(),key));
            if(cue.code()==245)carriedAmbient=cue;
            if(cue.code()==201&&cue.args().get(0).intValue()==1)carriedAmbient=null;
        }
        snapshot=incoming;predictedFrames=incoming.elapsedFrames();advanceRequestSequence=-1;skipRequestSequence=-1;
        return List.copyOf(effects);
    }

    public void advanceClientFrames(int frames,boolean worldReady) {
        if(frames<0||frames>60)throw new IllegalArgumentException("Unbounded cinematic frame step");
        if(snapshot==null||snapshot.finished()||snapshot.skipped()||snapshot.awaitingDestination()||!worldReady)return;
        var cue=OriginalOpeningPresentationData.program(snapshot.program()).cue(snapshot.command());
        int limit=cue.holdForInput()?1000000:Math.max(120,Math.max(cue.blockingFrames(),snapshot.elapsedFrames()));
        predictedFrames=Math.min(limit,Math.min(1000000,predictedFrames+frames));
    }

    public boolean requestAdvance() {
        if(snapshot==null||advanceRequestSequence==snapshot.sequence()||!frame().canAdvance())return false;
        advanceRequestSequence=snapshot.sequence();return true;
    }

    public boolean requestReveal() {
        var frame=frame();
        if(snapshot==null||advanceRequestSequence==snapshot.sequence()||frame.page()==null||frame.awaitingDestination())return false;
        int finalGlyph=frame.page().glyphs().stream().mapToInt(OriginalOpeningPresentationMessage.Glyph::frame).max().orElse(0);
        if(predictedFrames>=finalGlyph)return false;
        advanceRequestSequence=snapshot.sequence();return true;
    }

    public boolean requestSkip() {
        if(snapshot==null||snapshot.finished()||snapshot.skipped()||skipRequestSequence==snapshot.sequence())return false;
        skipRequestSequence=snapshot.sequence();return true;
    }

    public Frame frame() {
        return frameAt(0);
    }

    public Frame renderFrame(float partialTick) {
        if(!Float.isFinite(partialTick))throw new IllegalArgumentException("Invalid cinematic partial tick");
        return frameAt(snapshot==null||snapshot.awaitingDestination()?0:Math.max(0,Math.min(2,(int)(partialTick*3))));
    }

    private Frame frameAt(int lookahead) {
        if(snapshot==null||snapshot.finished())return empty();
        if(snapshot.skipped())return snapshot.awaitingDestination()?new Frame(0,new double[4],new double[4],0,null,0,false,0,0,true,true,true):empty();
        int sampledFrames=Math.min(1000000,predictedFrames+lookahead);
        var state=new OriginalNpcPresentationState();
        var program=OriginalOpeningPresentationData.program(snapshot.program());
        int settlingBound=30;
        for(Cue cue:program.cues()) {
            if(cue.code()==223||cue.code()==224)settlingBound=Math.max(settlingBound,cue.args().get(4).intValue()+1);
            if(cue.code()==225)settlingBound=Math.max(settlingBound,cue.args().get(2).intValue()+80);
        }
        OriginalOpeningPresentationMessage.Page page=null;
        boolean fogStopped=false;
        int balloon=0,balloonAge=0;
        for(Cue cue:program.cues()) {
            if(cue.command()>snapshot.command())break;
            if(cue.code()==213) { balloon=cue.args().get(0).intValue();balloonAge=0; }
            if(cue.code()==221||cue.code()==222||cue.code()==223||cue.code()==224||cue.code()==225)state.apply(cue.code(),cue.args());
            if(cue.code()==201&&cue.args().get(0).intValue()==1)state.apply(223,List.of(-34.0D,-34.0D,-34.0D,0.0D,0.0D));
            if(cue.code()==355)fogStopped=true;
            if(cue.command()==snapshot.command()) {
                if(cue.code()==101)page=OriginalOpeningPresentationMessage.compile(cue.lines(),cue.style().equals("title"));
                advanceCue(state,cue,sampledFrames,settlingBound);balloonAge+=sampledFrames;
            } else {
                advanceCue(state,cue,cue.blockingFrames(),settlingBound);balloonAge+=cue.blockingFrames();
            }
        }
        if(balloonAge>=76)balloon=0;
        int brightness=snapshot.awaitingDestination()?0:state.brightness();
        return new Frame(brightness,state.tone(),state.flash(),state.shake(),page,sampledFrames,
                page!=null&&predictedFrames>=page.requiredFrames()+1&&!snapshot.awaitingDestination(),balloon,
                OriginalNpcPresentationState.balloonFrame(Math.max(0,76-balloonAge)),fogStopped,snapshot.awaitingDestination(),true);
    }

    public Cue ambient() {
        if(snapshot==null||snapshot.skipped()||snapshot.finished())return null;
        Cue result=carriedAmbient;
        for(Cue cue:OriginalOpeningPresentationData.program(snapshot.program()).cues()) {
            if(cue.command()>snapshot.command())break;
            if(cue.code()==245)result=cue;
            if(cue.code()==201&&cue.args().get(0).intValue()==1)result=null;
        }
        return result;
    }

    public double ambientGain() {
        Cue loop=ambient();
        if(loop==null||loop.sound().isEmpty())return 0;
        for(Cue cue:OriginalOpeningPresentationData.program(snapshot.program()).cues()) {
            if(cue.command()>snapshot.command())break;
            if(cue.command()>loop.command()&&cue.code()==246) {
                int elapsed=cue.command()==snapshot.command()?predictedFrames:cue.blockingFrames()+predictedFrames;
                for(Cue later:OriginalOpeningPresentationData.program(snapshot.program()).cues()) {
                    if(later.command()>cue.command()&&later.command()<snapshot.command())elapsed+=later.blockingFrames();
                }
                int duration=cue.args().get(0).intValue()*60;
                return duration==0?0:Math.max(0,1-elapsed/(double)duration);
            }
        }
        return 1;
    }

    public int ambientFadeFrames() {
        Cue loop=ambient();
        if(loop==null||snapshot==null)return 0;
        return OriginalOpeningPresentationData.program(snapshot.program()).cues().stream()
                .filter(cue->cue.command()>loop.command()&&cue.command()<=snapshot.command()&&cue.code()==246)
                .mapToInt(cue->cue.args().get(0).intValue()*60).findFirst().orElse(0);
    }

    public Snapshot snapshot() { return snapshot; }
    private static void advanceCue(OriginalNpcPresentationState state,Cue cue,int frames,int settlingBound) {
        if(cue.code()==201&&cue.args().get(0).intValue()==1) {
            state.advanceFrames(Math.min(45,frames));
            if(frames>45) { state.apply(222,List.of());state.advanceFrames(Math.min(30,frames-45)); }
        } else state.advanceFrames(Math.min(frames,settlingBound));
    }
    public int messageOrdinal() {
        if(snapshot==null||!OriginalOpeningPresentationData.program(snapshot.program()).cue(snapshot.command()).holdForInput())return -1;
        return (int)OriginalOpeningPresentationData.program(snapshot.program()).cues().stream().filter(cue->cue.holdForInput()&&cue.command()<snapshot.command()).count();
    }
    public int predictedFrames() { return predictedFrames; }
    public void clear() { snapshot=null;predictedFrames=0;emitted.clear();carriedAmbient=null;advanceRequestSequence=-1;skipRequestSequence=-1; }
    private static Frame empty() { return new Frame(255,new double[4],new double[4],0,null,0,false,0,0,false,false,false); }
}
