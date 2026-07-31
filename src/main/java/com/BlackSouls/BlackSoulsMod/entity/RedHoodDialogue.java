package com.BlackSouls.BlackSoulsMod.entity;

import java.util.List;
import java.util.Locale;

public final class RedHoodDialogue {
    private static final Scene INTRO = new Scene("intro", List.of(), keys("intro", 7));
    private static final List<Scene> SCENES = List.of(
            new Scene("oxford", List.of("牛津学院", "オックス", "oxford"), keys("oxford", 2)),
            new Scene("beach", List.of("叹息的海边", "嘆きの浜辺", "beach"), keys("beach", 2)),
            new Scene("carol_river", List.of("卡罗尔川", "キャロル川", "carol"), keys("carol_river", 2)),
            new Scene("liddell_cemetery", List.of("利德尔墓地", "リデル墓地", "liddell"), keys("liddell_cemetery", 2)),
            new Scene("ripon_cathedral", List.of("里彭大圣堂", "リポン大聖堂", "ripon"), keys("ripon_cathedral", 2)),
            new Scene("heart_garden", List.of("心脏的庭园", "心臓の庭園", "heart garden"), keys("heart_garden", 2)),
            new Scene("frissel", List.of("红城弗里塞尔", "紅城フリッセル", "frissel"), keys("frissel", 1)),
            new Scene("mushroom_village", List.of("茸村", "蘑菇村", "mushroom"), keys("mushroom_village", 1)),
            new Scene("rabbit_hole", List.of("兔子洞", "ウサギ穴", "rabbit hole"), keys("rabbit_hole", 2)),
            new Scene("duchess_manor", List.of("公爵夫人之馆", "公爵夫人の館", "duchess"), keys("duchess_manor", 1)),
            new Scene("fish_market", List.of("比林斯门鱼市场", "ビリングスゲート", "fish market"), keys("fish_market", 2)),
            new Scene("slaughterhouse", List.of("屠宰场", "屠殺場", "slaughterhouse"), keys("slaughterhouse", 2)),
            new Scene("miserable_inn", List.of("凄惨的旅宿", "悲惨な旅宿", "miserable inn"), keys("miserable_inn", 1)),
            new Scene("smoky_forest", List.of("熏烟之森", "燻煙の森", "smoky forest"), keys("smoky_forest", 2)),
            new Scene("ruined_brothel", List.of("荒废的娼馆", "廃娼館", "ruined brothel"), keys("ruined_brothel", 5))
    );

    public static String resolveScene(String bonfireName, int storyStage) {
        if (storyStage <= 0) {
            return INTRO.id();
        }
        String normalized = bonfireName == null ? "" : bonfireName.toLowerCase(Locale.ROOT);
        for (Scene scene : SCENES) {
            if (scene.matches().stream().anyMatch(normalized::contains)) {
                return scene.id();
            }
        }
        return SCENES.get(Math.min(storyStage - 1, SCENES.size() - 1)).id();
    }

    public static String[] keysFor(String sceneId, int storyStage) {
        if (INTRO.id().equals(sceneId) || storyStage <= 0) {
            return INTRO.keys();
        }
        for (Scene scene : SCENES) {
            if (scene.id().equals(sceneId)) {
                return scene.keys();
            }
        }
        return SCENES.get(Math.min(Math.max(0, storyStage - 1), SCENES.size() - 1)).keys();
    }

    public static boolean hasNext(int storyStage) {
        return storyStage < SCENES.size();
    }

    private static String[] keys(String scene, int count) {
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = "dialogue.blacksouls.red_hood." + scene + "_" + (i + 1);
        }
        return result;
    }

    private record Scene(String id, List<String> matches, String[] keys) {
    }

    private RedHoodDialogue() {
    }
}
