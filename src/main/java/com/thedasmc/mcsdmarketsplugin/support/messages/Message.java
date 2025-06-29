package com.thedasmc.mcsdmarketsplugin.support.messages;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public enum Message {

    WEB_ERROR("web-error"),
    TIMEOUT_ERROR("timeout-error"),
    INVALID_MATERIAL("invalid-material"),
    INVALID_MATERIALS("invalid-materials"),
    INVALID_QUANTITY("invalid-quantity"),
    VAULT_ERROR("vault-error"),
    INSUFFICIENT_QUANTITY("insufficient-quantity"),
    CHECK_PRICE("check-price"),
    PURCHASE("purchase-successful"),
    SALE_SUCCESSFUL("sale-successful"),
    BATCH_SALE_SUCCESSFUL("batch-sale-successful"),
    PORTFOLIO_WITHDRAWAL("portfolio-withdrawal"),
    PARTIAL_WITHDRAWAL("partial-withdrawal"),
    SAVE_ERROR("save-error");

    private static FileConfiguration messagesConfig;

    private final String path;

    Message(String path) {
        this.path = path;
    }

    public String getText() {
        return ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(messagesConfig.getString(path), "Message not found for path " + path));
    }

    public String getText(MessageVariable... variables) {
        String text = getText();

        for (MessageVariable variable : variables) {
            text = text.replace(variable.getPlaceholder().getValue(), variable.getValue());
        }

        return text;
    }

    public static void setMessagesConfig(FileConfiguration config) {
        if (messagesConfig != null)
            throw new IllegalStateException("messages config already set!");

        messagesConfig = config;
    }
}
