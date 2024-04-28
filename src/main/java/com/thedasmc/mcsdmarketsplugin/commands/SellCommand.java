package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.enums.TransactionType;
import com.thedasmc.mcsdmarketsapi.request.CreateTransactionRequest;
import com.thedasmc.mcsdmarketsapi.response.wrapper.CreateTransactionResponseWrapper;
import com.thedasmc.mcsdmarketsapi.response.wrapper.ItemResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import com.thedasmc.mcsdmarketsplugin.support.messages.MessageVariable;
import com.thedasmc.mcsdmarketsplugin.support.messages.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.BUY_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class SellCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private MCSDMarketsAPI mcsdMarketsAPI;
    @Dependency private Economy economy;

    @Subcommand("sell")
    @CommandPermission(BUY_COMMAND_PERMISSION)
    @Syntax("<material> <quantity>")
    @Description("Sell items")
    @CommandCompletion("@materials")
    public void handleSellCommand(Player player, String materialName, @co.aikar.commands.annotation.Optional @Conditions("gt0") final Integer quantity) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (!optionalMaterial.isPresent()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Material material = optionalMaterial.get();

        final boolean sellAll = quantity == null;
        Inventory inventory = player.getInventory();
        int taken = ItemUtil.takeContracts(plugin, material, inventory, sellAll ? -1 : quantity);

        if (!sellAll && taken < quantity) {
            refund(material, inventory, taken);
            player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemResponseWrapper itemResponseWrapper;

            try {
                itemResponseWrapper = mcsdMarketsAPI.getItem(material.name());
            } catch (IOException e) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                return;
            }

            if (!itemResponseWrapper.isSuccessful()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    refund(material, inventory, quantity);
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, itemResponseWrapper.getErrorResponse().getMessage())));
                });

                return;
            }

            BigDecimal price = itemResponseWrapper.getSuccessfulResponse().getCurrentPrice()
                .multiply(BigDecimal.valueOf(taken));

            CreateTransactionRequest createTransactionRequest = new CreateTransactionRequest();
            createTransactionRequest.setPlayerId(player.getUniqueId());
            createTransactionRequest.setTransactionType(TransactionType.SALE);
            createTransactionRequest.setMaterial(material.name());
            createTransactionRequest.setQuantity(quantity);

            CreateTransactionResponseWrapper createTransactionResponseWrapper;

            try {
                createTransactionResponseWrapper = mcsdMarketsAPI.createTransaction(createTransactionRequest);
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    refund(material, inventory, quantity);
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                });

                return;
            }

            if (!createTransactionResponseWrapper.isSuccessful()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    refund(material, inventory, quantity);
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, createTransactionResponseWrapper.getErrorResponse().getMessage())));
                });

                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                economy.depositPlayer(player, price.doubleValue());
                player.sendMessage(Message.SALE_SUCCESSFUL.getText(new MessageVariable(Placeholder.ITEM, material.name()), new MessageVariable(Placeholder.PRICE, price.toPlainString())));
            });
        });
    }

    private void refund(Material material, Inventory inventory, Integer quantity) {
        if (quantity == 0)
            return;

        ItemStack contractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, inventory);

        if (contractItem == null) {
            contractItem = ItemUtil.getPurchaseContractItem(plugin, material, quantity);
            inventory.addItem(contractItem);
        } else {
            ItemUtil.addQuantity(plugin, contractItem, quantity);
        }
    }

}
