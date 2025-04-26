package com.thedasmc.mcsdmarketsplugin.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Entity
public class PlayerVirtualItem {

    @Valid
    @EmbeddedId
    private PlayerVirtualItemPK id;

    @Column(name = "quantity", nullable = false)
    @Positive(message = "Quantity must be > 0")
    private Long quantity;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public PlayerVirtualItemPK getId() {
        return id;
    }

    public void setId(PlayerVirtualItemPK id) {
        this.id = id;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
