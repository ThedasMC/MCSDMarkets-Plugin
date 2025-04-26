package com.thedasmc.mcsdmarketsplugin.listener;

import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseEventListener implements Listener {

    private final GUISupport guiSupport;

    public InventoryCloseEventListener(GUISupport guiSupport) {
        this.guiSupport = guiSupport;
    }

    @EventHandler
    public void handleInventoryCloseEvent(InventoryCloseEvent event) {
        guiSupport.inventoryClosed(event.getPlayer().getUniqueId());
    }
}
