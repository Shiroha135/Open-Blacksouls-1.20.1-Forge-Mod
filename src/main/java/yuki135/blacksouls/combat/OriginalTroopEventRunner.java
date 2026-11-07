package yuki135.blacksouls.combat;

import yuki135.blacksouls.story.OriginalNpcInterpreter;
import yuki135.blacksouls.util.BSOriginalNpcData;
import yuki135.blacksouls.util.BSOriginalTroopData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class OriginalTroopEventRunner {
    private static final int PAGE_BUDGET = 64;
    private final BSOriginalTroopData.Entry troop;
    private final Context context;
    private final boolean[] executed;
    private OriginalNpcInterpreter interpreter;
    private List<BSOriginalNpcData.Command> reservedCommonCommands;
    private Step pending;
    private int pageIndex = -1;
    private int turnCount;
    private boolean turnEnding;
    private boolean budgetWait;
    private long revision;

    public OriginalTroopEventRunner(BSOriginalTroopData.Entry troop, Context context) {
        this.troop = Objects.requireNonNull(troop);
        this.context = Objects.requireNonNull(context);
        executed = new boolean[troop.pages().size()];
    }

    public boolean reserveCommonEvent(int commonId) {
        List<BSOriginalNpcData.Command> commands = context.commonEvent(commonId);
        if (commands == null) return false;
        try {
            validateCommands(commands, new HashSet<>(), 0, "common:" + commonId + ":");
        } catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
        reservedCommonCommands = List.copyOf(commands);
        return true;
    }

    public boolean hasReservedCommonEvent() {
        return reservedCommonCommands != null;
    }

    public Step pump() {
        if (pending != null) {
            return pending;
        }
        for (int pages = 0; pages < PAGE_BUDGET; pages++) {
            if (interpreter == null) {
                try {
                    if (reservedCommonCommands != null) {
                        List<BSOriginalNpcData.Command> commands = reservedCommonCommands;
                        reservedCommonCommands = null;
                        pageIndex = -1;
                        interpreter = new OriginalNpcInterpreter(commands, context);
                    } else {
                        pageIndex = selectPage();
                        if (pageIndex < 0) {
                            return idle();
                        }
                        BSOriginalTroopData.Page page = troop.pages().get(pageIndex);
                        if (page.span() < 0 || page.span() > 2) {
                            throw new UnsupportedOperationException("span:" + page.span());
                        }
                        if (page.span() <= 1) {
                            executed[pageIndex] = true;
                        }
                        List<BSOriginalNpcData.Command> commands = page.commands().stream()
                                .map(command -> new BSOriginalNpcData.Command(command.code(), command.indent(), command.parameters()))
                                .toList();
                        validateCommands(commands, new HashSet<>(), 0, "");
                        interpreter = new OriginalNpcInterpreter(commands, context);
                    }
                } catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException exception) {
                    return blocked(exception.getMessage());
                }
            }
            Step step = accept(interpreter.advance(-1));
            if (step != null) {
                return step;
            }
        }
        budgetWait = true;
        return present(Kind.WAIT, new OriginalNpcInterpreter.Prompt(0, OriginalNpcInterpreter.Type.WAIT,
                "", "", 0, List.of(), List.of(), "execution_budget", null, 1));
    }

    public Step acknowledge(long expectedRevision, int selection) {
        if (pending == null) {
            return idle();
        }
        if (pending.prompt().revision() != expectedRevision
                || pending.kind() != Kind.TEXT && pending.kind() != Kind.CHOICE) {
            return pending;
        }
        OriginalNpcInterpreter.Prompt previous = interpreter.pendingPrompt();
        OriginalNpcInterpreter.Prompt next = interpreter.advance(selection);
        if (next == previous) {
            return pending;
        }
        pending = null;
        Step step = accept(next);
        return step == null ? pump() : step;
    }

    public Step resumeWait() {
        if (pending == null || pending.kind() != Kind.WAIT) {
            return pending == null ? idle() : pending;
        }
        pending = null;
        if (budgetWait) {
            budgetWait = false;
            return pump();
        }
        Step step = accept(interpreter.resumeWait());
        return step == null ? pump() : step;
    }

    public void increaseTurn() {
        turnCount = Math.addExact(turnCount, 1);
        for (int index = 0; index < executed.length; index++) {
            if (troop.pages().get(index).span() == 1) {
                executed[index] = false;
            }
        }
    }

    public void setTurnEnding(boolean value) {
        turnEnding = value;
    }

    public int turnCount() {
        return turnCount;
    }

    public boolean hasPending() {
        return pending != null || interpreter != null;
    }

    private int selectPage() {
        for (int index = 0; index < troop.pages().size(); index++) {
            pageIndex = index;
            if (!executed[index] && available(troop.pages().get(index).condition())) {
                return index;
            }
        }
        return -1;
    }

    private boolean available(JsonObject condition) {
        boolean ending = flag(condition, "turn_ending");
        boolean turn = flag(condition, "turn_valid");
        boolean enemy = flag(condition, "enemy_valid");
        boolean actor = flag(condition, "actor_valid");
        boolean switched = flag(condition, "switch_valid");
        if (!ending && !turn && !enemy && !actor && !switched) {
            return false;
        }
        if (ending && !turnEnding) {
            return false;
        }
        if (turn) {
            int first = number(condition, "turn_a");
            int interval = number(condition, "turn_b");
            require(first >= 0 && interval >= 0, "turn_condition");
            if (interval == 0 ? turnCount != first
                    : turnCount < 1 || turnCount < first || turnCount % interval != first % interval) {
                return false;
            }
        }
        if (enemy) {
            int slot = number(condition, "enemy_index");
            require(slot >= 0 && slot < troop.members().size(), "enemy_index:" + slot);
            int threshold = number(condition, "enemy_hp");
            require(threshold >= 0 && threshold <= 100, "enemy_hp:" + threshold);
            double percent = context.enemyHpPercent(slot);
            require(Double.isFinite(percent), "enemy_hp_unavailable:" + slot);
            if (percent > threshold) {
                return false;
            }
        }
        if (actor) {
            int id = number(condition, "actor_id");
            int threshold = number(condition, "actor_hp");
            require(id > 0 && threshold >= 0 && threshold <= 100, "actor_condition");
            Double percent = context.actorHpPercent(id);
            if (percent == null) {
                return false;
            }
            require(Double.isFinite(percent), "actor_hp_unavailable:" + id);
            if (percent > threshold) {
                return false;
            }
        }
        if (switched) {
            int id = number(condition, "switch_id");
            require(id > 0, "switch_id:" + id);
            return context.switchValue(id);
        }
        return true;
    }

    private Step accept(OriginalNpcInterpreter.Prompt prompt) {
        return switch (prompt.type()) {
            case END -> {
                interpreter = null;
                yield null;
            }
            case TEXT -> present(Kind.TEXT, prompt);
            case CHOICE -> present(Kind.CHOICE, prompt);
            case WAIT -> present(Kind.WAIT, prompt);
            case BLOCKED -> blocked(prompt.reason());
            default -> blocked("prompt:" + prompt.type());
        };
    }

    private Step present(Kind kind, OriginalNpcInterpreter.Prompt prompt) {
        OriginalNpcInterpreter.Prompt identified = new OriginalNpcInterpreter.Prompt(++revision, prompt.type(),
                prompt.text(), prompt.portrait(), prompt.portraitIndex(), prompt.choices(), prompt.goods(),
                prompt.reason(), prompt.battle(), prompt.waitTicks());
        pending = new Step(kind, identified, pageIndex, prompt.reason());
        return pending;
    }

    private Step blocked(String reason) {
        String location = "troop:" + troop.id() + ":page:" + pageIndex;
        if (interpreter != null) {
            JsonObject snapshot = interpreter.snapshot();
            if (!snapshot.getAsJsonArray("frames").isEmpty()) {
                JsonObject frame = snapshot.getAsJsonArray("frames").get(0).getAsJsonObject();
                int position = frame.get("position").getAsInt();
                location += ":command:" + position;
                if (position < frame.getAsJsonArray("commands").size()) {
                    location += ":code:" + frame.getAsJsonArray("commands").get(position)
                            .getAsJsonObject().get("code").getAsInt();
                }
            }
        }
        return present(Kind.BLOCKED, new OriginalNpcInterpreter.Prompt(0, OriginalNpcInterpreter.Type.BLOCKED,
                "", "", 0, List.of(), List.of(), location + ":" + reason));
    }

    private static Step idle() {
        return new Step(Kind.IDLE, null, -1, "");
    }

    private void validateCommands(List<BSOriginalNpcData.Command> commands, Set<Integer> commonStack,
                                  int depth, String path) {
        require(depth < 32 && commands.size() <= 4096, path + "command_budget");
        for (int index = 0; index < commands.size(); index++) {
            BSOriginalNpcData.Command command = commands.get(index);
            String location = path + "command:" + index + ":code:" + command.code() + ":";
            try {
                require(command.indent() >= 0, "indent");
                List<JsonElement> p = command.parameters();
                switch (command.code()) {
                    case 0, 403, 404, 411, 412, 112, 113, 413, 115 -> size(p, 0);
                    case 108, 408, 401 -> {
                        size(p, 1);
                        string(p.get(0));
                    }
                    case 101 -> {
                        size(p, 4);
                        string(p.get(0));
                        bounded(p, 1, 0, 7);
                        bounded(p, 2, 0, 2);
                        bounded(p, 3, 0, 2);
                    }
                    case 102 -> {
                        size(p, 2);
                        require(p.get(0).isJsonArray() && !p.get(0).getAsJsonArray().isEmpty(), "choices");
                        p.get(0).getAsJsonArray().forEach(OriginalTroopEventRunner::string);
                        bounded(p, 1, 0, 5);
                    }
                    case 402 -> {
                        size(p, 2);
                        bounded(p, 0, 0, Integer.MAX_VALUE);
                        string(p.get(1));
                    }
                    case 111 -> validateCondition(p);
                    case 117 -> {
                        size(p, 1);
                        int id = bounded(p, 0, 1, Integer.MAX_VALUE);
                        List<BSOriginalNpcData.Command> common = context.commonEvent(id);
                        require(common != null, "common_event:" + id);
                        require(commonStack.add(id), "common_cycle:" + id);
                        validateCommands(common, commonStack, depth + 1, "common:" + id + ":");
                        commonStack.remove(id);
                    }
                    case 121 -> {
                        size(p, 3);
                        variableRange(p);
                        bounded(p, 2, 0, 1);
                    }
                    case 122 -> validateVariables(p);
                    case 230 -> {
                        size(p, 1);
                        OriginalNpcInterpreter.framesToTicks(integer(p, 0));
                    }
                    case 250 -> {
                        size(p, 1);
                        JsonObject sound = p.get(0).getAsJsonObject();
                        string(sound.get("name"));
                        require(number(sound, "volume") >= 0 && number(sound, "volume") <= 100, "sound_volume");
                        require(number(sound, "pitch") >= 50 && number(sound, "pitch") <= 150, "sound_pitch");
                    }
                    case 331, 332 -> {
                        size(p, command.code() == 331 ? 5 : 4);
                        enemySlot(p);
                        bounded(p, 1, 0, 1);
                        int operand = bounded(p, 2, 0, 1);
                        long value = p.get(3).getAsBigDecimal().longValueExact();
                        require(operand == 0 || value > 0 && value <= Integer.MAX_VALUE, "variable_id");
                        if (command.code() == 331) {
                            require(p.get(4).isJsonPrimitive() && p.get(4).getAsJsonPrimitive().isBoolean(), "allow_death");
                        }
                    }
                    case 333 -> {
                        size(p, 3);
                        enemySlot(p);
                        bounded(p, 1, 0, 1);
                        bounded(p, 2, 1, Integer.MAX_VALUE);
                    }
                    case 334, 335 -> {
                        size(p, 1);
                        enemySlot(p);
                    }
                    case 336, 337 -> {
                        size(p, 2);
                        enemySlot(p);
                        bounded(p, 1, command.code() == 336 ? 1 : 0, Integer.MAX_VALUE);
                    }
                    default -> throw new UnsupportedOperationException("unsupported_command");
                }
            } catch (RuntimeException exception) {
                throw new UnsupportedOperationException(location + exception.getMessage(), exception);
            }
        }
    }

    private void validateCondition(List<JsonElement> p) {
        require(!p.isEmpty(), "condition");
        switch (integer(p, 0)) {
            case 0 -> {
                size(p, 3);
                bounded(p, 1, 1, Integer.MAX_VALUE);
                bounded(p, 2, 0, 1);
            }
            case 1 -> {
                size(p, 5);
                bounded(p, 1, 1, Integer.MAX_VALUE);
                int operand = bounded(p, 2, 0, 1);
                long value = p.get(3).getAsBigDecimal().longValueExact();
                require(operand == 0 || value > 0 && value <= Integer.MAX_VALUE, "variable_id");
                bounded(p, 4, 0, 5);
            }
            case 4 -> {
                require(p.size() == 3 || p.size() == 4, "actor_condition");
                bounded(p, 1, 1, Integer.MAX_VALUE);
                int condition = bounded(p, 2, 0, 6);
                require(condition == 0 || p.size() == 4, "actor_operand");
            }
            case 5 -> {
                require(p.size() == 3 || p.size() == 4, "enemy_condition");
                bounded(p, 1, 0, troop.members().size() - 1);
                int condition = bounded(p, 2, 0, 1);
                require(condition == 0 || p.size() == 4, "enemy_operand");
                if (condition == 1) {
                    bounded(p, 3, 1, Integer.MAX_VALUE);
                }
            }
            default -> throw new UnsupportedOperationException("condition:" + integer(p, 0));
        }
    }

    private static void validateVariables(List<JsonElement> p) {
        require(p.size() >= 5, "variable_parameters");
        variableRange(p);
        bounded(p, 2, 0, 5);
        switch (bounded(p, 3, 0, 3)) {
            case 0 -> {
                size(p, 5);
                p.get(4).getAsBigDecimal().longValueExact();
            }
            case 1 -> {
                size(p, 5);
                bounded(p, 4, 1, Integer.MAX_VALUE);
            }
            case 2 -> {
                size(p, 6);
                require(integer(p, 4) <= integer(p, 5), "random_range");
            }
            case 3 -> {
                size(p, 7);
                bounded(p, 4, 0, 7);
                bounded(p, 5, 0, Integer.MAX_VALUE);
                bounded(p, 6, 0, Integer.MAX_VALUE);
            }
        }
    }

    private static void variableRange(List<JsonElement> parameters) {
        int first = bounded(parameters, 0, 1, Integer.MAX_VALUE);
        int last = bounded(parameters, 1, first, Integer.MAX_VALUE);
        require((long) last - first <= 4096L, "invalid_range");
    }

    private void enemySlot(List<JsonElement> parameters) {
        bounded(parameters, 0, -1, troop.members().size() - 1);
    }

    private static void size(List<JsonElement> parameters, int length) {
        require(parameters.size() == length, "parameter_count:" + parameters.size());
    }

    private static int bounded(List<JsonElement> parameters, int index, int minimum, int maximum) {
        int value = integer(parameters, index);
        require(value >= minimum && value <= maximum, "parameter:" + index + ":" + value);
        return value;
    }

    private static int integer(List<JsonElement> parameters, int index) {
        return parameters.get(index).getAsBigDecimal().intValueExact();
    }

    private static int number(JsonObject object, String key) {
        require(object.has(key), "missing:" + key);
        try {
            require(object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(),
                    "number:" + key);
            return object.get(key).getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            throw new UnsupportedOperationException("integer:" + key, exception);
        }
    }

    private static boolean flag(JsonObject object, String key) {
        if (!object.has(key)) {
            return false;
        }
        require(object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(),
                "flag:" + key);
        return object.get(key).getAsBoolean();
    }

    private static void string(JsonElement element) {
        require(element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(), "string");
    }

    private static void require(boolean condition, String reason) {
        if (!condition) {
            throw new UnsupportedOperationException(reason);
        }
    }

    public enum Kind {
        IDLE, TEXT, CHOICE, WAIT, BLOCKED
    }

    public record Step(Kind kind, OriginalNpcInterpreter.Prompt prompt, int pageIndex, String reason) {
    }

    public interface Context extends OriginalNpcInterpreter.Context {
        double enemyHpPercent(int originalSlot);
        Double actorHpPercent(int originalActorId);
    }
}
