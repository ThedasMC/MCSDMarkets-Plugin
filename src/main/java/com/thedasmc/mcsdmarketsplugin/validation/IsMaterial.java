package com.thedasmc.mcsdmarketsplugin.validation;

import com.thedasmc.mcsdmarketsplugin.validation.validator.MaterialValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MaterialValidator.class)
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsMaterial {

    String message() default "Invalid material";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
