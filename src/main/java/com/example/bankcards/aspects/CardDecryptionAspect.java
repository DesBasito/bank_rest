package com.example.bankcards.aspects;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CardDecryptionAspect {
    private final EncryptionUtil encryptionUtil;

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.repositories.CardRepository.*(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardBlockRequestRepository.*(..))",
            returning = "result")
    public void decryptCardData(Object result) {
        if (result instanceof List<?> list) {
            decryptCardsInList(list);
        } else if (result instanceof Page<?> page) {
            decryptCardsInList(page.getContent());
        } else if (result instanceof Optional<?> optional && optional.isPresent()) {
            decryptCardsInObject(optional.get());
        } else if (result != null) {
            decryptCardsInObject(result);
        }
    }

    private void decryptCardsInList(List<?> list) {
        list.forEach(this::decryptCardsInObject);
    }

    private void decryptCardsInObject(Object item) {
        if (item instanceof Card card) {
            decryptCardNumber(card);
        } else if (item instanceof CardBlockRequest request) {
            decryptCardNumber(request.getCard());
        }
    }

    private void decryptCardNumber(Card card) {
        try {
            if (card != null && card.getCardNumber() != null) {
                if (isAlreadyDecrypted(card.getCardNumber())) {
                    log.debug("Номер карты ID: {} уже расшифрован", card.getId());
                    return;
                }

                String decryptedNumber = encryptionUtil.decryptCardNumber(card.getCardNumber());
                card.setCardNumber(decryptedNumber);

                log.debug("Номер карты ID: {} успешно расшифрован", card.getId());
            }
        } catch (Exception e) {
            log.warn("Ошибка при расшифровке номера карты ID: {}, причина: {}",
                    card != null ? card.getId() : "unknown", e.getMessage());
        }
    }

    private boolean isAlreadyDecrypted(String cardNumber) {
        return cardNumber.matches("\\d{13,19}");
    }
}