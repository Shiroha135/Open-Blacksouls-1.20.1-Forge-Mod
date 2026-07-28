package com.BlackSouls.BlackSoulsMod.combat;

import java.util.List;
import java.util.Map;

public final class TurnBattleDomainData {
    private static final Domain FLORENCE_DOMAIN = new Domain(
            "天兽领域\"屠灭病魔的天使歌声\"",
            List.of("-攻击力、魔力 -100%", "-HP恢复造成伤害"),
            false, false, false, false, false,
            0.0D, 0.0D, 1.0D, 1.0D);

    private static final Map<Integer, Domain> DOMAINS = Map.ofEntries(
            Map.entry(493, FLORENCE_DOMAIN),
            Map.entry(494, FLORENCE_DOMAIN),
            Map.entry(495, FLORENCE_DOMAIN),
            Map.entry(499, new Domain("无风领域\"乘风行走之物\"",
                    List.of("-通常攻击不可", "-物理闪避率-100%", "-防御力-70%"),
                    false, false, false, false, true,
                    1.0D, 1.0D, 0.3D, 1.0D)),
            Map.entry(511, new Domain("神圣领域\"瘟疫万魔殿\"",
                    List.of("-每回合 HP-10%伤害", "-道具使用不可"),
                    true, false, false, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(518, new Domain("溶解领域\"炉心融解\"",
                    List.of("-最大HP-90%", "-暴击率-100%"),
                    false, false, false, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(545, new Domain("黑之领域\"制裁罪恶的魔狼\"",
                    List.of("-暗耐性-100%", "-暴击闪避率-100%"),
                    false, false, false, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(550, new Domain("黑之领域\"决斗审判\"",
                    List.of("-双方 不死", "-双方 攻击力、魔力+900%"),
                    false, false, false, false, false,
                    10.0D, 10.0D, 1.0D, 1.0D)),
            Map.entry(556, new Domain("白之领域\"不可见之独角兽\"",
                    List.of("-必中攻击无效", "-强化不可"),
                    false, false, true, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(557, new Domain("白之领域\"不可见之独角兽\"",
                    List.of("-必中攻击无效", "-强化不可"),
                    false, false, true, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(558, new Domain("白之领域\"狮子奋迅\"",
                    List.of("-技·魔法 使用不可", "-随着回合增加防御力、闪避率下降直至-100%"),
                    false, true, false, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(561, new Domain("恶辣剧场\"破灭的红颜祸水\"",
                    List.of("-随着回合增加攻击力-100%→防御力-100%→魔力-100%→魔法防御-100%"),
                    false, false, false, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(564, new Domain("恶辣剧场\"狮子与独角兽\"",
                    List.of("-强化不可", "-随着回合增加防御力、闪避率下降直至-100%"),
                    false, false, true, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(566, new Domain("恶辣剧场\"机械装置的失乐园\"",
                    List.of("-道具使用不可", "-对敌人造成的百分比伤害减半",
                            "-随着回合增加最大HP、速度下降直至-99%"),
                    true, false, false, false, false,
                    1.0D, 1.0D, 1.0D, 1.0D)),
            Map.entry(567, new Domain("恶辣剧场\"混沌\"",
                    List.of("-防御力、魔法防御-95%", "-状态异常耐性-100%",
                            "-随着回合增加敌人的行动次数增加"),
                    false, false, false, false, false,
                    1.0D, 1.0D, 0.05D, 0.05D)),
            Map.entry(568, new Domain("恶辣剧场\"爱丽丝婚姻奇谭\"",
                    List.of("-全属性-30%", "-光、暗、炎、冰、雷耐性-100%",
                            "-命中率、暴击率、闪避率、暴击闪避率-30%"),
                    false, false, false, false, false,
                    0.7D, 0.7D, 0.7D, 0.7D)),
            Map.entry(570, new Domain("恶辣剧场\"最后的爱之呼唤\"",
                    List.of("-闪避禁止", "-防御禁止"),
                    false, false, false, true, false,
                    1.0D, 1.0D, 1.0D, 1.0D))
    );

    private TurnBattleDomainData() {
    }

    public static Domain get(int profileId) {
        return DOMAINS.get(profileId);
    }

    public record Domain(String title, List<String> lines, boolean itemDisabled,
                         boolean skillDisabled, boolean buffDisabled,
                         boolean guardDisabled, boolean normalAttackDisabled,
                         double attackRate, double magicRate,
                         double defenseRate, double magicDefenseRate) {
    }
}
