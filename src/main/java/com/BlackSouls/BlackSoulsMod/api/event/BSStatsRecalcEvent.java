package com.BlackSouls.BlackSoulsMod.api.event;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;


public class BSStatsRecalcEvent extends Event {

    private final Player player;
    private final BSPlayerStats stats;

    public BSStatsRecalcEvent(Player player, BSPlayerStats stats) {
        this.player = player;
        this.stats = stats;
    }

    
    public Player getPlayer() {
        return player;
    }

    
    public BSPlayerStats getStats() {
        return stats;
    }
}
