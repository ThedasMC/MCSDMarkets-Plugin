package com.thedasmc.mcsdmarketsplugin.listeners;

import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import com.thedasmc.mcsdmarketsplugin.support.gui.PageInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport.*;

public class InventoryClickEventListener implements Listener {

    private final MCSDMarkets plugin;
    private final GUISupport guiSupport;

    public InventoryClickEventListener(MCSDMarkets plugin, GUISupport guiSupport) {
        this.plugin = plugin;
        this.guiSupport = guiSupport;
    }

    @EventHandler
    public void handleInventoryClickEvent(InventoryClickEvent event) {
        PageInfo pageInfo = guiSupport.getPageInfo(event.getWhoClicked().getUniqueId());

        if (pageInfo == null)
            return;

        event.setCancelled(true);
        Inventory inventory = event.getClickedInventory();

        if (!pageInfo.getInventory().equals(inventory))
            return;

        if (event.getCurrentItem() == null)
            return;

        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == PREVIOUS_BUTTON_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(BASE_COMMAND + " view " + (pageInfo.getPage() - 1));
            });
        } else if (slot == NEXT_BUTTON_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(BASE_COMMAND + " view " + (pageInfo.getPage() + 1));
            });
        } else if (slot == CLOSE_BUTTON_SLOT) {
            Bukkit.getScheduler().runTask(plugin, player::closeInventory);
        } else {
            //TODO: Open buy & sell gui
        }
    }

}
