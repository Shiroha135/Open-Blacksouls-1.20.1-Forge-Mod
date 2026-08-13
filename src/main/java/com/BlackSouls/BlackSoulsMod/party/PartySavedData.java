package com.BlackSouls.BlackSoulsMod.party;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PartySavedData extends SavedData {
    private static final String DATA_NAME = BlackSouls.MODID + "_parties";
    private final List<Entry> parties = new ArrayList<>();

    public List<Entry> parties() {
        return List.copyOf(parties);
    }

    public void replace(List<Entry> entries) {
        parties.clear();
        parties.addAll(entries);
        setDirty();
    }

    public static PartySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PartySavedData::load, PartySavedData::new, DATA_NAME);
    }

    private static PartySavedData load(CompoundTag tag) {
        PartySavedData data = new PartySavedData();
        ListTag parties = tag.getList("Parties", Tag.TAG_COMPOUND);
        for (int i = 0; i < parties.size(); i++) {
            CompoundTag partyTag = parties.getCompound(i);
            if (!partyTag.hasUUID("Id") || !partyTag.hasUUID("Leader")) continue;
            Set<UUID> members = new LinkedHashSet<>();
            ListTag memberTags = partyTag.getList("Members", Tag.TAG_STRING);
            for (int j = 0; j < memberTags.size(); j++) {
                try {
                    members.add(UUID.fromString(memberTags.getString(j)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            UUID leader = partyTag.getUUID("Leader");
            if (members.size() >= 2 && members.size() <= PartyManager.MAX_MEMBERS && members.contains(leader)) {
                data.parties.add(new Entry(partyTag.getUUID("Id"), leader, members));
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag parties = new ListTag();
        for (Entry party : this.parties) {
            CompoundTag partyTag = new CompoundTag();
            partyTag.putUUID("Id", party.id());
            partyTag.putUUID("Leader", party.leader());
            ListTag members = new ListTag();
            for (UUID member : party.members()) members.add(StringTag.valueOf(member.toString()));
            partyTag.put("Members", members);
            parties.add(partyTag);
        }
        tag.put("Parties", parties);
        return tag;
    }

    public record Entry(UUID id, UUID leader, Set<UUID> members) {
        public Entry {
            members = new LinkedHashSet<>(members);
        }
    }
}
