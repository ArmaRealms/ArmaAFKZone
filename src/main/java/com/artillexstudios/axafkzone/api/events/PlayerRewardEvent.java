package com.artillexstudios.axafkzone.api.events;

import com.artillexstudios.axafkzone.reward.Reward;
import com.artillexstudios.axafkzone.zones.Zone;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired once per reward after its console commands have been dispatched and its
 * items have been added to the inventory or dropped at the player's location.
 * This notification is not cancellable. It runs on the main thread on Bukkit
 * and on the player's entity scheduler on Folia.
 * Command dispatch does not guarantee that another plugin granted a prize.
 */
public class PlayerRewardEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Reward reward;
    private final Zone zone;

    public PlayerRewardEvent(@NotNull Player player, @NotNull Reward reward, @Nullable Zone zone) {
        super(player);
        this.reward = reward;
        this.zone = zone;
    }

    /** Returns the awarded reward configuration; listeners should not modify it. */
    @NotNull
    public Reward getReward() {
        return reward;
    }

    /** Returns the source zone, or null for a direct {@link Reward#run(Player)} call. */
    @Nullable
    public Zone getZone() {
        return zone;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
