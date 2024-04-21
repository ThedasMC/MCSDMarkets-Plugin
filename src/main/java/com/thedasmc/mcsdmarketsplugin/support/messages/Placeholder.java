package com.thedasmc.mcsdmarketsplugin.support.messages;

public enum Placeholder {

    ERROR("{error}"),
    PRICE("{price}"),
    ITEM("{item}");

    private final String value;

    Placeholder(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
