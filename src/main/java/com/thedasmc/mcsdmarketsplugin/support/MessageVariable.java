package com.thedasmc.mcsdmarketsplugin.support;

public class MessageVariable {

    private final Placeholder placeholder;
    private final String value;

    public MessageVariable(Placeholder placeholder, String value) {
        this.placeholder = placeholder;
        this.value = value;
    }

    public Placeholder getPlaceholder() {
        return placeholder;
    }

    public String getValue() {
        return value;
    }
}
