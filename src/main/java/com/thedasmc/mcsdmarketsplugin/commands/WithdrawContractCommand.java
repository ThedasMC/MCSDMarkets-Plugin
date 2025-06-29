package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.tchristofferson.betterscheduler.BSCallable;
import com.tchristofferson.betterscheduler.TaskQueueRunner;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import com.thedasmc.mcsdmarketsplugin.support.Constants;
import com.thedasmc.mcsdmarketsplugin.support.ItemUtil;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import jakarta.persistence.OptimisticLockException;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.WITHDRAW_CONTRACT_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class WithdrawContractCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private PlayerVirtualItemDao playerVirtualItemDao;
    @Dependency private TaskQueueRunner taskQueueRunner;

    @Subcommand("contract withdraw")
    @CommandPermission(WITHDRAW_CONTRACT_PERMISSION)
    @Syntax("<material> <quantity>")
    @Description("Take the material from the contract quantity and add to your inventory")
    @CommandCompletion("@materials")
    public void handleWithdrawContractCommand(Player player, String materialName, @Conditions("gt0") final Integer quantity) {
        Optional<Material> optionalMaterial = ItemUtil.getMaterial(materialName);

        if (optionalMaterial.isEmpty()) {
            player.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Material material = optionalMaterial.get();

        taskQueueRunner.scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() throws Exception {
                PlayerVirtualItemPK id = new PlayerVirtualItemPK(player.getUniqueId().toString(), material.name());
                Optional<PlayerVirtualItem> optionalPlayerVirtualItem = playerVirtualItemDao.findById(id);

                if (optionalPlayerVirtualItem.isEmpty() || optionalPlayerVirtualItem.get().getQuantity() < quantity) {
                    player.sendMessage(Message.INSUFFICIENT_QUANTITY.getText());
                    return;
                }

                PlayerVirtualItem playerVirtualItem = optionalPlayerVirtualItem.get();
                int newQuantity = playerVirtualItem.getQuantity() - quantity;

                if (newQuantity == 0) {
                    playerVirtualItemDao.delete(playerVirtualItem);
                } else {
                    playerVirtualItem.setQuantity(playerVirtualItem.getQuantity() - quantity);
                    playerVirtualItemDao.save(playerVirtualItem);
                }

                Future<Integer> futureAmountNotAddedToInv = taskQueueRunner.submitSyncTask(new BSCallable<>() {
                    @Override
                    protected Integer execute() {
                        if (!player.isOnline())
                            return quantity;

                        ItemStack itemStack = new ItemStack(material, quantity);
                        Map<Integer, ItemStack> notAdded = player.getInventory().addItem(itemStack);

                        return notAdded.values().stream()
                            .map(ItemStack::getAmount)
                            .reduce(0, Integer::sum);
                    }
                });

                int amountNotAddedToInv = futureAmountNotAddedToInv.get(Constants.MAX_SYN_THREAD_WAIT.toMillis(), TimeUnit.MILLISECONDS);

                if (amountNotAddedToInv > 0) {
                    //While loop will allow retry if optimistic lock exception occurs
                    while (true) {
                        try {
                            optionalPlayerVirtualItem = playerVirtualItemDao.findById(id);

                            if (optionalPlayerVirtualItem.isPresent()) {
                                playerVirtualItem = optionalPlayerVirtualItem.get();
                                playerVirtualItem.setQuantity(playerVirtualItem.getQuantity() + amountNotAddedToInv);
                            } else {
                                playerVirtualItem = new PlayerVirtualItem();
                                playerVirtualItem.setId(id);
                                playerVirtualItem.setQuantity(amountNotAddedToInv);
                            }

                            playerVirtualItemDao.save(playerVirtualItem);
                            break;
                        } catch (OptimisticLockException ignored) {}
                    }

                    player.sendMessage(Message.PARTIAL_WITHDRAWAL.getText());
                } else {
                    player.sendMessage(Message.PORTFOLIO_WITHDRAWAL.getText());
                }
            }
        });
    }

}
