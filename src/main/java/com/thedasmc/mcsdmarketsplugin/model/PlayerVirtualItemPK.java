package com.thedasmc.mcsdmarketsplugin.model;

import com.thedasmc.mcsdmarketsplugin.validation.IsMaterial;
import com.thedasmc.mcsdmarketsplugin.validation.IsUuid;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;

import java.util.Objects;

@Embeddable
public class PlayerVirtualItemPK {

    @IsUuid
    @Column(length = 36)
    @NotEmpty(message = "uuid cannot be empty!")
    private String uuid;

    @IsMaterial
    @Column(length = 64)
    @NotEmpty(message = "material cannot be empty!")
    private String material;

    public PlayerVirtualItemPK() {
    }

    public PlayerVirtualItemPK(String uuid, String material) {
        this.uuid = uuid;
        this.material = material;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PlayerVirtualItemPK that = (PlayerVirtualItemPK) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(material, that.material);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, material);
    }
}
