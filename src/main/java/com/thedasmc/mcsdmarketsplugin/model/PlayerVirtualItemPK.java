package com.thedasmc.mcsdmarketsplugin.model;

import com.thedasmc.mcsdmarketsplugin.validation.IsMaterial;
import com.thedasmc.mcsdmarketsplugin.validation.IsUuid;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class PlayerVirtualItemPK {

    @IsUuid
    private String uuid;

    @IsMaterial
    private String material;

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
