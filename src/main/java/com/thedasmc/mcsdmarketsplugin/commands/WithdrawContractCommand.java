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

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.WITHDRAW_CONTRACT_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class WithdrawContractCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;

    @Subcommand("contract withdraw")
    @CommandPermission(WITHDRAW_CONTRACT_PERMISSION)
    @Syntax("<material> [quantity]")
    @Description("Take the material from the contract quantity and add to your inventory")
    @CommandCompletion("@materials")
    public void handleWithdrawContractCommand(Player player, String materialName, @Optional @Conditions("gt0") final Integer quantity) {
        Material material = Material.getMaterial(materialName.trim().toUpperCase());

        if (material == null) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        final boolean withdrawAll = quantity == null;
        Inventory inventory = player.getInventory();
        int amountSubtracted = 0;
        int foundQuantity = 0;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack itemStack = inventory.getItem(slot);

            if (itemStack == null || !ItemUtil.isPurchaseContractItem(plugin, material, itemStack))
                continue;

            int q = ItemUtil.getPurchaseContractQuantity(plugin, itemStack);
            foundQuantity += q;

            if (!withdrawAll && foundQuantity > quantity) {
                int newQuantity = foundQuantity - quantity;
                int toSub = q - newQuantity;
                ItemUtil.subtractQuantity(plugin, itemStack, toSub);
                amountSubtracted += toSub;
            } else {//remove item
                inventory.setItem(slot, null);
                amountSubtracted += q;
            }

            if (!withdrawAll && foundQuantity >= quantity)
                break;
        }

        if ((!withdrawAll && foundQuantity < quantity) || (withdrawAll && amountSubtracted == 0)) {
            ItemStack existingContractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, inventory);

            if (amountSubtracted > 0) {
                if (existingContractItem == null) {
                    inventory.addItem(ItemUtil.getPurchaseContractItem(plugin, material, amountSubtracted));
                } else {
                    ItemUtil.addQuantity(plugin, existingContractItem, amountSubtracted);
                }
            }

            player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
            return;
        }

        Map<Integer, ItemStack> map = inventory.addItem(new ItemStack(material, amountSubtracted));
        ItemStack failed = map.get(0);

        if (failed != null && failed.getAmount() > 0) {
            inventory.removeItem(new ItemStack(material, amountSubtracted - failed.getAmount()));
            ItemStack contractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, inventory);

            if (contractItem == null) {
                inventory.addItem(ItemUtil.getPurchaseContractItem(plugin, material, amountSubtracted));
            } else {
                ItemUtil.addQuantity(plugin, contractItem, amountSubtracted);
            }

            player.sendMessage(Message.NO_INVENTORY_SPACE.getText());
            return;
        }

        player.sendMessage(Message.CONTRACT_WITHDRAWAL.getText());
    }

}
