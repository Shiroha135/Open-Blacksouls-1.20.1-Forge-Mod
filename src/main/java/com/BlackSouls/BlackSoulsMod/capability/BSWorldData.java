package com.BlackSouls.BlackSoulsMod.capability;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.util.BonfireMetadata;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BSWorldData extends SavedData {
    private static final String DATA_NAME = BlackSouls.MODID + "_WorldData";
    private static final int CURRENT_BONFIRE_DATA_VERSION = 1;
    public int difficulty = 0;
    public int deathCount = 0;
    public int loopCount = 0;
    private boolean revengeMode = false;
    private boolean deathMode = false;
    private boolean legendaryMode = false;
    private boolean maliceMode = false;
    private boolean eternityMode = false;
    private boolean revengeUnlocked = false;
    private boolean deathUnlocked = false;
    private boolean legendaryUnlocked = false;
    private boolean maliceUnlocked = false;
    private boolean eternityUnlocked = false;
    private int bonfireDataVersion = CURRENT_BONFIRE_DATA_VERSION;
    private int redHoodStoryStage = 0;
    private boolean redHoodAwaitingNextBonfire = false;
    private String redHoodLastBonfireDimension = "";
    private long redHoodLastBonfirePosition = 0L;

    public List<BonfireEntry> activatedBonfires = new ArrayList<>();

    public BSWorldData() {}

    public boolean addBonfire(Level level, BlockPos pos, Player player) {
        GlobalPos gpos = GlobalPos.of(level.dimension(), pos);
        BonfireMetadata.Data metadata = BonfireMetadata.read(level, pos);
        for (BonfireEntry entry : activatedBonfires) {
            if (entry.pos.equals(gpos)) {
                if (!Objects.equals(entry.name, metadata.name())
                        || !Objects.equals(entry.description, metadata.description())) {
                    entry.name = metadata.name();
                    entry.description = metadata.description();
                    this.setDirty();
                }
                return false;
            }
        }

        activatedBonfires.add(new BonfireEntry(gpos, metadata.name(), metadata.description()));
        this.setDirty();
        return true;
    }

    public void updateBonfire(GlobalPos pos, String name, String description) {
        for (BonfireEntry entry : activatedBonfires) {
            if (entry.pos.equals(pos)) {
                entry.name = name;
                entry.description = description;
                this.setDirty();
                return;
            }
        }
    }

    public boolean isBonfireActivated(Level level, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
        return activatedBonfires.stream().anyMatch(entry -> entry.pos.equals(globalPos));
    }

    public static BSWorldData load(CompoundTag nbt) {
        BSWorldData data = new BSWorldData();
        data.difficulty = nbt.contains("Difficulty", Tag.TAG_INT) ? nbt.getInt("Difficulty") : 0;
        data.deathCount = nbt.getInt("DeathCount");
        data.loopCount = nbt.getInt("LoopCount");
        data.revengeMode = nbt.getBoolean("RevengeMode");
        data.deathMode = nbt.getBoolean("DeathMode");
        data.legendaryMode = nbt.getBoolean("LegendaryMode");
        data.maliceMode = nbt.getBoolean("MaliceMode");
        data.eternityMode = nbt.getBoolean("EternityMode");
        data.revengeUnlocked = nbt.getBoolean("RevengeUnlocked");
        data.deathUnlocked = nbt.getBoolean("DeathUnlocked");
        data.legendaryUnlocked = nbt.getBoolean("LegendaryUnlocked");
        data.maliceUnlocked = nbt.getBoolean("MaliceUnlocked");
        data.eternityUnlocked = nbt.getBoolean("EternityUnlocked");
        data.redHoodStoryStage = Math.max(0, nbt.getInt("RedHoodStoryStage"));
        data.redHoodAwaitingNextBonfire = nbt.getBoolean("RedHoodAwaitingNextBonfire");
        data.redHoodLastBonfireDimension = nbt.getString("RedHoodLastBonfireDimension");
        data.redHoodLastBonfirePosition = nbt.getLong("RedHoodLastBonfirePosition");
        data.bonfireDataVersion = nbt.contains("BonfireDataVersion", Tag.TAG_INT)
                ? nbt.getInt("BonfireDataVersion")
                : 0;
        if (nbt.contains("Bonfires", Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList("Bonfires", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                data.activatedBonfires.add(BonfireEntry.load(entryTag));
            }
        }
        if (data.bonfireDataVersion < CURRENT_BONFIRE_DATA_VERSION) {
            data.activatedBonfires.clear();
            data.bonfireDataVersion = CURRENT_BONFIRE_DATA_VERSION;
            data.setDirty();
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.putInt("Difficulty", this.difficulty);
        nbt.putInt("DeathCount", this.deathCount);
        nbt.putInt("LoopCount", this.loopCount);
        nbt.putBoolean("RevengeMode", this.revengeMode);
        nbt.putBoolean("DeathMode", this.deathMode);
        nbt.putBoolean("LegendaryMode", this.legendaryMode);
        nbt.putBoolean("MaliceMode", this.maliceMode);
        nbt.putBoolean("EternityMode", this.eternityMode);
        nbt.putBoolean("RevengeUnlocked", this.revengeUnlocked);
        nbt.putBoolean("DeathUnlocked", this.deathUnlocked);
        nbt.putBoolean("LegendaryUnlocked", this.legendaryUnlocked);
        nbt.putBoolean("MaliceUnlocked", this.maliceUnlocked);
        nbt.putBoolean("EternityUnlocked", this.eternityUnlocked);
        nbt.putInt("RedHoodStoryStage", this.redHoodStoryStage);
        nbt.putBoolean("RedHoodAwaitingNextBonfire", this.redHoodAwaitingNextBonfire);
        nbt.putString("RedHoodLastBonfireDimension", this.redHoodLastBonfireDimension);
        nbt.putLong("RedHoodLastBonfirePosition", this.redHoodLastBonfirePosition);
        nbt.putInt("BonfireDataVersion", this.bonfireDataVersion);
        ListTag listTag = new ListTag();
        for (BonfireEntry entry : this.activatedBonfires) {
            listTag.add(entry.save());
        }
        nbt.put("Bonfires", listTag);
        return nbt;
    }

    public static BSWorldData get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(BSWorldData::load, BSWorldData::new, DATA_NAME);
    }

    public boolean isRevengeMode() {
        return revengeMode;
    }

    public boolean isDeathMode() {
        return deathMode;
    }

    public boolean isEternityMode() {
        return eternityMode;
    }

    public boolean isLegendaryMode() {
        return legendaryMode;
    }

    public boolean isMaliceMode() {
        return maliceMode;
    }

    public boolean isRevengeUnlocked() {
        return revengeUnlocked;
    }

    public boolean isDeathUnlocked() {
        return deathUnlocked;
    }

    public boolean isLegendaryUnlocked() {
        return legendaryUnlocked;
    }

    public boolean isMaliceUnlocked() {
        return maliceUnlocked;
    }

    public boolean isEternityUnlocked() {
        return eternityUnlocked;
    }

    public boolean toggleRevengeMode() {
        revengeMode = !revengeMode;
        setDirty();
        return revengeMode;
    }

    public boolean toggleDeathMode() {
        deathMode = !deathMode;
        setDirty();
        return deathMode;
    }

    public boolean toggleLegendaryMode() {
        legendaryMode = !legendaryMode;
        setDirty();
        return legendaryMode;
    }

    public boolean toggleMaliceMode() {
        maliceMode = !maliceMode;
        setDirty();
        return maliceMode;
    }

    public boolean toggleEternityMode() {
        eternityMode = !eternityMode;
        setDirty();
        return eternityMode;
    }

    public void setRevengeMode(boolean enabled) {
        revengeMode = enabled;
        setDirty();
    }

    public void setDeathMode(boolean enabled) {
        deathMode = enabled;
        setDirty();
    }

    public void setLegendaryMode(boolean enabled) {
        legendaryMode = enabled;
        setDirty();
    }

    public void setMaliceMode(boolean enabled) {
        maliceMode = enabled;
        setDirty();
    }

    public void setEternityMode(boolean enabled) {
        eternityMode = enabled;
        setDirty();
    }

    public void unlockRevengeMode() {
        revengeUnlocked = true;
        setDirty();
    }

    public void unlockDeathMode() {
        deathUnlocked = true;
        setDirty();
    }

    public void unlockLegendaryMode() {
        legendaryUnlocked = true;
        setDirty();
    }

    public void unlockMaliceMode() {
        maliceUnlocked = true;
        setDirty();
    }

    public void unlockEternityMode() {
        eternityUnlocked = true;
        setDirty();
    }

    public boolean removeBonfire(Level level, BlockPos pos) {
        GlobalPos gpos = GlobalPos.of(level.dimension(), pos);
        boolean removed = activatedBonfires.removeIf(entry -> entry.pos.equals(gpos));
        if (removed) {
            this.setDirty();
        }
        return removed;
    }

    public int getRedHoodStoryStage() {
        return this.redHoodStoryStage;
    }

    public boolean isRedHoodAwaitingNextBonfire() {
        return this.redHoodAwaitingNextBonfire;
    }

    public boolean isRedHoodLastBonfire(GlobalPos bonfire) {
        return bonfire != null
                && !this.redHoodLastBonfireDimension.isBlank()
                && this.redHoodLastBonfireDimension.equals(bonfire.dimension().location().toString())
                && this.redHoodLastBonfirePosition == bonfire.pos().asLong();
    }

    public void advanceRedHoodStory(GlobalPos previousBonfire) {
        this.redHoodStoryStage++;
        this.redHoodAwaitingNextBonfire = true;
        setRedHoodLastBonfire(previousBonfire);
        this.setDirty();
    }

    public void markRedHoodSpawned(GlobalPos bonfire) {
        this.redHoodAwaitingNextBonfire = false;
        setRedHoodLastBonfire(bonfire);
        this.setDirty();
    }

    public void resetRedHoodStory() {
        this.redHoodStoryStage = 0;
        this.redHoodAwaitingNextBonfire = false;
        this.redHoodLastBonfireDimension = "";
        this.redHoodLastBonfirePosition = 0L;
        this.setDirty();
    }

    private void setRedHoodLastBonfire(GlobalPos bonfire) {
        if (bonfire == null) {
            this.redHoodLastBonfireDimension = "";
            this.redHoodLastBonfirePosition = 0L;
            return;
        }
        this.redHoodLastBonfireDimension = bonfire.dimension().location().toString();
        this.redHoodLastBonfirePosition = bonfire.pos().asLong();
    }
}
