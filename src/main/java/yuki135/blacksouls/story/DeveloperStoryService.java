package yuki135.blacksouls.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.compat.scene.SceneSpawnerBossData;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.entity.EntityCheshireCat;
import yuki135.blacksouls.item.ItemDevTool;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundDevStoryPacket;
import yuki135.blacksouls.network.packets.ServerboundDevStoryPacket;
import yuki135.blacksouls.util.BSOriginalNpcData;
import yuki135.blacksouls.util.OriginalCovenantData;
import yuki135.blacksouls.util.OriginalActorBridge;

@Mod.EventBusSubscriber(modid="blacksouls")
public final class DeveloperStoryService {
    private static final JsonObject NAMES=names();
    private static final Map<MinecraftServer,Map<UUID,Pending>> PENDING=new WeakHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,Change>> UNDO=new WeakHashMap<>();
    private record Change(CompoundTag before,CompoundTag after,String label) { }
    private record Pending(String token,long expires,Change change) { }
    private DeveloperStoryService() { }

    public static boolean permitted(ServerPlayer player) {
        return player!=null&&player.isAlive()&&player.isCreative()&&(player.hasPermissions(4)
                ||player.server.isSingleplayerOwner(player.getGameProfile())&&player.hasPermissions(2))
                &&(player.getMainHandItem().getItem() instanceof ItemDevTool||player.getOffhandItem().getItem() instanceof ItemDevTool);
    }

    public static void handle(ServerPlayer player,ServerboundDevStoryPacket packet) {
        if(player==null)return;
        JsonObject reply;
        try {
            if(!permitted(player))throw new IllegalArgumentException("需要创造模式并手持开发者法杖；服务器需权限4，单人需开启作弊的房主。");
            reply=execute(player,packet.action(),packet.key(),packet.value(),packet.token());
        }catch(Exception failure){reply=new JsonObject();reply.addProperty("message",failure.getMessage()==null?failure.toString():failure.getMessage());reply.addProperty("ok",false);}
        NetworkHandler.sendToPlayer(new ClientboundDevStoryPacket(packet.request(),reply.toString()),player);
    }

    public static JsonObject execute(ServerPlayer player,String action,String key,String value,String token)throws Exception {
        if(!permitted(player)||!player.server.isSameThread())throw new IllegalArgumentException("开发者权限或线程不符。");
        var response=new JsonObject();response.addProperty("ok",true);
        if(action.equals("overview"))return overview(player);
        if(action.equals("inspect"))return inspect(player,key);
        if(action.equals("save")) {
            idle(player,false);int slot=slot(key);DeveloperStoryPoints.get(player.server).store(player.getUUID(),slot,value.isBlank()?"测试点"+slot:value,capture(player));persist(player);
            response=overview(player);response.addProperty("message","已保存测试点 "+slot+"：仅世界开关/变量/独立开关/隐藏状态与Boss记录。");return response;
        }
        if(action.equals("preview")) {
            idle(player,key.startsWith("resume:"));
            var before=capture(player);var after=before.copy();String label=plan(player,key,value,after);
            var change=new Change(before,after,label);String id=UUID.randomUUID().toString();
            PENDING.computeIfAbsent(player.server,ignored->new java.util.HashMap<>()).put(player.getUUID(),new Pending(id,player.server.getTickCount()+1200,change));
            response.addProperty("token",id);response.addProperty("preview",true);response.addProperty("message",label+"\n"+difference(before,after)+"\n世界共享状态；影响同存档所有玩家。\n不会回退背包、奖励、职业、誓约或个人SEN。\n应用时先保存NBT备份；预览60秒内有效。");return response;
        }
        if(action.equals("apply")) {
            var pending=PENDING.getOrDefault(player.server,Map.of()).get(player.getUUID());
            if(pending==null||!pending.token().equals(token)||player.server.getTickCount()>pending.expires())throw new IllegalArgumentException("预览已过期，请重新预览。");
            idle(player,pending.change().label().startsWith("清除本人对话续接"));
            if(!capture(player).equals(pending.change().before()))throw new IllegalArgumentException("状态已发生变化，未覆盖；请重新预览。");
            commit(player,pending.change());PENDING.get(player.server).remove(player.getUUID());
            response=overview(player);response.addProperty("message","已应用："+pending.change().label());return response;
        }
        if(action.equals("undo")) {
            var change=UNDO.getOrDefault(player.server,Map.of()).get(player.getUUID());
            if(change==null)throw new IllegalArgumentException("本次运行没有可撤回操作。");
            idle(player,change.label().startsWith("清除本人对话续接"));
            if(!capture(player).equals(change.after()))throw new IllegalArgumentException("剧情已继续或其他人改了状态，不能直接撤回覆盖；请用测试点预览。");
            validateResumeChanges(player,change.after(),change.before());
            backup(player,change.after(),"undo");restore(player,change.before());UNDO.get(player.server).remove(player.getUUID());
            response=overview(player);response.addProperty("message","已撤回："+change.label());return response;
        }
        throw new IllegalArgumentException("未知开发者剧情操作。");
    }

    private static JsonObject overview(ServerPlayer player) {
        var response=new JsonObject();response.addProperty("ok",true);response.addProperty("catalog",true);
        response.addProperty("progress",OriginalNpcProgress.readVariable(player,11));
        response.addProperty("message","世界共享剧情状态。预设用于对白条件测试，不模拟奖励发放或完整通关。\n猎颅兽：原Map052事件23胜利后 S56=开、S57=开、V11+1；当前桥接也使用V11。\n测试点不回退背包/奖励/职业；要重测赠礼，请用独立测试存档。");
        var switches=new JsonArray();var variables=new JsonArray();
        for(int kind=0;kind<2;kind++) {
            var source=NAMES.getAsJsonArray(kind==0?"switches":"variables");
            for(int id=1;id<source.size();id++) {
                String name=name(kind==0?"switches":"variables",id);
                var row=new JsonObject();row.addProperty("key",(kind==0?"S:":"V:")+id);row.addProperty("name",(kind==0?"S":"V")+id+" "+name);
                row.addProperty("value",kind==0?Boolean.toString(readSwitch(player,id)):Long.toString(OriginalNpcProgress.readVariable(player,id)));
                row.addProperty("readonly",kind==0?protectedSwitch(id):protectedVariable(id));
                (kind==0?switches:variables).add(row);
            }
        }
        response.add("switches",switches);response.add("variables",variables);
        var events=new JsonArray();
        for(var event:BSOriginalNpcData.values().stream().sorted(Comparator.comparing(BSOriginalNpcData.Entry::id)).toList()) {
            var row=new JsonObject();row.addProperty("key",event.id());row.addProperty("name",event.id()+" "+event.name());events.add(row);
        }
        response.add("events",events);var points=new JsonArray();
        for(int slot=1;slot<=3;slot++) {var point=DeveloperStoryPoints.get(player.server).point(player.getUUID(),slot);points.add(point==null?"空":point.getString("Name"));}
        response.add("points",points);response.addProperty("undo",UNDO.getOrDefault(player.server,Map.of()).containsKey(player.getUUID()));return response;
    }

    public static JsonObject inspect(ServerPlayer player,String id) {
        var event=BSOriginalNpcData.getEvent(id);if(event==null)throw new IllegalArgumentException("没有这个已移植原作事件："+id);
        var progress=OriginalNpcProgress.get(player.server);var lines=new ArrayList<String>();
        lines.add(event.id()+"  "+event.name()+"  原坐标("+event.x()+","+event.y()+")");
        lines.add("独立开关："+java.util.stream.Stream.of("A","B","C","D").map(k->k+"="+progress.selfSwitch(id,k)).collect(java.util.stream.Collectors.joining("  ")));
        lines.add("隐藏="+progress.isHidden(id)+"；本人续接="+(progress.conversation(player.getUUID(),id)!=null)+"；他人续接="+progress.hasOtherConversation(player.getUUID(),java.util.Set.of(id)));
        var saved=progress.conversation(player.getUUID(),id);
        if(saved!=null&&saved.has("interpreter")) {
            var interpreter=saved.getAsJsonObject("interpreter");
            if(interpreter.has("pending")&&!interpreter.get("pending").isJsonNull())lines.add("已保存执行节点："+interpreter.get("pending"));
        }
        var context=readContext(player,id);int selected=-1;String selectionError="";
        try {var page=OriginalNpcInterpreter.selectPage(event,context);selected=page==null?-1:event.pages().indexOf(page);}catch(Exception failure){selectionError=failure.getMessage();}
        lines.add("正式选页器："+(selected<0?"无有效页":"第 "+(selected+1)+" 页")+(selectionError.isEmpty()?"":" / "+selectionError));
        int loaded=0;
        for(var level:player.server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof EntityOriginalNpc npc&&npc.getNpcId().equals(id)) {loaded++;lines.add("已加载实体 "+npc.getUUID()+"  "+level.dimension().location()+"  当前页="+(npc.currentPage()==null?"无":event.pages().indexOf(npc.currentPage())+1));}
        lines.add("加载实体数="+loaded+"；0只表示此刻未加载/使用专用实体/静态绑定，不证明未部署。");
        for(int i=0;i<event.pages().size();i++) {
            var page=event.pages().get(i);String eligible;
            try{eligible=OriginalNpcInterpreter.pageAvailable(page,context)?"条件满足":"条件未满足";}catch(Exception failure){eligible="条件暂不支持："+failure.getMessage();}
            lines.add("第"+(i+1)+"页 "+(i==selected?"【选中】 ":"")+eligible+" / "+trigger(page.trigger()));
            var condition=page.condition();boolean constrained=false;
            for(int n=1;n<=2;n++)if(condition.get("switch"+n+"_valid").getAsBoolean()) {int key=condition.get("switch"+n+"_id").getAsInt();lines.add("  S"+key+" "+name("switches",key)+" = 开；现在 "+readSwitch(player,key));constrained=true;}
            if(condition.get("variable_valid").getAsBoolean()) {int key=condition.get("variable_id").getAsInt();lines.add("  V"+key+" "+name("variables",key)+" >= "+condition.get("variable_value")+"；现在 "+OriginalNpcProgress.readVariable(player,key));constrained=true;}
            if(condition.get("self_switch_valid").getAsBoolean()){String key=condition.get("self_switch_ch").getAsString();lines.add("  独立开关"+key+"=开；现在 "+progress.selfSwitch(id,key));constrained=true;}
            if(condition.get("item_valid").getAsBoolean()){lines.add("  持有道具ID "+condition.get("item_id"));constrained=true;}
            if(condition.get("actor_valid").getAsBoolean()){lines.add("  队员条件Actor "+condition.get("actor_id"));constrained=true;}
            if(!constrained)lines.add("  无页面条件");
            for(var command:page.commands())if(command.code()==111&&command.parameters().size()>=3) {
                var p=command.parameters();int kind=p.get(0).getAsInt();
                if(kind==0) {int key=p.get(1).getAsInt();lines.add("  页内分支 S"+key+" "+name("switches",key)+" == "+(p.get(2).getAsInt()==0)+"；现在 "+readSwitch(player,key));}
                else if(kind==1&&p.size()>=5) {int key=p.get(1).getAsInt();lines.add("  页内分支 V"+key+" "+name("variables",key)+"；现在 "+OriginalNpcProgress.readVariable(player,key)+"，比较参数="+p.subList(2,5));}
            }
            var text=page.commands().stream().filter(c->c.code()==401).limit(2).map(c->c.parameters().get(0).getAsString()).toList();
            lines.add("  "+(text.isEmpty()?"此页无直接文本（可能调用公共事件/为空页）":String.join(" / ",text)));
            lines.add("  指令="+page.commands().size()+"；条件可选不等于整段指令/音画已验收。");
        }
        var response=new JsonObject();response.addProperty("ok",true);response.addProperty("event",id);response.addProperty("message",String.join("\n",lines));return response;
    }

    private static OriginalNpcInterpreter.Context readContext(ServerPlayer player,String event) {
        return (OriginalNpcInterpreter.Context)java.lang.reflect.Proxy.newProxyInstance(DeveloperStoryService.class.getClassLoader(),new Class[]{OriginalNpcInterpreter.Context.class},(proxy,method,args)->switch(method.getName()) {
            case "switchValue"->readSwitch(player,(Integer)args[0]);
            case "variable"->OriginalNpcProgress.readVariable(player,(Integer)args[0]);
            case "selfSwitch"->OriginalNpcProgress.get(player.server).selfSwitch(event,(String)args[0]);
            case "itemCount"->OriginalNpcInventoryService.count(player,(Integer)args[0],(Integer)args[1]);
            case "hasActor"->OriginalActorBridge.isPlayerActor(player,(Integer)args[0]);
            default->throw new UnsupportedOperationException("只读选页上下文："+method.getName());
        });
    }

    private static String plan(ServerPlayer player,String key,String value,CompoundTag after) {
        var state=OriginalNpcProgress.load(after.getCompound("Progress"));var bosses=SceneSpawnerBossData.load(after.getCompound("Bosses"));
        String label;
        if(key.equals("beast_before")||key.equals("beast_after")) {
            boolean beaten=key.equals("beast_after");state.setVariable(11,beaten?1:0);bosses.setStoryProgress(beaten?1:0);
            state.setSwitch(56,beaten);state.setSwitch(57,beaten);state.setSwitch(7,false);state.setSwitch(576,false);state.setSelfSwitch("map052_ev023","A",beaten);
            label=beaten?"猎颅之兽战后对白预设：V11=1 / S56、57=开":"猎颅之兽战前对白预设：V11=0 / S56、57=关";
            label+="\n仅对白条件：不击杀/复活Boss，不补发灵魂和掉落。";
        } else if(key.startsWith("load:")) {
            int slot=slot(key.substring(5));var point=DeveloperStoryPoints.get(player.server).point(player.getUUID(),slot);
            if(point==null)throw new IllegalArgumentException("测试点为空。");after.merge(point.getCompound("State"));after.put("Progress",worldState(after.getCompound("Progress")));return "读取测试点 "+slot+" "+point.getString("Name");
        } else if(key.startsWith("S:")) {
            int id=id(key.substring(2),"switches");if(protectedSwitch(id))throw new IllegalArgumentException("此开关由个人SEN/誓约拥有，仅查看；请使用对应系统。");
            boolean enabled=bool(value);if(id==20&&!enabled)throw new IllegalArgumentException("本模组固定省略H场景，不能关闭S20。");
            state.setSwitch(id,enabled);label="世界开关 S"+id+" "+name("switches",id)+" = "+enabled;
        } else if(key.startsWith("V:")) {
            int id=id(key.substring(2),"variables");if(protectedVariable(id))throw new IllegalArgumentException("此变量属于职业/个人SEN/誓约，只读，不能直接覆写。");
            long number=Long.parseLong(value);if(id==11&&(number<0||number>Integer.MAX_VALUE))throw new IllegalArgumentException("主线V11必须为非负整数。");
            state.setVariable(id,number);if(id==11)bosses.setStoryProgress((int)number);label="世界变量 V"+id+" "+name("variables",id)+" = "+number;
        } else if(key.startsWith("self:")||key.startsWith("hidden:")||key.startsWith("resume:")) {
            String[] fields=key.split(":");String event=fields[1];if(BSOriginalNpcData.getEvent(event)==null)throw new IllegalArgumentException("未知事件。");
            if(OriginalNpcProgress.get(player.server).hasOtherConversation(player.getUUID(),java.util.Set.of(event)))throw new IllegalArgumentException("其他玩家有该事件的未完对话，不可修改。");
            if(fields[0].equals("self")){if(fields.length!=3||!fields[2].matches("[A-D]"))throw new IllegalArgumentException("独立开关只能A-D");state.setSelfSwitch(event,fields[2],bool(value));label="独立开关 "+event+" "+fields[2]+" = "+value;}
            else if(fields[0].equals("hidden")){state.setHidden(event,bool(value));label="隐藏事件 "+event+" = "+value;}
            else {
                after.getCompound("Resume").remove(event);return "清除本人对话续接 "+event+"；从当前有效页开头重读，已获得奖励不回收，重跑赠礼可能再发。";
            }
        } else throw new IllegalArgumentException("未知状态项。");
        after.put("Progress",state.developerState());after.put("Bosses",bosses.save(new CompoundTag()));return label;
    }

    public static CompoundTag capture(ServerPlayer player) {
        var state=new CompoundTag();state.put("Progress",worldState(OriginalNpcProgress.get(player.server).developerState()));state.put("Bosses",SceneSpawnerBossData.get(player.server).save(new CompoundTag()));
        var resumes=new CompoundTag();var progress=OriginalNpcProgress.get(player.server);
        for(String event:progress.conversationIds(player.getUUID()))resumes.putByteArray(event,progress.conversation(player.getUUID(),event).toString().getBytes(StandardCharsets.UTF_8));
        state.put("Resume",resumes);return state;
    }
    private static void restore(ServerPlayer player,CompoundTag state) {
        var progress=OriginalNpcProgress.get(player.server);var current=progress.developerState();var target=worldState(state.getCompound("Progress"));
        var switches=new java.util.TreeSet<Integer>();for(int id:target.getIntArray("Switches"))switches.add(id);for(int id:current.getIntArray("Switches"))if(protectedSwitch(id))switches.add(id);
        target.putIntArray("Switches",switches.stream().mapToInt(Integer::intValue).toArray());
        var variables=current.getCompound("Variables");for(String id:variables.getAllKeys())if(protectedVariable(Integer.parseInt(id)))target.getCompound("Variables").put(id,variables.get(id).copy());
        progress.restoreDeveloperState(target);SceneSpawnerBossData.get(player.server).restoreDeveloperState(state.getCompound("Bosses"));
        var resumes=state.getCompound("Resume");for(String event:progress.conversationIds(player.getUUID()))progress.setConversation(player.getUUID(),event,null);
        for(String event:resumes.getAllKeys())progress.setConversation(player.getUUID(),event,JsonParser.parseString(new String(resumes.getByteArray(event),StandardCharsets.UTF_8)).getAsJsonObject());
        persist(player);refresh(player);
    }
    private static CompoundTag worldState(CompoundTag source) {
        var state=source.copy();state.putIntArray("Switches",java.util.Arrays.stream(source.getIntArray("Switches")).filter(id->!protectedSwitch(id)).toArray());
        var variables=state.getCompound("Variables");for(String key:java.util.Set.copyOf(variables.getAllKeys()))if(protectedVariable(Integer.parseInt(key)))variables.remove(key);
        return state;
    }
    private static void commit(ServerPlayer player,Change change)throws Exception {
        validateResumeChanges(player,change.before(),change.after());
        backup(player,change.before(),"before");restore(player,change.after());
        UNDO.computeIfAbsent(player.server,ignored->new java.util.HashMap<>()).put(player.getUUID(),new Change(change.before(),capture(player),change.label()));
    }
    private static void validateResumeChanges(ServerPlayer player,CompoundTag before,CompoundTag after) {
        var first=before.getCompound("Resume");var second=after.getCompound("Resume");var events=new java.util.HashSet<>(first.getAllKeys());events.addAll(second.getAllKeys());
        for(String event:events)if(!java.util.Objects.equals(first.get(event),second.get(event))
                &&OriginalNpcProgress.get(player.server).hasOtherConversation(player.getUUID(),java.util.Set.of(event)))throw new IllegalArgumentException("其他玩家已占用该事件的续接，未覆盖："+event);
    }
    private static void backup(ServerPlayer player,CompoundTag state,String label)throws Exception {
        var path=player.server.getWorldPath(LevelResource.ROOT).resolve("data/blacksouls_dev_story_backups");Files.createDirectories(path);
        NbtIo.writeCompressed(state,path.resolve(System.currentTimeMillis()+"-"+player.getUUID()+"-"+UUID.randomUUID()+"-"+label+".nbt").toFile());
    }
    private static void persist(ServerPlayer player) {player.server.overworld().getDataStorage().save();}
    private static void refresh(ServerPlayer player) {
        for(var level:player.server.getAllLevels())for(var entity:level.getAllEntities()) {
            if(entity instanceof EntityOriginalNpc npc)OriginalNpcService.refreshPage(npc,player);
            else if(entity instanceof EntityCheshireCat cat)cat.refreshVisibility();
        }
        CheshireSourceEncounter.forget(player);
    }
    private static void idle(ServerPlayer player,boolean clearResume) {
        for(var other:player.server.getPlayerList().getPlayers())if(OriginalOpeningService.managed(other)||OriginalNpcService.isConversing(other)
                ||yuki135.blacksouls.combat.TurnBattleManager.isInBattle(other)||OriginalWorldTransferService.pending(other))throw new IllegalArgumentException("有玩家正在剧情/对话/战斗/传送，请结束后再改状态。");
        if(OriginalNpcMovementData.get(player.server).tickets.values().stream().anyMatch(t->!t.complete&&!t.released)
                ||OriginalNpcAnimationData.get(player.server).tickets.values().stream().anyMatch(t->!t.released&&!OriginalNpcAnimationService.isComplete(player.server,t.id)))throw new IllegalArgumentException("尚有未完成的原作路线或动画，请先结束演出。");
        if(!clearResume&&OriginalNpcProgress.get(player.server).hasSavedConversations())throw new IllegalArgumentException("有存档对话续接。请先完成对话，或在事件页清除本人的指定续接后再切进度。");
    }
    private static String difference(CompoundTag before,CompoundTag after) {
        var a=OriginalNpcProgress.load(before.getCompound("Progress"));var b=OriginalNpcProgress.load(after.getCompound("Progress"));var lines=new ArrayList<String>();
        for(int i=1;i<NAMES.getAsJsonArray("switches").size();i++)if(a.switchValue(i)!=b.switchValue(i))lines.add("S"+i+" "+name("switches",i)+": "+a.switchValue(i)+" → "+b.switchValue(i));
        for(int i=1;i<NAMES.getAsJsonArray("variables").size();i++)if(a.variable(i)!=b.variable(i))lines.add("V"+i+" "+name("variables",i)+": "+a.variable(i)+" → "+b.variable(i));
        for(String field:List.of("SelfSwitches","HiddenEvents"))if(!before.getCompound("Progress").get(field).equals(after.getCompound("Progress").get(field)))lines.add(field+" 将变化（选中事件/测试点记录）");
        if(!before.getCompound("Bosses").equals(after.getCompound("Bosses")))lines.add("Boss主线/击破记录将按预览或测试点更新");
        if(!before.getCompound("Resume").equals(after.getCompound("Resume")))lines.add("本人的对话续接记录将变化");
        return lines.isEmpty()?"当前已是该状态。":String.join("\n",lines);
    }
    private static boolean readSwitch(ServerPlayer player,int id) {return id==20||((id==11||id==12)?OriginalPlayerMoveSpeed.switchValue(player,id):OriginalNpcProgress.readSwitch(player,id));}
    private static boolean protectedSwitch(int id) {return id==11||id==12||OriginalSenService.isThresholdSwitch(id)||OriginalCovenantData.forSwitch(id)!=null||id==157;}
    private static boolean protectedVariable(int id) {return id==6||id==8||id==14||id==38||id==68||OriginalCovenantData.forVariable(id)!=null;}
    private static boolean bool(String value) {if(!value.equals("true")&&!value.equals("false"))throw new IllegalArgumentException("开关值为true或false");return Boolean.parseBoolean(value);}
    private static int slot(String value) {int slot=Integer.parseInt(value);if(slot<1||slot>3)throw new IllegalArgumentException("测试点槽位为1至3");return slot;}
    private static int id(String value,String category) {int id=Integer.parseInt(value);if(id<1||id>=NAMES.getAsJsonArray(category).size())throw new IllegalArgumentException("编号不在原作范围。");return id;}
    private static String name(String category,int id) {
        var values=NAMES.getAsJsonArray(category);String original=id<values.size()&&!values.get(id).isJsonNull()?values.get(id).getAsString():"";
        String translated=category.equals("variables")?switch(id){case 11->"主线进度";case 38->"个人SEN";default->"";}
                :switch(id){case 7->"战斗演出中";case 20->"省略H场景";case 56->"猎颅兽已击破";case 57->"猎颅兽初见已演出";case 107->"兔洞柴郡猫出现";case 108->"已与兔洞柴郡猫交谈";case 109->"兔洞柴郡猫赠礼待领取";default->"";};
        return translated.isEmpty()?original:translated+"（"+original+"）";
    }
    private static String trigger(int type) {return switch(type){case 0->"交谈";case 1->"玩家接触";case 2->"事件接触";case 3->"自动执行";case 4->"并行";default->"触发"+type;};}
    private static JsonObject names() {try(var input=DeveloperStoryService.class.getResourceAsStream("/data/blacksouls/developer_story_names.json")){return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){throw new IllegalStateException(e);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {PENDING.remove(event.getServer());UNDO.remove(event.getServer());}
}
