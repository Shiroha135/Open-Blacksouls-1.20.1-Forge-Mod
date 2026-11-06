package yuki135.blacksouls.combat;

import yuki135.blacksouls.entity.EntityOriginalDatabaseEnemy;
import yuki135.blacksouls.entity.EntityTurnBattleMonster;
import yuki135.blacksouls.entity.EntityOriginalTurnBattleEnemy;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EnemyHeadSearch {
    private static final Definition DEFINITION = loadDefinition();
    private final Map<UUID, Set<Integer>> enemyStates;
    private final Map<UUID, Integer> remainingActions = new HashMap<>();

    public EnemyHeadSearch(Map<UUID, Set<Integer>> enemyStates) {
        this.enemyStates = enemyStates;
    }

    public boolean blocksAction(EntityTurnBattleMonster enemy) {
        return remainingActions.containsKey(enemy.getUUID())
                && enemyStates.getOrDefault(enemy.getUUID(), Set.of()).contains(DEFINITION.stateId());
    }

    public Component actionMessage(EntityTurnBattleMonster enemy) {
        return Component.literal(enemy.getDisplayName().getString() + DEFINITION.actionMessage());
    }

    public void onSkillUsed(int skillId, List<EntityTurnBattleMonster> enemies, RandomSource random) {
        if (skillId != DEFINITION.skillId()) {
            return;
        }
        for (EntityTurnBattleMonster enemy : enemies) {
            if (enemy.isTurnBattleDefeated()) {
                continue;
            }
            int duration = DEFINITION.minActions()
                    + random.nextInt(DEFINITION.maxActions() - DEFINITION.minActions() + 1);
            enemyStates.computeIfAbsent(enemy.getUUID(), ignored -> new java.util.HashSet<>())
                    .add(DEFINITION.stateId());
            remainingActions.put(enemy.getUUID(), duration);
        }
    }

    public Component onActionEnd(EntityTurnBattleMonster enemy) {
        Integer remaining = remainingActions.get(enemy.getUUID());
        if (remaining == null) {
            return Component.empty();
        }
        if (!enemyStates.getOrDefault(enemy.getUUID(), Set.of()).contains(DEFINITION.stateId())) {
            remainingActions.remove(enemy.getUUID());
            return Component.empty();
        }
        if (remaining > 1) {
            remainingActions.put(enemy.getUUID(), remaining - 1);
            return Component.empty();
        }
        remainingActions.remove(enemy.getUUID());
        enemyStates.get(enemy.getUUID()).remove(DEFINITION.stateId());
        return Component.literal(enemy.getDisplayName().getString() + DEFINITION.removedMessage());
    }

    public void updateForms(List<EntityTurnBattleMonster> enemies) {
        for (EntityTurnBattleMonster enemy : enemies) {
            if (enemy instanceof EntityOriginalTurnBattleEnemy legacy
                    && legacy.getProfile() == EntityOriginalTurnBattleEnemy.Profile.WEREWOLF) {
                legacy.setHeadSearching(enemyStates.getOrDefault(enemy.getUUID(), Set.of())
                        .contains(DEFINITION.stateId()));
                continue;
            }
            if (!(enemy instanceof EntityOriginalDatabaseEnemy original) || enemy.isTurnBattleDefeated()) {
                continue;
            }
            for (Transformation transformation : DEFINITION.transformations()) {
                if (original.getProfileId() == transformation.normalProfileId()
                        || original.getProfileId() == transformation.searchingProfileId()) {
                    boolean searching = enemyStates.getOrDefault(enemy.getUUID(), Set.of())
                            .contains(DEFINITION.stateId());
                    original.transformTurnBattleProfile(searching
                            ? transformation.searchingProfileId() : transformation.normalProfileId());
                    break;
                }
            }
        }
    }

    public void clear(List<EntityTurnBattleMonster> enemies) {
        remainingActions.clear();
        for (EntityTurnBattleMonster enemy : enemies) {
            Set<Integer> states = enemyStates.get(enemy.getUUID());
            if (states != null) {
                states.remove(DEFINITION.stateId());
            }
        }
        updateForms(enemies);
    }

    private static Definition loadDefinition() {
        String path = "/data/blacksouls/original_enemy_head_search.json";
        try (InputStream stream = EnemyHeadSearch.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing original enemy head search data");
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            List<Transformation> transformations = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("transformations")) {
                JsonObject transformation = element.getAsJsonObject();
                transformations.add(new Transformation(transformation.get("normalProfileId").getAsInt(),
                        transformation.get("searchingProfileId").getAsInt()));
            }
            Definition definition = new Definition(json.get("skillId").getAsInt(),
                    json.get("stateId").getAsInt(), json.get("minActions").getAsInt(),
                    json.get("maxActions").getAsInt(), json.get("actionMessage").getAsString(),
                    json.get("removedMessage").getAsString(), List.copyOf(transformations));
            if (definition.minActions() < 1 || definition.maxActions() < definition.minActions()) {
                throw new IllegalStateException("Invalid original head search action duration");
            }
            return definition;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load original enemy head search data", exception);
        }
    }

    private record Definition(int skillId, int stateId, int minActions, int maxActions,
                              String actionMessage, String removedMessage, List<Transformation> transformations) {
    }

    private record Transformation(int normalProfileId, int searchingProfileId) {
    }
}
