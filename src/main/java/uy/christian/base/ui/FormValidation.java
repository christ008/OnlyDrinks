package uy.christian.base.ui;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Optional;

public final class FormValidation {

    private FormValidation() {
    }

    public static <T> Optional<String> firstViolationMessage(Validator validator, T formData) {
        return validator.validate(formData).stream()
                .map(ConstraintViolation::getMessage)
                .findFirst();
    }
}
