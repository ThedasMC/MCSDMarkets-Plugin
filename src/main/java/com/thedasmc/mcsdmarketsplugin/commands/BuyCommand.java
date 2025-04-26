package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.enums.TransactionType;
import com.thedasmc.mcsdmarketsapi.request.CreateTransactionRequest;
import com.thedasmc.mcsdmarketsapi.response.impl.ItemResponse;
import com.thedasmc.mcsdmarketsapi.response.wrapper.CreateTransactionResponseWrapper;
import com.thedasmc.mcsdmarketsapi.response.wrapper.ItemResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import com.thedasmc.mcsdmarketsplugin.support.messages.MessageVariable;
import com.thedasmc.mcsdmarketsplugin.support.messages.Placeholder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.BUY_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class BuyCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private MCSDMarketsAPI mcsdMarketsAPI;
    @Dependency private Economy economy;
    @Dependency private PlayerVirtualItemDao playerVirtualItemDao;

    @Subcommand("buy")
    @CommandPermission(BUY_COMMAND_PERMISSION)
    @Syntax("<material> <quantity>")
    @Description("Buy items")
    @CommandCompletion("@materials")
    public void handleBuyCommand(Player player, String materialName, @Conditions("gt0") Integer quantity) {
        final UUID playerId = player.getUniqueId();
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Material material = optionalMaterial.get();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            //STEP 1: Get the item from MCSDMarkets web call for pricing info
            ItemResponseWrapper itemResponseWrapper;

            try {
                itemResponseWrapper = mcsdMarketsAPI.getItem(material.name());
            } catch (IOException e) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                return;
            }

            if (!itemResponseWrapper.isSuccessful()) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, itemResponseWrapper.getErrorResponse().getMessage())));
                return;
            }

            //STEP 2: Check if the player has enough funds to buy the item
            ItemResponse itemResponse = itemResponseWrapper.getSuccessfulResponse();
            BigDecimal cost = itemResponse.getCurrentPrice().multiply(BigDecimal.valueOf(quantity));
            EconomyResponse withdrawResponse;

            try {
                withdrawResponse = Bukkit.getScheduler().callSyncMethod(plugin, () -> economy.withdrawPlayer(player, cost.doubleValue())).get(3, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new RuntimeException("Failed to call sync method to withdraw player funds!", e);
            }

            if (!withdrawResponse.transactionSuccess()) {
                player.sendMessage(Message.VAULT_WITHDRAW_ERROR.getText(new MessageVariable(Placeholder.ERROR, withdrawResponse.errorMessage)));
                return;
            }

            //STEP 3: Save the item to the player's virtual inventory
            PlayerVirtualItemPK pk = new PlayerVirtualItemPK(playerId.toString(), material.name());
            Optional<PlayerVirtualItem> optionalPlayerVirtualItem = playerVirtualItemDao.findById(pk);
            PlayerVirtualItem playerVirtualItem;

            if (optionalPlayerVirtualItem.isPresent()) {
                playerVirtualItem = optionalPlayerVirtualItem.get();
                playerVirtualItem.setQuantity(playerVirtualItem.getQuantity() + quantity.longValue());
            } else {
                playerVirtualItem = new PlayerVirtualItem();
                playerVirtualItem.setId(pk);
                playerVirtualItem.setQuantity(quantity.longValue());
            }

            try {
                playerVirtualItemDao.save(playerVirtualItem);
            } catch (Exception e) {
                player.sendMessage(Message.SAVE_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                Bukkit.getScheduler().callSyncMethod(plugin, () -> economy.depositPlayer(player, cost.doubleValue()));
                return;
            }

            //STEP 4: Call MCSDMarkets to create a transaction to record the purchase
            CreateTransactionRequest request = new CreateTransactionRequest();
            request.setPlayerId(playerId);
            request.setTransactionType(TransactionType.PURCHASE);
            request.setMaterial(material.name());
            request.setQuantity(quantity);

            CreateTransactionResponseWrapper createTransactionResponseWrapper;

            try {
                createTransactionResponseWrapper = mcsdMarketsAPI.createTransaction(request);
            } catch (IOException e) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                Bukkit.getScheduler().callSyncMethod(plugin, () -> economy.depositPlayer(player, cost.doubleValue()));
                return;
            }

            if (!createTransactionResponseWrapper.isSuccessful()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    economy.depositPlayer(player, cost.doubleValue());
                    ItemStack contractItem = ItemUtil.findFirstPurchaseContractItem(plugin, material, player.getInventory());
                    ItemUtil.subtractQuantity(plugin, contractItem, quantity);
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, createTransactionResponseWrapper.getErrorResponse().getMessage())));
                });
            }
        });
    }

}
