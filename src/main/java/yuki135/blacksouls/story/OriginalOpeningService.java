package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.entity.EntityOpeningActor;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOpeningPacket;
import yuki135.blacksouls.network.packets.ClientboundOpeningPresentationPacket;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcAnimationPacket;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPresentationPacket;
import yuki135.blacksouls.network.packets.ClientboundStoryNamePacket;
import yuki135.blacksouls.network.packets.ServerboundOpeningPacket;
import yuki135.blacksouls.util.HokoniwaDestination;
import yuki135.blacksouls.util.OriginalOpeningPersistence;
import yuki135.blacksouls.util.OriginalOpeningStartingKit;
import yuki135.blacksouls.util.StoryNameData;
import com.mojang.brigadier.Command;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static yuki135.blacksouls.story.OriginalOpeningProfile.*;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalOpeningService {
    public static final int MAX_SESSIONS = 64;
    public static final int WORLD_WRITE_BUDGET = 256;
    private static final int REPLAY_RETURN_COMMAND = 1_000_000;
    private static final String FALL_GRAVITY="BlacksoulsOpeningFallNoGravity";
    private static final TicketType<UUID> TICKET = TicketType.create("blacksouls_opening", Comparator.comparing(UUID::toString), 80);
    private static final Map<MinecraftServer, LinkedHashMap<UUID, Session>> SESSIONS = new WeakHashMap<>();
    private static final Map<MinecraftServer, Integer> ROUND_ROBIN = new WeakHashMap<>();
    private static final Map<SceneKind,List<Cell>> MOVEMENT_TRIGGERS=new java.util.EnumMap<>(SceneKind.class);

    private OriginalOpeningService() {
    }

    public static boolean managed(ServerPlayer player) {
        var stats = stats(player);
        if (stats == null) return false;
        var profile = stats.openingProfile();
        return profile.previewActive() || profile.readState() == ReadState.REAL
                && profile.realStage().orElse(Stage.COMPLETE) != Stage.COMPLETE;
    }

    public static boolean inventoryLocked(ServerPlayer player) {
        if (!managed(player)) return false;
        var profile=stats(player).openingProfile();
        return profile.previewActive() || profile.realStage().orElse(Stage.COMPLETE)!=Stage.ORIGIN_SELECTION
                || OriginalOpeningGreeting.active(profile.realCursor().commandIndex());
    }

    public static boolean ownsOpening(ServerPlayer player) {
        var stats = stats(player);
        return stats != null && (stats.openingProfile().previewActive() || stats.openingProfile().readState() == ReadState.REAL);
    }

    public static boolean resumePlayer(ServerPlayer player) {
        if (!live(player)) return false;
        var stats = stats(player);
        if (stats == null || stats.openingProfile().hasRejectedData()) return false;
        var profile = stats.openingProfile();
        boolean cleanup = profile.readState() == ReadState.REAL && profile.realStage().orElse(null) == Stage.COMPLETE && profile.realSlot().isPresent();
        if (!managed(player) && !cleanup) return false;
        var sessions = SESSIONS.computeIfAbsent(player.server, ignored -> new LinkedHashMap<>());
        if (sessions.containsKey(player.getUUID())) return true;
        if (sessions.size() >= MAX_SESSIONS) {
            if (player.server.getTickCount()%100==0) player.sendSystemMessage(Component.literal("序章正在准备其他旅人的场景，请稍后重试。"));
            return true;
        }
        var flow = new Flow(profile);
        stabilize(flow);
        normalizeCoda(player,flow);
        stopFall(player);
        var session = new Session(player, profile);
        session.cleanup = cleanup || profile.previewActive() && flow.stage() == Stage.COMPLETE;
        sessions.put(player.getUUID(), session);
        if (!profile.previewActive() && !StoryNameData.hasStarted(player)) StoryNameData.start(player);
        if (profile.previewActive() && flow.stage() == Stage.NAME_PENDING && !aborting(flow)) named(flow);
        if (!profile.previewActive() && flow.stage() == Stage.NAME_PENDING && StoryNameData.isConfirmed(player)) named(flow);
        if (StoryNameData.isConfirmed(player)) NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(false,StoryNameData.get(player)),player);
        save(player, session);
        send(player, session);
        return true;
    }

    public static boolean nameConfirmed(ServerPlayer player) {
        if (!live(player) || !managed(player)) return false;
        var flow = flow(player);
        if (flow.preview() || flow.stage() != Stage.NAME_PENDING || !StoryNameData.isConfirmed(player)) return false;
        resumePlayer(player);
        var session = session(player);
        if (session == null) return false;
        flow = flow(player);
        if (flow.stage() == Stage.NAME_PENDING) named(flow);
        if (flow.stage() != Stage.ORIGIN_SELECTION) return false;
        flow.checkpoint(point(player));
        session.focus = 0;
        changed(player, session);
        return save(player, session);
    }

    private static void named(Flow flow) {
        var pending=flow.pending();
        flow.advance(Stage.ORIGIN_SELECTION);
        flow.cursor(OriginalOpeningGreeting.cursor(OriginalOpeningGreeting.FIRST_COMMAND));
        if (pending.isPresent()) {
            flow.clearPending(pending.get());
            flow.pending(new PendingTransfer(pending.get().targetSourceMapId(),Stage.ORIGIN_SELECTION,pending.get().destination()));
        }
    }

    private static void stabilize(Flow flow) {
        if (flow.stage()!=Stage.FALLING || flow.cinematic().isPresent()) return;
        if (flow.pending().isPresent()) flow.clearPending(flow.pending().get());
        flow.recover();
    }

    public static InteractionResult interactActor(ServerPlayer player, EntityOpeningActor actor) {
        if (!live(player) || actor == null || actor.level() != player.level() || !DIMENSION.equals(player.level().dimension())
                || !player.getUUID().equals(actor.ownerId()) || actor.isRemoved() || !actor.isAlive()
                || player.serverLevel().getEntity(actor.getUUID()) != actor || player.distanceToSqr(actor) > 16.0D) return InteractionResult.FAIL;
        var session = session(player);
        if (session == null || session.error || session.entering || session.awaitingReady || session.lease == null
                || actor.leaseSlot() != session.lease.slot() || session.cleanup) return InteractionResult.FAIL;
        var flow = flow(player);
        if (flow.stage() == Stage.ORIGIN_SELECTION && !OriginalOpeningGreeting.active(flow.cursor().commandIndex())
                && OriginalOpeningGreeting.selectable(actor.originId())) {
            session.focus = actor.originId();
            session.focusActor = actor.getUUID();
            OriginalOpeningDialogueView.focus(player,actor);
            session.hold=point(player);
            changed(player, session);
            return InteractionResult.CONSUME;
        }
        if (flow.stage() == Stage.ALICE_SELECTION && actor.originId() == 0) {
            if(OriginalOpeningCoda.aliceChoice(flow.cursor().commandIndex()))OriginalOpeningDialogueView.focus(player,actor);
            changed(player, session);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    public static void handle(ServerPlayer player, ServerboundOpeningPacket packet) {
        if (!live(player) || !player.isAlive() || stats(player)==null || stats(player).openingProfile().hasRejectedData()) return;
        var session = session(player);
        if (session == null || !session.token.equals(packet.session()) || session.revision != packet.revision()) return;
        int now = player.server.getTickCount();
        int command=flow(player).cursor().commandIndex();
        boolean codaInput=packet.action()==ServerboundOpeningPacket.Action.NEXT&&OriginalOpeningCoda.active(command)
                ||packet.action()==ServerboundOpeningPacket.Action.RELATIONSHIP&&OriginalOpeningCoda.aliceChoice(command);
        if (now == session.lastActionTick&&!codaInput) return;
        session.lastActionTick = now;
        if (packet.action() == ServerboundOpeningPacket.Action.RETRY) {
            var flow=flow(player);
            stabilize(flow);
            session.error = false;
            session.notice = "";
            session.entering = !session.cleanup && flow.stage()!=Stage.COMPLETE;
            session.awaitingReady = false;
            session.sceneReady = false;
            session.motion = null;
            session.fallReady=false;
            stopFall(player);
            if(save(player, session) && flow.stage()==Stage.COMPLETE)session.cleanup=true;
            changed(player, session);
            return;
        }
        var flow = flow(player);
        if (packet.action() == ServerboundOpeningPacket.Action.EXIT_REPLAY && flow.preview() && !session.cleanup && flow.stage()!=Stage.COMPLETE) {
            abortPreview(player, session, flow);
            return;
        }
        if (session.error || session.cleanup) return;
        if (packet.action() == ServerboundOpeningPacket.Action.READY) {
            acceptReady(player, session, flow);
            return;
        }
        if (packet.action() == ServerboundOpeningPacket.Action.SKIP) {
            if (flow.stage() == Stage.NAME_PENDING) return;
            flow.requestSkip();
            if (flow.stage()==Stage.FALLING) { player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0; }
            changed(player, session);
            save(player, session);
            return;
        }
        if(packet.action()==ServerboundOpeningPacket.Action.NEXT && flow.stage()==Stage.FALLING && flow.cinematic().isPresent()) {
            var cinematic=flow.cinematic().orElseThrow();
            if(!session.entering&&!session.awaitingReady&&DIMENSION.equals(player.level().dimension())&&cinematic.next(packet.value())) {
                if(!save(player,session))return;
                changed(player,session);
            }
            return;
        }
        if (session.entering || session.awaitingReady || session.motion != null || !DIMENSION.equals(player.level().dimension())) return;
        switch (packet.action()) {
            case CONFIRM_ORIGIN -> confirmOrigin(player, session, flow, packet.value());
            case RELATIONSHIP -> {
                if (flow.stage() != Stage.ALICE_SELECTION || packet.value() < 1 || packet.value() > 3) return;
                if(OriginalOpeningCoda.active(flow.cursor().commandIndex())&&!OriginalOpeningCoda.aliceChoice(flow.cursor().commandIndex()))return;
                flow.confirmRelationship(packet.value());
                flow.cursor(OriginalOpeningCoda.cursor(OriginalOpeningCoda.memoryStart(packet.value()),0));
                flow.checkpoint(point(player));
                if(flow.skip()) {
                    flow.skipToArrival();
                    var cinematic=OriginalOpeningCinematic.start("arrival");cinematic.skip();flow.cinematic(cinematic);
                    session.entering=true;session.sceneReady=false;
                    changed(player,session);save(player,session);return;
                }
                if (!OriginalOpeningActors.ensure(player,session.lease,SceneKind.ISLAND_110,flow.relationship(),true)) {
                    fail(player,session,"爱丽丝尚未准备好，请重试。","alice_actor_relationship_update_failed");
                    save(player,session);
                    return;
                }
                changed(player, session);
                save(player, session);
            }
            case NEXT -> {
                if(flow.stage()==Stage.ORIGIN_SELECTION)nextGreeting(player,session,flow,packet.value());
                else nextCoda(player,session,flow,packet.value());
            }
            case DOWN -> { }
            case CLOSE_PREVIEW -> {
                if (flow.stage() != Stage.ORIGIN_SELECTION) return;
                session.focus = 0;
                session.focusActor = null;
                changed(player, session);
            }
            default -> { }
        }
    }

    private static void confirmOrigin(ServerPlayer player, Session session, Flow flow, int origin) {
        if (flow.stage() != Stage.ORIGIN_SELECTION || OriginalOpeningGreeting.active(flow.cursor().commandIndex())
                || !OriginalOpeningGreeting.selectable(origin) || origin != session.focus || session.focusActor == null
                || !(player.serverLevel().getEntity(session.focusActor) instanceof EntityOpeningActor actor)
                || !player.getUUID().equals(actor.ownerId()) || actor.leaseSlot() != session.lease.slot()
                || actor.originId() != origin || player.distanceToSqr(actor) > 16.0D) return;
        if (!flow.preview()) {
            var ready = OriginalOpeningStartingKit.preflight(player, origin);
            if (!ready.ready()) { fail(player, session, "请先腾出出身装备所需的位置，再重新确认。", ready.detail()); return; }
        }
        flow.confirmOrigin(origin);
        session.focus = 0;
        session.focusActor = null;
        if (!commitOriginKit(player, session)) return;
        flow = flow(player);
        flow.cursor(originCodaCursor(OriginalOpeningCoda.ORIGIN_ANIMATION,flow.origin(),0));
        flow.checkpoint(point(player));
        changed(player, session);
        if(save(player, session))tickCoda(player,session,flow,false);
    }

    private static boolean commitOriginKit(ServerPlayer player, Session session) {
        var flow = flow(player);
        if (flow.preview()) return true;
        var result = OriginalOpeningStartingKit.grant(player);
        if (!result.committed()) { fail(player, session, "出身内容尚未保存，请重试。", result.detail()); return false; }
        return true;
    }

    private static boolean commitCodaReward(ServerPlayer player,Session session) {
        if(flow(player).preview())return true;
        var result=OriginalOpeningStartingKit.opCoda(player);
        if (!result.committed()) { fail(player, session, "旅途的初始赠礼尚未保存，请重试。", result.detail()); return false; }
        return true;
    }

    private static void nextGreeting(ServerPlayer player,Session session,Flow flow,int value) {
        var cursor=flow.cursor();
        if(cursor.waitTicksRemaining()!=0)return;
        int next=OriginalOpeningGreeting.next(cursor.commandIndex(),value);
        if(next==cursor.commandIndex())return;
        flow.cursor(OriginalOpeningGreeting.cursor(next));
        flow.checkpoint(point(player));
        changed(player,session);
        if(!save(player,session))return;
        if(OriginalOpeningGreeting.animation(next))sendGreetingAnimations(player,session,flow);
    }

    private static void sendGreetingAnimations(ServerPlayer player,Session session,Flow flow) {
        if(session.lease==null)return;
        for(var entity:player.serverLevel().getAllEntities()) {
            if(!(entity instanceof EntityOpeningActor actor)||!player.getUUID().equals(actor.ownerId())
                    ||actor.leaseSlot()!=session.lease.slot()||!OriginalOpeningGreeting.selectable(actor.originId())||actor.isRemoved())continue;
            int mask=1<<(actor.originId()-1);
            int frame=OriginalOpeningGreeting.animationFrame(flow.cursor().waitTicksRemaining(),actor.originId());
            if(frame<0||frame>=OriginalOpeningGreeting.ANIMATION_FRAMES||(session.greetingAnimations&mask)!=0)continue;
            var playback=UUID.nameUUIDFromBytes(("blacksouls:opening-greeting:"+session.token+":"+actor.originId()).getBytes(StandardCharsets.UTF_8));
            NetworkHandler.sendToPlayer(new ClientboundOriginalNpcAnimationPacket(playback,actor.getId(),player.level().dimension().location(),
                    OriginalOpeningGreeting.ANIMATION_ID,frame,1,actor.getX(),actor.getY(),actor.getZ(),frame==0,false),player);
            session.greetingAnimations|=mask;
        }
    }

    private static void normalizeCoda(ServerPlayer player,Flow flow) {
        if(aborting(flow))return;
        int command=flow.cursor().commandIndex();
        if(flow.stage()==Stage.ORIGIN_CONFIRMED&&!OriginalOpeningCoda.active(command))
            flow.cursor(originCodaCursor(OriginalOpeningCoda.migrateOrigin(command),flow.origin(),0));
        else if(flow.stage()==Stage.ALICE_SELECTION&&!OriginalOpeningCoda.active(command))flow.cursor(OriginalOpeningCoda.cursor(OriginalOpeningCoda.OP_SKIP,255));
        else if(flow.stage()==Stage.MEMORY_DIALOGUE&&!OriginalOpeningCoda.active(command))flow.cursor(OriginalOpeningCoda.cursor(OriginalOpeningCoda.memoryStart(flow.relationship()),0));
        if(flow.stage()==Stage.ORIGIN_CONFIRMED||flow.stage()==Stage.ALICE_SELECTION||flow.stage()==Stage.MEMORY_DIALOGUE)ensureNoH(player);
    }

    private static void ensureNoH(ServerPlayer player) {
        var progress=yuki135.blacksouls.capability.OriginalNpcProgress.get(player.server);
        if(!progress.switchValue(20)) {
            progress.setSwitch(20,true);
            player.server.overworld().getDataStorage().save();
        }
    }

    private static Cursor originCodaCursor(int command,int origin,int state) {
        var cursor=OriginalOpeningCoda.cursor(command,state);
        return command>=OriginalOpeningCoda.ORIGIN_ANIMATION&&command<=OriginalOpeningCoda.ORIGIN_FADE_IN
                ?new Cursor(7,origin+2,0,command,state,cursor.waitTicksRemaining()):cursor;
    }

    private static void enterCoda(ServerPlayer player,Session session,Flow flow,int command) {
        int visual=OriginalOpeningCoda.enterVisualState(command,flow.cursor().commandIndex(),flow.cursor().messageOffset());
        flow.cursor(originCodaCursor(command,flow.origin(),visual));
        session.codaEffectCommand=-1;session.codaRouteKey=-1;
        if(session.lease!=null)OriginalOpeningDialogueView.focusAlice(player,session.lease.slot(),command);
        flow.checkpoint(point(player));
        changed(player,session);
    }

    private static void nextCoda(ServerPlayer player,Session session,Flow flow,int value) {
        int command=flow.cursor().commandIndex();
        if(!OriginalOpeningCoda.active(command)||!OriginalOpeningCoda.acceptsInput(command)||flow.cursor().waitTicksRemaining()>0
                ||OriginalOpeningCoda.aliceChoice(command))return;
        int next=OriginalOpeningCoda.next(command,value);if(next<0)return;
        if(command==OriginalOpeningCoda.OP_SKIP) { if(value==1)flow.requestSkip();else flow.clearSkip(); }
        ensureNoH(player);
        enterCoda(player,session,flow,next);
        tickCoda(player,session,flow,false);
    }

    private static void tickCoda(ServerPlayer player,Session session,Flow flow,boolean advanceTime) {
        if(!OriginalOpeningCoda.active(flow.cursor().commandIndex()))return;
        ensureNoH(player);
        if(advanceTime) {
            var cursor=flow.cursor();
            int visual=OriginalOpeningCoda.advanceVisualState(cursor.commandIndex(),cursor.messageOffset(),3);
            if(visual!=cursor.messageOffset())flow.cursor(new Cursor(cursor.sourceMapId(),cursor.sourceEventId(),cursor.pageIndex(),cursor.commandIndex(),visual,cursor.waitTicksRemaining()));
        }
        boolean changed=false;
        for(int guard=0;guard<128;guard++) {
            var cursor=flow.cursor();var cue=OriginalOpeningCoda.cue(cursor.commandIndex());
            changed=sendCodaEffects(player,session,flow,cue)||changed;
            if(OriginalOpeningCoda.acceptsInput(cue.command())||cursor.waitTicksRemaining()>0) { if(changed)save(player,session);return; }
            if(cue.durationFrames()>0&&(cue.code()==212||cue.code()==213)) {
                var p=cue.parameters();var subject=codaSubject(player,session,p.get(0).getAsInt());
                if(cue.code()==212)NetworkHandler.sendToPlayer(new ClientboundOriginalNpcAnimationPacket(codaPlaybackId(session,cue.command()),subject.getId(),
                        player.level().dimension().location(),p.get(1).getAsInt(),Math.max(OriginalOpeningGreeting.ANIMATION_FRAMES,cue.durationFrames()),1,
                        subject.getX(),subject.getY(),subject.getZ(),false,false),player);
                else NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPresentationPacket(213,subject.getId(),player.level().dimension().location(),
                        List.of(p.get(1).getAsDouble(),0D),""),player);
            }
            if(cue.kind().equals("origin_transfer")) {
                OriginalOpeningActors.clear(player.serverLevel(),player.getUUID(),session.lease);
                var feet=feet(session.lease,SceneKind.OP,new Cell(10,9));
                var destination=new WorldPoint(DIMENSION.location().toString(),feet.x,feet.y,feet.z,0,0);
                if(!safe(player.serverLevel(),player,destination)) { notice(player,session,"正在等待石柱前的落脚点空出……");return; }
                player.connection.teleport(feet.x,feet.y,feet.z,0,0);player.setYHeadRot(0);player.setYBodyRot(0);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
                session.hold=destination;session.previousCell=new Cell(10,9);session.notice="";
                flow.checkpoint(destination);
                if(!save(player,session))return;
            }
            if(cue.kind().equals("coda_finish")) {
                if(!flow.preview())OriginalSenService.writeValue(player,100);
                if(!commitCodaReward(player,session))return;
                flow.advance(Stage.ALICE_SELECTION);flow.cursor(OriginalOpeningCoda.cursor(OriginalOpeningCoda.OP_SKIP,255));
                flow.checkpoint(point(player));session.entering=true;session.sceneReady=false;
                session.codaEffectCommand=-1;changed(player,session);save(player,session);return;
            }
            if(cue.kind().equals("memory_finish")) {
                OriginalOpeningActors.clear(player.serverLevel(),player.getUUID(),session.lease);
                flow.advance(Stage.FIRST_STEP);flow.cursor(new Cursor(110,9,0,0,0,0));flow.checkpoint(point(player));
                session.codaEffectCommand=-1;changed(player,session);save(player,session);return;
            }
            if(cue.next()==0)throw new IllegalStateException("Opening coda reached a missing terminal");
            enterCoda(player,session,flow,cue.next());changed=true;
        }
        throw new IllegalStateException("Opening coda exceeded its transition bound");
    }

    private static Entity codaSubject(ServerPlayer player,Session session,int target) {
        if(target==-1)return player;
        for(var entity:player.serverLevel().getAllEntities())if(entity instanceof EntityOpeningActor actor&&actor.originId()==0
                &&player.getUUID().equals(actor.ownerId())&&actor.leaseSlot()==session.lease.slot()&&!actor.isRemoved())return actor;
        return player;
    }

    private static UUID codaPlaybackId(Session session,int command) {
        return UUID.nameUUIDFromBytes(("blacksouls:opening-coda:"+session.token+":"+command).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean sendCodaEffects(ServerPlayer player,Session session,Flow flow,OriginalOpeningCoda.Cue cue) {
        var cursor=flow.cursor();int elapsed=OriginalOpeningCoda.elapsedFrames(cue.command(),cursor.waitTicksRemaining());
        boolean seen=(cursor.messageOffset()&OriginalOpeningCoda.EFFECT_PLAYED)!=0;
        boolean changed=false;
        if(session.codaEffectCommand!=cue.command()) {
            var p=cue.parameters();
            if(OriginalOpeningCoda.memory(cue.command())&&cue.code()==121&&p.get(0).getAsInt()<=103
                    &&p.get(1).getAsInt()>=103&&p.get(2).getAsInt()==0)
                OriginalOpeningActors.clear(player.serverLevel(),player.getUUID(),session.lease);
            if(OriginalOpeningCoda.memory(cue.command())) {
                var subject=codaSubject(player,session,6);
                if(subject instanceof EntityOpeningActor actor) {
                    float yaw=switch(OriginalOpeningCoda.aliceDirection(cue.command())) {case 2->0;case 4->90;case 6->270;default->180;};
                    actor.setYRot(yaw);actor.setYHeadRot(yaw);actor.setYBodyRot(yaw);
                }
            }
            if(cue.code()==212) {
                var subject=codaSubject(player,session,p.get(0).getAsInt());
                if(cue.durationFrames()==0||elapsed<cue.durationFrames())NetworkHandler.sendToPlayer(new ClientboundOriginalNpcAnimationPacket(codaPlaybackId(session,cue.command()),subject.getId(),
                        player.level().dimension().location(),p.get(1).getAsInt(),seen?Math.max(1,elapsed):elapsed,1,
                        subject.getX(),subject.getY(),subject.getZ(),!seen&&elapsed==0,false),player);
            } else if(cue.code()==213) {
                var subject=codaSubject(player,session,p.get(0).getAsInt());int remaining=Math.max(0,76-elapsed);
                if(remaining>0)NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPresentationPacket(213,subject.getId(),player.level().dimension().location(),List.of(p.get(1).getAsDouble(),(double)remaining),""),player);
            } else if((cue.code()==241||cue.code()==242)&&!seen) {
                var parameters=new java.util.ArrayList<com.google.gson.JsonElement>();p.forEach(parameters::add);
                OriginalNpcPresentationService.apply(player,player,cue.code(),parameters);
            }
            session.codaEffectCommand=cue.command();
            flow.cursor(new Cursor(cursor.sourceMapId(),cursor.sourceEventId(),cursor.pageIndex(),cursor.commandIndex(),cursor.messageOffset()|OriginalOpeningCoda.EFFECT_PLAYED,cursor.waitTicksRemaining()));
            changed=!seen;
        }
        if(cue.code()==205)applyCodaRoute(player,session,cue,elapsed);
        return changed;
    }

    private static void applyCodaRoute(ServerPlayer player,Session session,OriginalOpeningCoda.Cue cue,int elapsed) {
        var p=cue.parameters();var subject=codaSubject(player,session,p.get(0).getAsInt());
        int frame=0,direction=0;double jump=0;
        for(var value:p.get(1).getAsJsonObject().getAsJsonArray("list")) {
            var move=value.getAsJsonObject();int code=move.get("code").getAsInt();var args=move.getAsJsonArray("parameters");
            if(code==0)break;
            int duration=code==15?args.get(0).getAsInt():code==14?14:1;
            if(code>=16&&code<=19&&elapsed>=frame)direction=switch(code) {case 16->2;case 17->4;case 18->6;default->8;};
            if(code==14&&elapsed>=frame&&elapsed<frame+duration) {
                double count=duration-(elapsed-frame);jump=(49-Math.pow(Math.abs(count-7),2))/64D;
            }
            frame+=duration;
        }
        if(direction!=0) {
            int key=cue.command()*10+direction;
            if(session.codaRouteKey!=key) {
                float yaw=switch(direction) {case 2->0;case 4->90;case 6->270;default->180;};
                subject.setYRot(yaw);
                if(subject instanceof net.minecraft.world.entity.LivingEntity living) { living.setYHeadRot(yaw);living.setYBodyRot(yaw); }
                if(subject==player)player.connection.teleport(player.getX(),player.getY(),player.getZ(),yaw,player.getXRot());
                session.codaRouteKey=key;
            }
        }
        if(subject instanceof EntityOpeningActor actor&&session.lease!=null) {
            var anchor=aliceAnchor(session.lease,SceneKind.ISLAND_110).feet();actor.setPos(anchor.x,anchor.y+jump,anchor.z);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        for (var player : List.copyOf(server.getPlayerList().getPlayers())) {
            var data = stats(player);
            if(data!=null&&!data.openingProfile().hasRejectedData()&&!data.openingProfile().previewActive()&&data.openingProfile().realInitialRoomSilent()) {
                String scene=player.getPersistentData().getString("Blacksouls2CurrentScene");
                if(!scene.isEmpty()&&!scene.equals("blacksouls:hokoniwa#map_001")&&data.openingProfile().realStage().orElse(null)==Stage.COMPLETE) {
                    data.openingProfile().setRealInitialRoomSilent(false);
                    if(OriginalOpeningPersistence.saveAndVerify(player))sendRoomPolicy(player,false);
                }
            }
            if (data != null && !data.openingProfile().hasRejectedData() && (managed(player)
                    || data.openingProfile().readState() == ReadState.REAL && data.openingProfile().realSlot().isPresent())) resumePlayer(player);
        }
        var sessions = SESSIONS.get(server);
        if (sessions == null || sessions.isEmpty()) return;
        var owners = List.copyOf(sessions.keySet());
        int start = Math.floorMod(ROUND_ROBIN.getOrDefault(server,0), owners.size());
        ROUND_ROBIN.put(server, start + 1);
        int budget = WORLD_WRITE_BUDGET;
        for (int index = 0; index < owners.size(); index++) {
            var owner = owners.get((start + index) % owners.size());
            var session = sessions.get(owner);
            var player = server.getPlayerList().getPlayer(owner);
            if (session == null) continue;
            if (player == null) { release(session); sessions.remove(owner); continue; }
            try {
                if (stats(player) == null || stats(player).openingProfile().hasRejectedData()) {
                    release(session); sessions.remove(owner); continue;
                }
                if (session.profile != stats(player).openingProfile()) {
                    session.profile = stats(player).openingProfile();
                    stabilize(flow(player));
                    normalizeCoda(player,flow(player));
                    session.entering = !session.cleanup;
                    session.awaitingReady=false;
                    session.sceneReady = false;
                    session.motion=null;
                    session.fallReady=false;
                    stopFall(player);
                    session.removeTemporaryAfterReady=false;
                    session.focus=0;
                    session.focusActor=null;
                    session.hold=point(player);
                    changed(player,session);
                }
                budget -= tickPlayer(player, session, Math.min(64,budget));
            } catch (RuntimeException failure) {
                fail(player, session, "序章停在了安全节点，请重试。", failure.toString());
            }
        }
    }

    private static int tickPlayer(ServerPlayer player, Session session, int budget) {
        var flow = flow(player);
        int fallWrites=0;
        refreshTickets(session);
        if (session.cleanup) {
            if (session.error) { if(flow.preview())hold(player,session);periodic(player,session);return 0; }
            return clean(player, session, flow, budget);
        }
        if (session.error) { hold(player, session); periodic(player, session); return 0; }
        if (aborting(flow)) { prepareReturn(player, session, flow); hold(player, session); periodic(player, session); return 0; }
        if (flow.stage() == Stage.ARRIVAL_PENDING) {
            ensureArrivalCinematic(player,session,flow);
            var cinematic=flow.cinematic().orElseThrow();
            if(cinematic.awaitingDestination()||flow.pending().isPresent()||session.awaitingReady)prepareArrival(player,session,flow);
            else {
                var destination=flow.preview()?flow.profile.previewReturnPoint():OriginalOpeningArrival.destination();
                var destinationLevel=level(player.server,destination);
                if(destinationLevel==null||player.level()!=destinationLevel||player.position().distanceToSqr(new Vec3(destination.x(),destination.y(),destination.z()))>.16D
                        ||!destinationSafe(destinationLevel,player,destination,flow)) {
                    notice(player,session,"正在等待抵达地点安全就绪……");hold(player,session);periodic(player,session);return 0;
                }
                if(!session.notice.isEmpty())notice(player,session,"");
                tickCinematic(player,session,flow);
                if(flow.cinematic().orElseThrow().finished())finishArrival(player,session,flow);
            }
            hold(player, session);
            periodic(player, session);
            return 0;
        }
        if(!session.entering&&!session.awaitingReady&&needsOpRetirement(player,session)) {
            session.entering=true;session.sceneReady=false;
            changed(player,session);
        }
        if (session.entering) {
            int used = enterScene(player, session, flow, budget);
            hold(player, session);
            periodic(player, session);
            return used;
        }
        if (session.awaitingReady) {
            int used=retireOp(player,session,budget);
            hold(player,session);periodic(player,session);return used;
        }
        if(flow.stage()==Stage.COLLAPSE1||flow.stage()==Stage.COLLAPSE2||flow.stage()==Stage.COLLAPSE3||flow.stage()==Stage.FALLING) {
            ensureStageCinematic(player,session,flow);
            tickCinematic(player,session,flow);
        }
        var cursor = flow.cursor();
        if (cursor.waitTicksRemaining() > 0) flow.cursor(new Cursor(cursor.sourceMapId(),cursor.sourceEventId(),cursor.pageIndex(),cursor.commandIndex(),cursor.messageOffset(),cursor.waitTicksRemaining()-1));
        if (session.motion != null) { move(player, session, flow); periodic(player, session); return 0; }
        switch (flow.stage()) {
            case NAME_PENDING -> {
                hold(player, session);
                if (StoryNameData.isConfirmed(player)) {
                    named(flow);
                    flow.checkpoint(point(player));
                    changed(player,session);
                    save(player,session);
                    break;
                }
                if (!session.nameSent) {
                    session.nameSent = true;
                    NetworkHandler.sendToPlayer(new ClientboundStoryNamePacket(true,""), player);
                }
            }
            case ORIGIN_SELECTION -> {
                if(OriginalOpeningGreeting.active(flow.cursor().commandIndex())) {
                    hold(player,session);
                    if(OriginalOpeningGreeting.animation(flow.cursor().commandIndex())) {
                        sendGreetingAnimations(player,session,flow);
                        if(flow.cursor().waitTicksRemaining()==0) {
                            flow.cursor(OriginalOpeningGreeting.cursor(OriginalOpeningGreeting.LAST_COMMAND));
                            flow.checkpoint(point(player));
                            changed(player,session);
                            save(player,session);
                        }
                    }
                } else if(session.focus>0)hold(player,session);
                else guardMovement(player,session,flow);
            }
            case ORIGIN_CONFIRMED -> {
                hold(player, session);
                if (!flow.preview() && !flow.profile.realKitGrantCommitted()) {
                    if (!commitOriginKit(player, session)) return 0;
                    flow = flow(player);
                }
                normalizeCoda(player,flow);tickCoda(player,session,flow,true);
            }
            case ALICE_SELECTION -> { hold(player,session);normalizeCoda(player,flow);tickCoda(player,session,flow,true); }
            case MEMORY_DIALOGUE -> {
                hold(player,session);normalizeCoda(player,flow);tickCoda(player,session,flow,true);
            }
            case FIRST_STEP -> guardFirstStepMovement(player,session,flow);
            case COLLAPSE1,COLLAPSE2 -> guardMovement(player,session,flow);
            case COLLAPSE3 -> {
                hold(player,session);
                if(flow.cinematic().orElseThrow().finished()) {
                    flow.checkpoint(point(player));
                    var cinematic=flow.cinematic().orElseThrow();
                    flow.clearCinematic();
                    flow.advance(Stage.FALLING);
                    cinematic.changeProgram("finale99");flow.cinematic(cinematic);
                    flow.cursor(new Cursor(99,2,0,0,0,0));
                    changed(player,session);
                    if (!save(player,session)) return 0;
                    fallWrites=prepareFall(player,session,flow,budget);
                    if (!session.fallReady) {
                        session.entering=true;session.sceneReady=false;
                        periodic(player,session);return fallWrites;
                    }
                }
            }
            case FALLING -> {
                if (!flow.skip()&&!moveFall(player,session,flow)) { periodic(player,session);return fallWrites; }
                if(flow.cinematic().orElseThrow().awaitingDestination()) {
                    flow.advance(Stage.ARRIVAL_PENDING);
                    flow.cursor(new Cursor(1,0,0,0,0,0));
                    session.entering=true;
                    changed(player,session);save(player,session);
                }
            }
            default -> { }
        }
        flow = flow(player);
        if (flow.skip() && flow.origin() > 0 && flow.relationship() > 0 && flow.stage().ordinal() >= Stage.MEMORY_DIALOGUE.ordinal()
                && flow.stage().ordinal() <= Stage.FALLING.ordinal()) {
            session.motion=null;
            var cinematic=flow.cinematic().orElse(null);
            flow.clearCinematic();
            flow.skipToArrival();
            if(cinematic==null)cinematic=OriginalOpeningCinematic.start("arrival");
            cinematic.skip();flow.cinematic(cinematic);
            session.entering=true;
            changed(player,session);
            save(player,session);
        }
        periodic(player, session);
        return fallWrites;
    }

    private static int enterScene(ServerPlayer player, Session session, Flow flow, int budget) {
        var level = player.server.getLevel(DIMENSION);
        if (level == null) { notice(player,session,"序章空间尚未就绪，正在等待。"); return 0; }
        var data = OriginalOpeningSceneData.get(level);
        UUID identity = flow.preview() ? previewIdentity(player.getUUID()) : player.getUUID();
        var lease = data.lease(identity);
        if (flow.slot().isPresent() && flow.slot().getAsLong() != lease.slot()) throw new IllegalStateException("Opening slot disagrees with its canonical lease");
        if (flow.slot().isEmpty()) { flow.assign(lease.slot()); if (!save(player,session)) return 0; }
        session.lease=lease;
        session.cinematicOrigin=origin(lease,SceneKind.ISLAND_110);
        var kind = kind(flow.stage());
        tickets(session,level,OriginalOpeningSceneWorld.requiredChunks(lease));
        if (!loaded(level,OriginalOpeningSceneWorld.requiredChunks(lease))) { notice(player,session,"正在准备序章场景……"); return 0; }
        if (flow.stage()==Stage.FALLING) {
            int used=prepareFall(player,session,flow,budget);
            if (!session.fallReady) return used;
            var destination=fallPoint(flow,lease);
            if (!fallDestinationSafe(level,player,destination,flow,session)) {
                notice(player,session,"坠落路径暂时被占用，正在等待安全位置。");return used;
            }
            if (flow.pending().isPresent()&&!flow.pending().orElseThrow().destination().equals(destination)) {
                var pending=flow.pending().orElseThrow();var anchor=fallAnchor(flow,lease);
                if (pending.targetSourceMapId()!=99||pending.targetStage()!=Stage.FALLING||!pending.destination().equals(anchor)) throw new IllegalStateException("Opening fall transfer disagrees with persisted source progress");
                flow.clearPending(pending);
            }
            transfer(player,session,flow,99,destination);
            session.scene=SceneKind.ISLAND_99;
            session.previousCell=sourceCell(lease,SceneKind.ISLAND_99,new Vec3(destination.x(),destination.y(),destination.z()));
            session.entering=false;
            return used;
        }
        var view=data.view(identity,lease).orElseThrow();
        if(kind.island()&&view.journal()!=null&&!view.journal().restore()&&view.journal().zone()==SceneKind.OP&&!view.journal().targetOp()) {
            session.scene=kind;
            int used=retireOp(player,session,budget);
            if(needsOpRetirement(player,session)||session.error)return used;
            return used;
        }
        Set<Cell> temporary = Set.of();
        Cell landingCell = desiredCell(flow,lease,kind);
        if (kind.island() && !sourceSupport(kind,landingCell) && sourcePit(kind,landingCell)) temporary=Set.of(landingCell);
        if (view.journal()==null && view.island()==kind && !view.temporarySupports().isEmpty()
                && DIMENSION.equals(player.level().dimension()) && sourceSupport(kind,landingCell)) {
            Cell current=null;
            try { current=sourceCell(lease,kind,player.position()); } catch (IllegalArgumentException ignored) { }
            if (current!=null && view.temporarySupports().contains(current)) {
                var refuge=checkpoint(plan(lease,kind,view.temporarySupports()),landingCell);
                if (!OriginalOpeningSceneWorld.verifyCheckpoint(level,identity,refuge,player)) return 0;
                transfer(player,session,flow,kind.sourceMap(),new WorldPoint(DIMENSION.location().toString(),refuge.feet().x,refuge.feet().y,refuge.feet().z,0,0));
                session.scene=kind;
                session.previousCell=landingCell;
                session.removeTemporaryAfterReady=true;
                session.entering=false;
                return 0;
            }
        }
        int used=0;
        if (!session.sceneReady) {
            OriginalOpeningActors.clear(level,player.getUUID(),lease);
            OriginalOpeningSceneWorld.Result result;
            if (view.journal()!=null) {
                var journal=view.journal();
                if (journal.restore() || journal.zone()!=kind || kind!=SceneKind.OP && (journal.targetIsland()!=kind || !journal.temporarySupports().equals(temporary))) {
                    throw new IllegalStateException("Unmatched saved opening scene journal");
                }
                if (budget==0) return 0;
                result=OriginalOpeningSceneWorld.advance(level,identity,lease,journal.id(),budget);
                used=result.status()==OriginalOpeningSceneWorld.Status.WRITE_FAILED?budget:result.status()==OriginalOpeningSceneWorld.Status.COMPLETE?Math.min(budget,journal.total()-journal.cursor()):Math.max(0,result.cursor()-journal.cursor());
            } else result=OriginalOpeningSceneWorld.beginApply(level,identity,lease,kind,temporary);
            if (result.status()==OriginalOpeningSceneWorld.Status.PROGRESS || result.status()==OriginalOpeningSceneWorld.Status.WAIT_CHUNKS) return Math.min(budget,used);
            if (result.status()!=OriginalOpeningSceneWorld.Status.COMPLETE && result.status()!=OriginalOpeningSceneWorld.Status.ALREADY_READY) {
                fail(player,session,"序章场景暂时不能安全更新，请重试。",sceneFailure(level,result));
                return Math.min(budget,used);
            }
            session.sceneReady=true;
        }
        var checkpoint=checkpoint(plan(lease,kind,temporary),landingCell);
        if (!OriginalOpeningSceneWorld.verifyCheckpoint(level,identity,checkpoint,player)) {
            notice(player,session,"落脚点被占用，正在等待安全位置。");
            return Math.min(budget,used);
        }
        boolean alice=kind==SceneKind.ISLAND_110 && (flow.stage()==Stage.ALICE_SELECTION || flow.stage()==Stage.MEMORY_DIALOGUE)
                &&OriginalOpeningCoda.aliceVisible(flow.cursor().commandIndex());
        boolean professions=flow.stage()!=Stage.ORIGIN_CONFIRMED||OriginalOpeningCoda.professionsPresent(flow.cursor().commandIndex());
        if (!OriginalOpeningActors.ensure(player,lease,kind,flow.relationship(),alice,professions)) {
            notice(player,session,"序章人物尚未就绪，请稍候。");
            return Math.min(budget,used);
        }
        var saved=flow.pending().map(PendingTransfer::destination).orElseGet(()->flow.checkpoint().position().orElse(null));
        float yaw=saved==null?(kind.island()?180:0):saved.yaw(),pitch=saved==null?0:saved.pitch();
        var destination=new WorldPoint(DIMENSION.location().toString(),checkpoint.feet().x,checkpoint.feet().y,checkpoint.feet().z,yaw,pitch);
        transfer(player,session,flow,kind.sourceMap(),destination);
        session.scene=kind;
        session.previousCell=landingCell;
        session.entering=false;
        return Math.min(budget,used);
    }

    private static String sceneFailure(ServerLevel level, OriginalOpeningSceneWorld.Result result) {
        return result.status().name()+(result.conflict()==null?"":" at "+result.conflict()+" actual="+level.getBlockState(result.conflict()));
    }

    private static Cell desiredCell(Flow flow, OriginalOpeningSceneData.Lease lease, SceneKind kind) {
        if (flow.pending().isPresent()) {
            var pending=flow.pending().get();
            if (pending.targetSourceMapId()!=kind.sourceMap() || pending.targetStage()!=flow.stage()
                    || !pending.destination().dimension().equals(DIMENSION.location().toString())) throw new IllegalStateException("Opening pending transfer does not match current source stage");
            var destination=pending.destination();
            return sourceCell(lease,kind,new Vec3(destination.x(),destination.y(),destination.z()));
        }
        var checkpoint=flow.checkpoint();
        if (checkpoint.stage()==flow.stage() && checkpoint.position().isPresent()) {
            var position=checkpoint.position().get();
            if (position.dimension().equals(DIMENSION.location().toString())) {
                try { return sourceCell(lease,kind,new Vec3(position.x(),position.y(),position.z())); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return kind==SceneKind.OP?new Cell(10,9):kind==SceneKind.ISLAND_110?new Cell(15,12):new Cell(15,13);
    }

    private static void transfer(ServerPlayer player, Session session, Flow flow, int map, WorldPoint destination) {
        if (session.awaitingReady) return;
        var pending=new PendingTransfer(map,flow.stage(),destination);
        if (flow.pending().isPresent() && !flow.pending().get().equals(pending)) throw new IllegalStateException("Opening transfer destination changed without acknowledgment");
        if (flow.pending().isEmpty()) { flow.pending(pending); if (!save(player,session)) return; }
        var level=level(player.server,destination);
        if (level==null) return;
        player.stopRiding();
        if (flow.stage()==Stage.FALLING) freezeFall(player);else stopFall(player);
        if(flow.stage()==Stage.ARRIVAL_PENDING&&!flow.preview()&&!OriginalOpeningBonfireFlow.prepareFallingRoom(player))return;
        player.teleportTo(level,destination.x(),destination.y(),destination.z(),destination.yaw(),destination.pitch());
        if (player.level()!=level || player.position().distanceToSqr(new Vec3(destination.x(),destination.y(),destination.z()))>1.0E-6D) {
            fail(player,session,"暂时无法前往目的地，进度已保留，请重试。","opening_transfer_not_completed");
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance=0;
        flow.cinematic().ifPresent(OriginalOpeningCinematic::touch);
        session.hold=destination;
        session.awaitingReady=true;
        session.notice="";
        changed(player,session);
    }

    private static void acceptReady(ServerPlayer player, Session session, Flow flow) {
        if (!session.awaitingReady || flow.pending().isEmpty()) return;
        if(DIMENSION.equals(player.level().dimension())&&needsOpRetirement(player,session))return;
        if (flow.stage()==Stage.FALLING&&flow.cinematic().isEmpty()) {
            stabilize(flow);
            session.awaitingReady=false;
            session.entering=true;
            session.sceneReady=false;
            changed(player,session);
            save(player,session);
            return;
        }
        var pending=flow.pending().get();
        var destination=pending.destination();
        var level=level(player.server,destination);
        if (level==null || player.level()!=level || player.position().distanceToSqr(new Vec3(destination.x(),destination.y(),destination.z()))>.16D
                || !destinationSafe(level,player,destination,flow)) return;
        if (flow.stage()==Stage.ARRIVAL_PENDING && !flow.preview() && !OriginalOpeningArrival.acknowledge(player,destination)) return;
        flow.clearPending(pending);
        session.awaitingReady=false;
        session.entering=false;
        if(flow.stage()!=Stage.FALLING)flow.checkpoint(destination);
        if (session.removeTemporaryAfterReady) {
            session.removeTemporaryAfterReady=false;
            session.entering=true;
            session.sceneReady=false;
            changed(player,session);
            save(player,session);
            return;
        }
        if (aborting(flow)) {
            session.cleanup=true;
        } else if (flow.stage()==Stage.ARRIVAL_PENDING) {
            ensureArrivalCinematic(player,session,flow);
            if(!flow.preview())flow.profile.setRealInitialRoomSilent(true);
            flow.cinematic().orElseThrow().destinationReady();
        } else if (flow.stage()!=Stage.FALLING && session.scene!=null && session.lease!=null && session.scene.island()) {
            var data=OriginalOpeningSceneData.get(player.serverLevel());
            var view=data.view(session.lease.owner(),session.lease).orElseThrow();
            var cell=sourceCell(session.lease,session.scene,player.position());
            if (view.temporarySupports().contains(cell)) {
                var refuge=safeSpawn(session.lease,session.scene);
                if (!OriginalOpeningSceneWorld.verifyCheckpoint(player.serverLevel(),session.lease.owner(),refuge,player)) {
                    fail(player,session,"暂时无法离开破碎的落脚点，请重试。","opening_temporary_landing_refuge_occupied");
                    save(player,session);
                    return;
                }
                var feet=refuge.feet();
                session.removeTemporaryAfterReady=true;
                session.previousCell=refuge.sourceCell();
                transfer(player,session,flow,session.scene.sourceMap(),new WorldPoint(DIMENSION.location().toString(),feet.x,feet.y,feet.z,0,0));
                save(player,session);
                return;
            }
        }
        if(session.lease!=null&&OriginalOpeningDialogueView.focusAlice(player,session.lease.slot(),flow.cursor().commandIndex()))flow.checkpoint(point(player));
        changed(player,session);
        save(player,session);
    }

    private static boolean needsOpRetirement(ServerPlayer player,Session session) {
        if(session.lease==null||session.scene==null||!session.scene.island())return false;
        var level=player.server.getLevel(DIMENSION);
        return level!=null&&OriginalOpeningSceneData.get(level).view(session.lease.owner(),session.lease).map(OriginalOpeningSceneData.View::opBuilt).orElse(false);
    }

    private static int retireOp(ServerPlayer player,Session session,int budget) {
        if(!needsOpRetirement(player,session))return 0;
        var level=player.server.getLevel(DIMENSION);
        var result=OriginalOpeningSceneWorld.beginRetireOp(level,session.lease.owner(),session.lease);
        if(result.status()==OriginalOpeningSceneWorld.Status.WAIT_CHUNKS)return 0;
        if(result.status()==OriginalOpeningSceneWorld.Status.ALREADY_READY)return 0;
        if(result.status()!=OriginalOpeningSceneWorld.Status.PROGRESS&&result.status()!=OriginalOpeningSceneWorld.Status.COMPLETE) {
            fail(player,session,"先前的出身场景暂时不能安全收起，请重试。",sceneFailure(level,result));return 0;
        }
        if(budget==0)return 0;
        int before=result.cursor();
        result=OriginalOpeningSceneWorld.advance(level,session.lease.owner(),session.lease,result.journal(),budget);
        int used=result.status()==OriginalOpeningSceneWorld.Status.WRITE_FAILED?budget:Math.max(0,result.cursor()-before);
        if(result.status()==OriginalOpeningSceneWorld.Status.COMPLETE) {
            for(var entity:level.getAllEntities())if(entity instanceof EntityOpeningActor actor&&player.getUUID().equals(actor.ownerId())&&actor.leaseSlot()==session.lease.slot()&&actor.originId()>0)actor.discard();
        } else if(result.status()!=OriginalOpeningSceneWorld.Status.PROGRESS&&result.status()!=OriginalOpeningSceneWorld.Status.WAIT_CHUNKS) {
            fail(player,session,"先前的出身场景暂时不能安全收起，请重试。",sceneFailure(level,result));
        }
        return Math.min(budget,used);
    }

    private static void ensureStageCinematic(ServerPlayer player,Session session,Flow flow) {
        if(flow.cinematic().isPresent())return;
        String program=switch(flow.stage()) {
            case COLLAPSE1->"collapse110";case COLLAPSE2->"collapse97";case COLLAPSE3->"collapse98";case FALLING->"finale99";
            default->throw new IllegalStateException("No opening cinematic at stage");
        };
        flow.cinematic(OriginalOpeningCinematic.start(program));
        session.presentationRestoring=flow.stage()==Stage.FALLING;
        save(player,session);changed(player,session);
    }

    private static void ensureArrivalCinematic(ServerPlayer player,Session session,Flow flow) {
        if(flow.cinematic().isPresent())return;
        var cinematic=OriginalOpeningCinematic.start("arrival");
        if(flow.skip())cinematic.skip();
        flow.cinematic(cinematic);session.presentationRestoring=true;
        save(player,session);changed(player,session);
    }

    private static void tickCinematic(ServerPlayer player,Session session,Flow flow) {
        var cinematic=flow.cinematic().orElse(null);
        if(cinematic==null||cinematic.finished())return;
        int command=cinematic.command();
        if(!cinematic.tick())return;
        if(flow.stage()!=Stage.FALLING&&(command!=cinematic.command()||player.server.getTickCount()%20==0))if(!save(player,session))return;
        if(flow.stage()!=Stage.ARRIVAL_PENDING||!cinematic.finished())sendCinematic(player,session,flow,false);
    }

    private static int prepareFall(ServerPlayer player, Session session, Flow flow, int budget) {
        session.fallReady=false;
        if (session.lease==null || !DIMENSION.equals(player.level().dimension())) return 0;
        var anchor=fallAnchor(flow,session.lease);
        var cell=sourceCell(session.lease,SceneKind.ISLAND_99,new Vec3(anchor.x(),anchor.y(),anchor.z()));
        var level=player.serverLevel();var owner=session.lease.owner();
        var view=OriginalOpeningSceneData.get(level).view(owner,session.lease).orElseThrow();
        OriginalOpeningSceneWorld.Result result;
        int used=0;
        if (view.journal()!=null) {
            if (budget==0) return 0;
            var journal=view.journal();
            result=OriginalOpeningSceneWorld.advanceFall(level,owner,session.lease,journal.id(),budget,player);
            used=result.status()==OriginalOpeningSceneWorld.Status.WRITE_FAILED?budget:Math.max(0,result.cursor()-journal.cursor());
        } else {
            result=OriginalOpeningSceneWorld.beginFall(level,owner,session.lease,cell,player);
            if (result.status()==OriginalOpeningSceneWorld.Status.PROGRESS && budget>0) {
                result=OriginalOpeningSceneWorld.advanceFall(level,owner,session.lease,result.journal(),budget,player);
                used=result.status()==OriginalOpeningSceneWorld.Status.WRITE_FAILED?budget:result.cursor();
            }
        }
        if (result.status()==OriginalOpeningSceneWorld.Status.PROGRESS||result.status()==OriginalOpeningSceneWorld.Status.WAIT_CHUNKS||result.status()==OriginalOpeningSceneWorld.Status.OCCUPIED) {
            notice(player,session,"正在准备安全的坠落路径……");return used;
        }
        if (result.status()!=OriginalOpeningSceneWorld.Status.COMPLETE&&result.status()!=OriginalOpeningSceneWorld.Status.ALREADY_READY) {
            fail(player,session,"坠落路径暂时不能安全更新，请重试。",sceneFailure(level,result));return used;
        }
        session.fallReady=OriginalOpeningSceneWorld.fallReady(level,owner,session.lease,cell,player);
        if (session.fallReady) freezeFall(player);
        return used;
    }

    private static WorldPoint fallAnchor(Flow flow, OriginalOpeningSceneData.Lease lease) {
        var stable=flow.checkpoint();var anchor=stable.position().orElseThrow(()->new IllegalStateException("Opening fall has no stable anchor"));
        if (stable.stage()!=Stage.COLLAPSE3 || !anchor.dimension().equals(DIMENSION.location().toString()) || Math.abs(anchor.y()-FEET_Y)>1.0E-6D) throw new IllegalStateException("Opening fall anchor is not the final stable island");
        var cell=sourceCell(lease,SceneKind.ISLAND_99,new Vec3(anchor.x(),anchor.y(),anchor.z()));
        var center=feet(lease,SceneKind.ISLAND_99,cell);
        if (Math.abs(anchor.x()-center.x)>.19D||Math.abs(anchor.z()-center.z)>.19D) throw new IllegalStateException("Opening fall body escapes its single owned shaft");
        return anchor;
    }

    private static WorldPoint fallPoint(Flow flow, OriginalOpeningSceneData.Lease lease) {
        var anchor=fallAnchor(flow,lease);
        double distance=OriginalOpeningFall.distance(OriginalOpeningFall.frames(flow.cinematic().orElseThrow()));
        return new WorldPoint(anchor.dimension(),anchor.x(),anchor.y()-distance,anchor.z(),anchor.yaw(),anchor.pitch());
    }

    private static boolean fallDestinationSafe(ServerLevel level, ServerPlayer player, WorldPoint destination, Flow flow, Session session) {
        if (session==null||session.lease==null||!session.fallReady||!DIMENSION.equals(level.dimension())) return false;
        var expected=fallPoint(flow,session.lease);
        if (!expected.equals(destination)) return false;
        var cell=sourceCell(session.lease,SceneKind.ISLAND_99,new Vec3(destination.x(),destination.y(),destination.z()));
        if (!OriginalOpeningSceneWorld.fallReady(level,session.lease.owner(),session.lease,cell,player)) return false;
        var body=new AABB(destination.x()-.3D,destination.y(),destination.z()-.3D,destination.x()+.3D,destination.y()+1.8D,destination.z()+.3D);
        var bounds=bounds(session.lease,SceneKind.ISLAND_99);
        return body.minX>=bounds.minX&&body.maxX<=bounds.maxX&&body.minZ>=bounds.minZ&&body.maxZ<=bounds.maxZ
                && body.minY>=bounds.minY&&body.maxY<=bounds.maxY&&level.noCollision(player,body)&&!level.containsAnyLiquid(body)
                && level.getEntities(player,body,entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty()
                && level.getServer().getPlayerList().getPlayers().stream().noneMatch(other->other!=player&&other.level()==level&&other.isAlive()&&!other.isSpectator()&&other.getBoundingBox().intersects(body));
    }

    private static boolean moveFall(ServerPlayer player, Session session, Flow flow) {
        var destination=fallPoint(flow,session.lease);
        if (!fallDestinationSafe(player.serverLevel(),player,destination,flow,session)) {
            hold(player,session);fail(player,session,"坠落路径暂时被占用，请重试。","opening_controlled_fall_obstructed");return false;
        }
        var target=new Vec3(destination.x(),destination.y(),destination.z());
        var swept=player.getBoundingBox().minmax(new AABB(target.x-.3D,target.y,target.z-.3D,target.x+.3D,target.y+1.8D,target.z+.3D));
        if (!player.serverLevel().noCollision(player,swept)||player.serverLevel().containsAnyLiquid(swept)) {
            hold(player,session);fail(player,session,"坠落路径暂时被占用，请重试。","opening_controlled_fall_sweep_obstructed");return false;
        }
        freezeFall(player);
        if (player.position().distanceToSqr(target)>1.0E-12D||player.getYRot()!=destination.yaw()||player.getXRot()!=destination.pitch()) {
            player.connection.teleport(destination.x(),destination.y(),destination.z(),destination.yaw(),destination.pitch());
        }
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        session.hold=destination;
        int command=flow.cinematic().orElseThrow().command();
        if (session.fallSavedCommand!=command||player.server.getTickCount()%20==0) {
            session.fallSavedCommand=command;
            if (!save(player,session)) return false;
        }
        return true;
    }

    private static void freezeFall(ServerPlayer player) {
        var tag=player.getPersistentData();
        if (!tag.contains(FALL_GRAVITY,net.minecraft.nbt.Tag.TAG_BYTE)) tag.putBoolean(FALL_GRAVITY,player.isNoGravity());
        player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
    }

    private static void stopFall(ServerPlayer player) {
        var tag=player.getPersistentData();
        if (!tag.contains(FALL_GRAVITY,net.minecraft.nbt.Tag.TAG_BYTE)) return;
        player.setNoGravity(tag.getBoolean(FALL_GRAVITY));tag.remove(FALL_GRAVITY);
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
    }

    private static void finishArrival(ServerPlayer player,Session session,Flow flow) {
        if(!flow.cinematic().orElseThrow().finished()||flow.pending().isPresent()||session.awaitingReady)return;
        if(!flow.preview()) {
            if(!OriginalOpeningArrival.ready(player.serverLevel(),player,OriginalOpeningArrival.destination()))return;
            var result=OriginalOpeningStartingKit.arrivalCleanup(player);
            if(!result.committed()) { fail(player,session,"序章抵达状态尚未保存，请重试。",result.detail());return; }
            flow=flow(player);
            if(!OriginalOpeningBonfireFlow.arriveAtFallingRoom(player)) {
                fail(player,session,"坠落之间的篝火尚未就绪，请重试。","opening_arrival_bonfire_unavailable");return;
            }
            StatEventHandler.applyStats(player);player.setHealth(player.getMaxHealth());
            flow.cursor(new Cursor(1,0,0,0,0,0));flow.region(1,0);
        }
        session.completedCinematic=flow.cinematic().orElseThrow().copy();session.completedPreview=flow.preview();
        flow.cinematic().orElseThrow().touch();session.completedCinematic=flow.cinematic().orElseThrow().copy();
        flow.advance(Stage.COMPLETE);flow.checkpoint(point(player));
        if(!flow.preview())StatEventHandler.finishOpeningLoginItems(player);
        if(!save(player,session))return;
        session.cleanup=true;changed(player,session);
    }

    private static void sendCinematic(ServerPlayer player,Session session,Flow flow,boolean finalPacket) {
        var cinematic=finalPacket?session.completedCinematic:flow.cinematic().orElse(null);
        if(cinematic==null) {
            if(!finalPacket)return;
            cinematic=OriginalOpeningCinematic.start("arrival");
        }
        if(finalPacket) {
            cinematic=cinematic.copy();
            if(!cinematic.finished() || !cinematic.program().equals("arrival")&&!cinematic.program().equals("finale99")) {
                cinematic.changeProgram("arrival");
                cinematic.destinationReady();
                for(int tick=0;tick<31&&!cinematic.finished();tick++)cinematic.tick();
                if(!cinematic.finished())throw new IllegalStateException("Opening terminal bridge did not finish");
            }
            cinematic.touch();
        }
        boolean finished=finalPacket||flow.stage()==Stage.COMPLETE&&cinematic.finished();
        boolean preview=finalPacket?session.completedPreview:flow.preview();
        if(!finalPacket&&flow.slot().isPresent())session.cinematicOrigin=origin(new OriginalOpeningSceneData.Lease(preview?previewIdentity(player.getUUID()):player.getUUID(),(int)flow.slot().getAsLong()),SceneKind.ISLAND_110);
        if(!cinematic.episode().equals(session.sentEpisode)||cinematic.sequence()!=session.sentSequence) {
            session.sentEpisode=cinematic.episode();session.sentSequence=cinematic.sequence();session.sentRestoring=session.presentationRestoring;
            session.presentationRestoring=false;
        }
        NetworkHandler.sendToPlayer(new ClientboundOpeningPresentationPacket(session.token,session.revision,cinematic.episode(),cinematic.sequence(),
                cinematic.program(),cinematic.command(),cinematic.displayFrames(),session.sentRestoring,cinematic.awaitingDestination(),finished,
                cinematic.skipped(),preview,player.level().dimension().location(),session.cinematicOrigin,flow.profile.realInitialRoomSilent()),player);
    }

    public static void allowInitialRoomClock(ServerPlayer player) {
        var data=stats(player);
        if(!live(player)||data==null||data.openingProfile().previewActive()||!data.openingProfile().realInitialRoomSilent())return;
        data.openingProfile().setRealInitialRoomSilent(false);
        if(OriginalOpeningPersistence.saveAndVerify(player))sendRoomPolicy(player,false);
    }

    @SubscribeEvent
    public static void onRoomPolicyLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            var data=stats(player);
            if(data!=null&&!data.openingProfile().hasRejectedData()&&data.openingProfile().realInitialRoomSilent()&&!managed(player))sendRoomPolicy(player,true);
        }
    }

    private static void sendRoomPolicy(ServerPlayer player,boolean quiet) {
        NetworkHandler.sendToPlayer(ClientboundOpeningPresentationPacket.policy(player.getUUID(),quiet),player);
    }

    private static void prepareArrival(ServerPlayer player, Session session, Flow flow) {
        if (flow.preview()) { prepareReturn(player,session,flow); return; }
        var destination=OriginalOpeningArrival.destination();
        prepareDestination(player,session,flow,destination,1);
    }

    private static void prepareReturn(ServerPlayer player, Session session, Flow flow) {
        prepareDestination(player,session,flow,flow.profile.previewReturnPoint(),1);
    }

    private static void prepareDestination(ServerPlayer player, Session session, Flow flow, WorldPoint destination, int sourceMap) {
        if (session.awaitingReady) return;
        var level=level(player.server,destination);
        if (level==null) { notice(player,session,"目的地尚未就绪；当前进度已保留，可以稍后重试。"); return; }
        var feet=BlockPos.containing(destination.x(),destination.y(),destination.z());
        var chunks=new LinkedHashSet<ChunkPos>();
        for (int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) chunks.add(new ChunkPos((feet.getX()>>4)+x,(feet.getZ()>>4)+z));
        tickets(session,level,chunks);
        if (!loaded(level,chunks)) { notice(player,session,"正在准备抵达地点……"); return; }
        if (!destinationSafe(level,player,destination,flow)) { notice(player,session,"抵达地点暂时不能安全站立，序章进度已保留。"); return; }
        transfer(player,session,flow,sourceMap,destination);
    }

    private static void guardMovement(ServerPlayer player, Session session, Flow flow) {
        if (session.scene==null || session.lease==null || !DIMENSION.equals(player.level().dimension())) { hold(player,session); return; }
        if (session.scene==SceneKind.OP) { guardProfessionMovement(player,session,flow); return; }
        Cell after;
        try { after=sourceCell(session.lease,session.scene,player.position()); }
        catch (IllegalArgumentException failure) { hold(player,session); return; }
        if(startMovementTrigger(player,session,flow))return;
        if (player.getY()<63.8 || player.getY()>64.6) { hold(player,session); return; }
        var before=session.previousCell;
        if (!player.serverLevel().getEntities(player,player.getBoundingBox(),entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty()) { hold(player,session);return; }
        if (before==null) { session.previousCell=after; session.hold=point(player); return; }
        if (!after.equals(before) && !canSourceStep(session.scene,before,after)) { hold(player,session); return; }
        if (!sourceSupport(session.scene,after)) { hold(player,session); return; }
        session.previousCell=after;
        session.hold=point(player);
        flow.region(session.scene.sourceMap(),regionAt(session.scene,after));
        if (!after.equals(before)) flow.checkpoint(point(player));
    }

    private static void guardFirstStepMovement(ServerPlayer player,Session session,Flow flow) {
        if(session.scene!=SceneKind.ISLAND_110||session.lease==null||!DIMENSION.equals(player.level().dimension())
                ||flow.cursor().waitTicksRemaining()>0) { hold(player,session);return; }
        if(!session.firstStepReleased) {
            var target=feet(session.lease,SceneKind.ISLAND_110,new Cell(15,13));
            if(player.position().distanceToSqr(target)>.00001D)
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.add(0,player.getEyeHeight(),0));
            session.firstStepReleased=true;
            flow.checkpoint(point(player));
            changed(player,session);
            if(!save(player,session))return;
        }
        var body=player.getBoundingBox();var bounds=bounds(session.lease,SceneKind.ISLAND_110);
        if(body.minX<bounds.minX||body.maxX>bounds.maxX||body.minY<bounds.minY||body.maxY>bounds.maxY
                ||body.minZ<bounds.minZ||body.maxZ>bounds.maxZ||!player.serverLevel().noCollision(player,body)
                ||player.serverLevel().containsAnyLiquid(body)) { hold(player,session);return; }
        var cell=sourceCell(session.lease,SceneKind.ISLAND_110,player.position());
        if(startMovementTrigger(player,session,flow))return;
        if(!sourceSupport(SceneKind.ISLAND_110,cell)) { hold(player,session);return; }
        var previous=session.previousCell;
        if(player.onGround()) {
            session.previousCell=cell;session.hold=point(player);
            flow.region(SceneKind.ISLAND_110.sourceMap(),regionAt(SceneKind.ISLAND_110,cell));
            if(!cell.equals(previous))flow.checkpoint(point(player));
        }
    }

    private static boolean startMovementTrigger(ServerPlayer player,Session session,Flow flow) {
        if(flow.stage()!=Stage.FIRST_STEP&&flow.stage()!=Stage.COLLAPSE1&&flow.stage()!=Stage.COLLAPSE2)return false;
        var current=player.position();
        if(session.triggerScene!=session.scene) {
            session.triggerScene=session.scene;session.pendingTrigger=null;
            var held=session.hold;
            session.triggerFrom=held!=null&&held.dimension().equals(player.level().dimension().location().toString())
                    ?new Vec3(held.x(),held.y(),held.z()):current;
        }
        var from=session.triggerFrom==null?current:session.triggerFrom;
        session.triggerFrom=current;
        if(session.pendingTrigger==null) {
            var cells=MOVEMENT_TRIGGERS.computeIfAbsent(session.scene,scene->{
                if(scene==SceneKind.ISLAND_110)return List.of(new Cell(15,13));
                var result=new java.util.ArrayList<Cell>();
                for(int z=0;z<scene.height();z++)for(int x=0;x<scene.width();x++) {
                    var cell=new Cell(x,z);if(regionAt(scene,cell)==10)result.add(cell);
                }
                return List.copyOf(result);
            });
            double first=Double.POSITIVE_INFINITY;
            for(var cell:cells) {
                var feet=feet(session.lease,session.scene,cell);
                var area=new AABB(feet.x-.5,FEET_Y-.2,feet.z-.5,feet.x+.5,FEET_Y+2,feet.z+.5);
                var hit=area.contains(from)?Optional.of(from):area.clip(from,current);
                if(hit.isEmpty()&&area.contains(current))hit=Optional.of(current);
                if(hit.isPresent()&&from.distanceToSqr(hit.get())<first) {
                    first=from.distanceToSqr(hit.get());session.pendingTrigger=cell;
                }
            }
        }
        if(session.pendingTrigger==null)return false;
        var next=flow.stage()==Stage.FIRST_STEP?Stage.COLLAPSE1:flow.stage()==Stage.COLLAPSE1?Stage.COLLAPSE2:Stage.COLLAPSE3;
        var nextScene=flow.stage()==Stage.FIRST_STEP?SceneKind.ISLAND_97:flow.stage()==Stage.COLLAPSE1?SceneKind.ISLAND_98:SceneKind.ISLAND_99;
        var destination=flow.stage()==Stage.FIRST_STEP?session.pendingTrigger:session.previousCell;
        if(destination==null||!sourceSupport(session.scene,destination))destination=sourceCell(session.lease,session.scene,current);
        beginMotion(player,session,flow,destination,next,nextScene);
        if(session.motion==null)hold(player,session);
        return true;
    }

    private static void guardProfessionMovement(ServerPlayer player, Session session, Flow flow) {
        var body=player.getBoundingBox();
        var bounds=bounds(session.lease,SceneKind.OP);
        if (body.minX<bounds.minX || body.maxX>bounds.maxX || body.minY<bounds.minY || body.maxY>bounds.maxY
                || body.minZ<bounds.minZ || body.maxZ>bounds.maxZ || !player.serverLevel().noCollision(player,body)
                || player.serverLevel().containsAnyLiquid(body)) { hold(player,session); return; }
        var cell=sourceCell(session.lease,SceneKind.OP,player.position());
        if (!player.onGround()) return;
        session.hold=point(player);
        if (cell.equals(session.previousCell) || Math.abs(player.getY()-FEET_Y)>.01D) return;
        try { checkpoint(plan(session.lease,SceneKind.OP),cell); }
        catch (IllegalArgumentException failure) { return; }
        session.previousCell=cell;
        flow.region(SceneKind.OP.sourceMap(),regionAt(SceneKind.OP,cell));
        flow.checkpoint(point(player));
    }

    private static void beginMotion(ServerPlayer player, Session session, Flow flow, Cell destination, Stage next, SceneKind nextScene) {
        if (session.motion!=null || session.lease==null || session.scene==null || !DIMENSION.equals(player.level().dimension())) return;
        var position=feet(session.lease,session.scene,destination);
        var body=new AABB(position.x-.3,position.y,position.z-.3,position.x+.3,position.y+1.8,position.z+.3);
        var swept=player.getBoundingBox().minmax(body);
        if (!player.serverLevel().noCollision(player,swept) || player.serverLevel().containsAnyLiquid(swept)
                || !player.serverLevel().getEntities(player,swept,entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty()) return;
        session.motion=new Motion(player.position(),position,destination,next,nextScene);
        session.focus=0;
        changed(player,session);
    }

    private static void move(ServerPlayer player, Session session, Flow flow) {
        var motion=session.motion;
        motion.elapsed++;
        double progress=Math.min(1.0D,motion.elapsed/12.0D);
        Vec3 destination=motion.from.lerp(motion.to,progress);
        var body=new AABB(destination.x-.3,destination.y,destination.z-.3,destination.x+.3,destination.y+1.8,destination.z+.3);
        if (!player.serverLevel().noCollision(player,body) || player.serverLevel().containsAnyLiquid(body)
                || !player.serverLevel().getEntities(player,body,entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty()) {
            session.motion=null;
            fail(player,session,"前方暂时不能安全通行，请重试。","opening_controlled_motion_obstructed");
            return;
        }
        player.connection.teleport(destination.x,destination.y,destination.z,player.getYRot(),player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance=0;
        session.hold=point(player);
        if (progress<1) return;
        session.motion=null;
        var cinematic=flow.cinematic().orElse(null);
        flow.clearCinematic();
        flow.advance(motion.next);
        String program=motion.next==Stage.COLLAPSE1?"collapse110":motion.next==Stage.COLLAPSE2?"collapse97":"collapse98";
        if(cinematic==null)cinematic=OriginalOpeningCinematic.start(program);else cinematic.changeProgram(program);
        flow.cinematic(cinematic);session.presentationRestoring=false;
        flow.cursor(new Cursor(motion.scene.sourceMap(),1,0,0,0,0));
        flow.region(motion.scene.sourceMap(),0);
        var target=feet(session.lease,motion.scene,motion.cell);
        flow.pending(new PendingTransfer(motion.scene.sourceMap(),flow.stage(),new WorldPoint(DIMENSION.location().toString(),target.x,target.y,target.z,player.getYRot(),player.getXRot())));
        session.entering=true;
        session.sceneReady=false;
        changed(player,session);
        save(player,session);
    }

    private static void abortPreview(ServerPlayer player, Session session, Flow flow) {
        session.completedCinematic=flow.cinematic().map(OriginalOpeningCinematic::copy).orElse(null);
        session.completedPreview=true;
        flow.clearCinematic();
        if (flow.stage()==Stage.FALLING) flow.recover();
        flow.cursor(new Cursor(185,0,0,REPLAY_RETURN_COMMAND,0,0));
        if (flow.pending().isPresent()) flow.clearPending(flow.pending().get());
        session.motion=null;
        session.fallReady=false;
        stopFall(player);
        session.removeTemporaryAfterReady=false;
        session.awaitingReady=false;
        session.entering=true;
        session.cleanup=false;
        session.error=false;
        session.notice="";
        changed(player,session);
        save(player,session);
    }

    private static int clean(ServerPlayer player, Session session, Flow flow, int budget) {
        var before=stats(player).serializeNBT();
        int consumed=0;
        if (session.lease==null && flow.slot().isPresent()) {
            var level=player.server.getLevel(DIMENSION);
            if (level==null) return 0;
            UUID owner=flow.preview()?previewIdentity(player.getUUID()):player.getUUID();
            session.lease=OriginalOpeningSceneData.get(level).find(owner).orElse(null);
            if (session.lease==null || session.lease.slot()!=flow.slot().getAsLong()) throw new IllegalStateException("Opening cleanup lease is missing");
        }
        if (session.lease!=null) {
            var level=player.server.getLevel(DIMENSION);
            if (level==null) return 0;
            tickets(session,level,OriginalOpeningSceneWorld.requiredChunks(session.lease));
            if (!loaded(level,OriginalOpeningSceneWorld.requiredChunks(session.lease))) return 0;
            OriginalOpeningActors.clear(level,player.getUUID(),session.lease);
            var data=OriginalOpeningSceneData.get(level);
            var view=data.view(session.lease.owner(),session.lease).orElseThrow();
            OriginalOpeningSceneWorld.Result result;
            int used=0;
            if (view.journal()!=null) {
                if (budget==0) return 0;
                result=OriginalOpeningSceneWorld.advance(level,session.lease.owner(),session.lease,view.journal().id(),budget);
                used=result.status()==OriginalOpeningSceneWorld.Status.WRITE_FAILED?budget:result.status()==OriginalOpeningSceneWorld.Status.COMPLETE?Math.min(budget,view.journal().total()-view.journal().cursor()):Math.max(0,result.cursor()-view.journal().cursor());
                consumed=Math.min(used,budget);
                if (!view.journal().restore() && result.status()==OriginalOpeningSceneWorld.Status.COMPLETE) return consumed;
            } else result=OriginalOpeningSceneWorld.beginRestore(level,session.lease.owner(),session.lease);
            if (result.status()==OriginalOpeningSceneWorld.Status.PROGRESS || result.status()==OriginalOpeningSceneWorld.Status.WAIT_CHUNKS) return Math.min(used,budget);
            if (result.status()!=OriginalOpeningSceneWorld.Status.COMPLETE && result.status()!=OriginalOpeningSceneWorld.Status.ALREADY_READY) {
                fail(player,session,"序章场景尚未安全收尾，请重试。",sceneFailure(level,result));
                return Math.min(used,budget);
            }
            flow.clearSlot(session.lease.slot());
        }
        if (flow.preview()) flow.profile.endPreview();
        if (!OriginalOpeningPersistence.saveAndVerify(player)) {
            stats(player).deserializeNBT(before);
            session.profile=stats(player).openingProfile();
            fail(player,session,"序章收尾状态尚未保存，请重试。","opening_cleanup_save_failed");
            return consumed;
        }
        sendComplete(player,session);
        release(session);
        SESSIONS.get(player.server).remove(player.getUUID());
        return consumed;
    }

    private static boolean destinationSafe(ServerLevel level, ServerPlayer player, WorldPoint destination, Flow flow) {
        if (flow.stage()==Stage.FALLING) return fallDestinationSafe(level,player,destination,flow,session(player));
        return flow.stage()==Stage.ARRIVAL_PENDING && !flow.preview()
                ? OriginalOpeningArrival.ready(level,player,destination) : safe(level,player,destination);
    }

    private static boolean safe(ServerLevel level, ServerPlayer player, WorldPoint destination) {
        var feet=BlockPos.containing(destination.x(),destination.y(),destination.z());
        var body=new AABB(destination.x()-.3,destination.y(),destination.z()-.3,destination.x()+.3,destination.y()+1.8,destination.z()+.3);
        return level.hasChunkAt(feet) && level.areEntitiesLoaded(new ChunkPos(feet).toLong()) && level.getWorldBorder().isWithinBounds(feet)
                && feet.getY()>=level.getMinBuildHeight() && feet.getY()+2<level.getMaxBuildHeight()
                && !level.containsAnyLiquid(body) && level.noCollision(player,body) && !level.noCollision(player,body.move(0,-.05D,0))
                && level.getEntities(player,body,entity->!entity.isRemoved()&&!entity.isSpectator()).isEmpty();
    }

    private static void hold(ServerPlayer player, Session session) {
        if (session.hold==null || !session.hold.dimension().equals(player.level().dimension().location().toString())) return;
        var point=session.hold;
        if (player.position().distanceToSqr(new Vec3(point.x(),point.y(),point.z()))>.00001D) player.connection.teleport(point.x(),point.y(),point.z(),player.getYRot(),player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance=0;
    }

    private static void tickets(Session session, ServerLevel level, Set<ChunkPos> chunks) {
        var old=session.tickets.computeIfAbsent(level,ignored->new LinkedHashSet<>());
        for(var chunk:chunks) if(old.add(chunk)) level.getChunkSource().addRegionTicket(TICKET,chunk,2,session.owner);
    }

    private static void refreshTickets(Session session) {
        if (session.server.getTickCount()%20!=0) return;
        session.tickets.forEach((level,chunks)->chunks.forEach(chunk->level.getChunkSource().addRegionTicket(TICKET,chunk,2,session.owner)));
    }

    private static void release(Session session) {
        var player=session.server.getPlayerList().getPlayer(session.owner);
        if (player!=null) stopFall(player);
        session.tickets.forEach((level,chunks)->chunks.forEach(chunk->level.getChunkSource().removeRegionTicket(TICKET,chunk,2,session.owner)));
        session.tickets.clear();
    }

    private static boolean loaded(ServerLevel level, Set<ChunkPos> chunks) {
        return chunks.stream().allMatch(chunk->level.getChunkSource().getChunkNow(chunk.x,chunk.z)!=null && level.areEntitiesLoaded(chunk.toLong()));
    }

    private static SceneKind kind(Stage stage) {
        return switch(stage) {
            case NAME_PENDING,ORIGIN_SELECTION,ORIGIN_CONFIRMED -> SceneKind.OP;
            case ALICE_SELECTION,MEMORY_DIALOGUE,FIRST_STEP -> SceneKind.ISLAND_110;
            case COLLAPSE1 -> SceneKind.ISLAND_97;
            case COLLAPSE2 -> SceneKind.ISLAND_98;
            case COLLAPSE3,FALLING,ARRIVAL_PENDING,COMPLETE -> SceneKind.ISLAND_99;
        };
    }

    private static void changed(ServerPlayer player, Session session) { session.revision++; send(player,session); }
    private static void periodic(ServerPlayer player, Session session) { if (player.server.getTickCount()%40==0) send(player,session); }
    private static void notice(ServerPlayer player, Session session, String notice) {
        if (session.notice.equals(notice)) return;
        session.notice=notice;
        changed(player,session);
    }

    private static void fail(ServerPlayer player, Session session, String notice, String detail) {
        if (!session.error) org.slf4j.LoggerFactory.getLogger(OriginalOpeningService.class).warn("Opening paused for {}: {}",player.getUUID(),detail);
        session.error=true;
        if (session.cleanup) release(session);
        notice(player,session,notice);
    }

    private static boolean save(ServerPlayer player, Session session) {
        if (OriginalOpeningPersistence.saveAndVerify(player) && savedPositionMatches(player)) return true;
        fail(player,session,"序章进度尚未写入存档，请重试。","player_save_readback_failed");
        return false;
    }

    private static boolean savedPositionMatches(ServerPlayer player) {
        try {
            var file=player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID()+".dat");
            var saved=net.minecraft.nbt.NbtIo.readCompressed(file.toFile());
            var expected=player.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            return java.util.Objects.equals(saved.get("Dimension"),expected.get("Dimension"))
                    && java.util.Objects.equals(saved.get("Pos"),expected.get("Pos"))
                    && java.util.Objects.equals(saved.get("Rotation"),expected.get("Rotation"));
        } catch (java.io.IOException failure) { return false; }
    }

    private static void send(ServerPlayer player, Session session) {
        var stats=stats(player);
        if (stats==null) return;
        var flow=new Flow(stats.openingProfile());
        var stage=session.cleanup?Stage.COMPLETE:flow.stage();
        boolean locked=session.cleanup?flow.preview():(session.error||session.entering||session.awaitingReady||session.motion!=null
                || stage==Stage.ORIGIN_SELECTION&&(OriginalOpeningGreeting.active(flow.cursor().commandIndex())||session.focus>0)
                || stage==Stage.FIRST_STEP&&(!session.firstStepReleased||flow.cursor().waitTicksRemaining()>0)
                || stage!=Stage.ORIGIN_SELECTION&&stage!=Stage.FIRST_STEP&&stage!=Stage.COLLAPSE1&&stage!=Stage.COLLAPSE2&&stage!=Stage.COMPLETE);
        var position=session.hold==null?point(player):session.hold;
        String notice=session.notice;
        if(!session.error&&(notice.equals("正在准备序章场景……")||notice.equals("正在准备抵达地点……")||notice.equals("正在准备安全的坠落路径……")))notice="";
        NetworkHandler.sendToPlayer(new ClientboundOpeningPacket(session.token,session.revision,ResourceLocation.tryParse(position.dimension()),
                BlockPos.containing(position.x(),position.y(),position.z()),stage,flow.preview(),locked,session.entering||session.awaitingReady,
                session.focus,flow.relationship(),flow.cursor().commandIndex(),flow.cursor().waitTicksRemaining(),notice,
                OriginalOpeningCoda.active(flow.cursor().commandIndex())?flow.cursor().messageOffset():0,flow.origin()),player);
        sendCinematic(player,session,flow,false);
    }

    private static void sendComplete(ServerPlayer player, Session session) {
        StatEventHandler.syncToClient(player);
        NetworkHandler.sendToPlayer(new ClientboundOpeningPacket(session.token,session.revision+1,player.level().dimension().location(),player.blockPosition(),
                Stage.COMPLETE,false,false,false,0,0,0,0,""),player);
        session.revision++;
        sendCinematic(player,session,new Flow(stats(player).openingProfile()),true);
    }

    private static BSPlayerStats stats(ServerPlayer player) { return player==null?null:player.getCapability(BSPlayerStats.CAPABILITY).orElse(null); }
    private static Flow flow(ServerPlayer player) { return new Flow(stats(player).openingProfile()); }
    private static Session session(ServerPlayer player) { var sessions=SESSIONS.get(player.server); return sessions==null?null:sessions.get(player.getUUID()); }
    private static boolean live(ServerPlayer player) { return player!=null&&player.server.isSameThread()&&player.server.getPlayerList().getPlayer(player.getUUID())==player&&!player.isRemoved(); }
    private static UUID previewIdentity(UUID player) { return UUID.nameUUIDFromBytes(("blacksouls:opening-preview:"+player).getBytes(StandardCharsets.UTF_8)); }
    private static boolean aborting(Flow flow) { return flow.preview()&&flow.cursor().sourceMapId()==185&&flow.cursor().commandIndex()==REPLAY_RETURN_COMMAND; }
    private static WorldPoint point(ServerPlayer player) { return new WorldPoint(player.level().dimension().location().toString(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()); }
    private static ServerLevel level(MinecraftServer server, WorldPoint point) { return server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.tryParse(point.dimension()))); }

    public static int activeSessionCount(MinecraftServer server) { var sessions=SESSIONS.get(server); return sessions==null?0:sessions.size(); }
    public static int heldTicketCount(MinecraftServer server) { var sessions=SESSIONS.get(server); return sessions==null?0:sessions.values().stream().mapToInt(session->session.tickets.values().stream().mapToInt(Set::size).sum()).sum(); }

    public static boolean replay(ServerPlayer player) {
        if (!live(player)||!player.isAlive()||player.isSpectator()||player.isSleeping()||player.isPassenger()||player.isVehicle()
                ||player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty()
                ||session(player)!=null||managed(player)||player.server.getLevel(DIMENSION)==null||!safe(player.serverLevel(),player,point(player))
                ||yuki135.blacksouls.combat.TurnBattleManager.isInBattle(player)||OriginalNpcService.isConversing(player)
                ||!yuki135.blacksouls.capability.OriginalNpcProgress.get(player.server).conversationIds(player.getUUID()).isEmpty()
                ||OriginalWorldTransferService.pending(player)
                || !OriginalOpeningPersistence.canSave(player)) return false;
        var data=stats(player);
        var sessions=SESSIONS.get(player.server);
        if (data==null||data.openingProfile().hasRejectedData()||sessions!=null&&sessions.size()>=MAX_SESSIONS) return false;
        data.openingProfile().beginPreview(point(player));
        return resumePlayer(player);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var sessions=SESSIONS.get(player.server);
        var session=sessions==null?null:sessions.remove(player.getUUID());
        stopFall(player);
        if (session!=null) { save(player,session);release(session); }
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        var sessions=SESSIONS.remove(event.getServer());
        if (sessions!=null) sessions.values().forEach(OriginalOpeningService::release);
        ROUND_ROBIN.remove(event.getServer());
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onDamage(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && managed(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onHeal(net.minecraftforge.event.entity.living.LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && preview(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onPickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && preview(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onExperience(net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp event) {
        if (event.getEntity() instanceof ServerPlayer player && preview(player)) event.setCanceled(true);
    }

    private static boolean preview(ServerPlayer player) { var stats=stats(player);return stats!=null&&stats.openingProfile().previewActive(); }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (managed(player)||DIMENSION.equals(player.level().dimension()))) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level&&DIMENSION.equals(level.dimension())) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level&&DIMENSION.equals(level.dimension())) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && (managed(player)||DIMENSION.equals(player.level().dimension()))) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && (managed(player)||DIMENSION.equals(player.level().dimension()))) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && managed(player) && !(event.getTarget() instanceof EntityOpeningActor)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onUseEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && managed(player) && !(event.getTarget() instanceof EntityOpeningActor)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bs2_opening").requires(source->source.hasPermission(2))
                .then(Commands.literal("replay").executes(context->{
                    var player=context.getSource().getPlayerOrException();
                    if (replay(player)) return Command.SINGLE_SUCCESS;
                    context.getSource().sendFailure(Component.literal("请先结束当前战斗、对话和移动，关闭其他界面后再重播序章。"));
                    return 0;
                }))
                .then(Commands.literal("retry").executes(context->{
                    var player=context.getSource().getPlayerOrException();
                    resumePlayer(player);
                    var session=session(player);
                    if (session==null) return 0;
                    handle(player,new ServerboundOpeningPacket(session.token,session.revision,ServerboundOpeningPacket.Action.RETRY,0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("return").executes(context->{
                    var player=context.getSource().getPlayerOrException();
                    var session=session(player);
                    if (session==null||session.cleanup||!flow(player).preview()||flow(player).stage()==Stage.COMPLETE) return 0;
                    abortPreview(player,session,flow(player));
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static final class Motion {
        final Vec3 from,to;
        final Cell cell;
        final Stage next;
        final SceneKind scene;
        int elapsed;
        Motion(Vec3 from,Vec3 to,Cell cell,Stage next,SceneKind scene) { this.from=from;this.to=to;this.cell=cell;this.next=next;this.scene=scene; }
    }

    private static final class Session {
        final MinecraftServer server;
        final UUID owner;
        final UUID token=UUID.randomUUID();
        final Map<ServerLevel,Set<ChunkPos>> tickets=new LinkedHashMap<>();
        OriginalOpeningProfile profile;
        OriginalOpeningSceneData.Lease lease;
        SceneKind scene;
        WorldPoint hold;
        Cell previousCell;
        SceneKind triggerScene;
        Vec3 triggerFrom;
        Cell pendingTrigger;
        Motion motion;
        UUID focusActor;
        int revision=1,focus,lastActionTick=-1,fallSavedCommand=-1,greetingAnimations,codaEffectCommand=-1,codaRouteKey=-1;
        boolean entering=true,awaitingReady,sceneReady,error,cleanup,nameSent,removeTemporaryAfterReady,firstStepReleased;
        boolean presentationRestoring=true,completedPreview,fallReady;
        boolean sentRestoring;
        UUID sentEpisode;
        long sentSequence=-1;
        BlockPos cinematicOrigin=BlockPos.ZERO;
        OriginalOpeningCinematic completedCinematic;
        String notice="";
        Session(ServerPlayer player,OriginalOpeningProfile profile) { server=player.server;owner=player.getUUID();this.profile=profile;hold=point(player); }
    }

    private record Flow(OriginalOpeningProfile profile) {
        boolean preview() { return profile.previewActive(); }
        Stage stage() { return preview()?profile.previewStage():profile.realStage().orElseThrow(); }
        int origin() { return preview()?profile.previewOriginId():profile.realOriginId(); }
        int relationship() { return preview()?profile.previewRelationshipId():profile.realRelationshipId(); }
        Cursor cursor() { return preview()?profile.previewCursor():profile.realCursor(); }
        OptionalLong slot() { return preview()?profile.previewSlot():profile.realSlot(); }
        Optional<PendingTransfer> pending() { return preview()?profile.previewPendingTransfer():profile.realPendingTransfer(); }
        OriginalOpeningProfile.Checkpoint checkpoint() { return preview()?profile.previewCheckpoint():profile.realCheckpoint(); }
        boolean skip() { return preview()?profile.previewSkipRequested():profile.realSkipRequested(); }
        void advance(Stage value) { if(preview())profile.advancePreview(value);else profile.advanceReal(value); }
        void confirmOrigin(int value) { if(preview())profile.confirmPreviewOrigin(value);else profile.confirmRealOrigin(value); }
        void confirmRelationship(int value) { if(preview())profile.confirmPreviewRelationship(value);else profile.confirmRealRelationship(value); }
        void cursor(Cursor value) { if(preview())profile.setPreviewCursor(value);else profile.setRealCursor(value); }
        void checkpoint(WorldPoint value) { if(preview())profile.checkpointPreview(value);else profile.checkpointReal(value); }
        void region(int map,int region) { if(preview())profile.recordPreviewRegion(map,region);else profile.recordRealRegion(map,region); }
        void recover() { if(preview())profile.recoverPreviewCheckpoint();else profile.recoverRealCheckpoint(); }
        void assign(long value) { if(preview())profile.assignPreviewSlot(value);else profile.assignRealSlot(value); }
        void clearSlot(long value) { if(preview())profile.clearPreviewSlot(value);else profile.clearRealSlot(value); }
        void pending(PendingTransfer value) { if(preview())profile.setPreviewPendingTransfer(value);else profile.setRealPendingTransfer(value); }
        void clearPending(PendingTransfer value) { if(preview())profile.clearPreviewPendingTransfer(value);else profile.clearRealPendingTransfer(value); }
        void requestSkip() { if(preview())profile.requestPreviewSkip();else profile.requestRealSkip(); }
        void clearSkip() { if(preview())profile.clearPreviewSkip();else profile.clearRealSkip(); }
        void skipToArrival() { if(preview())profile.advancePreviewSkipToArrival();else profile.advanceRealSkipToArrival(); }
        Optional<OriginalOpeningCinematic> cinematic() { return profile.activeCinematic(); }
        void cinematic(OriginalOpeningCinematic value) { profile.setActiveCinematic(value); }
        void clearCinematic() { profile.clearActiveCinematic(); }
    }
}
