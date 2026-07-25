package com.BlackSouls.BlackSoulsMod.event;

import com.BlackSouls.BlackSoulsMod.capability.DoorLockSavedData.DoorLock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class LockedDoorInteractEvent extends Event {
    private final ServerPlayer player;
    private final ServerLevel level;
    private final BlockPos pos;
    private final DoorLock lock;

    public LockedDoorInteractEvent(ServerPlayer player, ServerLevel level, BlockPos pos, DoorLock lock) {
        this.player = player;
        this.level = level;
        this.pos = pos.immutable();
        this.lock = lock;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public DoorLock getLock() {
        return lock;
    }
}
