package com.example.bankcards.validations;

import com.example.bankcards.enums.CardType;
import com.example.bankcards.enums.EnumInterface;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.example.bankcards.enums.EnumInterface.getEnumDescription;

public class CardTypeValidator implements ConstraintValidator<ValidTransactionRequest, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean isValid = true;

        context.disableDefaultConstraintViolation();

        if (!EnumInterface.isExists(CardType.class, value)) {
            context.buildConstraintViolationWithTemplate(getEnumDescription(CardType.class))
                    .addPropertyNode("cardType")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
