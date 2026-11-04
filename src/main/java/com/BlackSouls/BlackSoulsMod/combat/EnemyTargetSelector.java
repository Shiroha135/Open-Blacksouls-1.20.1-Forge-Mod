package com.BlackSouls.BlackSoulsMod.combat;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class EnemyTargetSelector {
    private EnemyTargetSelector() {
    }

    public static List<LivingEntity> select(int scope, LivingEntity caster, List<? extends LivingEntity> allies,
                                           List<? extends LivingEntity> opponents, Predicate<LivingEntity> alive,
                                           ToDoubleFunction<LivingEntity> weight, RandomSource random) {
        if (scope == 0) {
            return List.of();
        }
        if (scope == 11) {
            return List.of(caster);
        }
        boolean friend = scope >= 7;
        boolean dead = scope == 9 || scope == 10;
        List<LivingEntity> candidates = (friend ? allies : opponents).stream()
                .filter(entity -> alive.test(entity) != dead).map(entity -> (LivingEntity) entity).toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        if (scope == 2 || scope == 8 || scope == 10) {
            return candidates;
        }
        int count = scope >= 3 && scope <= 6 ? scope - 2 : 1;
        if (candidates.size() == 1) {
            return java.util.Collections.nCopies(count, candidates.get(0));
        }
        List<LivingEntity> targets = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double total = candidates.stream().mapToDouble(entity -> Math.max(0.0D, weight.applyAsDouble(entity))).sum();
            if (total <= 0.0D) {
                targets.add(candidates.get(random.nextInt(candidates.size())));
                continue;
            }
            double roll = random.nextDouble() * total;
            LivingEntity selected = candidates.get(candidates.size() - 1);
            for (LivingEntity candidate : candidates) {
                roll -= Math.max(0.0D, weight.applyAsDouble(candidate));
                if (roll < 0.0D) {
                    selected = candidate;
                    break;
                }
            }
            targets.add(selected);
        }
        return List.copyOf(targets);
    }
}
