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

import java.util.Map;
import java.util.Optional;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.WITHDRAW_CONTRACT_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class WithdrawContractCommand extends BaseCommand {

    @Dependency
    private MCSDMarkets plugin;

    @Subcommand("contract withdraw")
    @CommandPermission(WITHDRAW_CONTRACT_PERMISSION)
    @Syntax("<material> [quantity]")
    @Description("Take the material from the contract quantity and add to your inventory")
    @CommandCompletion("@materials")
    public void handleWithdrawContractCommand(Player player, String materialName, @co.aikar.commands.annotation.Optional @Conditions("gt0") final Integer quantity) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (!optionalMaterial.isPresent()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Material material = optionalMaterial.get();

        final boolean withdrawAll = quantity == null;
        Inventory inventory = player.getInventory();
        int taken = ItemUtil.takeContracts(plugin, material, inventory, withdrawAll ? -1 : quantity);

        if ((!withdrawAll && taken < quantity) || (withdrawAll && taken == 0)) {
            ItemStack existingContractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, inventory);

            if (taken > 0) {
                if (existingContractItem == null) {
                    inventory.addItem(ItemUtil.getPurchaseContractItem(plugin, material, taken));
                } else {
                    ItemUtil.addQuantity(plugin, existingContractItem, taken);
                }
            }

            player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
            return;
        }

        Map<Integer, ItemStack> map = inventory.addItem(new ItemStack(material, taken));
        ItemStack failed = map.get(0);

        if (failed != null && failed.getAmount() > 0) {
            inventory.removeItem(new ItemStack(material, taken - failed.getAmount()));
            ItemStack contractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, inventory);

            if (contractItem == null) {
                inventory.addItem(ItemUtil.getPurchaseContractItem(plugin, material, taken));
            } else {
                ItemUtil.addQuantity(plugin, contractItem, taken);
            }

            player.sendMessage(Message.NO_INVENTORY_SPACE.getText());
            return;
        }

        player.sendMessage(Message.CONTRACT_WITHDRAWAL.getText());
    }

}
