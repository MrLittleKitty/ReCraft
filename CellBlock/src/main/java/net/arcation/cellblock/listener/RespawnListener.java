package net.arcation.cellblock.listener;

import net.arcation.cellblock.api.CellBlockManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class RespawnListener implements Listener {

    private CellBlockManager manager;

    public RespawnListener(final CellBlockManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleRespawn(final PlayerRespawnEvent event) {
        final UUID player = event.getPlayer().getUniqueId();
        if (manager.isImprisoned(player)) {
            event.setRespawnLocation(manager.getPrisonLocation(player));
        }
    }
}
