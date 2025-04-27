package com.thedasmc.mcsdmarketsplugin.validation.validator;

import com.thedasmc.mcsdmarketsplugin.validation.IsMaterial;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.bukkit.Material;

public class MaterialValidator implements ConstraintValidator<IsMaterial, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        Material material = Material.getMaterial(value);
        if (material == null) Material.getMaterial(value, true);

        return material != null;
    }
}
