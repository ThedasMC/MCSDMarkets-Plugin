package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.tchristofferson.betterscheduler.BSCallable;
import com.tchristofferson.betterscheduler.TaskQueueRunner;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.enums.TransactionType;
import com.thedasmc.mcsdmarketsapi.request.CreateTransactionRequest;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.BUY_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class SellCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private MCSDMarketsAPI mcsdMarketsAPI;
    @Dependency private Economy economy;
    @Dependency private PlayerVirtualItemDao playerVirtualItemDao;
    @Dependency private TaskQueueRunner taskQueueRunner;

    @Subcommand("sell")
    @CommandPermission(BUY_COMMAND_PERMISSION)
    @Syntax("<material> <quantity>")
    @Description("Sell items from item portfolio. Will not sell items from your inventory, to do that, don't pass any args to open sell inventory.")
    @CommandCompletion("@materials")
    public void handleSellCommand(Player player, String materialName, @co.aikar.commands.annotation.Optional @Conditions("gt0") final Integer quantity) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Material material = optionalMaterial.get();

        taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                PlayerVirtualItemPK id = new PlayerVirtualItemPK(player.getUniqueId().toString(), material.name());
                Optional<PlayerVirtualItem> optionalPlayerVirtualItem = playerVirtualItemDao.findById(id);

                if (optionalPlayerVirtualItem.isEmpty()) {
                    player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
                    return;
                }

                ItemResponseWrapper itemResponseWrapper;

                try {
                    itemResponseWrapper = mcsdMarketsAPI.getItem(material.name());
                } catch (IOException e) {
                    player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                    return;
                }

                if (!itemResponseWrapper.isSuccessful()) {
                    taskQueueRunner.submitSyncTask(new BSCallable<Void>() {
                        @Override
                        protected Void execute() {
                            player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, itemResponseWrapper.getErrorResponse().getMessage())));
                            return null;
                        }
                    });

                    return;
                }

                PlayerVirtualItem playerVirtualItem = optionalPlayerVirtualItem.get();
                final boolean sellAll = quantity == null;
                final int toSell = sellAll ? playerVirtualItem.getQuantity() : quantity;

                if (playerVirtualItem.getQuantity() < toSell) {
                    player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
                    return;
                }

                if (sellAll || playerVirtualItem.getQuantity() == toSell) {
                    playerVirtualItemDao.delete(playerVirtualItem);
                } else {
                    playerVirtualItem.setQuantity(playerVirtualItem.getQuantity() - toSell);
                }

                BigDecimal price = itemResponseWrapper.getSuccessfulResponse().getCurrentPrice()
                    .multiply(BigDecimal.valueOf(toSell));

                CreateTransactionRequest createTransactionRequest = new CreateTransactionRequest();
                createTransactionRequest.setPlayerId(player.getUniqueId());
                createTransactionRequest.setTransactionType(TransactionType.SALE);
                createTransactionRequest.setMaterial(material.name());
                createTransactionRequest.setQuantity(toSell);

                CreateTransactionResponseWrapper createTransactionResponseWrapper;

                try {
                    createTransactionResponseWrapper = mcsdMarketsAPI.createTransaction(createTransactionRequest);
                } catch (IOException e) {
                    taskQueueRunner.submitSyncTask(new BSCallable<Void>() {
                        @Override
                        protected Void execute() {
                            player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                            refund(player.getUniqueId(), material, toSell);
                            return null;
                        }
                    });

                    return;
                }

                if (!createTransactionResponseWrapper.isSuccessful()) {
                    taskQueueRunner.submitSyncTask(new BSCallable<Void>() {
                        @Override
                        protected Void execute() {
                            player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, createTransactionResponseWrapper.getErrorResponse().getMessage())));
                            refund(player.getUniqueId(), material, toSell);
                            return null;
                        }
                    });

                    return;
                }

                taskQueueRunner.submitSyncTask(new BSCallable<Void>() {
                    @Override
                    protected Void execute() {
                        economy.depositPlayer(player, price.doubleValue());
                        player.sendMessage(Message.SALE_SUCCESSFUL.getText(new MessageVariable(Placeholder.ITEM, material.name()), new MessageVariable(Placeholder.PRICE, price.toPlainString())));
                        return null;
                    }
                });
            }
        });
    }

    private void refund(UUID playerId, Material material, Integer quantity) {
        PlayerVirtualItemPK id = new PlayerVirtualItemPK(playerId.toString(), material.name());
        Optional<PlayerVirtualItem> optionalPlayerVirtualItem = playerVirtualItemDao.findById(id);

        if (optionalPlayerVirtualItem.isEmpty()) {
            PlayerVirtualItem playerVirtualItem = new PlayerVirtualItem();
            playerVirtualItem.setId(id);
            playerVirtualItem.setQuantity(quantity);
            playerVirtualItemDao.save(playerVirtualItem);
        } else {
            PlayerVirtualItem playerVirtualItem = optionalPlayerVirtualItem.get();
            playerVirtualItem.setQuantity(playerVirtualItem.getQuantity() + quantity);
        }
    }

}
