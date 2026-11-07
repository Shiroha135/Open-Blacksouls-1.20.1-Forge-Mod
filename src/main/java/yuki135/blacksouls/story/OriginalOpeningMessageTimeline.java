package yuki135.blacksouls.story;

import java.util.ArrayList;
import java.util.List;

public final class OriginalOpeningMessageTimeline {
    public record Wait(int firstFrame,int endFrame) { }
    public record Advance(int frame,int consumed,boolean ready) { }
    private final int normalFrames;
    private final int requiredFrames;
    private final List<Wait> waits;

    private OriginalOpeningMessageTimeline(int normalFrames,List<Wait> waits) {
        this.normalFrames=normalFrames;
        this.requiredFrames=Math.addExact(normalFrames,1);
        this.waits=List.copyOf(waits);
    }

    public static OriginalOpeningMessageTimeline compile(List<String> source) {
        if(source==null||source.isEmpty()||source.size()>8)throw new IllegalArgumentException("Invalid opening message lines");
        int frame=0;
        var waits=new ArrayList<Wait>();
        for(String text:source) {
            if(text==null||text.length()>2048)throw new IllegalArgumentException("Invalid opening message line");
            for(int index=0;index<text.length();) {
                int code=text.codePointAt(index);index+=Character.charCount(code);
                if(code=='\\'&&index<text.length()) {
                    char escape=text.charAt(index++);
                    if(escape=='c'||escape=='C') {
                        if(index>=text.length()||text.charAt(index++)!='[')throw new IllegalArgumentException("Invalid opening color escape");
                        int end=text.indexOf(']',index);
                        if(end<0)throw new IllegalArgumentException("Unterminated opening color escape");
                        String value=text.substring(index,end);
                        if(!value.matches("[0-9]{1,2}")||Integer.parseInt(value)>31)throw new IllegalArgumentException("Invalid opening message color");
                        index=end+1;
                    } else if(escape=='{'||escape=='}') {
                    } else if(escape=='|'||escape=='.') {
                        int count=escape=='|'?60:15;
                        waits.add(new Wait(frame,Math.addExact(frame,count)));frame+=count;
                    } else if(escape=='\\')frame++;
                    else throw new IllegalArgumentException("Unsupported opening message escape");
                } else frame++;
                if(frame>=1_000_000)throw new IllegalArgumentException("Opening message exceeds frame bound");
            }
        }
        return new OriginalOpeningMessageTimeline(frame,waits);
    }

    public int normalFrames() { return normalFrames; }
    public int requiredFrames() { return requiredFrames; }
    public List<Wait> waits() { return waits; }
    public boolean inWait(int frame) {
        requireFrame(frame);
        return waits.stream().anyMatch(wait->frame>=wait.firstFrame()&&frame<wait.endFrame());
    }

    public int fastRevealFrame(int frame) {
        requireFrame(frame);
        if(frame>=normalFrames||inWait(frame))return frame;
        for(var wait:waits)if(wait.firstFrame()>=frame)return wait.firstFrame();
        return normalFrames;
    }

    public Advance advance(int frame,boolean fast,int budget) {
        requireFrame(frame);
        if(budget<0||budget>60)throw new IllegalArgumentException("Unbounded opening message frame budget");
        int consumed=0;
        for(int guard=0;guard<waits.size()+3;guard++) {
            if(frame>=requiredFrames)return new Advance(frame,consumed,true);
            if(fast)frame=fastRevealFrame(frame);
            if(frame>=requiredFrames)return new Advance(frame,consumed,true);
            if(consumed==budget)return new Advance(frame,consumed,false);
            int limit=requiredFrames;
            for(var wait:waits)if(frame>=wait.firstFrame()&&frame<wait.endFrame()) { limit=wait.endFrame();break; }
            if(!fast) {
                int amount=Math.min(budget-consumed,requiredFrames-frame);
                return new Advance(frame+amount,consumed+amount,frame+amount==requiredFrames);
            }
            int amount=Math.min(budget-consumed,limit-frame);
            frame+=amount;consumed+=amount;
        }
        throw new IllegalStateException("Opening message failed to consume a bounded frame budget");
    }

    private void requireFrame(int frame) { if(frame<0||frame>requiredFrames)throw new IllegalArgumentException("Invalid opening message frame"); }
}
