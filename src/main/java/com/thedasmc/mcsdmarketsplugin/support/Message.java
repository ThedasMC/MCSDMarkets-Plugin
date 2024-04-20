package com.thedasmc.mcsdmarketsplugin.support;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public enum Message {

    WEB_ERROR("web-error"),
    INVALID_MATERIAL("invalid-material"),
    CHECK_PRICE("check-price");

    private static FileConfiguration messagesConfig;

    private final String path;

    Message(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
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
