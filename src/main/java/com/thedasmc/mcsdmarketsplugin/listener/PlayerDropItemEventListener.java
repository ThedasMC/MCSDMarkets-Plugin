package com.thedasmc.mcsdmarketsplugin.listener;

import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemEventListener implements Listener {

    private final MCSDMarkets plugin;

    public PlayerDropItemEventListener(MCSDMarkets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handlePlayerDropItemEvent(PlayerDropItemEvent event) {
        if (!event.isCancelled() && !plugin.getConfig().getBoolean("contract-drops-enabled", false) && ItemUtil.isPurchaseContractItem(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Message.CANNOT_DROP_CONTRACT.getText());
        }
    }

}
