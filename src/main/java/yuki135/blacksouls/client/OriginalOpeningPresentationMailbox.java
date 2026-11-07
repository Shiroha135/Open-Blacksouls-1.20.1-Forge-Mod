package yuki135.blacksouls.client;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import yuki135.blacksouls.network.packets.ClientboundOpeningPresentationPacket;

public final class OriginalOpeningPresentationMailbox {
    public enum Decision { ACCEPT, QUEUE, REJECT }
    private UUID session;
    private int revision=-1;
    private UUID episode;
    private long sequence=-1;
    private boolean terminal,finished;
    private ClientboundOpeningPresentationPacket latest,pending;
    private final LinkedHashSet<UUID> closedSessions=new LinkedHashSet<>();
    private final LinkedHashSet<UUID> closedEpisodes=new LinkedHashSet<>();

    public boolean acceptMain(UUID token,int version,boolean complete) {
        Objects.requireNonNull(token);
        if(version<0||closedSessions.contains(token)||token.equals(session)&&version<revision)return false;
        if(!token.equals(session)) {
            if(session!=null)remember(closedSessions,session,16);
            session=token;revision=-1;episode=null;sequence=-1;latest=null;finished=false;closedEpisodes.clear();
        }
        revision=version;terminal=complete;
        if(pending!=null&&pending.session().equals(session)&&pending.revision()<revision)pending=null;
        return true;
    }

    public Decision offer(ClientboundOpeningPresentationPacket packet) {
        Objects.requireNonNull(packet);
        if(packet.policyOnly())return Decision.REJECT;
        if(closedSessions.contains(packet.session()))return Decision.REJECT;
        if(session==null||!session.equals(packet.session())||packet.revision()>revision) {
            if(pending!=null&&pending.session().equals(packet.session())) {
                if(packet.revision()<pending.revision()||packet.revision()==pending.revision()&&packet.episode().equals(pending.episode())&&packet.sequence()<=pending.sequence())return Decision.REJECT;
            }
            pending=packet;return Decision.QUEUE;
        }
        if(packet.revision()<revision||closedEpisodes.contains(packet.episode()))return Decision.REJECT;
        if(packet.episode().equals(episode)) {
            if(packet.sequence()<=sequence)return Decision.REJECT;
        } else {
            if(episode!=null)remember(closedEpisodes,episode,4);
            episode=packet.episode();sequence=-1;finished=false;
        }
        sequence=packet.sequence();latest=packet;
        if(pending==packet||pending!=null&&pending.session().equals(packet.session())&&pending.revision()<=revision)pending=null;
        return Decision.ACCEPT;
    }

    public ClientboundOpeningPresentationPacket drain() {
        if(pending==null||session==null||!pending.session().equals(session)||pending.revision()!=revision)return null;
        var result=pending;pending=null;
        return offer(result)==Decision.ACCEPT?result:null;
    }

    public boolean applied(ClientboundOpeningPresentationPacket packet) {
        if(latest!=packet||session==null||!session.equals(packet.session())||revision!=packet.revision())return false;
        if(packet.finished()&&(packet.program().equals("finale99")||packet.program().equals("arrival")))finished=true;
        else if(!packet.finished())finished=false;
        return true;
    }

    public boolean canClose() { return session!=null&&terminal&&finished; }
    public UUID session() { return session; }
    public int revision() { return revision; }
    public int queuedCount() { return pending==null?0:1; }
    public boolean terminalPending() { return terminal&&!finished; }

    public void close() {
        if(session!=null)remember(closedSessions,session,16);
        if(pending!=null&&pending.session().equals(session))pending=null;
        session=null;revision=-1;episode=null;sequence=-1;latest=null;terminal=false;finished=false;closedEpisodes.clear();
    }

    public void clear() { close();pending=null;closedSessions.clear();closedEpisodes.clear(); }
    private static void remember(LinkedHashSet<UUID> values,UUID value,int bound) { values.add(value);while(values.size()>bound)values.remove(values.iterator().next()); }
}
