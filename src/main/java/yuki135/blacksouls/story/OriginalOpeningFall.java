package yuki135.blacksouls.story;

public final class OriginalOpeningFall {
    public static final double DROP=3.0D;
    public static final int VISIBLE_FRAMES=166;

    private OriginalOpeningFall() {
    }

    public static int frames(OriginalOpeningCinematic cinematic) {
        if (!cinematic.program().equals("finale99")) throw new IllegalArgumentException("Opening fall requires its source finale");
        int elapsed=0;
        for (var cue:OriginalOpeningPresentationData.program("finale99").cues()) {
            if (cue.command()==cinematic.command()) return Math.min(VISIBLE_FRAMES,elapsed+cinematic.elapsedFrames());
            elapsed+=cue.blockingFrames();
            if (elapsed>=VISIBLE_FRAMES) return VISIBLE_FRAMES;
        }
        throw new IllegalArgumentException("Unknown opening fall source cue");
    }

    public static double distance(int sourceFrames) {
        if (sourceFrames<0) throw new IllegalArgumentException("Negative opening fall source frame");
        double progress=Math.min(VISIBLE_FRAMES,sourceFrames)/(double)VISIBLE_FRAMES;
        return DROP*progress*progress;
    }
}
