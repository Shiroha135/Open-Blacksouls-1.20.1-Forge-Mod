package yuki135.blacksouls.story;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import yuki135.blacksouls.story.OriginalOpeningPresentationData.Cue;

public final class OriginalOpeningCinematic {
    public static final int VERSION=1;
    public static final int FRAMES_PER_TICK=3;
    private static final Set<String> FIELDS=Set.of("Version","Catalog","Episode","Sequence","Program","Command","ElapsedFrames","MessageFrames","Fast","AwaitingDestination","TransferReady","Finished","EverSkipped","Skipped");
    private final UUID episode;
    private String program;
    private long sequence;
    private int index;
    private int elapsedFrames;
    private int messageFrames;
    private boolean fast;
    private boolean awaitingDestination;
    private boolean transferReady;
    private boolean finished;
    private boolean everSkipped;
    private boolean skipped;

    private OriginalOpeningCinematic(UUID episode,String program) {
        this.episode=Objects.requireNonNull(episode);
        this.program=OriginalOpeningPresentationData.program(program).id();
    }

    public static OriginalOpeningCinematic start(String program) {
        var result=new OriginalOpeningCinematic(UUID.randomUUID(),program);
        result.sequence=1;
        result.awaitingDestination=program.equals("arrival");
        return result;
    }

    public UUID episode() { return episode; }
    public long sequence() { return sequence; }
    public String program() { return program; }
    public int command() { return cue().command(); }
    public int elapsedFrames() { return elapsedFrames; }
    public int displayFrames() { return cue().holdForInput()?messageFrames:elapsedFrames; }
    public boolean awaitingDestination() { return awaitingDestination; }
    public boolean finished() { return finished; }
    public boolean skipped() { return skipped; }
    public boolean everSkipped() { return everSkipped; }
    public boolean fastMessage() { return fast; }
    public void touch() { ensureSequence();bump(); }
    public int messageOrdinal() {
        if(!cue().holdForInput())return -1;
        int count=0;
        for(int cursor=0;cursor<index;cursor++)if(cues().get(cursor).holdForInput())count++;
        return count;
    }
    public boolean messageReady() { return !finished&&!awaitingDestination&&cue().holdForInput()&&messageFrames==message().requiredFrames(); }

    public boolean tick() {
        if(finished||awaitingDestination)return false;
        ensureSequence();
        CompoundTag before=save();
        advance(FRAMES_PER_TICK);
        return changed(before);
    }

    public boolean next(int ordinal) {
        if(finished||awaitingDestination||ordinal!=messageOrdinal()||!cue().holdForInput())return false;
        var message=message();
        if(messageReady()) {
            ensureSequence();
            CompoundTag before=save();
            enterNext();advance(0);
            return changed(before);
        }
        if(fast||message.inWait(messageFrames)||messageFrames>=message.normalFrames())return false;
        int target=message.fastRevealFrame(messageFrames);
        if(target==messageFrames)return false;
        ensureSequence();fast=true;messageFrames=target;bump();return true;
    }

    public boolean destinationReady() {
        if(finished||!awaitingDestination)return false;
        ensureSequence();
        awaitingDestination=false;skipped=false;
        transferReady=geographicTransfer();
        bump();return true;
    }

    public boolean skip() {
        if(program.equals("arrival")&&everSkipped)return false;
        ensureSequence();
        resetProgram("arrival");everSkipped=true;skipped=true;awaitingDestination=true;
        bump();return true;
    }

    public boolean changeProgram(String nextProgram) {
        OriginalOpeningPresentationData.program(nextProgram);
        if(program.equals(nextProgram))return false;
        ensureSequence();resetProgram(nextProgram);bump();return true;
    }

    private void resetProgram(String nextProgram) {
        program=nextProgram;index=0;elapsedFrames=0;messageFrames=0;fast=false;finished=false;
        transferReady=false;awaitingDestination=nextProgram.equals("arrival");skipped=false;
    }

    private void advance(int budget) {
        for(int guard=0;guard<=cues().size();guard++) {
            if(finished||awaitingDestination)return;
            Cue cue=cue();
            if(geographicTransfer()&&!transferReady) { awaitingDestination=true;return; }
            if(cue.holdForInput()) {
                var result=message().advance(messageFrames,fast,budget);
                messageFrames=result.frame();elapsedFrames+=result.consumed();
                return;
            }
            int remaining=duration()-elapsedFrames;
            if(remaining>0) {
                int count=Math.min(remaining,budget);
                elapsedFrames+=count;budget-=count;
                if(elapsedFrames<duration())return;
            }
            if(index==cues().size()-1) { finished=true;return; }
            enterNext();
        }
        throw new IllegalStateException("Opening cinematic exceeded its cue budget");
    }

    private void enterNext() {
        if(index>=cues().size()-1) { finished=true;return; }
        index++;elapsedFrames=0;messageFrames=0;fast=false;transferReady=false;
    }

    private int duration() {
        Cue cue=cue();
        return program.startsWith("collapse")&&index==cues().size()-1?Math.max(31,cue.blockingFrames()):cue.blockingFrames();
    }
    private Cue cue() { return cues().get(index); }
    private boolean geographicTransfer() { return cue().code()==201&&cue().args().get(0).intValue()==1; }
    private List<Cue> cues() { return OriginalOpeningPresentationData.program(program).cues(); }
    private OriginalOpeningMessageTimeline message() { return OriginalOpeningMessageTimeline.compile(cue().lines()); }
    private boolean changed(CompoundTag before) { if(before.equals(save()))return false;bump();return true; }
    private void ensureSequence() { if(sequence>=Long.MAX_VALUE-1)throw new IllegalStateException("Opening cinematic sequence exhausted"); }
    private void bump() { sequence++; }

    public CompoundTag save() {
        var tag=new CompoundTag();
        tag.putInt("Version",VERSION);tag.putString("Catalog",OriginalOpeningPresentationData.HASH);tag.putUUID("Episode",episode);tag.putLong("Sequence",sequence);
        tag.putString("Program",program);tag.putInt("Command",command());tag.putInt("ElapsedFrames",elapsedFrames);tag.putInt("MessageFrames",messageFrames);
        tag.putBoolean("Fast",fast);tag.putBoolean("AwaitingDestination",awaitingDestination);tag.putBoolean("TransferReady",transferReady);
        tag.putBoolean("Finished",finished);tag.putBoolean("EverSkipped",everSkipped);tag.putBoolean("Skipped",skipped);
        return tag;
    }

    public OriginalOpeningCinematic copy() { return load(save()); }

    public static OriginalOpeningCinematic load(CompoundTag tag) {
        Objects.requireNonNull(tag);
        if(!tag.getAllKeys().equals(FIELDS)||integer(tag,"Version")!=VERSION||!tag.contains("Catalog",Tag.TAG_STRING)
                ||!OriginalOpeningPresentationData.HASH.equals(tag.getString("Catalog"))||!tag.hasUUID("Episode")
                ||!tag.contains("Sequence",Tag.TAG_LONG)||!tag.contains("Program",Tag.TAG_STRING))throw new IllegalArgumentException("Invalid opening cinematic schema");
        var result=new OriginalOpeningCinematic(tag.getUUID("Episode"),tag.getString("Program"));
        long sequence=tag.getLong("Sequence");
        if(sequence<1||sequence>=Long.MAX_VALUE)throw new IllegalArgumentException("Invalid opening cinematic sequence");
        result.sequence=sequence;
        int command=integer(tag,"Command");
        result.index=-1;
        for(int index=0;index<result.cues().size();index++)if(result.cues().get(index).command()==command)result.index=index;
        if(result.index<0)throw new IllegalArgumentException("Unknown opening cinematic cursor");
        result.elapsedFrames=integer(tag,"ElapsedFrames");result.messageFrames=integer(tag,"MessageFrames");
        result.fast=flag(tag,"Fast");result.awaitingDestination=flag(tag,"AwaitingDestination");result.transferReady=flag(tag,"TransferReady");
        result.finished=flag(tag,"Finished");result.everSkipped=flag(tag,"EverSkipped");result.skipped=flag(tag,"Skipped");
        result.validate();return result;
    }

    private void validate() {
        if(elapsedFrames<0||elapsedFrames>1_000_000||messageFrames<0||messageFrames>1_000_000)throw new IllegalArgumentException("Opening cinematic frame outside bound");
        Cue cue=cue();
        if(cue.holdForInput()) {
            int required=message().requiredFrames();
            if(messageFrames>required||elapsedFrames>messageFrames||!fast&&elapsedFrames!=messageFrames||awaitingDestination||transferReady||finished)throw new IllegalArgumentException("Invalid opening message progress");
        } else if(messageFrames!=0||fast||elapsedFrames>duration())throw new IllegalArgumentException("Invalid timed opening cinematic progress");
        if(transferReady&&(!geographicTransfer()||awaitingDestination))throw new IllegalArgumentException("Invalid cinematic transfer receipt");
        if(awaitingDestination&&(finished||transferReady||!(geographicTransfer()||program.equals("arrival")&&index==0&&elapsedFrames==0)))throw new IllegalArgumentException("Invalid cinematic destination gate");
        if(geographicTransfer()&&!transferReady&&elapsedFrames!=0)throw new IllegalArgumentException("Unacknowledged cinematic transfer already elapsed");
        if(finished&&(index!=cues().size()-1||elapsedFrames!=duration()||geographicTransfer()&&!transferReady))throw new IllegalArgumentException("Invalid cinematic completion");
        if(skipped&&(!everSkipped||!program.equals("arrival")||!awaitingDestination))throw new IllegalArgumentException("Invalid cinematic skip marker");
    }

    private static int integer(CompoundTag tag,String key) { if(!tag.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Invalid opening cinematic integer");return tag.getInt(key); }
    private static boolean flag(CompoundTag tag,String key) {
        if(!tag.contains(key,Tag.TAG_BYTE)||(tag.getByte(key)!=0&&tag.getByte(key)!=1))throw new IllegalArgumentException("Invalid opening cinematic flag");
        return tag.getBoolean(key);
    }
}
