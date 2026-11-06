package yuki135.blacksouls.combat;

import yuki135.blacksouls.entity.EntityOriginalDatabaseEnemy;
import yuki135.blacksouls.entity.EntityOriginalTurnBattleEnemy;
import yuki135.blacksouls.entity.EntityTurnBattleMonster;
import yuki135.blacksouls.util.BSOriginalCombatData;
import yuki135.blacksouls.capability.BSPlayerStats;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OriginalBattleState {
    private final Map<UUID, Set<Integer>> activeStates;
    private final Map<UUID, Map<Integer, StateDuration>> stateDurations = new HashMap<>();
    private final Map<UUID, Map<Integer, ParameterBuff>> parameterBuffs = new HashMap<>();

    public OriginalBattleState(Map<UUID, Set<Integer>> enemyStates) {
        this.activeStates = enemyStates;
    }

    public boolean addState(LivingEntity entity, int stateId, RandomSource random) {
        return addState(entity, stateId, random, false);
    }

    public boolean addStateDirect(LivingEntity entity, int stateId, RandomSource random) {
        return addState(entity, stateId, random, true);
    }

    private boolean addState(LivingEntity entity, int stateId, RandomSource random, boolean direct) {
        BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
        if (!isAlive(entity) || state == null || hasFeature(entity, 14, stateId)
                || !direct && stateRate(entity, stateId) <= 0.0D
                || state.removeByRestriction() && restriction(entity) > 0) {
            return false;
        }
        Set<Integer> states = activeStates.computeIfAbsent(entity.getUUID(), ignored -> new HashSet<>());
        boolean existed = states.contains(stateId);
        StateDuration previous = stateDurations.getOrDefault(entity.getUUID(), Map.of()).get(stateId);
        states.add(stateId);
        stateDurations.computeIfAbsent(entity.getUUID(), ignored -> new HashMap<>())
                .put(stateId, new StateDuration(randomTurns(state, random),
                        previous == null ? !existed : previous.owned()));
        if (!existed && restriction(entity) > 0) {
            for (int activeId : List.copyOf(states)) {
                BSOriginalCombatData.State active = BSOriginalCombatData.state(activeId);
                if (active != null && active.removeByRestriction()) {
                    removeState(entity, activeId);
                }
            }
        }
        refreshImmunities(entity);
        return hasState(entity, stateId);
    }

    public boolean removeState(LivingEntity entity, int stateId) {
        Map<Integer, StateDuration> durations = stateDurations.get(entity.getUUID());
        if (durations != null) {
            durations.remove(stateId);
        }
        Set<Integer> states = activeStates.get(entity.getUUID());
        boolean removed = states != null && states.remove(stateId);
        if (removed) {
            refreshImmunities(entity);
        }
        return removed;
    }

    public boolean hasState(LivingEntity entity, int stateId) {
        return activeStates.getOrDefault(entity.getUUID(), Set.of()).contains(stateId);
    }

    public void initializeStates(LivingEntity entity, RandomSource random) {
        Map<Integer, StateDuration> durations = stateDurations.computeIfAbsent(
                entity.getUUID(), ignored -> new HashMap<>());
        for (int stateId : activeStates.getOrDefault(entity.getUUID(), Set.of())) {
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            if (state != null && !durations.containsKey(stateId)) {
                durations.put(stateId, new StateDuration(randomTurns(state, random), false));
            }
        }
    }

    public boolean addBuff(LivingEntity entity, int paramId, int turns, boolean debuff) {
        if (!isAlive(entity) || !isParameter(paramId)) {
            return false;
        }
        Map<Integer, ParameterBuff> buffs = parameterBuffs.computeIfAbsent(
                entity.getUUID(), ignored -> new HashMap<>());
        ParameterBuff previous = buffs.getOrDefault(paramId, new ParameterBuff(0, 0));
        int level = previous.level();
        level = debuff ? Math.max(-2, level - 1) : Math.min(2, level + 1);
        boolean erased = debuff && level > 0 || !debuff && level < 0;
        if (erased) {
            level = 0;
        }
        int duration = erased ? Math.max(0, turns)
                : Math.max(previous.turns(), Math.max(0, turns));
        buffs.put(paramId, new ParameterBuff(level, duration));
        return true;
    }

    public boolean removeBuff(LivingEntity entity, int paramId, boolean debuff) {
        if (!isAlive(entity) || !isParameter(paramId)) {
            return false;
        }
        Map<Integer, ParameterBuff> buffs = parameterBuffs.get(entity.getUUID());
        ParameterBuff buff = buffs == null ? null : buffs.get(paramId);
        if (buff == null || (debuff ? buff.level() >= 0 : buff.level() <= 0)) {
            return false;
        }
        buffs.remove(paramId);
        return true;
    }

    public int buffLevel(LivingEntity entity, int paramId) {
        ParameterBuff buff = parameterBuffs.getOrDefault(entity.getUUID(), Map.of()).get(paramId);
        return buff == null ? 0 : buff.level();
    }

    public int buffTurns(LivingEntity entity, int paramId) {
        ParameterBuff buff = parameterBuffs.getOrDefault(entity.getUUID(), Map.of()).get(paramId);
        return buff == null ? 0 : buff.turns();
    }

    public int stateTurns(LivingEntity entity, int stateId) {
        StateDuration duration = stateDurations.getOrDefault(entity.getUUID(), Map.of()).get(stateId);
        return duration == null ? -1 : duration.turns();
    }

    public double parameterRate(LivingEntity entity, int paramId) {
        return product(entity, 21, paramId) * (1.0D + buffLevel(entity, paramId) * 0.25D);
    }

    public double stateRate(LivingEntity entity, int stateId) {
        return product(entity, 13, stateId);
    }

    public double debuffRate(LivingEntity entity, int paramId) {
        return product(entity, 12, paramId);
    }

    public double elementRate(LivingEntity entity, int elementId) {
        return product(entity, 11, elementId);
    }

    public double xparam(LivingEntity entity, int paramId) {
        double value = 0.0D;
        for (BSOriginalCombatData.Feature feature : features(entity)) {
            if (feature.code() == 22 && feature.dataId() == paramId) {
                value += feature.value();
            }
        }
        return value;
    }

    public double sparam(LivingEntity entity, int paramId) {
        return product(entity, 23, paramId);
    }

    public boolean hasFeature(LivingEntity entity, int code, int dataId) {
        return features(entity).stream().anyMatch(feature -> feature.code() == code && feature.dataId() == dataId);
    }

    public List<BSOriginalCombatData.Feature> features(LivingEntity entity) {
        List<BSOriginalCombatData.Feature> features = new ArrayList<>();
        entity.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            var spirit = stats.getOriginalSpirit();
            if (spirit != null) features.addAll(spirit.battleFeatures());
        });
        int profileId = entity instanceof EntityOriginalDatabaseEnemy original ? original.getProfileId()
                : entity instanceof EntityOriginalTurnBattleEnemy legacy ? legacy.getOriginalBattleProfileId() : 0;
        if (profileId > 0) {
            features.addAll(BSOriginalCombatData.enemy(profileId).features());
        }
        for (int stateId : activeStates.getOrDefault(entity.getUUID(), Set.of())) {
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            if (state != null) {
                features.addAll(state.features());
            }
        }
        return List.copyOf(features);
    }

    public int restriction(LivingEntity entity) {
        int restriction = 0;
        for (int stateId : activeStates.getOrDefault(entity.getUUID(), Set.of())) {
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            if (state != null) {
                restriction = Math.max(restriction, state.restriction());
            }
        }
        return restriction;
    }

    public Component advanceAction(LivingEntity entity) {
        Component message = advanceStates(entity, 1);
        Map<Integer, ParameterBuff> buffs = parameterBuffs.get(entity.getUUID());
        if (buffs != null) {
            buffs.replaceAll((paramId, buff) -> new ParameterBuff(buff.level(), Math.max(0, buff.turns() - 1)));
            buffs.entrySet().removeIf(entry -> entry.getValue().turns() <= 0);
        }
        return message;
    }

    public Component advanceRound(LivingEntity entity) {
        return advanceStates(entity, 2);
    }

    public void onDamage(LivingEntity entity, RandomSource random) {
        for (int stateId : List.copyOf(activeStates.getOrDefault(entity.getUUID(), Set.of()))) {
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            if (state != null && state.removeByDamage() && random.nextInt(100) < state.chanceByDamage()) {
                removeState(entity, stateId);
            }
        }
    }

    public void clear(List<? extends LivingEntity> entities) {
        for (LivingEntity entity : entities) {
            Map<Integer, StateDuration> durations = stateDurations.remove(entity.getUUID());
            if (durations != null) {
                Set<Integer> states = activeStates.get(entity.getUUID());
                if (states != null) {
                    for (Map.Entry<Integer, StateDuration> entry : durations.entrySet()) {
                        BSOriginalCombatData.State state = BSOriginalCombatData.state(entry.getKey());
                        if (entry.getValue().owned() && state != null && state.removeAtBattleEnd()) {
                            states.remove(entry.getKey());
                        }
                    }
                }
            }
            parameterBuffs.remove(entity.getUUID());
        }
    }

    private Component advanceStates(LivingEntity entity, int timing) {
        Map<Integer, StateDuration> durations = stateDurations.get(entity.getUUID());
        if (durations == null) {
            return Component.empty();
        }
        List<String> messages = new ArrayList<>();
        for (Map.Entry<Integer, StateDuration> entry : List.copyOf(durations.entrySet())) {
            int stateId = entry.getKey();
            if (!hasState(entity, stateId)) {
                durations.remove(stateId);
                continue;
            }
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            if (state == null || state.autoRemovalTiming() != timing) {
                continue;
            }
            int remaining = Math.max(0, entry.getValue().turns() - 1);
            if (remaining == 0) {
                removeState(entity, stateId);
                if (!state.message4().isEmpty()) {
                    messages.add(entity.getDisplayName().getString() + state.message4());
                }
            } else {
                durations.put(stateId, new StateDuration(remaining, entry.getValue().owned()));
            }
        }
        return Component.literal(String.join("\n", messages));
    }

    private void refreshImmunities(LivingEntity entity) {
        Set<Integer> states = activeStates.get(entity.getUUID());
        if (states == null) {
            return;
        }
        Set<Integer> immune = new HashSet<>();
        for (BSOriginalCombatData.Feature feature : features(entity)) {
            if (feature.code() == 14) {
                immune.add(feature.dataId());
            }
        }
        states.removeAll(immune);
        Map<Integer, StateDuration> durations = stateDurations.get(entity.getUUID());
        if (durations != null) {
            immune.forEach(durations::remove);
        }
    }

    private double product(LivingEntity entity, int code, int dataId) {
        double value = 1.0D;
        for (BSOriginalCombatData.Feature feature : features(entity)) {
            if (feature.code() == code && feature.dataId() == dataId) {
                value *= feature.value();
            }
        }
        return value;
    }

    private static int randomTurns(BSOriginalCombatData.State state, RandomSource random) {
        int min = Math.max(0, state.minTurns());
        int max = Math.max(min, state.maxTurns());
        return min + random.nextInt(max - min + 1);
    }

    private static boolean isParameter(int paramId) {
        return paramId >= 0 && paramId < 8;
    }

    private static boolean isAlive(LivingEntity entity) {
        return entity instanceof EntityTurnBattleMonster enemy ? !enemy.isTurnBattleDefeated() : entity.isAlive();
    }

    private record StateDuration(int turns, boolean owned) {
    }

    private record ParameterBuff(int level, int turns) {
    }
}
