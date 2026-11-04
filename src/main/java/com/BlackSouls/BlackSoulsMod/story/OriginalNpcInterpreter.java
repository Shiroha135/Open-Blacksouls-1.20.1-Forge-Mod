package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OriginalNpcInterpreter {
    private static final int SNAPSHOT_VERSION = 1;
    private final Context context;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private Prompt pending;
    private int pendingIndent;
    private int pendingCancel;
    private int battleIndent;
    private long revision;

    public OriginalNpcInterpreter(List<BSOriginalNpcData.Command> commands, Context context) {
        this.context = Objects.requireNonNull(context);
        frames.push(new Frame(commands));
    }

    public Prompt advance(int selection) {
        return advance(selection, false);
    }

    private Prompt advance(int selection, boolean deferPresentation) {
        if (pending != null && (pending.type() == Type.BATTLE || pending.type() == Type.WAIT
                || pending.type() == Type.ROUTE_WAIT || pending.type() == Type.ANIMATION_WAIT)) {
            return pending;
        }
        if (pending != null && pending.type() == Type.CHOICE) {
            if (selection < 0 && selection != -2) {
                return pending;
            }
            int selected = selection;
            if (selected == -2) {
                if (pendingCancel == 0) {
                    return pending;
                }
                selected = pendingCancel == 5 ? -1 : pendingCancel - 1;
            }
            if (selected < -1 || selected >= pending.choices().size()) {
                return pending;
            }
            frames.peek().choices.put(pendingIndent, selected);
        } else if (pending != null && pending.type() == Type.SHOP) {
            if (selection >= 0 && selection < pending.goods().size()) {
                context.buy(pending.goods().get(selection));
                return emit(Type.SHOP, pending.text(), pending.portrait(), pending.portraitIndex(),
                        pending.choices(), pending.goods(), pending.reason());
            }
            if (selection != -1) {
                return pending;
            }
        } else if (pending != null && pending.type() == Type.BLOCKED) {
            return pending;
        }
        pending = null;
        for (int steps = 0; steps < 4096 && !frames.isEmpty(); steps++) {
            Frame frame = frames.peek();
            if (frame.position >= frame.commands.size()) {
                frames.pop();
                continue;
            }
            int commandPosition = frame.position;
            BSOriginalNpcData.Command command = frame.commands.get(frame.position++);
            if (deferPresentation && presentationCommand(command.code())) {
                frame.position--;
                return waitPrompt(0);
            }
            List<JsonElement> p = command.parameters();
            try {
                switch (command.code()) {
                    case 0, 505 -> {
                    }
                    case 108, 408 -> context.comment(text(p, 0));
                    case 101 -> {
                        List<String> lines = new ArrayList<>();
                        while (frame.position < frame.commands.size()
                                && frame.commands.get(frame.position).code() == 401) {
                            lines.add(frame.commands.get(frame.position++).parameters().get(0).getAsString());
                        }
                        return emit(Type.TEXT, String.join("\n", lines), text(p, 0), integer(p, 1), List.of(), List.of(), "");
                    }
                    case 102 -> {
                        List<String> choices = new ArrayList<>();
                        p.get(0).getAsJsonArray().forEach(value -> choices.add(value.getAsString()));
                        pendingIndent = command.indent();
                        pendingCancel = integer(p, 1);
                        return emit(Type.CHOICE, "", "", 0, choices, List.of(), "");
                    }
                    case 402, 403 -> {
                        int branch = command.code() == 403 ? -1 : integer(p, 0);
                        if (frame.choices.getOrDefault(command.indent(), Integer.MIN_VALUE) != branch) {
                            frame.position = find(frame, command.indent(), 402, 403, 404);
                        }
                    }
                    case 404 -> frame.choices.remove(command.indent());
                    case 111 -> {
                        if (!condition(p, context)) {
                            int next = find(frame, command.indent(), 411, 412);
                            frame.position = next < frame.commands.size() ? next + 1 : next;
                        }
                    }
                    case 411 -> {
                        int next = find(frame, command.indent(), 412);
                        frame.position = next < frame.commands.size() ? next + 1 : next;
                    }
                    case 412 -> {
                    }
                    case 112 -> frame.loops.put(command.indent(), frame.position);
                    case 113 -> {
                        int next = find(frame, command.indent() - 1, 413);
                        frame.position = next < frame.commands.size() ? next + 1 : next;
                        frame.loops.remove(command.indent() - 1);
                    }
                    case 413 -> frame.position = frame.loops.getOrDefault(command.indent(), frame.position);
                    case 115 -> frames.pop();
                    case 117 -> {
                        List<BSOriginalNpcData.Command> common = context.commonEvent(integer(p, 0));
                        if (common == null || frames.size() >= 32) {
                            throw new UnsupportedOperationException("common_event:" + integer(p, 0));
                        }
                        frames.push(new Frame(common));
                    }
                    case 121 -> {
                        range(integer(p, 0), integer(p, 1));
                        for (int id = integer(p, 0); id <= integer(p, 1); id++) {
                            context.setSwitch(id, integer(p, 2) == 0);
                        }
                    }
                    case 122 -> variables(p);
                    case 123 -> context.setSelfSwitch(text(p, 0), integer(p, 1) == 0);
                    case 125 -> context.changeGold(operand(p, 1, 2) * (integer(p, 0) == 0 ? 1L : -1L));
                    case 132 -> context.changeBattleBgm(p.get(0).getAsJsonObject());
                    case 126, 127, 128 -> {
                        long amount = operand(p, 2, 3) * (integer(p, 1) == 0 ? 1L : -1L);
                        if (!context.changeItem(command.code() - 126, integer(p, 0), amount)) {
                            throw new UnsupportedOperationException("item:" + (command.code() - 126) + ":" + integer(p, 0));
                        }
                    }
                    case 205 -> {
                        JsonObject route = p.get(1).getAsJsonObject();
                        if (!route.has("wait") || !route.get("wait").isJsonPrimitive()
                                || !route.getAsJsonPrimitive("wait").isBoolean()) {
                            throw new UnsupportedOperationException("move_route_wait");
                        }
                        String ticket = context.moveRoute(integer(p, 0), route);
                        if (route.get("wait").getAsBoolean()) {
                            if (ticket == null || ticket.isBlank()) {
                                throw new UnsupportedOperationException("move_route_ticket");
                            }
                            return emit(Type.ROUTE_WAIT, "", "", 0, List.of(), List.of(), ticket);
                        }
                    }
                    case 214 -> context.eraseEvent();
                    case 212 -> {
                        if (p.size() < 3 || !p.get(2).isJsonPrimitive()
                                || !p.get(2).getAsJsonPrimitive().isBoolean()) {
                            throw new UnsupportedOperationException("animation_wait");
                        }
                        if (!p.get(0).isJsonPrimitive() || !p.get(0).getAsJsonPrimitive().isNumber()
                                || !p.get(1).isJsonPrimitive() || !p.get(1).getAsJsonPrimitive().isNumber()) {
                            throw new UnsupportedOperationException("animation_target_or_id");
                        }
                        boolean wait = p.get(2).getAsBoolean();
                        int id = p.get(1).getAsBigDecimal().intValueExact();
                        String ticket = context.animation(p.get(0).getAsBigDecimal().intValueExact(), id, wait);
                        if (wait && id > 0) {
                            if (ticket == null || ticket.isBlank()) {
                                throw new UnsupportedOperationException("animation_ticket");
                            }
                            return emit(Type.ANIMATION_WAIT, "", "", 0, List.of(), List.of(), ticket);
                        }
                    }
                    case 250 -> context.sound(p.get(0).getAsJsonObject());
                    case 314 -> context.recoverAll(integer(p, 0), integer(p, 1));
                    case 355 -> {
                        StringBuilder script = new StringBuilder(text(p, 0));
                        while (frame.position < frame.commands.size() && frame.commands.get(frame.position).code() == 655) {
                            script.append('\n').append(text(frame.commands.get(frame.position++).parameters(), 0));
                        }
                        context.unlockAchievement(OriginalNpcAchievements.parse(script.toString()));
                    }
                    case 331, 332, 333, 334, 335, 336, 337, 339 -> {
                        int ticks = context.battleCommand(command.code(), p);
                        if (ticks > 0) {
                            return waitPrompt(ticks);
                        }
                    }
                    case 302 -> {
                        List<Goods> goods = new ArrayList<>();
                        goods.add(goods(p));
                        while (frame.position < frame.commands.size()
                                && frame.commands.get(frame.position).code() == 605) {
                            goods.add(goods(frame.commands.get(frame.position++).parameters()));
                        }
                        if (goods.stream().anyMatch(value -> !context.canTrade(value))) {
                            throw new UnsupportedOperationException("unmapped_shop_goods");
                        }
                        return emit(Type.SHOP, "", "", 0, List.of(), goods, "");
                    }
                    case 301 -> {
                        int mode = integer(p, 0);
                        long troop = mode == 0 ? p.get(1).getAsLong()
                                : mode == 1 ? context.variable(integer(p, 1)) : -1L;
                        if (troop <= 0L || troop > Integer.MAX_VALUE) {
                            throw new UnsupportedOperationException("battle_operand:" + mode);
                        }
                        battleIndent = command.indent();
                        pending = new Prompt(++revision, Type.BATTLE, "", "", 0, List.of(), List.of(), "",
                                new BattleRequest((int) troop, p.get(2).getAsBoolean(), p.get(3).getAsBoolean()), 0);
                        return pending;
                    }
                    case 601, 602, 603 -> {
                        if (frame.battles.getOrDefault(command.indent(), -1) != command.code() - 601) {
                            frame.position = find(frame, command.indent(), 601, 602, 603, 604);
                        }
                    }
                    case 604 -> frame.battles.remove(command.indent());
                    case 230 -> {
                        int ticks = framesToTicks(integer(p, 0));
                        if (ticks > 0) {
                            return waitPrompt(ticks);
                        }
                    }
                    case 213, 221, 222, 223, 224, 225, 231, 232, 235, 241, 242, 243, 244, 245, 246, 249, 251 -> {
                        int ticks = context.presentation(command.code(), p);
                        if (ticks > 0) {
                            return waitPrompt(ticks);
                        }
                    }
                    default -> throw new UnsupportedOperationException("command:" + command.code());
                }
            } catch (UnsupportedOperationException | ArithmeticException exception) {
                frame.position = commandPosition;
                return emit(Type.BLOCKED, "", "", 0, List.of(), List.of(), exception.getMessage());
            }
        }
        return emit(frames.isEmpty() ? Type.END : Type.BLOCKED, "", "", 0,
                List.of(), List.of(), frames.isEmpty() ? "" : "execution_budget");
    }

    public Prompt resumeBattle(BattleOutcome outcome) {
        return resumeBattle(outcome, false);
    }

    public Prompt resumeBattle(BattleOutcome outcome, boolean deferPresentation) {
        if (pending == null || pending.type() != Type.BATTLE || frames.isEmpty()) {
            throw new IllegalStateException("No pending original NPC battle");
        }
        BattleRequest request = pending.battle();
        if (outcome == BattleOutcome.DEFEAT && !request.canLose()) {
            frames.clear();
            return emit(Type.END, "", "", 0, List.of(), List.of(), "game_over");
        }
        if (outcome == BattleOutcome.ESCAPED && !request.canEscape()) {
            return emit(Type.BLOCKED, "", "", 0, List.of(), List.of(), "unexpected_battle_escape");
        }
        frames.peek().battles.put(battleIndent, outcome.ordinal());
        pending = null;
        return advance(-1, deferPresentation);
    }

    public Prompt retryBlocked() {
        if (pending == null || pending.type() != Type.BLOCKED) {
            throw new IllegalStateException("No blocked original NPC event");
        }
        pending = null;
        return advance(-1);
    }

    public Prompt pendingPrompt() {
        return pending;
    }

    public JsonObject snapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("version", SNAPSHOT_VERSION);
        result.addProperty("revision", revision);
        result.addProperty("pendingIndent", pendingIndent);
        result.addProperty("pendingCancel", pendingCancel);
        result.addProperty("battleIndent", battleIndent);
        JsonArray stack = new JsonArray();
        for (Frame frame : frames) {
            JsonObject saved = new JsonObject();
            JsonArray commands = new JsonArray();
            for (BSOriginalNpcData.Command command : frame.commands) {
                JsonObject value = new JsonObject();
                value.addProperty("code", command.code());
                value.addProperty("indent", command.indent());
                JsonArray parameters = new JsonArray();
                command.parameters().forEach(parameters::add);
                value.add("parameters", parameters);
                commands.add(value);
            }
            saved.add("commands", commands);
            saved.addProperty("position", frame.position);
            saved.add("choices", saveMap(frame.choices));
            saved.add("loops", saveMap(frame.loops));
            saved.add("battles", saveMap(frame.battles));
            stack.add(saved);
        }
        result.add("frames", stack);
        result.add("pending", pending == null ? JsonNull.INSTANCE : savePrompt(pending));
        return result;
    }

    public static OriginalNpcInterpreter restore(JsonObject snapshot, Context context) {
        Objects.requireNonNull(snapshot);
        if (savedInt(snapshot, "version") != SNAPSHOT_VERSION) {
            throw new IllegalArgumentException("Unsupported original NPC snapshot version");
        }
        OriginalNpcInterpreter result = new OriginalNpcInterpreter(List.of(), context);
        result.frames.clear();
        result.revision = savedLong(snapshot, "revision");
        result.pendingIndent = savedInt(snapshot, "pendingIndent");
        result.pendingCancel = savedInt(snapshot, "pendingCancel");
        result.battleIndent = savedInt(snapshot, "battleIndent");
        if (result.revision < 0 || result.pendingIndent < 0 || result.battleIndent < 0
                || result.pendingCancel < 0 || result.pendingCancel > 5) {
            throw new IllegalArgumentException("Invalid original NPC snapshot state");
        }
        JsonArray stack = snapshot.getAsJsonArray("frames");
        if (stack.size() > 32) {
            throw new IllegalArgumentException("Invalid original NPC snapshot stack");
        }
        for (JsonElement element : stack) {
            JsonObject saved = element.getAsJsonObject();
            List<BSOriginalNpcData.Command> commands = new ArrayList<>();
            for (JsonElement commandElement : saved.getAsJsonArray("commands")) {
                JsonObject command = commandElement.getAsJsonObject();
                List<JsonElement> parameters = new ArrayList<>();
                command.getAsJsonArray("parameters").forEach(parameters::add);
                int indent = savedInt(command, "indent");
                if (indent < 0) {
                    throw new IllegalArgumentException("Invalid original NPC command indent");
                }
                commands.add(new BSOriginalNpcData.Command(savedInt(command, "code"), indent, parameters));
            }
            Frame frame = new Frame(commands);
            frame.position = savedInt(saved, "position");
            if (frame.position < 0 || frame.position > commands.size()) {
                throw new IllegalArgumentException("Invalid original NPC snapshot position");
            }
            loadMap(saved.getAsJsonObject("choices"), frame.choices, -1, Integer.MAX_VALUE);
            loadMap(saved.getAsJsonObject("loops"), frame.loops, 0, commands.size());
            loadMap(saved.getAsJsonObject("battles"), frame.battles, 0, 2);
            result.frames.addLast(frame);
        }
        JsonElement pending = snapshot.get("pending");
        if (pending == null) {
            throw new IllegalArgumentException("Missing original NPC snapshot prompt");
        }
        result.pending = pending.isJsonNull() ? null : loadPrompt(pending.getAsJsonObject());
        if (result.pending != null && (result.pending.revision() != result.revision
                || result.pending.revision() <= 0 || result.frames.isEmpty()
                && result.pending.type() != Type.END && result.pending.type() != Type.BLOCKED)) {
            throw new IllegalArgumentException("Inconsistent original NPC snapshot prompt");
        }
        return result;
    }

    private static boolean presentationCommand(int code) {
        return switch (code) {
            case 205, 212, 213, 221, 222, 223, 224, 225, 231, 232, 235, 241, 242, 243, 244, 245, 246, 249, 250, 251 -> true;
            default -> false;
        };
    }

    private static JsonObject saveMap(Map<Integer, Integer> values) {
        JsonObject result = new JsonObject();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.addProperty(entry.getKey().toString(), entry.getValue()));
        return result;
    }

    private static void loadMap(JsonObject saved, Map<Integer, Integer> target, int minimum, int maximum) {
        for (Map.Entry<String, JsonElement> entry : saved.entrySet()) {
            int key = Integer.parseInt(entry.getKey());
            int value = entry.getValue().getAsBigDecimal().intValueExact();
            if (key < 0 || value < minimum || value > maximum) {
                throw new IllegalArgumentException("Invalid original NPC snapshot branch state");
            }
            target.put(key, value);
        }
    }

    private static JsonObject savePrompt(Prompt prompt) {
        JsonObject result = new JsonObject();
        result.addProperty("revision", prompt.revision());
        result.addProperty("type", prompt.type().name());
        result.addProperty("text", prompt.text());
        result.addProperty("portrait", prompt.portrait());
        result.addProperty("portraitIndex", prompt.portraitIndex());
        result.addProperty("reason", prompt.reason());
        result.addProperty("waitTicks", prompt.waitTicks());
        JsonArray choices = new JsonArray();
        prompt.choices().forEach(choices::add);
        result.add("choices", choices);
        JsonArray goods = new JsonArray();
        for (Goods offer : prompt.goods()) {
            JsonObject value = new JsonObject();
            value.addProperty("kind", offer.kind());
            value.addProperty("id", offer.id());
            value.addProperty("price", offer.price());
            goods.add(value);
        }
        result.add("goods", goods);
        if (prompt.battle() == null) {
            result.add("battle", JsonNull.INSTANCE);
        } else {
            JsonObject battle = new JsonObject();
            battle.addProperty("troopId", prompt.battle().troopId());
            battle.addProperty("canEscape", prompt.battle().canEscape());
            battle.addProperty("canLose", prompt.battle().canLose());
            result.add("battle", battle);
        }
        return result;
    }

    private static Prompt loadPrompt(JsonObject saved) {
        Type type = Type.valueOf(saved.get("type").getAsString());
        List<String> choices = new ArrayList<>();
        saved.getAsJsonArray("choices").forEach(value -> choices.add(value.getAsString()));
        List<Goods> goods = new ArrayList<>();
        for (JsonElement element : saved.getAsJsonArray("goods")) {
            JsonObject value = element.getAsJsonObject();
            goods.add(new Goods(savedInt(value, "kind"), savedInt(value, "id"), savedLong(value, "price")));
        }
        JsonElement battleElement = saved.get("battle");
        BattleRequest battle = null;
        if (battleElement != null && !battleElement.isJsonNull()) {
            JsonObject value = battleElement.getAsJsonObject();
            battle = new BattleRequest(savedInt(value, "troopId"), value.get("canEscape").getAsBoolean(),
                    value.get("canLose").getAsBoolean());
        }
        int wait = savedInt(saved, "waitTicks");
        if (wait < 0 || wait > 120000 || (type == Type.BATTLE) != (battle != null)
                || battle != null && battle.troopId() <= 0 || type != Type.WAIT && wait != 0) {
            throw new IllegalArgumentException("Invalid original NPC saved prompt");
        }
        return new Prompt(savedLong(saved, "revision"), type, saved.get("text").getAsString(),
                saved.get("portrait").getAsString(), savedInt(saved, "portraitIndex"), choices, goods,
                saved.get("reason").getAsString(), battle, wait);
    }

    private static int savedInt(JsonObject object, String key) {
        return object.get(key).getAsBigDecimal().intValueExact();
    }

    private static long savedLong(JsonObject object, String key) {
        return object.get(key).getAsBigDecimal().longValueExact();
    }

    public Prompt resumeWait() {
        if (pending == null || pending.type() != Type.WAIT && pending.type() != Type.ROUTE_WAIT
                && pending.type() != Type.ANIMATION_WAIT) {
            throw new IllegalStateException("No pending original NPC wait");
        }
        pending = null;
        return advance(-1);
    }

    private Prompt waitPrompt(int ticks) {
        pending = new Prompt(++revision, Type.WAIT, "", "", 0, List.of(), List.of(), "", null, ticks);
        return pending;
    }

    public static int framesToTicks(int frames) {
        if (frames < 0 || frames > 360000) {
            throw new UnsupportedOperationException("wait_frames:" + frames);
        }
        return (frames + 2) / 3;
    }

    public static boolean pageAvailable(BSOriginalNpcData.Page page, Context context) {
        JsonObject c = page.condition();
        return (!flag(c, "switch1_valid") || context.switchValue(c.get("switch1_id").getAsInt()))
                && (!flag(c, "switch2_valid") || context.switchValue(c.get("switch2_id").getAsInt()))
                && (!flag(c, "variable_valid") || context.variable(c.get("variable_id").getAsInt())
                >= c.get("variable_value").getAsLong())
                && (!flag(c, "self_switch_valid") || context.selfSwitch(c.get("self_switch_ch").getAsString()))
                && (!flag(c, "item_valid") || context.itemCount(0, c.get("item_id").getAsInt()) > 0)
                && (!flag(c, "actor_valid") || context.hasActor(c.get("actor_id").getAsInt()));
    }

    public static BSOriginalNpcData.Page selectPage(BSOriginalNpcData.Entry entry, Context context) {
        for (int index = entry.pages().size() - 1; index >= 0; index--) {
            try {
                if (pageAvailable(entry.pages().get(index), context)) {
                    return entry.pages().get(index);
                }
            } catch (UnsupportedOperationException exception) {
                return null;
            }
        }
        return null;
    }

    private static boolean condition(List<JsonElement> p, Context context) {
        return switch (integer(p, 0)) {
            case 0 -> context.switchValue(integer(p, 1)) == (integer(p, 2) == 0);
            case 1 -> compare(context.variable(integer(p, 1)), integer(p, 2) == 0
                    ? p.get(3).getAsLong() : context.variable(integer(p, 3)), integer(p, 4));
            case 2 -> context.selfSwitch(text(p, 1)) == (integer(p, 2) == 0);
            case 4 -> context.actorCondition(integer(p, 1), integer(p, 2), p.size() > 3 ? p.get(3) : null);
            case 5 -> context.enemyCondition(integer(p, 1), integer(p, 2), p.size() > 3 ? p.get(3) : null);
            case 7 -> compare(context.gold(), p.get(1).getAsLong(),
                    integer(p, 2) == 0 ? 1 : integer(p, 2) == 1 ? 2 : 4);
            case 8, 9, 10 -> context.itemCount(integer(p, 0) - 8, integer(p, 1)) > 0;
            case 12 -> context.scriptCondition(text(p, 1));
            default -> throw new UnsupportedOperationException("condition:" + integer(p, 0));
        };
    }

    private void variables(List<JsonElement> p) {
        int type = integer(p, 3);
        long value = switch (type) {
            case 0 -> p.get(4).getAsLong();
            case 1 -> context.variable(integer(p, 4));
            case 2 -> context.random(integer(p, 4), integer(p, 5));
            case 3 -> context.gameData(integer(p, 4), integer(p, 5), integer(p, 6));
            default -> throw new UnsupportedOperationException("variable_operand:" + type);
        };
        range(integer(p, 0), integer(p, 1));
        for (int id = integer(p, 0); id <= integer(p, 1); id++) {
            long previous = context.variable(id);
            long result = switch (integer(p, 2)) {
                case 0 -> value;
                case 1 -> Math.addExact(previous, value);
                case 2 -> Math.subtractExact(previous, value);
                case 3 -> Math.multiplyExact(previous, value);
                case 4 -> value == 0 ? 0 : Math.floorDiv(previous, value);
                case 5 -> value == 0 ? 0 : Math.floorMod(previous, value);
                default -> throw new UnsupportedOperationException("variable_operation:" + integer(p, 2));
            };
            context.setVariable(id, result);
        }
    }

    private long operand(List<JsonElement> p, int type, int value) {
        return integer(p, type) == 0 ? p.get(value).getAsLong() : context.variable(integer(p, value));
    }

    private Prompt emit(Type type, String text, String portrait, int portraitIndex,
                        List<String> choices, List<Goods> goods, String reason) {
        pending = new Prompt(++revision, type, text, portrait, portraitIndex, choices, goods, reason);
        return pending;
    }

    private static int find(Frame frame, int indent, int... codes) {
        for (int index = frame.position; index < frame.commands.size(); index++) {
            BSOriginalNpcData.Command command = frame.commands.get(index);
            if (command.indent() == indent) {
                for (int code : codes) {
                    if (command.code() == code) {
                        return index;
                    }
                }
            }
        }
        return frame.commands.size();
    }

    private static boolean compare(long first, long second, int operation) {
        return switch (operation) {
            case 0 -> first == second;
            case 1 -> first >= second;
            case 2 -> first <= second;
            case 3 -> first > second;
            case 4 -> first < second;
            case 5 -> first != second;
            default -> throw new UnsupportedOperationException("comparison:" + operation);
        };
    }

    private static Goods goods(List<JsonElement> parameters) {
        return new Goods(integer(parameters, 0), integer(parameters, 1),
                integer(parameters, 2) == 0 ? -1L : parameters.get(3).getAsLong());
    }

    private static void range(int first, int last) {
        if (first <= 0 || last < first || (long) last - first > 4096L) {
            throw new UnsupportedOperationException("invalid_range");
        }
    }

    private static boolean flag(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }

    private static int integer(List<JsonElement> parameters, int index) {
        return parameters.get(index).getAsInt();
    }

    private static String text(List<JsonElement> parameters, int index) {
        return parameters.get(index).getAsString();
    }

    private static final class Frame {
        private final List<BSOriginalNpcData.Command> commands;
        private final Map<Integer, Integer> choices = new HashMap<>();
        private final Map<Integer, Integer> loops = new HashMap<>();
        private final Map<Integer, Integer> battles = new HashMap<>();
        private int position;

        private Frame(List<BSOriginalNpcData.Command> commands) {
            this.commands = List.copyOf(commands);
        }
    }

    public enum Type {
        TEXT, CHOICE, SHOP, END, BLOCKED, BATTLE, WAIT, ROUTE_WAIT, ANIMATION_WAIT
    }

    public enum BattleOutcome {
        VICTORY, ESCAPED, DEFEAT
    }

    public record BattleRequest(int troopId, boolean canEscape, boolean canLose) {
    }

    public record Goods(int kind, int id, long price) {
    }

    public record Prompt(long revision, Type type, String text, String portrait, int portraitIndex,
                         List<String> choices, List<Goods> goods, String reason, BattleRequest battle, int waitTicks) {
        public Prompt(long revision, Type type, String text, String portrait, int portraitIndex,
                      List<String> choices, List<Goods> goods, String reason) {
            this(revision, type, text, portrait, portraitIndex, choices, goods, reason, null, 0);
        }
        public Prompt {
            choices = List.copyOf(choices);
            goods = List.copyOf(goods);
        }
    }

    public interface Context {
        boolean switchValue(int id);
        void setSwitch(int id, boolean value);
        long variable(int id);
        void setVariable(int id, long value);
        boolean selfSwitch(String key);
        void setSelfSwitch(String key, boolean value);
        long gold();
        void changeGold(long amount);
        int itemCount(int kind, int id);
        boolean changeItem(int kind, int id, long amount);
        boolean hasActor(int actorId);
        boolean actorCondition(int actorId, int condition, JsonElement value);
        boolean scriptCondition(String expression);
        long gameData(int type, int first, int second);
        long random(int first, int last);
        List<BSOriginalNpcData.Command> commonEvent(int id);
        void eraseEvent();
        void animation(int id);
        void sound(JsonObject sound);
        void recoverAll(int type, int id);
        boolean canTrade(Goods goods);
        void buy(Goods goods);

        default boolean enemyCondition(int slot, int condition, JsonElement operand) {
            throw new UnsupportedOperationException("enemy_condition:" + condition);
        }

        default int battleCommand(int command, List<JsonElement> parameters) {
            throw new UnsupportedOperationException("command:" + command);
        }

        default void comment(String text) {
        }

        default int presentation(int command, List<JsonElement> parameters) {
            throw new UnsupportedOperationException("command:" + command);
        }

        default void changeBattleBgm(JsonObject bgm) {
            throw new UnsupportedOperationException("command:132");
        }

        default void unlockAchievement(String name) {
            throw new UnsupportedOperationException("original_achievement:" + name);
        }

        default String moveRoute(int target, JsonObject route) {
            throw new UnsupportedOperationException("command:205");
        }

        default String animation(int target, int id, boolean wait) {
            throw new UnsupportedOperationException("command:212");
        }
    }
}
