package com.BlackSouls.BlackSoulsMod.combat;

import com.BlackSouls.BlackSoulsMod.util.BSOriginalCommonEventData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnemyCommonEvents {
    private static final int MAX_COMMANDS = 512;
    private static final int MAX_BRANCH_DEPTH = 32;
    private static final String ACTOR_COOLDOWN_SCRIPT = "$game_temp.actor_loss_cool_time(99)";
    private final Map<Integer, BSOriginalCommonEventData.Entry> events;
    private final Map<Integer, Boolean> switches = new HashMap<>();

    public EnemyCommonEvents() {
        this(BSOriginalCommonEventData.values());
    }

    public EnemyCommonEvents(Map<Integer, BSOriginalCommonEventData.Entry> events) {
        this.events = Map.copyOf(events);
    }

    public boolean switchValue(int switchId) {
        return switches.getOrDefault(switchId, false);
    }

    public boolean supports(int eventId) {
        return eventId != 44 && valid(events.get(eventId));
    }

    public boolean execute(int eventId, Context context) {
        if (context == null || !supports(eventId)) {
            return false;
        }
        Deque<Branch> branches = new ArrayDeque<>();
        boolean enabled = true;
        for (BSOriginalCommonEventData.Command command : events.get(eventId).commands()) {
            List<JsonElement> parameters = command.parameters();
            if (command.code() == 111) {
                boolean condition = enabled && condition(parameters, context);
                branches.push(new Branch(enabled, condition));
                enabled = condition;
            } else if (command.code() == 411) {
                Branch branch = branches.peek();
                enabled = branch.parentEnabled() && !branch.condition();
            } else if (command.code() == 412) {
                enabled = branches.pop().parentEnabled();
            } else if (enabled && command.code() == 121) {
                for (int id = integer(parameters, 0); id <= integer(parameters, 1); id++) {
                    boolean enabledValue = integer(parameters, 2) == 0;
                    switches.put(id, enabledValue);
                    if (id == 22) context.setInformationEnabled(enabledValue);
                }
            } else if (enabled && command.code() == 122) {
                context.operatePlayerVariable(integer(parameters, 0), integer(parameters, 2), integer(parameters, 4));
            } else if (enabled && command.code() == 331) {
                long value = integer(parameters, 3);
                context.changeEnemyHealth(integer(parameters, 0), integer(parameters, 1) == 0 ? value : -value,
                        parameters.get(4).getAsBoolean());
            } else if (enabled && command.code() == 355) {
                context.reduceActorSkillCooldowns(99);
            } else if (enabled && command.code() == 333) {
                context.changeEnemyState(integer(parameters, 0), integer(parameters, 1) == 1,
                        integer(parameters, 2));
            }
        }
        return true;
    }

    private boolean condition(List<JsonElement> parameters, Context context) {
        if (integer(parameters, 0) == 0) {
            return switchValue(integer(parameters, 1)) == (integer(parameters, 2) == 0);
        }
        return context.hasEnemyState(integer(parameters, 1), integer(parameters, 3));
    }

    private static boolean valid(BSOriginalCommonEventData.Entry event) {
        if (event == null || event.commands().isEmpty() || event.commands().size() > MAX_COMMANDS) {
            return false;
        }
        Deque<Boolean> branches = new ArrayDeque<>();
        try {
            for (int index = 0; index < event.commands().size(); index++) {
                BSOriginalCommonEventData.Command command = event.commands().get(index);
                List<JsonElement> parameters = command.parameters();
                int code = command.code();
                if (code == 411 || code == 412) {
                    if (branches.isEmpty() || command.indent() != branches.size() - 1 || !parameters.isEmpty()) {
                        return false;
                    }
                    if (code == 411) {
                        if (branches.pop()) {
                            return false;
                        }
                        branches.push(true);
                    } else {
                        branches.pop();
                    }
                    continue;
                }
                if (command.indent() != branches.size()) {
                    return false;
                }
                switch (code) {
                    case 0 -> {
                        if (!parameters.isEmpty() || !branches.isEmpty() || index != event.commands().size() - 1) {
                            return false;
                        }
                    }
                    case 121 -> {
                        if (parameters.size() != 3 || !id(integer(parameters, 0))
                                || !id(integer(parameters, 1)) || integer(parameters, 1) < integer(parameters, 0)
                                || integer(parameters, 1) - integer(parameters, 0) > 4096
                                || !operation(integer(parameters, 2))) {
                            return false;
                        }
                    }
                    case 122 -> {
                        if (parameters.size() != 5 || integer(parameters, 0) != 38 || integer(parameters, 1) != 38
                                || integer(parameters, 2) != 0 || integer(parameters, 3) != 0 || integer(parameters, 4) != 100) {
                            return false;
                        }
                    }
                    case 331 -> {
                        if (parameters.size() != 5 || integer(parameters, 0) < -1
                                || integer(parameters, 0) > 999 || !operation(integer(parameters, 1))
                                || integer(parameters, 2) != 0 || integer(parameters, 3) < 0
                                || !parameters.get(4).isJsonPrimitive()
                                || !parameters.get(4).getAsJsonPrimitive().isBoolean()) {
                            return false;
                        }
                    }
                    case 333 -> {
                        if (parameters.size() != 3 || integer(parameters, 0) < -1
                                || integer(parameters, 0) > 999 || !operation(integer(parameters, 1))
                                || !id(integer(parameters, 2))) {
                            return false;
                        }
                    }
                    case 355 -> {
                        if (parameters.size() != 1 || !parameters.get(0).isJsonPrimitive()
                                || !parameters.get(0).getAsJsonPrimitive().isString()
                                || !ACTOR_COOLDOWN_SCRIPT.equals(parameters.get(0).getAsString())) {
                            return false;
                        }
                    }
                    case 230 -> {
                        if (parameters.size() != 1 || integer(parameters, 0) < 0 || integer(parameters, 0) > 36000) {
                            return false;
                        }
                    }
                    case 250 -> {
                        if (parameters.size() != 1 || !sound(parameters.get(0))) {
                            return false;
                        }
                    }
                    case 111 -> {
                        if (!validCondition(parameters) || branches.size() >= MAX_BRANCH_DEPTH) {
                            return false;
                        }
                        branches.push(false);
                    }
                    default -> {
                        return false;
                    }
                }
            }
            return branches.isEmpty() && event.commands().get(event.commands().size() - 1).code() == 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean validCondition(List<JsonElement> parameters) {
        if (parameters.isEmpty()) {
            return false;
        }
        if (integer(parameters, 0) == 0) {
            return parameters.size() == 3 && id(integer(parameters, 1)) && operation(integer(parameters, 2));
        }
        return integer(parameters, 0) == 5 && parameters.size() == 4
                && integer(parameters, 1) >= 0 && integer(parameters, 1) <= 999
                && integer(parameters, 2) == 1 && id(integer(parameters, 3));
    }

    private static boolean sound(JsonElement value) {
        if (!value.isJsonObject()) {
            return false;
        }
        JsonObject sound = value.getAsJsonObject();
        return sound.has("name") && sound.get("name").isJsonPrimitive()
                && sound.get("name").getAsJsonPrimitive().isString()
                && sound.has("volume") && sound.has("pitch")
                && integer(List.of(sound.get("volume")), 0) >= 0
                && integer(List.of(sound.get("volume")), 0) <= 100
                && integer(List.of(sound.get("pitch")), 0) >= 50
                && integer(List.of(sound.get("pitch")), 0) <= 150;
    }

    private static int integer(List<JsonElement> parameters, int index) {
        JsonElement value = parameters.get(index);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected integer event parameter");
        }
        return value.getAsBigDecimal().intValueExact();
    }

    private static boolean id(int value) {
        return value > 0 && value <= 65535;
    }

    private static boolean operation(int value) {
        return value == 0 || value == 1;
    }

    private record Branch(boolean parentEnabled, boolean condition) {
    }

    public interface Context {
        boolean hasEnemyState(int enemyIndex, int stateId);

        void changeEnemyState(int enemyIndex, boolean remove, int stateId);

        default void setInformationEnabled(boolean enabled) {
            throw new UnsupportedOperationException("enemy_information_switch");
        }

        default void operatePlayerVariable(int variableId, int operation, long operand) {
            throw new UnsupportedOperationException("enemy_player_variable");
        }

        default void reduceActorSkillCooldowns(int rounds) {
            throw new UnsupportedOperationException("enemy_actor_cooldown");
        }

        default void changeEnemyHealth(int enemyIndex, long amount, boolean allowDeath) {
            throw new UnsupportedOperationException("enemy_hp_command");
        }
    }
}
