package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.CREATE_CONTRACT_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class CreateContractCommand extends BaseCommand {

    @Dependency
    private MCSDMarkets plugin;

    @Subcommand("contract create")
    @CommandPermission(CREATE_CONTRACT_PERMISSION)
    @Syntax("<material> [quantity]")
    @Description("Take a specified amount of the specified material from your inventory and add to a contract item or create a new one")
    @CommandCompletion("@materials")
    public void handleCreateContractCommand(Player player, String materialName, @co.aikar.commands.annotation.Optional @Conditions("gt0") final Integer quantity) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Material material = optionalMaterial.get();

        final boolean takeAll = quantity == null;
        Inventory inventory = player.getInventory();
        int taken = ItemUtil.takeItems(plugin, material, inventory, takeAll ? -1 : quantity);

        if ((!takeAll && taken < quantity) || (takeAll && taken == 0)) {
            if (taken > 0)
                inventory.addItem(new ItemStack(material, taken));

            player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
            return;
        }

        ItemStack existingContractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, inventory);

        if (existingContractItem == null && inventory.firstEmpty() == -1) {
            inventory.addItem(new ItemStack(material, taken));
            player.sendMessage(Message.NO_INVENTORY_SPACE.getText());
            return;
        }

        if (existingContractItem == null) {
            inventory.addItem(ItemUtil.getPurchaseContractItem(plugin, material, taken));
        } else {
            ItemUtil.addQuantity(plugin, existingContractItem, taken);
        }

        player.sendMessage(Message.CONTRACT_CREATED.getText());
    }

}
