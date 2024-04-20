package com.thedasmc.mcsdmarketsplugin.support;

public enum Placeholder {

    ERROR("{error}"),
    PRICE("{price}");

    private final String value;

    Placeholder(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
