package com.thedasmc.mcsdmarketsplugin.listener;

import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.enums.TransactionType;
import com.thedasmc.mcsdmarketsapi.request.BatchSellRequest;
import com.thedasmc.mcsdmarketsapi.request.BatchTransactionRequest;
import com.thedasmc.mcsdmarketsapi.response.wrapper.BatchItemResponseWrapper;
import com.thedasmc.mcsdmarketsapi.response.wrapper.BatchSellResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.Constants;
import com.thedasmc.mcsdmarketsplugin.support.SellInventoryManager;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import com.thedasmc.mcsdmarketsplugin.support.messages.MessageVariable;
import com.thedasmc.mcsdmarketsplugin.support.messages.Placeholder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InventoryCloseEventListener implements Listener {

    private final MCSDMarkets plugin;
    private final Economy economy;
    private final MCSDMarketsAPI mcsdMarketsAPI;
    private final GUISupport guiSupport;
    private final SellInventoryManager sellInventoryManager;

    public InventoryCloseEventListener(MCSDMarkets plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.mcsdMarketsAPI = plugin.getMcsdMarketsAPI();
        this.guiSupport = plugin.getGuiSupport();
        this.sellInventoryManager = plugin.getSellInventoryManager();
    }

    @EventHandler
    public void handleInventoryCloseEvent(InventoryCloseEvent event) {
        guiSupport.inventoryClosed(event.getPlayer().getUniqueId());
        handleSellInventoryClose(event);
    }

    private void handleSellInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = event.getPlayer().getUniqueId();

        if (!sellInventoryManager.containsSellInventory(uuid))
            return;

        Inventory inventory = sellInventoryManager.removeSellInventory(uuid);

        if (inventory.getStorageContents().length == 0)
            return;

        final ItemStack[] storageContents = inventory.getStorageContents();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            //Step 1: Get items for the pricing info
            Map<String, Integer> itemAmountMap = Arrays.stream(storageContents)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(itemStack -> itemStack.getType().name(), Collectors.summingInt(ItemStack::getAmount)));

            if (itemAmountMap.isEmpty())
                return;

            BatchItemResponseWrapper getItemsResponseWrapper;

            try {
                getItemsResponseWrapper = mcsdMarketsAPI.getItems(itemAmountMap.keySet());
            } catch (IOException e) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                return;
            }

            if (!getItemsResponseWrapper.isSuccessful()) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, getItemsResponseWrapper.getErrorResponse().getMessage())));
                return;
            }

            //Step 2: Calculate the sale price from the get items response
            BigDecimal saleValue = getItemsResponseWrapper.getSuccessfulResponse().stream()
                .map(itemResponse -> {
                    BigDecimal price = itemResponse.getCurrentPrice();
                    Integer amount = itemAmountMap.getOrDefault(itemResponse.getMaterial(), 0);

                    return price.multiply(BigDecimal.valueOf(amount));
                }).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.DOWN);

            //Step 2: Deposit the funds into the player's account
                //This is done before creating the transactions in case the economy response is unsuccessful. There is no way to undo creating transactions
            EconomyResponse economyResponse;

            try {
                economyResponse = Bukkit.getScheduler().callSyncMethod(plugin, () -> economy.depositPlayer(player, saleValue.doubleValue()))
                    .get(Constants.MAX_SYN_THREAD_WAIT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Bukkit.getLogger().warning(String.format("[%s] - Interrupted while trying to deposit money to player with uuid %s. Amount: %s", plugin.getName(), uuid, saleValue.doubleValue()));
                refundItems(itemAmountMap, player);
                return;
            } catch (ExecutionException e) {
                Bukkit.getLogger().warning(String.format("[%s] - Failed to deposit money to player with uuid %s. Amount: %s", plugin.getName(), uuid, saleValue.doubleValue()));
                player.sendMessage(Message.VAULT_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                refundItems(itemAmountMap, player);
                return;
            } catch (TimeoutException e) {
                Bukkit.getLogger().warning(String.format("[%s] - Timed out while trying to deposit money to player with uuid %s. Amount: %s", plugin.getName(), uuid, saleValue.doubleValue()));
                player.sendMessage(Message.TIMEOUT_ERROR.getText());
                refundItems(itemAmountMap, player);
                return;
            }

            if (!economyResponse.transactionSuccess()) {
                player.sendMessage(Message.VAULT_ERROR.getText(new MessageVariable(Placeholder.ERROR, economyResponse.errorMessage)));
                refundItems(itemAmountMap, player);
                return;
            }

            //Step 3: Create batch sale
            List<BatchTransactionRequest> btrList = itemAmountMap.entrySet().stream()
                .map(entry -> {
                    BatchTransactionRequest btr = new BatchTransactionRequest();
                    btr.setTransactionType(TransactionType.SALE);
                    btr.setMaterial(entry.getKey());
                    btr.setQuantity(entry.getValue());

                    return btr;
                }).toList();

            BatchSellRequest request = new BatchSellRequest();
            request.setPlayerId(uuid);
            request.setTransactions(btrList);

            BatchSellResponseWrapper batchSellResponseWrapper;

            try {
                batchSellResponseWrapper = mcsdMarketsAPI.batchSell(request);
            } catch (IOException e) {
                refundItems(itemAmountMap, player);
                undoDeposit(player, saleValue.doubleValue());
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                return;
            }

            //If unsuccessful tell player, refund items, undo deposit
            if (!batchSellResponseWrapper.isSuccessful()) {
                refundItems(itemAmountMap, player);
                undoDeposit(player, saleValue.doubleValue());
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, batchSellResponseWrapper.getErrorResponse().getMessage())));
                return;
            }

            //Let the player know some items weren't sold
            if (!batchSellResponseWrapper.getSuccessfulResponse().getUnsellableMaterials().isEmpty()) {
                Set<String> unsellable = batchSellResponseWrapper.getSuccessfulResponse().getUnsellableMaterials();

                Map<String, Integer> toRefund = itemAmountMap.entrySet().stream()
                    .filter(entry -> unsellable.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                refundItems(toRefund, player);
                player.sendMessage(Message.INVALID_MATERIALS.getText());
            }

            //Tell the player items were sold
            if (!batchSellResponseWrapper.getSuccessfulResponse().getTransactions().isEmpty())
                player.sendMessage(Message.BATCH_SALE_SUCCESSFUL.getText(new MessageVariable(Placeholder.PRICE, saleValue.toPlainString())));
        });
    }

    private void refundItems(Map<String, Integer> itemAmountMap, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                itemAmountMap.entrySet().stream()
                    .map(entry -> new ItemStack(Material.valueOf(entry.getKey()), entry.getValue()))
                    .forEach(itemStack -> player.getInventory().addItem(itemStack));
            } else {
                File saveFile = new File(plugin.getDataFolder(), "/refund-items/" + player.getUniqueId() + ".yml");
                saveFile.mkdirs();
                FileConfiguration save = YamlConfiguration.loadConfiguration(saveFile);

                itemAmountMap.forEach((key, value) -> save.set(key, save.getInt(key, 0) + value));

                try {
                    save.save(saveFile);
                } catch (IOException e) {
                    Bukkit.getLogger().log(Level.SEVERE, String.format("[%s] - Failed to save refund items for player with uuid %s", plugin.getName(), player.getUniqueId()), e);
                }
            }
        });
    }

    private void undoDeposit(Player player, double amount) {
        Bukkit.getScheduler().runTask(plugin, () -> economy.withdrawPlayer(player, amount));
    }
}
