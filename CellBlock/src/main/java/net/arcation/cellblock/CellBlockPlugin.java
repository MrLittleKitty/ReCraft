package net.arcation.cellblock;

import net.arcation.cellblock.api.CellBlockManager;
import net.arcation.cellblock.api.CellItemManager;
import net.arcation.cellblock.listener.RespawnListener;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CellBlockPlugin extends JavaPlugin implements CellBlockManager {

    private final Map<UUID, Location> cellBlockLocations = new HashMap<>();

    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new RespawnListener(this), this);
        System.out.println("Enabled FUCK1");
    }

    @Override
    public void onDisable() {
        cellBlockLocations.clear();
        System.out.println("Disabled CellBlockPlugin");
    }

    @Override
    public boolean isImprisoned(final UUID player) {
        return cellBlockLocations.containsKey(player);
    }

    @Override
    public Location getPrisonLocation(UUID player) {
        return cellBlockLocations.get(player);
    }

    @Override
    public void freePlayer(final UUID player) {
        cellBlockLocations.remove(player);
    }

    @Override
    public void imprisonPlayer(final UUID player, final Location cellLocation) {
        cellBlockLocations.put(player, cellLocation);
    }

    @Override
    public CellItemManager getCellItemManager() {
        return null;
    }
}
