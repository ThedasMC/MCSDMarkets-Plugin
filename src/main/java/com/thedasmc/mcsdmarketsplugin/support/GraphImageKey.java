package com.thedasmc.mcsdmarketsplugin.support;

import org.bukkit.Material;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class GraphImageKey {

    private Material material;
    private LocalDateTime localDateTime;
    private ChronoUnit timeUnit;
    private int timeAmount;

    public GraphImageKey(Material material, LocalDateTime localDateTime, ChronoUnit timeUnit, int timeAmount) {
        this.material = material;
        this.localDateTime = localDateTime.truncatedTo(timeUnit);
        this.timeUnit = timeUnit;
        this.timeAmount = timeAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphImageKey that = (GraphImageKey) o;
        return timeAmount == that.timeAmount && material == that.material && Objects.equals(localDateTime, that.localDateTime) && timeUnit == that.timeUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, localDateTime, timeUnit, timeAmount);
    }

    @Override
    public String toString() {
        return "GraphImageKey{" +
            "material=" + material +
            ", localDateTime=" + localDateTime +
            ", timeUnit=" + timeUnit +
            ", timeAmount=" + timeAmount +
            '}';
    }
}
