package yuki135.blacksouls.story;

import yuki135.blacksouls.story.OriginalOpeningProfile.Cursor;

public final class OriginalOpeningGreeting {
    public static final int FIRST_COMMAND=80;
    public static final int SECOND_COMMAND=83;
    public static final int THIRD_COMMAND=87;
    public static final int ANIMATION_COMMAND=89;
    public static final int LAST_COMMAND=116;
    public static final int COMPLETE_COMMAND=118;
    public static final int DURATION_TICKS=57;
    public static final int ANIMATION_ID=109;
    public static final int ANIMATION_FRAMES=121;

    private OriginalOpeningGreeting() { }

    public static boolean selectable(int origin) { return origin>=1 && origin<=9; }

    public static boolean active(int command) { return page(command)>=0||animation(command); }
    public static boolean animation(int command) { return command==ANIMATION_COMMAND; }
    public static int page(int command) {
        return switch(command) {
            case FIRST_COMMAND->0;
            case SECOND_COMMAND->1;
            case THIRD_COMMAND->2;
            case LAST_COMMAND->3;
            default->-1;
        };
    }

    public static String message(int command,String name) {
        return switch(command) {
            case FIRST_COMMAND->"「"+name+"先生，早上好。\n　呵呵，您睡眼迷蒙的样子真是可爱呢」";
            case SECOND_COMMAND->"「我想您可能很难理解现在的状况吧。\n　不过现在…还请您听从我说的话。\n　这样肯定，对您是有好处的」";
            case THIRD_COMMAND->"「那么接下来，请选择你的出身吧」";
            case LAST_COMMAND->"「您只能选择一种出身，所以请务必慎重选择」";
            default->"";
        };
    }

    public static Cursor cursor(int command) {
        if(!active(command)&&command!=COMPLETE_COMMAND)throw new IllegalArgumentException("Invalid opening greeting cursor");
        return new Cursor(7,1,0,command,0,animation(command)?DURATION_TICKS:0);
    }

    public static int next(int command,int page) {
        if(page(command)!=page||page<0)return command;
        return switch(command) {
            case FIRST_COMMAND->SECOND_COMMAND;
            case SECOND_COMMAND->THIRD_COMMAND;
            case THIRD_COMMAND->ANIMATION_COMMAND;
            case LAST_COMMAND->COMPLETE_COMMAND;
            default->command;
        };
    }

    public static int animationFrame(int remainingTicks,int origin) {
        if(origin<1||origin>9)throw new IllegalArgumentException("Invalid opening profession");
        return (DURATION_TICKS-Math.max(0,Math.min(DURATION_TICKS,remainingTicks))-(origin-1)*2)*3;
    }
}
