package com.ambercode.listener.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerFlaggedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final double suspicionScore;

    public PlayerFlaggedEvent(@NotNull Player player, double suspicionScore) {
        this.player = player;
        this.suspicionScore = suspicionScore;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public double getSuspicionScore() {
        return suspicionScore;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
