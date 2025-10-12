package com.thedasmc.mcsdmarketsplugin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;

import java.util.Objects;

@Entity
public class PriceHistoryMap {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String materialName;

    @Column(nullable = false)
    private String timeUnit;

    @Column(nullable = false)
    @Min(value = 1, message = "timeAmount must be > 0")
    private Integer timeAmount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getTimeAmount() {
        return timeAmount;
    }

    public void setTimeAmount(Integer timeAmount) {
        this.timeAmount = timeAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PriceHistoryMap that = (PriceHistoryMap) o;
        return Objects.equals(id, that.id) && Objects.equals(materialName, that.materialName) && Objects.equals(timeUnit, that.timeUnit) && Objects.equals(timeAmount, that.timeAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, materialName, timeUnit, timeAmount);
    }

    @Override
    public String toString() {
        return "PriceHistoryMap{" +
            "id=" + id +
            ", materialName='" + materialName + '\'' +
            ", timeUnit='" + timeUnit + '\'' +
            ", timeAmount=" + timeAmount +
            '}';
    }
}
