package com.thedasmc.mcsdmarketsplugin.validation;

import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItem;
import com.thedasmc.mcsdmarketsplugin.model.PlayerVirtualItemPK;
import jakarta.validation.*;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.thedasmc.mcsdmarketsplugin.util.PlayerVirtualItemUtil.createSamplePlayerVirtualItem;
import static org.junit.jupiter.api.Assertions.*;

public class HibernateValidationTest {

    private static final ValidatorFactory validatorFactory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();

    @Test
    public void ensureValidationOnEntityIsWorkingCorrectly() {
        Validator validator = validatorFactory.getValidator();
        PlayerVirtualItem playerVirtualItem = createSamplePlayerVirtualItem();
        playerVirtualItem.setQuantity(0L);

        Set<ConstraintViolation<PlayerVirtualItem>> violations = validator.validate(playerVirtualItem);
        assertEquals(1, violations.size(), "Issue with validation on quantity field!");
        playerVirtualItem.setQuantity(1L);

        PlayerVirtualItemPK id = playerVirtualItem.getId();
        playerVirtualItem.setId(null);
        violations = validator.validate(playerVirtualItem);
        assertEquals(1, violations.size(), "Issue with validation on id field!");
        playerVirtualItem.setId(id);

        String uuid = id.getUuid();
        id.setUuid(null);
        violations = validator.validate(playerVirtualItem);
        assertEquals(1, violations.size(), "Issue with validation on uuid field!");
        id.setUuid(uuid);

        id.setMaterial(null);
        violations = validator.validate(playerVirtualItem);
        assertEquals(1, violations.size(), "Issue with validation on material field!");
    }
}
