package com.thedasmc.mcsdmarketsplugin.validation;

import com.thedasmc.mcsdmarketsplugin.validation.validator.UuidValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UuidValidator.class)
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsUuid {

    String message() default "Invalid uuid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
