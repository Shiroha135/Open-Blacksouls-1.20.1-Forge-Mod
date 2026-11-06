package yuki135.blacksouls.entity;

/**
 * 路德维希市街 · 福尔摩斯（原事件 Map002 / EV059）。
 * 对话分三段：初见警告 → 递药 → 常态（可询问爱丽丝 / 杀害 / 离开）。
 */
public final class HolmesDialogue {
    private static final String PREFIX = "dialogue.blacksouls.holmes.";

    /** 初见：警告这片街区已成魔兽与狂人的狂宴场。 */
    public static String[] introductionKeys() {
        return keys("intro", 3);
    }

    /** 再次交谈：发现玩家受伤，赠予输血药。 */
    public static String[] woundedKeys() {
        return keys("wounded", 1);
    }

    /** 常态：可询问爱丽丝 / 杀害 / 离开。 */
    public static String[] mainKeys() {
        return keys("main", 1);
    }

    /** 询问爱丽丝的消息（分支）。 */
    public static String[] aliceAnswerKeys() {
        return keys("alice", 2);
    }

    /** 爱丽丝回答 + 开膛手杰克独白，串成一串连续对话。 */
    public static String[] aliceAndRipperKeys() {
        return new String[]{
                PREFIX + "alice_1",
                PREFIX + "alice_2",
                PREFIX + "ripper_1",
                PREFIX + "ripper_2",
                PREFIX + "ripper_3"
        };
    }

    /** 开膛手杰克的独白（接在爱丽丝分支之后，一次性）。 */
    public static String[] ripperKeys() {
        return keys("ripper", 3);
    }

    private static String[] keys(String group, int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = PREFIX + group + "_" + (index + 1);
        }
        return result;
    }

    public enum Mode {
        NONE,
        INTRO,
        WOUNDED,
        MAIN;

        public static Mode fromNetwork(int value) {
            return value >= 0 && value < values().length ? values()[value] : NONE;
        }
    }

    private HolmesDialogue() {
    }
}
