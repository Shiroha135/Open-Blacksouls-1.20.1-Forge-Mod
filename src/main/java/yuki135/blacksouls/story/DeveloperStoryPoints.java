package yuki135.blacksouls.story;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;

public final class DeveloperStoryPoints extends SavedData {
    private final Map<String,CompoundTag> points=new HashMap<>();
    private static String key(UUID owner,int slot) {if(slot<1||slot>3)throw new IllegalArgumentException("测试点槽位为1至3");return owner+"_"+slot;}
    public static DeveloperStoryPoints get(MinecraftServer server) {return server.overworld().getDataStorage().computeIfAbsent(DeveloperStoryPoints::load,DeveloperStoryPoints::new,"blacksouls_developer_story_points");}
    public CompoundTag point(UUID owner,int slot) {var value=points.get(key(owner,slot));return value==null?null:value.copy();}
    public void store(UUID owner,int slot,String name,CompoundTag state) {var value=new CompoundTag();value.putString("Name",name.substring(0,Math.min(40,name.length())));value.put("State",state.copy());points.put(key(owner,slot),value);setDirty();}
    public static DeveloperStoryPoints load(CompoundTag tag) {var data=new DeveloperStoryPoints();var entries=tag.getCompound("Points");for(String key:entries.getAllKeys())if(key.matches("[a-f0-9-]{36}_[1-3]"))data.points.put(key,entries.getCompound(key).copy());return data;}
    @Override public CompoundTag save(CompoundTag tag) {var entries=new CompoundTag();points.forEach((key,value)->entries.put(key,value.copy()));tag.put("Points",entries);return tag;}
}
