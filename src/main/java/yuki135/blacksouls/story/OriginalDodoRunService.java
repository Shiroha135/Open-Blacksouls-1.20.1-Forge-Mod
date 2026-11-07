package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPacket;
import yuki135.blacksouls.util.BSOriginalNpcData;
import yuki135.blacksouls.util.SkillUtils;
import yuki135.blacksouls.util.skill.SkillDodoRun;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalDodoRunService {
    public static final String STATE = "BSOriginalDodoRunEvent";
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private OriginalDodoRunService() {
    }

    public static boolean pending(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) || SkillUtils.getPersistedData(player).contains(STATE);
    }

    public static boolean canOpen(ServerPlayer player) {
        return usable(player) && !OriginalOpeningService.managed(player) && !OriginalNpcService.hasNpcConversation(player)
                && OriginalNpcProgress.get(player.server).conversationIds(player.getUUID()).isEmpty();
    }

    private static boolean usable(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && !TurnBattleManager.isInBattle(player)
                && SkillUtils.hasLearnedSkill(player, SkillDodoRun.ID);
    }

    public static boolean open(ServerPlayer player) {
        if (!canOpen(player)) return false;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            Context context = new Context(player);
            var data = SkillUtils.getPersistedData(player);
            OriginalNpcInterpreter interpreter;
            if (data.contains(STATE)) {
                if (!data.contains(STATE, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid Dodo event checkpoint");
                var saved = data.getCompound(STATE);
                if (!saved.getAllKeys().equals(Set.of("Version", "Owner", "Interpreter"))
                        || !saved.contains("Version", Tag.TAG_INT) || saved.getInt("Version") != 1
                        || !saved.hasUUID("Owner") || !saved.getUUID("Owner").equals(player.getUUID())
                        || !saved.contains("Interpreter", Tag.TAG_STRING) || saved.getString("Interpreter").length() > 65536) {
                    throw new IllegalArgumentException("Invalid Dodo event checkpoint");
                }
                JsonObject snapshot = JsonParser.parseString(saved.getString("Interpreter")).getAsJsonObject();
                validateSnapshot(snapshot);
                interpreter = OriginalNpcInterpreter.restore(snapshot, context);
                var prompt = interpreter.pendingPrompt();
                if (prompt == null || prompt.type() != OriginalNpcInterpreter.Type.CHOICE
                        && prompt.type() != OriginalNpcInterpreter.Type.ROUTE_WAIT
                        || prompt.type() == OriginalNpcInterpreter.Type.ROUTE_WAIT && context.runner == null) {
                    throw new IllegalArgumentException("Invalid Dodo event phase");
                }
            } else {
                interpreter = new OriginalNpcInterpreter(commands(), context);
                interpreter.advance(-1);
            }
            session = new Session(interpreter, context);
            SESSIONS.put(player.getUUID(), session);
        }
        publish(player, session);
        return true;
    }

    public static void respond(ServerPlayer player, int entityId, UUID token, long revision, int selection) {
        Session session = SESSIONS.get(player.getUUID());
        if (!usable(player) || session == null || entityId != player.getId() || !session.token.equals(token)
                || session.interpreter.pendingPrompt() == null || session.interpreter.pendingPrompt().revision() != revision
                || session.interpreter.pendingPrompt().type() != OriginalNpcInterpreter.Type.CHOICE) return;
        if (selection == -3) {
            checkpoint(player, session);
            SESSIONS.remove(player.getUUID());
            return;
        }
        if (selection != 0 && selection != 1 && selection != -2) return;
        session.interpreter.advance(selection);
        publish(player, session);
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (!usable(player) || session == null || session.interpreter.pendingPrompt().type() != OriginalNpcInterpreter.Type.ROUTE_WAIT) return;
        var runner = session.context.runner;
        if (runner == null) throw new IllegalStateException("Missing Dodo speed route");
        for (int frame = 0; frame < 3 && !runner.isComplete(); frame++) runner.advanceFrame(session.context.routeContext);
        if (runner.isComplete()) session.interpreter.resumeWait();
        publish(player, session);
    }

    private static void publish(ServerPlayer player, Session session) {
        var prompt = session.interpreter.pendingPrompt();
        if (prompt.type() == OriginalNpcInterpreter.Type.END) {
            SESSIONS.remove(player.getUUID());
            SkillUtils.getPersistedData(player).remove(STATE);
        } else checkpoint(player, session);
        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(player.getId(), session.token,
                "渡渡快跑", "", prompt, prompt.choices()), player);
    }

    private static void checkpoint(ServerPlayer player, Session session) {
        CompoundTag saved = new CompoundTag();
        saved.putInt("Version", 1);
        saved.putUUID("Owner", player.getUUID());
        saved.putString("Interpreter", session.interpreter.snapshot().toString());
        SkillUtils.getPersistedData(player).put(STATE, saved);
    }

    private static List<BSOriginalNpcData.Command> commands() {
        var commands = BSOriginalNpcData.commonEvent(43);
        if (commands == null) throw new IllegalStateException("Missing original Dodo common event");
        return commands;
    }

    private static void validateSnapshot(JsonObject snapshot) {
        JsonArray canonical = new JsonArray();
        for (var command : commands()) {
            JsonObject row = new JsonObject();
            row.addProperty("code", command.code());
            row.addProperty("indent", command.indent());
            JsonArray values = new JsonArray();
            command.parameters().forEach(values::add);
            row.add("parameters", values);
            canonical.add(row);
        }
        var frames = snapshot.getAsJsonArray("frames");
        if (frames.size() != 1 || !frames.get(0).getAsJsonObject().getAsJsonArray("commands").equals(canonical)) {
            throw new IllegalArgumentException("Changed original Dodo common event");
        }
        var frame = frames.get(0).getAsJsonObject();
        var prompt = snapshot.getAsJsonObject("pending");
        var state = snapshot.getAsJsonObject("contextState");
        int position = frame.get("position").getAsBigDecimal().intValueExact();
        if (frame.getAsJsonObject("loops").size() != 0 || frame.getAsJsonObject("battles").size() != 0) {
            throw new IllegalArgumentException("Invalid Dodo event branch checkpoint");
        }
        if (prompt.get("type").getAsString().equals("CHOICE")) {
            if (position != 3 || frame.getAsJsonObject("choices").size() != 0 || state.size() != 0
                    || !prompt.getAsJsonArray("choices").equals(commands().get(2).parameters().get(0))) {
                throw new IllegalArgumentException("Invalid Dodo choice checkpoint");
            }
        } else if (prompt.get("type").getAsString().equals("ROUTE_WAIT")) {
            if (position < 3 || position > commands().size() || frame.getAsJsonObject("choices").size() != 1
                    || !frame.getAsJsonObject("choices").has("0") || commands().get(position - 1).code() != 205
                    || commands().get(position - 3).code() != 402
                    || !frame.getAsJsonObject("choices").get("0").equals(commands().get(position - 3).parameters().get(0))
                    || state.size() != 1 || !state.has("route")
                    || !state.getAsJsonObject("route").getAsJsonObject("route").equals(commands().get(position - 1).parameters().get(1))
                    || !prompt.get("reason").getAsString().equals("original_dodo_speed")) {
                throw new IllegalArgumentException("Invalid Dodo waiting route checkpoint");
            }
        } else throw new IllegalArgumentException("Invalid Dodo event prompt checkpoint");
    }

    private static void validateRoute(JsonObject route) {
        if (commands().stream().filter(command -> command.code() == 205).noneMatch(command ->
                command.parameters().get(0).getAsInt() == -1 && command.parameters().get(1).equals(route))) {
            throw new UnsupportedOperationException("changed_original_dodo_speed_route");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) tick(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && pending(player)) open(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) suspend(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) suspend(player);
        var previous = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (previous.contains(STATE)) SkillUtils.getPersistedData(event.getEntity()).put(STATE, previous.get(STATE).copy());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && pending(player)) open(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            suspend(player);
            if (pending(player)) open(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SESSIONS.clear();
    }

    private static void suspend(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) checkpoint(player, session);
    }

    private static final class Session {
        private final UUID token = UUID.randomUUID();
        private final OriginalNpcInterpreter interpreter;
        private final Context context;

        private Session(OriginalNpcInterpreter interpreter, Context context) {
            this.interpreter = interpreter;
            this.context = context;
        }
    }

    private static final class Context implements OriginalNpcInterpreter.Context {
        private final ServerPlayer player;
        private OriginalNpcMoveRouteRunner runner;
        private final OriginalNpcMoveRouteRunner.Context routeContext = new OriginalNpcMoveRouteRunner.Context() {
            @Override public void validate(int code, List<JsonElement> parameters) {
                if (code != 0 && (code != 29 || parameters.size() != 1
                        || parameters.get(0).getAsInt() != 4 && parameters.get(0).getAsInt() != 5)) {
                    throw new UnsupportedOperationException("original_dodo_speed_route:" + code);
                }
            }
            @Override public void apply(int code, List<JsonElement> parameters) {
                if (code == 29) OriginalPlayerMoveSpeed.set(player, parameters.get(0).getAsInt());
                else throw new UnsupportedOperationException("original_dodo_speed_route:" + code);
            }
        };

        private Context(ServerPlayer player) { this.player = player; }
        @Override public boolean switchValue(int id) { return OriginalPlayerMoveSpeed.switchValue(player, id); }
        @Override public void setSwitch(int id, boolean value) { OriginalPlayerMoveSpeed.setSwitch(player, id, value); }
        @Override public String moveRoute(int target, JsonObject route) {
            if (target != -1) throw new UnsupportedOperationException("original_dodo_speed_target");
            validateRoute(route);
            var next = new OriginalNpcMoveRouteRunner(route);
            next.validate(routeContext);
            runner = next;
            return "original_dodo_speed";
        }
        @Override public JsonObject savePresentationState() {
            JsonObject state = new JsonObject();
            if (runner != null) state.add("route", runner.snapshot());
            return state;
        }
        @Override public void restorePresentationState(JsonObject state) {
            if (state.size() > 1 || state.size() == 1 && !state.has("route")) throw new IllegalArgumentException("Invalid Dodo route checkpoint");
            if (state.has("route")) {
                var snapshot = state.getAsJsonObject("route");
                validateRoute(snapshot.getAsJsonObject("route"));
                var next = OriginalNpcMoveRouteRunner.restore(snapshot);
                next.validate(routeContext);
                runner = next;
            }
        }
        @Override public long variable(int id) { throw unsupported(); }
        @Override public void setVariable(int id, long value) { throw unsupported(); }
        @Override public boolean selfSwitch(String key) { throw unsupported(); }
        @Override public void setSelfSwitch(String key, boolean value) { throw unsupported(); }
        @Override public long gold() { throw unsupported(); }
        @Override public void changeGold(long amount) { throw unsupported(); }
        @Override public int itemCount(int kind, int id) { throw unsupported(); }
        @Override public boolean changeItem(int kind, int id, long amount) { throw unsupported(); }
        @Override public boolean hasActor(int actorId) { throw unsupported(); }
        @Override public boolean actorCondition(int actorId, int condition, JsonElement value) { throw unsupported(); }
        @Override public boolean scriptCondition(String expression) { throw unsupported(); }
        @Override public long gameData(int type, int first, int second) { throw unsupported(); }
        @Override public long random(int first, int last) { throw unsupported(); }
        @Override public List<BSOriginalNpcData.Command> commonEvent(int id) { throw unsupported(); }
        @Override public void eraseEvent() { throw unsupported(); }
        @Override public void animation(int id) { throw unsupported(); }
        @Override public void sound(JsonObject sound) { throw unsupported(); }
        @Override public void recoverAll(int type, int id) { throw unsupported(); }
        @Override public boolean canTrade(OriginalNpcInterpreter.Goods goods) { throw unsupported(); }
        @Override public void buy(OriginalNpcInterpreter.Goods goods) { throw unsupported(); }
        private UnsupportedOperationException unsupported() { return new UnsupportedOperationException("unsupported_original_dodo_skill_command"); }
    }
}
