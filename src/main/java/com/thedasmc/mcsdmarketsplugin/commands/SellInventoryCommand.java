package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.thedasmc.mcsdmarketsplugin.support.SellInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.BUY_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class SellInventoryCommand extends BaseCommand {

    @Dependency private SellInventoryManager sellInventoryManager;

    @Subcommand("sell")
    @CommandPermission(BUY_COMMAND_PERMISSION)
    @Description("Open an inventory where everything placed in it is sold. If an item cannot be sold it will be returned to your inventory.")
    public void handleSellInventoryCommand(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "" + ChatColor.BOLD + "Sell Inventory");
        sellInventoryManager.addSellInventory(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

}
