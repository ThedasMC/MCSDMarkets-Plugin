package com.thedasmc.mcsdmarketsplugin.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

@Entity
public class PlayerVirtualItem {

    @Valid
    @NotNull
    @EmbeddedId
    private PlayerVirtualItemPK id;

    @Column(nullable = false)
    @Positive(message = "Quantity must be > 0")
    private Integer quantity;

    @Version
    @Column(nullable = false)
    private Integer version;

    public PlayerVirtualItemPK getId() {
        return id;
    }

    public void setId(PlayerVirtualItemPK id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PlayerVirtualItem that = (PlayerVirtualItem) o;
        return Objects.equals(id, that.id) && Objects.equals(quantity, that.quantity) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, quantity, version);
    }

    @Override
    public String toString() {
        return "PlayerVirtualItem{" +
            "id=" + id +
            ", quantity=" + quantity +
            ", version=" + version +
            '}';
    }
}
