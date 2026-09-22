package com.artillexstudios.axafkzone.reward;

import com.artillexstudios.axafkzone.api.events.PlayerRewardEvent;
import com.artillexstudios.axafkzone.zones.Zone;
import com.artillexstudios.axapi.scheduler.ScheduledTask;
import com.artillexstudios.axapi.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RewardTest {
    @Test
    void notifiesOnlyAfterCommandsAndItemsIncludingOverflow() {
        Scheduler scheduler = mock(Scheduler.class);
        Player player = mock(Player.class);
        Zone zone = mock(Zone.class);
        World world = mock(World.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        PluginManager plugins = mock(PluginManager.class);
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Location location = new Location(world, 0, 64, 0);
        Queue<Consumer<ScheduledTask>> globalTasks = new ArrayDeque<>();
        Queue<Consumer<ScheduledTask>> playerTasks = new ArrayDeque<>();
        doAnswer(call -> globalTasks.add(call.getArgument(0))).when(scheduler).run(any(Consumer.class));
        doAnswer(call -> playerTasks.add(call.getArgument(1))).when(scheduler).run(eq(player), any(), any());
        when(player.getName()).thenReturn("Tester");
        when(player.getLocation()).thenReturn(location);
        when(player.getInventory()).thenReturn(inventory);

        Reward reward = new Reward(Map.of("commands", List.of("give %player% diamond")));
        ItemStack configuredItem = new ItemStack(Material.DIAMOND, 3);
        reward.getItems().add(configuredItem);
        ItemStack overflow = new ItemStack(Material.DIAMOND, 1);
        when(inventory.addItem(any(ItemStack.class))).thenAnswer(call -> {
            ItemStack deliveredItem = call.getArgument(0);
            assertNotSame(configuredItem, deliveredItem);
            deliveredItem.setAmount(1);
            return new HashMap<>(Map.of(0, overflow));
        });

        try (MockedStatic<Scheduler> scheduling = mockStatic(Scheduler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduling.when(Scheduler::get).thenReturn(scheduler);
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

            reward.run(player, zone);
            verifyNoInteractions(plugins, inventory);
            globalTasks.remove().accept(null);
            bukkit.verify(() -> Bukkit.dispatchCommand(console, "give Tester diamond"));
            verifyNoInteractions(plugins, inventory);
            playerTasks.remove().accept(null);

            ArgumentCaptor<PlayerRewardEvent> event = ArgumentCaptor.forClass(PlayerRewardEvent.class);
            var order = inOrder(inventory, world, plugins);
            order.verify(inventory).addItem(any(ItemStack.class));
            order.verify(world).dropItem(location, overflow);
            order.verify(plugins).callEvent(event.capture());
            assertSame(player, event.getValue().getPlayer());
            assertSame(reward, event.getValue().getReward());
            assertSame(zone, event.getValue().getZone());
            assertSame(PlayerRewardEvent.getHandlerList(), event.getValue().getHandlers());
            assertFalse(event.getValue().isAsynchronous());
            assertEquals(3, configuredItem.getAmount());
        }
    }

    @Test
    void repeatedDirectCommandOnlyRewardsEachNotifyWithNoZone() {
        Scheduler scheduler = mock(Scheduler.class);
        Player player = mock(Player.class);
        PluginManager plugins = mock(PluginManager.class);
        doAnswer(call -> {
            call.<Consumer<ScheduledTask>>getArgument(0).accept(null);
            return null;
        }).when(scheduler).run(any(Consumer.class));
        doAnswer(call -> {
            call.<Consumer<ScheduledTask>>getArgument(1).accept(null);
            return null;
        }).when(scheduler).run(eq(player), any(), any());
        when(player.getName()).thenReturn("Tester");

        try (MockedStatic<Scheduler> scheduling = mockStatic(Scheduler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduling.when(Scheduler::get).thenReturn(scheduler);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            Reward reward = new Reward(Map.of("commands", List.of("reward %player%")));
            reward.run(player);
            reward.run(player);

            ArgumentCaptor<PlayerRewardEvent> events = ArgumentCaptor.forClass(PlayerRewardEvent.class);
            verify(plugins, times(2)).callEvent(events.capture());
            for (PlayerRewardEvent event : events.getAllValues()) {
                assertSame(reward, event.getReward());
                assertNull(event.getZone());
            }
            verify(player, never()).getInventory();
        }
    }
}
