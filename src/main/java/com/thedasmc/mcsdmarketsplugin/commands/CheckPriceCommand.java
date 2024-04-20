package com.thedasmc.mcsdmarketsplugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.thedasmc.mcsdmarketsapi.MCSDMarketsAPI;
import com.thedasmc.mcsdmarketsapi.response.impl.ItemResponse;
import com.thedasmc.mcsdmarketsapi.response.wrapper.ItemResponseWrapper;
import com.thedasmc.mcsdmarketsplugin.MCSDMarkets;
import com.thedasmc.mcsdmarketsplugin.support.Message;
import com.thedasmc.mcsdmarketsplugin.support.MessageVariable;
import com.thedasmc.mcsdmarketsplugin.support.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.io.IOException;

import static com.thedasmc.mcsdmarketsplugin.support.Constants.BASE_COMMAND;
import static com.thedasmc.mcsdmarketsplugin.support.Constants.CHECK_COMMAND_PERMISSION;

@CommandAlias(value = BASE_COMMAND)
public class CheckPriceCommand extends BaseCommand {

    @Dependency private MCSDMarkets plugin;
    @Dependency private MCSDMarketsAPI mcsdMarketsAPI;

    @Subcommand("check")
    @CommandPermission(CHECK_COMMAND_PERMISSION)
    @Syntax("<material>")
    @Description("Check the current price of an item.")
    @CommandCompletion("@materials")
    public void handleCheckCommand(CommandSender sender, String materialName) {
        Material material = Material.getMaterial(materialName);

        if (material == null) {
            sender.sendMessage(Message.INVALID_MATERIAL.getText());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemResponseWrapper responseWrapper;

            try {
                responseWrapper = mcsdMarketsAPI.getItem(material.name());
            } catch (IOException e) {
                sender.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, e.getMessage())));
                return;
            }

            if (responseWrapper.isSuccessful()) {
                ItemResponse response = responseWrapper.getSuccessfulResponse();
                sender.sendMessage(Message.CHECK_PRICE.getText(new MessageVariable(Placeholder.PRICE, response.getCurrentPrice().toPlainString())));
            } else {
                sender.sendMessage(Message.WEB_ERROR.getText(new MessageVariable(Placeholder.ERROR, responseWrapper.getErrorResponse().getMessage())));
            }
        });
    }

}
