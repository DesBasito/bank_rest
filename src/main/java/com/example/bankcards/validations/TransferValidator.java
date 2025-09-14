package com.example.bankcards.validations;

import com.example.bankcards.dto.transactions.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.repositories.CardRepository;
import com.example.bankcards.util.AuthenticatedUserUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Objects;

@RequiredArgsConstructor
public class TransferValidator implements ConstraintValidator<ValidTransactionRequest, TransferRequest> {
    private final CardRepository repository;
    private final AuthenticatedUserUtil userUtil;
    private static final String FROM_CARD_ID =  "fromCardId";
    private static final String TO_CARD_ID =  "toCardId";

    @Override
    public boolean isValid(TransferRequest value, ConstraintValidatorContext context) {
        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (Objects.equals(value.getFromCardId(), value.getToCardId())) {
            context.buildConstraintViolationWithTemplate("Нельзя перевести средства на ту же карту")
                    .addPropertyNode(FROM_CARD_ID)
                    .addPropertyNode(TO_CARD_ID)
                    .addConstraintViolation();
            isValid = false;
        }

        if (value.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            context.buildConstraintViolationWithTemplate("Сумма перевода должна быть положительной")
                    .addPropertyNode("amount")
                    .addConstraintViolation();
            isValid = false;
        }

        Card fromCard = repository.findById(value.getFromCardId())
                .orElseThrow(() -> new NoSuchElementException("Карта отправителя не найдена"));

        Card toCard = repository.findById(value.getToCardId())
                .orElseThrow(() -> new NoSuchElementException("Карта получателя не найдена"));


        if (!Objects.equals(fromCard.getOwner().getId(), userUtil.getCurrentUserId())) {
            context.buildConstraintViolationWithTemplate("Карта отправителя не принадлежит пользователю")
                    .addPropertyNode(FROM_CARD_ID)
                    .addConstraintViolation();
            isValid = false;
        }

        if (!Objects.equals(toCard.getOwner().getId(), userUtil.getCurrentUserId())) {
            context.buildConstraintViolationWithTemplate("Карта получателя не принадлежит пользователю")
                    .addPropertyNode(TO_CARD_ID)
                    .addConstraintViolation();
            isValid = false;
        }


        validateCardForTransaction(fromCard, "отправителя");
        validateCardForTransaction(toCard, "получателя");

        if (fromCard.getBalance().compareTo(value.getAmount()) < 0) {
            context.buildConstraintViolationWithTemplate("Недостаточно средств на карте отправителя")
                    .addPropertyNode(FROM_CARD_ID)
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

    private void validateCardForTransaction(Card card, String cardRole) {
        if ("BLOCKED".equals(card.getStatus())) {
            throw new IllegalArgumentException("Карта " + cardRole + " заблокирована");
        }

        if ("EXPIRED".equals(card.getStatus())) {
            throw new IllegalArgumentException("Срок действия карты " + cardRole + " истек");
        }
    }
}
