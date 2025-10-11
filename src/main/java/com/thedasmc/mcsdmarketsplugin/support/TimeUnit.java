package com.thedasmc.mcsdmarketsplugin.support;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

public enum TimeUnit {

    DAYS(ChronoUnit.DAYS),
    HOURS(ChronoUnit.HOURS),;

    public final ChronoUnit chronoUnit;

    TimeUnit(ChronoUnit chronoUnit) {
        this.chronoUnit = chronoUnit;
    }

    public static Optional<TimeUnit> getTimeUnit(String unit) {
        for (TimeUnit timeUnit : values()) {
            if (timeUnit.name().equalsIgnoreCase(unit.trim())) {
                return Optional.of(timeUnit);
            }
        }

        return Optional.empty();
    }
}
