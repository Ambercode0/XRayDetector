package com.ambercode.listener;

import com.ambercode.XRayDetector;
import com.ambercode.listener.events.PlayerFlaggedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FlagListener implements Listener {

    private final XRayDetector plugin;

    public FlagListener(XRayDetector plugin) {
        this.plugin = plugin;
    }

    private void notifyAdmins(Player flaggedPlayer) {
        Component message = Component.text("Player ")
                .color(NamedTextColor.RED)
                .append(Component.text(flaggedPlayer.getName())
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(" has been flagged for potential X-Ray!")
                        .color(NamedTextColor.RED));

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("xraydetector.notify"))
                .forEach(player -> {
                    player.sendMessage(message);
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 1f);
                });
    }

    @EventHandler
    public void onPlayerFlag(PlayerFlaggedEvent event) {
        notifyAdmins(event.getPlayer());
    }

}
