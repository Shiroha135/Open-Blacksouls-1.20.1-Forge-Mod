package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalReceptionist;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalReceptionistPlacement;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalWhiteBearPlacement;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalWhiteBear;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalCemeteryCat;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalMarketCat;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalMarketCatPlacement;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalCemeteryCatPlacement;
import com.BlackSouls.BlackSoulsMod.entity.EntityCheshireCat;
import com.BlackSouls.BlackSoulsMod.entity.EntityRabbitKnight;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalDatabaseEnemy;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundOriginalNpcTradePacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundTurnBattlePacket;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalTroopData;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcLogicData;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcNameWindow;
import com.BlackSouls.BlackSoulsMod.util.OriginalActorBridge;
import com.BlackSouls.BlackSoulsMod.util.PlayerStatService;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalNpcService {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Pattern VARIABLE = Pattern.compile("\\\\[Vv]\\[(\\d+)]");
    private static final Pattern ACTOR_NAME = Pattern.compile("\\\\[NnPp]\\[1]");

    public static final String CHESHIRE_LOGIC = "map051_ev095";
    private static final java.util.Set<String> CHESHIRE_EVENTS = java.util.Set.of(CHESHIRE_LOGIC, "map051_ev096", "map051_ev097");

    private OriginalNpcService() {
    }

    private static BSOriginalNpcData.Entry definition(String npcId) {
        BSOriginalNpcData.Entry entry = BSOriginalNpcData.getEvent(npcId);
        return entry == null ? OriginalNpcLogicData.get(npcId) : entry;
    }

    private static boolean matchesActor(PathfinderMob npc, String npcId) {
        if (npc instanceof EntityOriginalWhiteBear whiteBear && !OriginalWhiteBearPlacement.matches(whiteBear)) return false;
        if (npc instanceof EntityOriginalCemeteryCat cat && !OriginalCemeteryCatPlacement.matches(cat)) return false;
        if (npc instanceof EntityOriginalMarketCat cat && !OriginalMarketCatPlacement.matches(cat)) return false;
        if (npc instanceof EntityOriginalReceptionist receptionist
                && !OriginalReceptionistPlacement.matches(receptionist)) return false;
        if (npc instanceof EntityOriginalNpc original) return original.getNpcId().equals(npcId);
        if (npc instanceof EntityRabbitKnight rabbit) return rabbit.isOriginalSourceManaged() && OriginalRabbitKnightBinding.SOURCE_ID.equals(npcId) && OriginalRabbitKnightBinding.matches(rabbit);
        return npc instanceof EntityCheshireCat cat && cat.isOriginalSourceManaged() && CHESHIRE_EVENTS.contains(npcId)
                && OriginalNpcCharacterBindings.matches(cat, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE);
    }

    private static void refreshActor(PathfinderMob npc, ServerPlayer player) {
        if (npc instanceof EntityOriginalNpc original) refreshPage(original, player);
        else if (npc instanceof EntityCheshireCat cat) cat.refreshOriginalRoutePage();
        else if (npc instanceof EntityRabbitKnight rabbit) rabbit.refreshOriginalSourcePage();
    }

    public static void interactRabbitKnight(ServerPlayer player, EntityRabbitKnight rabbit) {
        rabbit.refreshOriginalSourcePage();
        if (!matchesActor(rabbit, OriginalRabbitKnightBinding.SOURCE_ID) || !canInteract(player, rabbit)
                || TurnBattleManager.isInBattle(player)) return;
        interact(player, rabbit, OriginalRabbitKnightBinding.SOURCE_ID, false);
    }

    public static void interactCheshire(ServerPlayer player, EntityCheshireCat cat) {
        cat.refreshOriginalRoutePage();
        if (!matchesActor(cat, CHESHIRE_LOGIC) || !canInteract(player, cat) || TurnBattleManager.isInBattle(player)) return;
        if (SESSIONS.containsKey(player.getUUID())) return;
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        if (progress.hasOtherConversation(player.getUUID(), CHESHIRE_EVENTS)) return;
        for (String id : CHESHIRE_EVENTS) {
            if (progress.conversation(player.getUUID(), id) != null) {
                interact(player, cat, id, true);
                return;
            }
        }
        if (migrateCheshireLegacy(player, cat)) return;
        if (!cat.isInvisible() && cat.getSpriteOpacity() > 0) interact(player, cat, CHESHIRE_LOGIC, false);
    }

    public static boolean startCheshireContact(ServerPlayer player, EntityCheshireCat cat, String npcId) {
        if (!"map051_ev096".equals(npcId) && !"map051_ev097".equals(npcId)) return false;
        cat.refreshOriginalRoutePage();
        if (!matchesActor(cat, npcId) || !canInteract(player, cat) || TurnBattleManager.isInBattle(player)
                || SESSIONS.containsKey(player.getUUID()) || !cat.forcedRouteTicket().isEmpty()) return false;
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        if (!progress.switchValue(108) || progress.switchValue(109) || progress.switchValue(14)
                || SceneSpawnerBossData.get(player.server).getStoryProgress() >= 1
                || progress.hasOtherConversation(player.getUUID(), CHESHIRE_EVENTS)
                || progress.conversationIds(player.getUUID()).stream().anyMatch(CHESHIRE_EVENTS::contains)) return false;
        interact(player, cat, npcId, false);
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.npcId.equals(npcId);
    }

    private static void resumeCheshireLegacyProgress(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || TurnBattleManager.isInBattle(player) || SESSIONS.containsKey(player.getUUID())) continue;
            OriginalNpcProgress progress = OriginalNpcProgress.get(server);
            if (progress.hasMigratedCheshireLegacy(player.getUUID())
                    || progress.hasOtherConversation(player.getUUID(), CHESHIRE_EVENTS)
                    || progress.conversationIds(player.getUUID()).stream().anyMatch(CHESHIRE_EVENTS::contains)) continue;
            try {
                EntityCheshireCat cat = OriginalNpcCharacterBindings.resolve(player.serverLevel(), OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE);
                if (canInteract(player, cat)) {
                    cat.refreshOriginalRoutePage();
                    migrateCheshireLegacy(player, cat);
                }
            } catch (UnsupportedOperationException ignored) {
            }
        }
    }

    private static boolean migrateCheshireLegacy(ServerPlayer player, EntityCheshireCat cat) {
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        if (progress.hasMigratedCheshireLegacy(player.getUUID())) return false;
        var legacy = player.getPersistentData();
        int stage = legacy.getInt("BlacksoulsCheshireRabbitHoleStage");
        boolean asked = legacy.getBoolean("BlacksoulsCheshireAskedAlice");
        if (stage >= 3) {
            progress.setSelfSwitch(CHESHIRE_LOGIC, "A", true);
        } else if ((stage == 1 || stage == 2 || asked) && !progress.switchValue(108)
                && !progress.switchValue(109) && !progress.selfSwitch(CHESHIRE_LOGIC, "A")) {
            if (!progress.switchValue(107) || progress.switchValue(14)
                    || SceneSpawnerBossData.get(player.server).getStoryProgress() >= 1) return true;
            for (Session current : SESSIONS.values()) {
                if (cat.getUUID().equals(current.npcUuid)) return true;
            }
            EventContext context = new EventContext(player, cat, CHESHIRE_LOGIC);
            BSOriginalNpcData.Page page = OriginalNpcLogicData.get(CHESHIRE_LOGIC).pages().get(0);
            JsonObject snapshot = new OriginalNpcInterpreter(page.commands(), context).snapshot();
            JsonObject frame = snapshot.getAsJsonArray("frames").get(0).getAsJsonObject();
            frame.addProperty("position", 20);
            frame.getAsJsonObject("choices").addProperty("0", 0);
            OriginalNpcInterpreter interpreter = OriginalNpcInterpreter.restore(snapshot, context);
            Session session = new Session(player.getUUID(), cat.getId(), CHESHIRE_LOGIC,
                    player.level().dimension().location(), interpreter);
            SESSIONS.put(player.getUUID(), session);
            progress.setSwitch(7, true);
            present(player, cat, session, interpreter.advance(-1));
            progress.markCheshireLegacyMigrated(player.getUUID());
            return true;
        }
        progress.markCheshireLegacyMigrated(player.getUUID());
        return false;
    }

    public static boolean isBattlePresentationPending(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.prompt != null && session.prompt.type() == OriginalNpcInterpreter.Type.BATTLE;
    }

    public static boolean isNumberInputResponse(ServerPlayer player, int entityId, UUID token, long revision, int value) {
        Session session = SESSIONS.get(player.getUUID());
        return value >= 0 && value <= 99999999 && session != null && session.entityId == entityId
                && session.token.equals(token) && session.prompt != null && session.prompt.revision() == revision
                && session.prompt.type() == OriginalNpcInterpreter.Type.NUMBER_INPUT;
    }

    public static boolean isPictureClockSuspended(ServerPlayer player) {
        return !SESSIONS.containsKey(player.getUUID())
                && OriginalNpcProgress.get(player.server).hasSavedTimedWait(player.getUUID());
    }

    public static boolean isConversing(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) || OriginalDodoRunService.pending(player);
    }

    static boolean hasNpcConversation(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    static boolean isConversationActor(UUID owner, EntityOriginalNpc actor) {
        Session session = SESSIONS.get(owner);
        return session != null && actor.getUUID().equals(session.npcUuid);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        suspend(event);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        suspend(event);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) suspend(player);
    }

    private static void suspend(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) suspend(player);
    }

    private static void suspend(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        OriginalNpcAnimationService.forgetObserver(player.server, player.getUUID());
        OriginalNpcPictureService.forgetObserver(player.server, player.getUUID());
        if (session != null) checkpoint(player, session);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.playerId);
            if (player != null) {
                checkpoint(player, session);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SESSIONS.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        OriginalNpcMovementService.tick(event.getServer());
        OriginalNpcAnimationService.tick(event.getServer());
        OriginalNpcPictureService.tick(event.getServer());
        OriginalWorldTransferService.tick(event.getServer());
        if (event.getServer().getTickCount() % 20 == 0) {
            resumeSavedCharacterWaits(event.getServer());
            resumeCheshireLegacyProgress(event.getServer());
        }
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.playerId);
            if (session.stagedPrompt != null && player != null
                    && player.serverLevel().getGameTime() - session.updated > 2400L) {
                checkpoint(player, session);
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            if (session.prompt == null || (session.prompt.type() != OriginalNpcInterpreter.Type.WAIT
                    && session.prompt.type() != OriginalNpcInterpreter.Type.ROUTE_WAIT
                    && session.prompt.type() != OriginalNpcInterpreter.Type.ANIMATION_WAIT)) {
                continue;
            }
            if (player == null || !player.level().dimension().location().equals(session.dimension)) {
                if (player != null) closeWaitDisplay(player, session);
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            Entity entity = player.serverLevel().getEntity(session.entityId);
            if (!(entity instanceof PathfinderMob npc) || !matchesActor(npc, session.npcId)
                    || session.npcUuid != null && !session.npcUuid.equals(npc.getUUID())
                    || !player.isAlive() || !npc.isAlive() || npc.isRemoved()) {
                checkpoint(player, session);
                closeWaitDisplay(player, session);
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            if (TurnBattleManager.isInBattle(player)) {
                session.waitUntil++;
                continue;
            }
            if (session.prompt.type() == OriginalNpcInterpreter.Type.WAIT && OriginalWorldTransferService.pending(player)
                    && player.serverLevel().getGameTime() >= session.waitUntil) {
                session.waitUntil++;
                continue;
            }
            boolean completed = switch (session.prompt.type()) {
                case ROUTE_WAIT -> OriginalNpcMovementService.isComplete(event.getServer(), session.prompt.reason());
                case ANIMATION_WAIT -> OriginalNpcAnimationService.isComplete(event.getServer(), session.prompt.reason());
                default -> player.serverLevel().getGameTime() >= session.waitUntil;
            };
            if (completed) {
                if (session.prompt.type() == OriginalNpcInterpreter.Type.ROUTE_WAIT) {
                    OriginalNpcMovementService.release(event.getServer(), session.prompt.reason());
                } else if (session.prompt.type() == OriginalNpcInterpreter.Type.ANIMATION_WAIT) {
                    OriginalNpcAnimationService.release(event.getServer(), session.prompt.reason());
                }
                present(player, npc, session, session.interpreter.resumeWait());
            }
        }
    }

    private static void closeWaitDisplay(ServerPlayer player, Session session) {
        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(session.entityId, session.token,
                session.prompt.revision(), "", "", "", 0, OriginalNpcInterpreter.Type.BLOCKED,
                List.of(), List.of()), player);
    }

    private static void resumeSavedCharacterWaits(net.minecraft.server.MinecraftServer server) {
        OriginalNpcProgress progress = OriginalNpcProgress.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SESSIONS.containsKey(player.getUUID()) || TurnBattleManager.isInBattle(player) || !player.isAlive()) {
                continue;
            }
            for (String npcId : progress.conversationIds(player.getUUID())) {
                JsonObject saved = progress.conversation(player.getUUID(), npcId);
                if (saved == null || !saved.has("entityUuid") || !saved.has("dimension")) {
                    continue;
                }
                JsonElement pending = saved.getAsJsonObject("interpreter").get("pending");
                if (!pending.isJsonObject() || !("ROUTE_WAIT".equals(pending.getAsJsonObject().get("type").getAsString())
                        || "ANIMATION_WAIT".equals(pending.getAsJsonObject().get("type").getAsString())
                        || "WAIT".equals(pending.getAsJsonObject().get("type").getAsString()))
                        || !player.level().dimension().location().toString().equals(saved.get("dimension").getAsString())) {
                    continue;
                }
                Entity entity = player.serverLevel().getEntity(UUID.fromString(saved.get("entityUuid").getAsString()));
                if (entity instanceof PathfinderMob npc && matchesActor(npc, npcId)
                        && npc.isAlive() && !npc.isRemoved() && npc.level() == player.level()) {
                    interact(player, npc, npcId, true);
                    if (SESSIONS.containsKey(player.getUUID())) {
                        break;
                    }
                }
            }
        }
    }

    public static void refreshPage(EntityOriginalNpc npc, ServerPlayer player) {
        BSOriginalNpcData.Entry definition = BSOriginalNpcData.getEvent(npc.getNpcId());
        if (definition == null || !(npc.level() instanceof ServerLevel)) {
            npc.setPageIndex(-1);
            return;
        }
        BSOriginalNpcData.Page page = OriginalNpcInterpreter.selectPage(definition, new EventContext(player, npc));
        npc.setPageIndex(page == null ? -1 : definition.pages().indexOf(page));
    }

    public static void interact(ServerPlayer player, EntityOriginalNpc npc) {
        if (!matchesActor(npc, npc.getNpcId())) return;
        if (com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(npc.getNpcId())
                && !OriginalWorldEventBindings.isBound(player, npc)) return;
        if (com.BlackSouls.BlackSoulsMod.compat.scene.OriginalBillPlacement.INITIAL_NPC.equals(npc.getNpcId())
                && OriginalBillInteraction.tryInteract(player)) return;
        interact(player, npc, npc.getNpcId(), false);
    }

    private static void interact(ServerPlayer player, PathfinderMob npc, String npcId, boolean savedContinuation) {
        if (OriginalDodoRunService.pending(player)) return;
        boolean available = savedContinuation
                ? player.isAlive() && npc.isAlive() && !npc.isRemoved() && player.level() == npc.level()
                : canInteract(player, npc);
        if (!available || TurnBattleManager.isInBattle(player)) {
            return;
        }
        Session previous = SESSIONS.get(player.getUUID());
        if (previous != null && isSuspended(previous)) {
            if (previous.prompt != null && previous.prompt.type() == OriginalNpcInterpreter.Type.NUMBER_INPUT) {
                if (!previous.npcId.equals(npcId) || previous.npcUuid == null || !previous.npcUuid.equals(npc.getUUID())
                        || !previous.dimension.equals(player.level().dimension().location())) return;
            } else if (previous.stagedPrompt == null) {
                return;
            }
            checkpoint(player, previous);
            SESSIONS.remove(player.getUUID(), previous);
        }
        SESSIONS.entrySet().removeIf(entry -> !isSuspended(entry.getValue())
                && entry.getValue().updated + 2400L < player.serverLevel().getGameTime());
        for (Session current : SESSIONS.values()) {
            if ((current.npcId.equals(npcId) || npc.getUUID().equals(current.npcUuid))
                    && !current.playerId.equals(player.getUUID())) {
                ServerPlayer owner = player.server.getPlayerList().getPlayer(current.playerId);
                if (owner != null && (isSuspended(current)
                        || player.serverLevel().getGameTime() - current.updated < 2400L)) {
                    return;
                }
            }
        }
        EventContext context = new EventContext(player, npc, npcId);
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        JsonObject saved = progress.conversation(player.getUUID(), npcId);
        if (progress.hasOtherConversation(player.getUUID(), npc instanceof EntityCheshireCat
                ? CHESHIRE_EVENTS : java.util.Set.of(npcId)) && (npc instanceof EntityCheshireCat || saved == null)) return;
        if (savedContinuation && saved == null) return;
        if (saved != null) {
            try {
                if (saved.has("entityUuid") && !npc.getUUID().toString().equals(saved.get("entityUuid").getAsString())
                        || saved.has("dimension") && !player.level().dimension().location().toString().equals(saved.get("dimension").getAsString())) return;
                OriginalNpcInterpreter interpreter = OriginalNpcInterpreter.restore(saved.getAsJsonObject("interpreter"), context);
                OriginalSpiritService.restoreLegacyCovenantProgress(player, saved.getAsJsonObject("interpreter"));
                Session session = new Session(player.getUUID(), npc.getId(), npcId,
                        player.level().dimension().location(), interpreter);
                session.lastText = saved.has("lastText") ? saved.get("lastText").getAsString() : "";
                session.lastPortrait = saved.has("lastPortrait") ? saved.get("lastPortrait").getAsString() : "";
                session.lastPortraitIndex = saved.has("lastPortraitIndex") ? saved.get("lastPortraitIndex").getAsInt() : 0;
                session.restoringPresentation = true;
                if (saved.has("synthesisRecipes")) {
                    List<Integer> recipes = new ArrayList<>();
                    for (JsonElement value : saved.getAsJsonArray("synthesisRecipes")) {
                        int id = value.getAsBigDecimal().intValueExact();
                        if (com.BlackSouls.BlackSoulsMod.util.OriginalSynthesisRecipes.get(1, id) == null || recipes.contains(id)) {
                            throw new IllegalArgumentException("Invalid saved synthesis recipe list");
                        }
                        recipes.add(id);
                    }
                    session.synthesisRecipes = List.copyOf(recipes);
                }
                progress.claimConversation(player.getUUID(), npcId);
                SESSIONS.put(player.getUUID(), session);
                OriginalNpcConversationMovement.start(player, npc);
                OriginalNpcInterpreter.Prompt prompt = interpreter.pendingPrompt();
                if (prompt == null) {
                    prompt = interpreter.advance(-1);
                } else if (prompt.type() == OriginalNpcInterpreter.Type.BLOCKED) {
                    prompt = interpreter.retryBlocked();
                } else if (prompt.type() == OriginalNpcInterpreter.Type.NUMBER_INPUT) {
                    prompt = interpreter.refreshNumberInput();
                }
                if (prompt.type() == OriginalNpcInterpreter.Type.WAIT && saved.has("waitRemaining")) {
                    prompt = new OriginalNpcInterpreter.Prompt(prompt.revision(), prompt.type(), prompt.text(),
                            prompt.portrait(), prompt.portraitIndex(), prompt.choices(), prompt.goods(), prompt.reason(),
                            prompt.battle(), Math.max(0, saved.get("waitRemaining").getAsInt()));
                }
                present(player, npc, session, prompt);
                OriginalWorldTransferService.restorePresentation(player);
            } catch (RuntimeException exception) {
                SESSIONS.remove(player.getUUID());
                BlackSouls.LOGGER.error("Failed to resume original NPC {} for {}", npcId, player.getUUID(), exception);
                player.sendSystemMessage(Component.literal("这段事件的进度暂时无法继续。"));
            }
            return;
        }
        refreshActor(npc, player);
        BSOriginalNpcData.Entry definition = definition(npcId);
        BSOriginalNpcData.Page page = definition == null ? null : OriginalNpcInterpreter.selectPage(definition, context);
        if (page == null || page.commands().isEmpty()) {
            return;
        }
        Session session = new Session(player.getUUID(), npc.getId(), npcId,
                player.level().dimension().location(), new OriginalNpcInterpreter(page.commands(), context));
        SESSIONS.put(player.getUUID(), session);
        OriginalNpcConversationMovement.start(player, npc);
        present(player, npc, session, session.interpreter.advance(-1));
    }

    public static void trade(ServerPlayer player, int entityId, UUID token, long revision,
                             ServerboundOriginalNpcTradePacket.Action action, int kind, int id, int quantity) {
        Session session = SESSIONS.get(player.getUUID());
        if (action == null || session == null || session.entityId != entityId || !session.token.equals(token)
                || session.prompt == null || session.prompt.type() != OriginalNpcInterpreter.Type.SHOP
                && session.prompt.type() != OriginalNpcInterpreter.Type.ITEM_KEEP
                && session.prompt.type() != OriginalNpcInterpreter.Type.SYNTHESIS
                || session.prompt.revision() != revision || !player.level().dimension().location().equals(session.dimension)
                || TurnBattleManager.isInBattle(player)) return;
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof PathfinderMob npc) || !matchesActor(npc, session.npcId)
                || session.npcUuid == null || !session.npcUuid.equals(npc.getUUID()) || !canInteract(player, npc)
                || player.serverLevel().getGameTime() - session.updated > 2400L) return;
        if (session.prompt.type() == OriginalNpcInterpreter.Type.SYNTHESIS) {
            if (action != ServerboundOriginalNpcTradePacket.Action.SYNTHESIZE) return;
            if (session.synthesisRecipes != null && session.synthesisRecipes.contains(id)) {
                OriginalNpcSynthesisService.synthesize(player, kind, id, quantity);
            }
            present(player, npc, session, session.interpreter.refreshSynthesis());
            return;
        }
        if (session.prompt.type() == OriginalNpcInterpreter.Type.ITEM_KEEP) {
            if (action != ServerboundOriginalNpcTradePacket.Action.DEPOSIT
                    && action != ServerboundOriginalNpcTradePacket.Action.WITHDRAW) return;
            OriginalNpcItemKeepService.transfer(player, action, kind, id, quantity);
            present(player, npc, session, session.interpreter.refreshItemKeep());
            return;
        }
        if (action != ServerboundOriginalNpcTradePacket.Action.BUY && action != ServerboundOriginalNpcTradePacket.Action.SELL
                && action != ServerboundOriginalNpcTradePacket.Action.TOGGLE_STORAGE) return;
        if (action == ServerboundOriginalNpcTradePacket.Action.TOGGLE_STORAGE) {
            if (kind != 0 || id != 0 || quantity != 0) return;
            if (!OriginalNpcProgress.get(player.server).switchValue(201)) session.shopStorage = !session.shopStorage;
        } else {
            OriginalNpcShopService.transact(player, session.prompt.goods(), session.shopStorage,
                    session.interpreter.shopPurchaseOnly(), action, kind, id, quantity);
        }
        present(player, npc, session, session.interpreter.refreshShop());
    }

    public static void respond(ServerPlayer player, int entityId, UUID sessionId, long revision, int selection) {
        if (entityId == player.getId()) {
            OriginalDodoRunService.respond(player, entityId, sessionId, revision, selection);
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId != entityId || !session.token.equals(sessionId)
                || session.prompt == null || session.prompt.revision() != revision) {
            return;
        }
        if (session.prompt.type() == OriginalNpcInterpreter.Type.WAIT
                || session.prompt.type() == OriginalNpcInterpreter.Type.ROUTE_WAIT
                || session.prompt.type() == OriginalNpcInterpreter.Type.ANIMATION_WAIT) {
            return;
        }
        if (!player.level().dimension().location().equals(session.dimension)) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof PathfinderMob npc) || !matchesActor(npc, session.npcId)
                    || session.npcUuid != null && !session.npcUuid.equals(npc.getUUID())
                || !canInteract(player, npc) || (!isSuspended(session)
                && player.serverLevel().getGameTime() - session.updated > 2400L)) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        if (session.prompt.type() == OriginalNpcInterpreter.Type.BATTLE) {
            if (selection != -4 || session.stagedPrompt == null || TurnBattleManager.isInBattle(player)) {
                return;
            }
            OriginalNpcInterpreter.Prompt staged = session.stagedPrompt;
            session.stagedPrompt = null;
            session.battleOutcome = null;
            present(player, npc, session, staged);
            return;
        }
        if (session.prompt.type() == OriginalNpcInterpreter.Type.SHOP && selection >= 0) {
            if (selection < session.prompt.goods().size()) {
                OriginalNpcInterpreter.Goods goods = session.prompt.goods().get(selection);
                trade(player, entityId, sessionId, revision, ServerboundOriginalNpcTradePacket.Action.BUY,
                        goods.kind(), goods.id(), 1);
            }
            return;
        }
        if (selection == -4 || TurnBattleManager.isInBattle(player)) {
            return;
        }
        if (session.prompt.type() == OriginalNpcInterpreter.Type.ITEM_KEEP && selection != -1 && selection != -3) return;
        if (session.prompt.type() == OriginalNpcInterpreter.Type.SYNTHESIS && selection != -1 && selection != -3) return;
        if (selection == -3) {
            checkpoint(player, session);
            SESSIONS.remove(player.getUUID());
            return;
        }
        if (npc instanceof EntityCheshireCat && CHESHIRE_LOGIC.equals(session.npcId)
                && unsupportedCheshireChoice(session, selection)) {
            checkpoint(player, session);
            SESSIONS.remove(player.getUUID(), session);
            player.sendSystemMessage(Component.literal("这条分支的原作位移尚未移植，可以重新交谈选择其他选项。"));
            NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(npc.getId(), session.token,
                    session.prompt.revision(), npc.getName().getString(), "", "", 0,
                    OriginalNpcInterpreter.Type.BLOCKED, List.of(), List.of()), player);
            return;
        }
        present(player, npc, session, session.interpreter.advance(selection));
    }

    private static boolean unsupportedCheshireChoice(Session session, int selection) {
        if (session.prompt.type() != OriginalNpcInterpreter.Type.CHOICE || selection < 0
                || selection >= session.prompt.choices().size()) return false;
        JsonObject snapshot = session.interpreter.snapshot();
        JsonObject frame = snapshot.getAsJsonArray("frames").get(0).getAsJsonObject();
        var commands = frame.getAsJsonArray("commands");
        int indent = snapshot.get("pendingIndent").getAsInt();
        boolean selected = false;
        for (int i = frame.get("position").getAsInt(); i < commands.size(); i++) {
            JsonObject command = commands.get(i).getAsJsonObject();
            int code = command.get("code").getAsInt();
            var parameters = command.getAsJsonArray("parameters");
            if (command.get("indent").getAsInt() == indent && (code == 402 || code == 403 || code == 404)) {
                if (selected || code == 404) return false;
                selected = code == 402 && parameters.get(0).getAsInt() == selection;
            } else if (selected && code == 205 && parameters.get(0).getAsInt() == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean canInteract(ServerPlayer player, PathfinderMob npc) {
        return (!(npc instanceof EntityOriginalReceptionist receptionist)
                || OriginalReceptionistPlacement.matches(receptionist) && !receptionist.isInvisible())
                && player.isAlive() && !OriginalDodoRunService.pending(player) && npc.isAlive() && !npc.isRemoved() && player.level() == npc.level()
                && player.distanceToSqr(npc) <= 64.0D;
    }

    private static boolean isSuspended(Session session) {
        return session.prompt != null && (session.prompt.type() == OriginalNpcInterpreter.Type.BATTLE
                || session.prompt.type() == OriginalNpcInterpreter.Type.WAIT
                || session.prompt.type() == OriginalNpcInterpreter.Type.ROUTE_WAIT
                || session.prompt.type() == OriginalNpcInterpreter.Type.ANIMATION_WAIT
                || session.prompt.type() == OriginalNpcInterpreter.Type.NUMBER_INPUT);
    }

    private static void checkpoint(ServerPlayer player, Session session) {
        OriginalNpcInterpreter.Prompt effective = session.stagedPrompt != null ? session.stagedPrompt : session.prompt;
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        if (effective == null || effective.type() == OriginalNpcInterpreter.Type.END) {
            progress.setConversation(session.playerId, session.npcId, null);
            return;
        }
        JsonObject saved = new JsonObject();
        saved.add("interpreter", session.interpreter.snapshot());
        if (session.npcUuid != null) {
            saved.addProperty("entityUuid", session.npcUuid.toString());
            saved.addProperty("dimension", session.dimension.toString());
        }
        saved.addProperty("lastText", session.lastText);
        saved.addProperty("lastPortrait", session.lastPortrait);
        saved.addProperty("lastPortraitIndex", session.lastPortraitIndex);
        if (effective.type() == OriginalNpcInterpreter.Type.SYNTHESIS && session.synthesisRecipes != null) {
            com.google.gson.JsonArray recipes = new com.google.gson.JsonArray();
            session.synthesisRecipes.forEach(recipes::add);
            saved.add("synthesisRecipes", recipes);
        }
        if (effective.type() == OriginalNpcInterpreter.Type.WAIT) {
            long remaining = session.stagedPrompt != null ? effective.waitTicks()
                    : Math.max(0L, session.waitUntil - player.serverLevel().getGameTime());
            saved.addProperty("waitRemaining", Math.min(120000L, remaining));
        }
        progress.setConversation(session.playerId, session.npcId, saved);
    }

    private static void present(ServerPlayer player, EntityOriginalNpc npc, Session session,
                                OriginalNpcInterpreter.Prompt prompt) {
        present(player, (PathfinderMob) npc, session, prompt);
    }

    private static void present(ServerPlayer player, PathfinderMob npc, Session session,
                                OriginalNpcInterpreter.Prompt prompt) {
        session.prompt = prompt;
        session.npcUuid = npc.getUUID();
        session.updated = player.serverLevel().getGameTime();
        if (prompt.type() == OriginalNpcInterpreter.Type.SYNTHESIS && session.synthesisRecipes == null) {
            session.synthesisRecipes = OriginalNpcSynthesisService.unlockedRecipes(player);
        } else if (prompt.type() != OriginalNpcInterpreter.Type.SYNTHESIS) {
            session.synthesisRecipes = null;
        }
        if (prompt.type() == OriginalNpcInterpreter.Type.TEXT) {
            session.lastText = resolveText(prompt.text(), player, session.interpreter);
            session.lastPortrait = prompt.portrait();
            session.lastPortraitIndex = prompt.portraitIndex();
        }
        refreshActor(npc, player);
        if (prompt.type() == OriginalNpcInterpreter.Type.WAIT) {
            session.waitUntil = session.updated + prompt.waitTicks();
            checkpoint(player, session);
            NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(npc.getId(), session.token,
                    npc.getName().getString(), "", prompt, List.of()), player);
            return;
        }
        if (prompt.type() == OriginalNpcInterpreter.Type.ROUTE_WAIT
                || prompt.type() == OriginalNpcInterpreter.Type.ANIMATION_WAIT) {
            checkpoint(player, session);
            NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(npc.getId(), session.token,
                    npc.getName().getString(), "", prompt, List.of()), player);
            return;
        }
        if (prompt.type() == OriginalNpcInterpreter.Type.BATTLE) {
            checkpoint(player, session);
            beginBattle(player, npc, session, prompt);
            return;
        }
        checkpoint(player, session);
        if (prompt.type() == OriginalNpcInterpreter.Type.BLOCKED) {
            BlackSouls.LOGGER.warn("Original NPC {} suspended at {}", session.npcId, prompt.reason());
            player.sendSystemMessage(Component.literal("这段对话暂时无法继续。"));
            SESSIONS.remove(player.getUUID());
        } else if (prompt.type() == OriginalNpcInterpreter.Type.END) {
            SESSIONS.remove(player.getUUID());
        }
        if (!SESSIONS.containsKey(player.getUUID())) OriginalNpcConversationMovement.release(player.getUUID());
        OriginalNpcInterpreter.Prompt display = prompt;
        if (session.restoringPresentation && prompt.text().isEmpty()
                && (prompt.type() == OriginalNpcInterpreter.Type.CHOICE || prompt.type() == OriginalNpcInterpreter.Type.SHOP
                || prompt.type() == OriginalNpcInterpreter.Type.ITEM_KEEP || prompt.type() == OriginalNpcInterpreter.Type.SYNTHESIS
                || prompt.type() == OriginalNpcInterpreter.Type.NUMBER_INPUT)) {
            display = new OriginalNpcInterpreter.Prompt(prompt.revision(), prompt.type(), session.lastText,
                    session.lastPortrait, session.lastPortraitIndex, prompt.choices(), prompt.goods(), prompt.reason(),
                    prompt.battle(), prompt.waitTicks(), prompt.numberInput());
        }
        session.restoringPresentation = false;
        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(npc.getId(), session.token,
                session.interpreter.speakerName(), resolveText(display.text(), player, session.interpreter), display,
                prompt.choices().stream().map(value -> resolveText(value, player, session.interpreter)).toList(),
                prompt.type() == OriginalNpcInterpreter.Type.SHOP ? OriginalNpcShopService.view(player, prompt.goods(),
                        session.shopStorage, session.interpreter.shopPurchaseOnly()) : null,
                prompt.type() == OriginalNpcInterpreter.Type.ITEM_KEEP ? OriginalNpcItemKeepService.view(player) : null,
                prompt.type() == OriginalNpcInterpreter.Type.SYNTHESIS ? OriginalNpcSynthesisService.view(player, session.synthesisRecipes) : null), player);
        StatEventHandler.syncToClient(player);
    }

    private static void beginBattle(ServerPlayer player, PathfinderMob npc, Session session,
                                    OriginalNpcInterpreter.Prompt prompt) {
        OriginalNpcInterpreter.BattleRequest request = prompt.battle();
        BSOriginalTroopData.Entry troop = BSOriginalTroopData.get(request.troopId());
        BSOriginalTroopData.Member first = troop == null ? null : troop.members().stream()
                .filter(member -> !member.hidden()).findFirst().orElse(null);
        if (first == null || troop.members().stream()
                .anyMatch(member -> BSOriginalEnemyData.get(member.enemyId()).id() != member.enemyId())) {
            blockBattle(player, npc, session, "unavailable_original_troop:" + request.troopId());
            return;
        }
        EntityOriginalDatabaseEnemy enemy = BSEntityRegistry.ORIGINAL_ENEMY.get().create(player.serverLevel());
        if (enemy == null) {
            blockBattle(player, npc, session, "original_troop_entity:" + request.troopId());
            return;
        }
        enemy.setProfileId(first.enemyId());
        enemy.moveTo(npc.getX(), npc.getY(), npc.getZ(), npc.getYRot(), 0.0F);
        if (!player.serverLevel().addFreshEntity(enemy)) {
            enemy.discard();
            blockBattle(player, npc, session, "original_troop_spawn:" + request.troopId());
            return;
        }
        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(npc.getId(), session.token,
                npc.getName().getString(), "", prompt, List.of()), player);
        boolean started = TurnBattleManager.tryStartOriginalTroop(player, enemy, request.troopId(),
                request.canEscape(), request.canLose(), outcome -> {
                    enemy.discard();
                    if (SESSIONS.get(session.playerId) != session || session.prompt != prompt) {
                        return;
                    }
                    session.updated = player.serverLevel().getGameTime();
                    if (outcome == ClientboundTurnBattlePacket.Outcome.NONE) {
                        checkpoint(player, session);
                        SESSIONS.remove(session.playerId, session);
                        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPacket(npc.getId(), session.token,
                                npc.getName().getString(), "", new OriginalNpcInterpreter.Prompt(prompt.revision() + 1,
                                OriginalNpcInterpreter.Type.END, "", "", 0, List.of(), List.of(), "battle_aborted"), List.of()), player);
                    } else if (outcome == ClientboundTurnBattlePacket.Outcome.DEFEAT && !request.canLose()) {
                        present(player, npc, session, new OriginalNpcInterpreter.Prompt(prompt.revision() + 1,
                                OriginalNpcInterpreter.Type.END, "", "", 0, List.of(), List.of(), "battle_ended"));
                    } else {
                        session.battleOutcome = outcome;
                        OriginalNpcInterpreter.BattleOutcome result = switch (outcome) {
                            case VICTORY -> OriginalNpcInterpreter.BattleOutcome.VICTORY;
                            case ESCAPED -> OriginalNpcInterpreter.BattleOutcome.ESCAPED;
                            case DEFEAT -> OriginalNpcInterpreter.BattleOutcome.DEFEAT;
                            default -> throw new IllegalStateException("Unexpected original battle outcome");
                        };
                        session.stagedPrompt = session.interpreter.resumeBattle(result, true);
                        checkpoint(player, session);
                        StatEventHandler.syncToClient(player);
                    }
                });
        if (!started) {
            enemy.discard();
            blockBattle(player, npc, session, "original_troop_unavailable:" + request.troopId());
        }
    }

    private static void blockBattle(ServerPlayer player, PathfinderMob npc, Session session, String reason) {
        present(player, npc, session, new OriginalNpcInterpreter.Prompt(session.prompt.revision() + 1,
                OriginalNpcInterpreter.Type.BLOCKED, "", "", 0, List.of(), List.of(), reason));
    }

    private static String resolveText(String text, ServerPlayer player, OriginalNpcInterpreter context) {
        String replaced = VARIABLE.matcher(text).replaceAll(match -> Long.toString(
                context.variable(Integer.parseInt(match.group(1)))));
        return ACTOR_NAME.matcher(replaced).replaceAll(java.util.regex.Matcher.quoteReplacement(StoryNameData.get(player)));
    }

    public static final class EventContext implements OriginalNpcInterpreter.Context {
        private final ServerPlayer player;
        private final PathfinderMob npc;
        private final String npcId;
        private final OriginalNpcProgress progress;
        private OriginalNpcNameWindow.State nameWindow;
        private final Map<Integer, Long> numericVariables = new HashMap<>();

        public EventContext(ServerPlayer player, EntityOriginalNpc npc) {
            this(player, npc, npc.getNpcId());
        }

        public EventContext(ServerPlayer player, PathfinderMob npc, String npcId) {
            if (!(npc instanceof EntityOriginalNpc original && original.getNpcId().equals(npcId))
                    && !(npc instanceof EntityCheshireCat && (CHESHIRE_EVENTS.contains(npcId)
                    || OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE.equals(npcId)))
                    && !(npc instanceof EntityRabbitKnight && OriginalRabbitKnightBinding.SOURCE_ID.equals(npcId))) {
                throw new IllegalArgumentException("Invalid original NPC actor " + npcId);
            }
            this.npcId = npcId;
            this.player = player;
            this.npc = npc;
            this.progress = OriginalNpcProgress.get(npc.level().getServer());
            JsonObject state = player == null ? null : progress.nameWindowState(player.getUUID());
            nameWindow = state == null ? new OriginalNpcNameWindow.State() : OriginalNpcNameWindow.State.restore(state);
        }

        private OriginalNpcNameWindow.Access nameAccess() {
            return new OriginalNpcNameWindow.Access() {
                @Override public boolean enabled() { return progress.nameWindowEnabled(); }
                @Override public void setEnabled(boolean enabled) { progress.setNameWindowEnabled(enabled); }
                @Override public String actorName(int id) {
                    if (id == 2 && player != null) return StoryNameData.get(player);
                    throw new UnsupportedOperationException("unmapped_name_window_actor:" + id);
                }
            };
        }

        @Override
        public void comment(String text) {
            nameWindow.comment(text, nameAccess());
            saveNameWindow();
        }

        @Override
        public void beginMessage(String faceName, int faceIndex) {
            nameWindow.beginMessage(faceName, faceIndex, nameAccess());
            saveNameWindow();
        }

        @Override
        public void closeMessage() {
            nameWindow.closeMessage();
            saveNameWindow();
        }

        private void saveNameWindow() {
            if (player != null) progress.setNameWindowState(player.getUUID(), nameWindow.snapshot());
        }

        @Override
        public String speakerName() {
            return nameWindow.visible() ? nameWindow.displayName() : "";
        }

        @Override
        public JsonObject savePresentationState() {
            JsonObject state = nameWindow.snapshot();
            state.addProperty("personalCovenants", true);
            JsonObject numeric = new JsonObject();
            numericVariables.forEach((key, value) -> numeric.addProperty(Integer.toString(key), value));
            if (!numericVariables.isEmpty()) state.add("numericVariables", numeric);
            return state;
        }

        @Override
        public void restorePresentationState(JsonObject saved) {
            if (saved.has("personalCovenants") && (!saved.get("personalCovenants").isJsonPrimitive()
                    || !saved.getAsJsonPrimitive("personalCovenants").isBoolean()
                    || !saved.get("personalCovenants").getAsBoolean())) throw new IllegalArgumentException("Invalid personal covenant checkpoint marker");
            nameWindow = OriginalNpcNameWindow.State.restore(saved);
            numericVariables.clear();
            if (saved.has("numericVariables")) {
                for (var entry : saved.getAsJsonObject("numericVariables").entrySet()) {
                    int id = Integer.parseInt(entry.getKey());
                    if (id != 26 && id != 27) throw new IllegalArgumentException("Invalid scoped number variable");
                    numericVariables.put(id, entry.getValue().getAsBigDecimal().longValueExact());
                }
            }
            saveNameWindow();
        }

        private BSPlayerStats stats() {
            if (player == null) {
                throw new UnsupportedOperationException("requires_player");
            }
            return player.getCapability(BSPlayerStats.CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Missing original NPC player stats"));
        }

        @Override
        public boolean switchValue(int id) {
            if (player != null && (id == 11 || id == 12)) return OriginalPlayerMoveSpeed.switchValue(player, id);
            return player != null ? OriginalNpcProgress.readSwitch(player, id) : progress.switchValue(id);
        }

        @Override
        public int characterDirection(int target) {
            if (!(npc instanceof EntityOriginalNpc original)) throw new UnsupportedOperationException("condition_character_source");
            Entity entity = OriginalNpcCharacterBindings.resolveEvent(player, original, target);
            if (entity instanceof EntityOriginalNpc character) return character.getSpriteDirection();
            var source = BSOriginalNpcData.get(npcId);
            if (entity == player && source != null && source.mapId() == 62) {
                return com.BlackSouls.BlackSoulsMod.compat.scene.OriginalBillPlacement.playerDirection(player);
            }
            throw new UnsupportedOperationException("condition_character_direction_mapping:" + target);
        }

        @Override
        public void openItemKeep() {
            if (player == null) throw new UnsupportedOperationException("requires_player");
        }

        @Override
        public void openSynthesis() {
            if (player == null) throw new UnsupportedOperationException("requires_player");
        }

        @Override
        public void unlockWeaponRecipes(List<Integer> ids) {
            if (ids.isEmpty() || ids.stream().anyMatch(id -> com.BlackSouls.BlackSoulsMod.util.OriginalSynthesisRecipes.get(1, id) == null)) {
                throw new UnsupportedOperationException("unmapped_original_recipe_unlock");
            }
            for (int id : ids) progress.setRecipeUnlocked(1, id, true);
        }

        @Override
        public void setSwitch(int id, boolean value) {
            if (player != null && (id == 11 || id == 12)) {
                OriginalPlayerMoveSpeed.setSwitch(player, id, value);
                return;
            }
            if (player != null) {
                OriginalNpcProgress.writeSwitch(player, id, value);
                if (id == 41) OriginalAltarEntry.synchronize(player.serverLevel());
                return;
            }
            progress.setSwitch(id, value);
        }

        @Override
        public long variable(int id) {
            if (numericVariables.containsKey(id)) return numericVariables.get(id);
            if (player != null) {
                return OriginalNpcProgress.readVariable(player, id);
            }
            if (id == 11) {
                return SceneSpawnerBossData.get(npc.level().getServer()).getStoryProgress();
            }
            return progress.variable(id);
        }

        @Override
        public void setVariable(int id, long value) {
            if (player != null && (id == 26 || id == 27)) {
                numericVariables.put(id, value);
                return;
            }
            if (player != null) {
                OriginalNpcProgress.writeVariable(player, id, value);
                return;
            }
            if (id == 38) {
                throw new UnsupportedOperationException("requires_player");
            }
            if (id == 11) {
                SceneSpawnerBossData.get(npc.level().getServer()).setStoryProgress((int) Math.max(0L,
                        Math.min(Integer.MAX_VALUE, value)));
            }
            progress.setVariable(id, value);
        }

        @Override
        public int transfer(int map, int x, int y, int direction, int fade) {
            if (!(npc instanceof EntityOriginalNpc original) || player == null) throw new UnsupportedOperationException("transfer_source");
            return OriginalWorldEventBindings.transfer(player, original, map, x, y, direction, fade);
        }

        @Override
        public boolean selfSwitch(String key) {
            return progress.selfSwitch(npcId, key);
        }

        @Override
        public void setSelfSwitch(String key, boolean value) {
            progress.setSelfSwitch(npcId, key, value);
        }

        @Override
        public long gold() {
            return player == null ? 0L : stats().souls;
        }

        @Override
        public void changeGold(long amount) {
            BSPlayerStats stats = stats();
            stats.souls = java.math.BigInteger.valueOf(stats.souls).add(java.math.BigInteger.valueOf(amount))
                    .max(java.math.BigInteger.ZERO).min(java.math.BigInteger.valueOf(
                            com.BlackSouls.BlackSoulsMod.util.OriginalNpcShopRules.MAX_GOLD)).longValueExact();
        }

        @Override
        public int itemCount(int kind, int id) {
            Item item = OriginalNpcItems.item(kind, id);
            if (item == null) {
                throw new UnsupportedOperationException("unmapped_item:" + kind + ":" + id);
            }
            return player == null ? 0 : OriginalNpcInventoryService.count(player, kind, id);
        }

        @Override
        public boolean itemPresent(int kind, int id, boolean includeEquipment) {
            return itemCount(kind, id) > 0 || includeEquipment && player != null
                    && OriginalActorBridge.equipped(player, 2, kind, id);
        }

        @Override
        public boolean changeItem(int kind, int id, long amount) {
            return changeItem(kind, id, amount, false);
        }

        @Override
        public boolean changeItem(int kind, int id, long amount, boolean includeEquipment) {
            return OriginalNpcInventoryService.change(player, kind, id, amount, includeEquipment);
        }

        @Override
        public boolean hasActor(int actorId) {
            return OriginalActorBridge.isPlayerActor(player, actorId);
        }

        @Override
        public boolean actorCondition(int actorId, int condition, JsonElement value) {
            if (condition == 0) {
                return hasActor(actorId);
            }
            if (!hasActor(actorId)) {
                throw new UnsupportedOperationException("unmapped_actor:" + actorId);
            }
            return switch (condition) {
                case 1 -> StoryNameData.get(player).equals(value.getAsString());
                case 2 -> value.getAsInt() == 1;
                case 3 -> {
                    if (value.getAsInt() != 340) throw new UnsupportedOperationException("actor_condition:3");
                    yield stats().unlockedSkills.contains(com.BlackSouls.BlackSoulsMod.util.OriginalActorSkills.mapped(340));
                }
                case 4, 5 -> OriginalActorBridge.equipped(player, actorId, condition == 4 ? 1 : 2, value.getAsInt());
                default -> throw new UnsupportedOperationException("actor_condition:" + condition);
            };
        }

        @Override
        public boolean scriptCondition(String expression) {
            throw new UnsupportedOperationException("script_condition:" + expression);
        }

        @Override
        public long gameData(int type, int first, int second) {
            if (type >= 0 && type <= 2) {
                return itemCount(type, first);
            }
            if (type == 7 && first == 2) {
                return gold();
            }
            if (type == 3) {
                return OriginalActorBridge.gameData(player, first, second);
            }
            throw new UnsupportedOperationException("game_data:" + type + ":" + first + ":" + second);
        }

        @Override
        public void prepareNumberInput(int variableId) {
            if (player == null) throw new UnsupportedOperationException("number_input_player");
            if (variableId == 26) {
                numericVariables.putIfAbsent(26, 0L);
                numericVariables.putIfAbsent(27, gold());
            }
        }

        @Override
        public long numberInputMaximum(int variableId, long maximum) {
            if (variableId != 26) return maximum;
            return Math.max(0L, Math.min(maximum, Math.min(gold(), Long.MAX_VALUE - Math.max(0L, stats().currentExp))));
        }

        @Override
        public boolean numberInputAllowed(int variableId, int value) {
            return variableId != 26 || value <= gold() && stats().currentExp >= 0 && value <= Long.MAX_VALUE - stats().currentExp;
        }

        @Override
        public String changeExperience(int actorId, long amount, boolean show) {
            return OriginalActorBridge.changeExperience(player, actorId, amount, show);
        }

        @Override
        public String convertExperience(int actorId, long amount, boolean show) {
            return OriginalActorBridge.convertExperience(player, actorId, amount, show);
        }

        @Override
        public void changeSkill(int actorId, int skillId, boolean learn) {
            com.BlackSouls.BlackSoulsMod.util.OriginalActorSkills.change(player, actorId, skillId, learn);
        }

        @Override
        public long random(int first, int last) {
            if (last < first || (long) last - first >= Integer.MAX_VALUE) {
                throw new UnsupportedOperationException("random_range");
            }
            return (long) first + npc.getRandom().nextInt(last - first + 1);
        }

        @Override
        public List<BSOriginalNpcData.Command> commonEvent(int id) {
            if (id == 80 && player != null) OriginalSpiritService.beginNodenConversation(player);
            return BSOriginalNpcData.commonEvent(id);
        }

        @Override
        public void changeSpirit(int spiritId, boolean add) {
            if ((npcId.equals("map101_ev009") || npcId.equals("map101_ev015")) && spiritId >= 1 && spiritId <= 4
                    && player != null && !stats().originalNodenProgressKnown) OriginalSpiritService.restoreLegacyNodenProgress(player);
            OriginalSpiritService.change(player, spiritId, add);
        }

        @Override
        public void eraseEvent() {
            if (npc instanceof EntityOriginalNpc original) original.eraseForCurrentVisit();
            else throw new UnsupportedOperationException("erase_bound_character:" + npcId);
        }

        @Override
        public void animation(int id) {
            NetworkHandler.sendToAllAround(new PacketPlayAnim(id, npc.getX(), npc.getY(), npc.getZ()), npc);
        }

        @Override
        public String animation(int target, int id, boolean wait) {
            if (npc instanceof EntityOriginalNpc original) return OriginalNpcAnimationService.start(player, original, target, id, wait);
            if (npc instanceof EntityRabbitKnight rabbit) {
                if (target != 0 || !matchesActor(rabbit, npcId)) throw new UnsupportedOperationException("rabbit_animation_target:" + target);
                return OriginalNpcAnimationService.startRabbitCharacter(player, rabbit, id, wait);
            }
            requireCheshireVisual(target);
            return OriginalNpcAnimationService.startBoundCharacter(player, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE, id, wait);
        }

        @Override
        public void sound(JsonObject sound) {
            OriginalNpcPresentationService.apply(player, npc, 250, List.of(sound));
        }

        @Override
        public int presentation(int command, List<JsonElement> parameters) {
            if (command == 231 || command == 232 || command == 235) {
                return OriginalNpcPictureService.apply(player, command, parameters);
            }
            OriginalNpcPresentationService.apply(player, npc, command, parameters);
            return OriginalNpcPresentationService.waitTicks(command, parameters);
        }

        @Override
        public void changeBattleBgm(JsonObject bgm) {
            OriginalBattleMusicService.set(player, bgm);
        }

        @Override
        public void unlockAchievement(String name) {
            OriginalNpcAchievements.grant(player, name);
        }

        @Override
        public String moveRoute(int target, JsonObject route) {
            if (npc instanceof EntityOriginalNpc original) return OriginalNpcMovementService.start(player, original, target, route);
            if (npc instanceof EntityRabbitKnight rabbit) {
                if (target != 0 || !matchesActor(rabbit, npcId)) throw new UnsupportedOperationException("rabbit_route_target:" + target);
                return OriginalNpcMovementService.startRabbitCharacter(player, rabbit, route);
            }
            requireCheshireVisual(target);
            return OriginalNpcMovementService.startBoundCharacter(player, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE, route);
        }

        private void requireCheshireVisual(int target) {
            if (target != 59 || !matchesActor(npc, npcId)
                    || OriginalNpcCharacterBindings.resolve((ServerLevel) npc.level(), OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE) != npc) {
                throw new UnsupportedOperationException("bound_character_target:" + npcId + ":" + target);
            }
        }

        @Override
        public void recoverAll(int type, int id) {
            long actorId = type == 0 ? id : type == 1 ? variable(id) : -1L;
            if (actorId == 0 || actorId == 2 && hasActor(2)) {
                PlayerStatService.recoverAll(player);
                return;
            }
            throw new UnsupportedOperationException("recover_actor:" + type + ":" + id);
        }

        @Override
        public boolean canTrade(OriginalNpcInterpreter.Goods goods) {
            return OriginalNpcItems.item(goods.kind(), goods.id()) != null;
        }

        @Override
        public void buy(OriginalNpcInterpreter.Goods goods) {
            OriginalNpcShopService.transact(player, List.of(goods), false, false,
                    ServerboundOriginalNpcTradePacket.Action.BUY, goods.kind(), goods.id(), 1);
        }
    }

    private static final class Session {
        private final UUID token = UUID.randomUUID();
        private final UUID playerId;
        private final int entityId;
        private final String npcId;
        private final ResourceLocation dimension;
        private final OriginalNpcInterpreter interpreter;
        private OriginalNpcInterpreter.Prompt prompt;
        private long updated;
        private long waitUntil;
        private ClientboundTurnBattlePacket.Outcome battleOutcome;
        private OriginalNpcInterpreter.Prompt stagedPrompt;
        private UUID npcUuid;
        private String lastText = "";
        private String lastPortrait = "";
        private int lastPortraitIndex;
        private boolean restoringPresentation;
        private boolean shopStorage;
        private List<Integer> synthesisRecipes;

        private Session(UUID playerId, int entityId, String npcId, ResourceLocation dimension,
                        OriginalNpcInterpreter interpreter) {
            this.playerId = playerId;
            this.entityId = entityId;
            this.npcId = npcId;
            this.dimension = dimension;
            this.interpreter = interpreter;
        }
    }
}
