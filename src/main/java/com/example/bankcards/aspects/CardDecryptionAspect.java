package com.example.bankcards.aspects;

import com.example.bankcards.entity.Card;
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
            pointcut = "execution(* com.example.bankcards.repositories.CardRepository.findById(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardRepository.findByCardNumber(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardRepository.save(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardRepository.getReferenceById(..))",
            returning = "result")
    public void decryptSingleCard(Object result) {
        if (result instanceof Optional<?> optional) {
            optional.ifPresent(item -> {
                if (item instanceof Card card) {
                    decryptCardNumber(card);
                }
            });
        } else if (result instanceof Card card) {
            decryptCardNumber(card);
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.repositories.CardRepository.findActiveCardsByOwnerId(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardRepository.findExpiredCards(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardRepository.findAll())",
            returning = "result")
    public void decryptCardList(Object result) {
        if (result instanceof List<?> list) {
            list.forEach(item -> {
                if (item instanceof Card card) {
                    decryptCardNumber(card);
                }
            });
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.repositories.CardRepository.findByOwnerId(..)) || " +
                       "execution(* com.example.bankcards.repositories.CardRepository.findByOwnerNameContaining(..))",
            returning = "result")
    public void decryptCardPage(Object result) {
        if (result instanceof Page<?> page) {
            page.getContent().forEach(item -> {
                if (item instanceof Card card) {
                    decryptCardNumber(card);
                }
            });
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