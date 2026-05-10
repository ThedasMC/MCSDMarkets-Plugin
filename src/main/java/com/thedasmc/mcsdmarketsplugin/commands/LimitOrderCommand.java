package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.tchristofferson.betterscheduler.BSCallable;
import com.tchristofferson.betterscheduler.TaskQueueRunner;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.enums.TransactionType;
import com.thedasmc.mcsdmarketsapi.request.LimitOrderPageRequest;
import com.thedasmc.mcsdmarketsapi.request.SubmitLimitOrderRequest;
import com.thedasmc.mcsdmarketsapi.response.impl.LimitOrderPageResponse;
import com.thedasmc.mcsdmarketsapi.response.wrapper.LimitOrderPageResponseWrapper;
import com.thedasmc.mcsdmarketsapi.response.wrapper.LimitOrderResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import com.thedasmc.mcsdmarketsplugin.support.messages.MessageVariable;
import com.thedasmc.mcsdmarketsplugin.support.messages.Placeholder;
import jakarta.persistence.OptimisticLockException;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.*;

@CommandAlias(BASE_COMMAND)
public class LimitOrderCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private Economy economy;
    @Dependency private MCSDMarketsAPI mcsdMarketsAPI;
    @Dependency private TaskQueueRunner taskQueueRunner;
    @Dependency private PlayerVirtualItemDao playerVirtualItemDao;
    @Dependency private GUISupport guiSupport;

    @Subcommand("limitorder|lo buy")
    @CommandPermission(LIMIT_ORDER_PERMISSION)
    @Syntax("<material> <qty> <price>")
    @Description("Creates a limit order to buy a material at a specific price (per item) or cheaper when available automatically")
    @CommandCompletion("@materials")
    public void handleLimitOrderBuy(Player player, String materialName, @Conditions("gt0") Integer qty, @Conditions("gt0") Double price) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        BigDecimal priceBigDecimal = BigDecimal.valueOf(price).setScale(2, RoundingMode.DOWN);
        double cost = BigDecimal.valueOf(qty).multiply(priceBigDecimal).doubleValue();

        EconomyResponse economyResponse = economy.withdrawPlayer(player, cost);

        if (!economyResponse.transactionSuccess()) {
            player.sendMessage(Message.VAULT_ERROR.getText(new MessageVariable(Placeholder.ERROR, economyResponse.errorMessage)));
            return;
        }

        SubmitLimitOrderRequest request = new SubmitLimitOrderRequest();
        request.setPlayerId(player.getUniqueId());
        request.setMaterial(optionalMaterial.get().name());
        request.setQuantity(qty);
        request.setTransactionType(TransactionType.PURCHASE);
        request.setLimitPrice(priceBigDecimal);

        //Used if an error occurs to deposit the funds back into the player's account
        BSCallable<Void> undoWithdrawalCallable = new BSCallable<>() {
            @Override
            protected Void execute() {
                economy.depositPlayer(player, cost);
                return null;
            }
        };

        taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                LimitOrderResponseWrapper response;

                try {
                    response = mcsdMarketsAPI.submitLimitOrder(request);
                } catch (IOException e) {
                    plugin.getLogger().warning(String.format("Failed to submit limit order for request %s: %s", request, e.getMessage()));
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                    taskQueueRunner.submitSyncTask(undoWithdrawalCallable);
                    return;
                }

                if (!response.isSuccessful()) {
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, response.getErrorResponse().getMessage())));
                    taskQueueRunner.submitSyncTask(undoWithdrawalCallable);
                    return;
                }

                player.sendMessage(Message.LIMIT_ORDER_CREATED.getText(new MessageVariable(Placeholder.QUANTITY, qty.toString()),
                        new MessageVariable(Placeholder.PRICE, priceBigDecimal.toPlainString())));
            }
        });
    }

    @Subcommand("limitorder|lo sell")
    @CommandPermission(LIMIT_ORDER_PERMISSION)
    @Syntax("<material> <qty> <price>")
    @Description("Creates a limit order to sell a material at a specific price (per item) when available automatically")
    @CommandCompletion("@materials")
    public void handleLimitOrderSell(Player player, String materialName, @Conditions("gt0") Integer qty, @Conditions("gt0") Double price) {
        final UUID playerId = player.getUniqueId();
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        final BigDecimal priceBigDecimal = BigDecimal.valueOf(price).setScale(2, RoundingMode.DOWN);

        taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() throws Exception {
                //STEP 1: Get the player virtual item to pull from first
                PlayerVirtualItemPK id = new PlayerVirtualItemPK(playerId.toString(), optionalMaterial.get().name());
                Optional<PlayerVirtualItem> playerVirtualItem = playerVirtualItemDao.findById(id);

                //STEP 2: Calculate how much is needed from where
                int amountNeededInVirtualInventory;
                final AtomicInteger amountNeededInInventory;

                if (playerVirtualItem.isPresent()) {
                    PlayerVirtualItem pvi = playerVirtualItem.get();

                    amountNeededInVirtualInventory = pvi.getQuantity() >= qty ? qty : pvi.getQuantity();
                    amountNeededInInventory = new AtomicInteger(qty - amountNeededInVirtualInventory);
                } else {
                    amountNeededInVirtualInventory = 0;
                    amountNeededInInventory = new AtomicInteger(qty);
                }

                //STEP 3: Attempt to take from virtual inventory
                if (amountNeededInVirtualInventory > 0) {
                    PlayerVirtualItem pvi = playerVirtualItem.get();

                    int retries = 0;
                    while (true) {
                        pvi.setQuantity(pvi.getQuantity() - amountNeededInVirtualInventory);

                        try {
                            if (pvi.getQuantity() == 0) {
                                playerVirtualItemDao.delete(pvi);
                            } else {
                                playerVirtualItemDao.save(pvi);
                            }

                            break;
                        } catch (OptimisticLockException e) {
                            if (++retries > 3) {
                                player.sendMessage(Message.SAVE_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                                throw e;
                            }

                            playerVirtualItem = playerVirtualItemDao.findById(id);

                            if (playerVirtualItem.isEmpty()) {
                                amountNeededInVirtualInventory = 0;
                                amountNeededInInventory.set(qty);
                                break;
                            } else {
                                pvi = playerVirtualItem.get();
                                amountNeededInVirtualInventory = pvi.getQuantity() >= qty ? qty : pvi.getQuantity();
                                amountNeededInInventory.set(qty - amountNeededInVirtualInventory);
                            }
                        }
                    }
                }

                //STEP 4: Attempt to take items from inventory
                boolean successfullyTookItemsFromInventory = true;

                if (amountNeededInInventory.get() > 0) {
                    successfullyTookItemsFromInventory = taskQueueRunner.submitSyncTask(new BSCallable<Boolean>() {
                        @Override
                        protected Boolean execute() {
                            if (!player.isOnline())
                                return false;

                            Inventory inventory = player.getInventory();
                            Map<Integer, ItemStack> originalStacksThatWereRemoved = new HashMap<>();

                            //Go through the player's inventory to see if they have enough of the item AND remove correct quantity
                                //Adds the items back if the player's inventory doesn't have enough
                            int count = 0;
                            for (int i = 0; i < inventory.getSize(); i++) {
                                ItemStack itemStack = inventory.getItem(i);

                                if (itemStack == null || itemStack.getType() != optionalMaterial.get() || itemStack.hasItemMeta())
                                    continue;

                                originalStacksThatWereRemoved.put(i, itemStack.clone());

                                if (amountNeededInInventory.get() - count >= itemStack.getAmount()) {
                                    count += itemStack.getAmount();
                                    inventory.setItem(i, null);
                                } else {
                                    int toTake = amountNeededInInventory.get() - count;
                                    itemStack.setAmount(itemStack.getAmount() - toTake);
                                    count += toTake;
                                }

                                if (count == amountNeededInInventory.get())
                                    return true;
                            }

                            //Didn't find enough items in inventory, put items back
                            originalStacksThatWereRemoved.forEach(inventory::setItem);
                            return false;
                        }
                    }).get(MAX_SYNC_THREAD_WAIT.toMillis(), TimeUnit.MILLISECONDS);
                }

                if (!successfullyTookItemsFromInventory) {
                    player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());

                    //Refund player's virtual inventory
                    if (amountNeededInVirtualInventory > 0)
                        refundVirtualInventory(player, amountNeededInVirtualInventory, playerVirtualItem.get());

                    return;
                }

                SubmitLimitOrderRequest request = new SubmitLimitOrderRequest();
                request.setPlayerId(playerId);
                request.setMaterial(optionalMaterial.get().name());
                request.setQuantity(qty);
                request.setTransactionType(TransactionType.SALE);
                request.setLimitPrice(priceBigDecimal);

                LimitOrderResponseWrapper response = null;
                IOException ioException = null;

                try {
                    response = mcsdMarketsAPI.submitLimitOrder(request);
                } catch (IOException e) {
                    plugin.getLogger().warning(String.format("Failed to submit limit order for request %s: %s", request, e.getMessage()));
                    ioException = e;
                }

                if (ioException != null || !response.isSuccessful()) {
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, ioException != null ? ioException.getMessage() : response.getErrorResponse().getMessage())));
                    PlayerVirtualItem pvi = playerVirtualItem.orElseGet(() -> new PlayerVirtualItem(id));
                    refundInventory(player, optionalMaterial.get(), amountNeededInInventory.get(), pvi);
                    refundVirtualInventory(player, amountNeededInVirtualInventory, pvi);

                    return;
                }

                player.sendMessage(Message.LIMIT_ORDER_CREATED.getText(new MessageVariable(Placeholder.QUANTITY, qty.toString()),
                        new MessageVariable(Placeholder.PRICE, priceBigDecimal.toPlainString())));
            }
        });
    }

    @Subcommand("limitorder|lo view|v")
    @CommandPermission(LIMIT_ORDER_PERMISSION)
    @Description("View your limit orders")
    public void handleViewLimitOrders(Player player, @Conditions("gt0") @Default("1") final Integer page) {
        UUID playerId = player.getUniqueId();

        taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                LimitOrderPageRequest request = new LimitOrderPageRequest();
                request.setPlayerId(playerId);
                request.setPage(page - 1);//API first page is 0, command first page is 1
                request.setPageSize(GUISupport.INVENTORY_SIZE - 9);

                LimitOrderPageResponseWrapper limitOrders;

                try {
                    limitOrders = mcsdMarketsAPI.getLimitOrders(request);
                } catch (IOException e) {
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                    return;
                }

                if (!limitOrders.isSuccessful()) {
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, limitOrders.getErrorResponse().getMessage())));
                    return;
                }

                LimitOrderPageResponse response = limitOrders.getSuccessfulResponse();

                taskQueueRunner.submitSyncTask(new BSCallable<Void>() {
                    @Override
                    protected Void execute() {
                        guiSupport.openLimitOrdersMenu(player, response, page);
                        return null;
                    }
                });
            }
        });
    }

    //TODO: Cashout

    //Run sync
    private void refundInventory(Player player, Material material, int quantity, PlayerVirtualItem pvi) {
        if (!player.isOnline()) {
            taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
                @Override
                public void run() {
                    refundVirtualInventory(player, quantity, pvi);
                }
            });

            return;
        }

        Inventory inventory = player.getInventory();
        ItemStack itemStack = new ItemStack(material, quantity);
        Optional<ItemStack> notAdded = inventory.addItem(itemStack).values().stream().findFirst();

        if (notAdded.isEmpty())
            return;

        taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                refundVirtualInventory(player, notAdded.get().getAmount(), pvi);
            }
        });
    }

    //run async
    private void refundVirtualInventory(Player player, int quantity, PlayerVirtualItem pvi) {
        int retries = 0;
        while (true) {
            pvi.setQuantity(pvi.getQuantity() + quantity);

            try {
                playerVirtualItemDao.save(pvi);
                break;
            } catch (OptimisticLockException e) {
                if (++retries > 3) {
                    player.sendMessage(Message.SAVE_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                    throw e;
                }

                PlayerVirtualItemPK id = pvi.getId();
                Optional<PlayerVirtualItem> playerVirtualItem = playerVirtualItemDao.findById(id);
                pvi = playerVirtualItem.orElseGet(() -> new PlayerVirtualItem(id));
            }
        }
    }

}
