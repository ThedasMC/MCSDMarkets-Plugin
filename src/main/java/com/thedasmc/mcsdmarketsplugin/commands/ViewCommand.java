package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.request.ItemPageRequest;
import com.thedasmc.mcsdmarketsapi.response.impl.ItemPageResponse;
import com.thedasmc.mcsdmarketsapi.response.wrapper.ItemPageResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.gui.GUISupport;
import com.thedasmc.mcsdmarketsplugin.support.messages.Message;
import com.thedasmc.mcsdmarketsplugin.support.messages.MessageVariable;
import com.thedasmc.mcsdmarketsplugin.support.messages.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.VIEW_COMMAND_PERMISSION;

@CommandAlias(BASE_COMMAND)
public class ViewCommand extends BaseCommand {

    private final String versionString = Bukkit.getServer().getBukkitVersion().split("-")[0];

    @Dependency private MCSDMarkets plugin;
    @Dependency private MCSDMarketsAPI mcsdMarketsAPI;
    @Dependency private GUISupport guiSupport;

    @Subcommand("view")
    @CommandPermission(VIEW_COMMAND_PERMISSION)
    @Syntax("[page]")
    @Description("Open inventory gui displaying the items available to trade")
    public void handleViewCommand(Player player, @co.aikar.commands.annotation.Optional @Conditions("gt0") final Integer page) {
        final int pageIndex = page == null ? 0 : page - 1;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemPageRequest request = new ItemPageRequest();
            request.setMcVersion(versionString);
            request.setPage(pageIndex);
            request.setPageSize(GUISupport.INVENTORY_SIZE - 9);

            ItemPageResponseWrapper itemPageResponseWrapper;

            try {
                itemPageResponseWrapper = mcsdMarketsAPI.getItems(request);
            } catch (IOException e) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                return;
            }

            if (!itemPageResponseWrapper.isSuccessful()) {
                player.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, itemPageResponseWrapper.getErrorResponse().getMessage())));
                return;
            }

            ItemPageResponse itemPageResponse = itemPageResponseWrapper.getSuccessfulResponse();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline())
                    guiSupport.openMenu(player, itemPageResponse);
            });
        });
    }

}
