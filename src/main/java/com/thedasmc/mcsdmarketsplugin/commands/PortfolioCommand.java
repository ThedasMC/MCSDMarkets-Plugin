package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.tchristofferson.betterscheduler.BSAsyncTask;
import com.tchristofferson.betterscheduler.BSCallable;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.dao.PlayerVirtualItemDao;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.support.PageResult;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import org.bukkit.entity.Player;

import java.util.UUID;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.PORTFOLIO_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class PortfolioCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private PlayerVirtualItemDao playerVirtualItemDao;
    @Dependency private GUISupport guiSupport;

    @Subcommand("portfolio|pf")
    @CommandPermission(PORTFOLIO_COMMAND_PERMISSION)
    @Syntax("portfolio")
    @Description("View your item portfolio")
    public void handleViewPortfolioCommand(Player player, @co.aikar.commands.annotation.Optional @Conditions("gt0") @Default("1") final Integer page) {
        final UUID playerId = player.getUniqueId();

        plugin.getTaskQueueRunner().scheduleAsyncTask(new BSAsyncTask(plugin) {
            @Override
            public void run() {
                PageResult<PlayerVirtualItem> playerVirtualItems = playerVirtualItemDao.findAllByPlayerId(playerId.toString(), page - 1, GUISupport.INVENTORY_SIZE - 9);

                plugin.getTaskQueueRunner().submitSyncTask(new BSCallable<Void>() {
                    @Override
                    protected Void execute() {
                        guiSupport.openPortfolio(player, playerVirtualItems, page);
                        return null;
                    }
                });
            }
        });
    }

}
