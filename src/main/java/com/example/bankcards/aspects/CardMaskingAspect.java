package com.example.bankcards.aspects;

import com.example.bankcards.dto.cards.CardBlockRequestDto;
import com.example.bankcards.dto.cards.CardDto;
import com.example.bankcards.dto.transactions.TransactionDto;
import com.example.bankcards.util.AuthenticatedUserUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CardMaskingAspect {
    private final EncryptionUtil encryptionUtil;
    private final AuthenticatedUserUtil userUtil;

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.dto.mappers.CardMapper.toDto*(..)) ||"+
                       "execution(* com.example.bankcards.dto.mappers.CardBlockRequestMapper.toDto*(..)) ||"+
                       "execution(* com.example.bankcards.dto.mappers.TransactionMapper.toDto*(..))",
            returning = "result")
    public void maskSingleCardDto(Object result) {
        if (result instanceof CardDto cardDto) {
            maskCardDto(cardDto);
        }else if (result instanceof  TransactionDto transactionDto){
            maskTransactionDto(transactionDto);
        } else if (result instanceof CardBlockRequestDto blockRequestDto) {

        }
    }

    private void maskCardDto(CardDto cardDto) {
        try {
            String currentUsername = userUtil.getCurrentUsername();
            boolean isAdmin = userUtil.isCurrentUserAdmin();
            boolean isOwner = userUtil.isCardOwner(cardDto.getOwnerId(), currentUsername);

            log.debug("Маскирование карты ID: {}, пользователь: {}, админ: {}, владелец: {}",
                    cardDto.getId(), currentUsername, isAdmin, isOwner);
            String maskedNumber;
            if (!isAlreadyDecrypted(cardDto.getCardNumber())) {
                cardDto.setCardNumber(encryptionUtil.decryptCardNumber(cardDto.getCardNumber()));
            }
            if (isAdmin) {
                maskedNumber = encryptionUtil.maskCardNumber(cardDto.getCardNumber());
            } else if (isOwner) {
                maskedNumber = cardDto.getCardNumber();
            } else {
                maskedNumber = encryptionUtil.maskCardNumber(cardDto.getCardNumber());
            }

            cardDto.setCardNumber(maskedNumber);

        } catch (Exception e) {
            log.warn("Ошибка при маскировании номера карты: {}", e.getMessage());
            cardDto.setCardNumber("****");
        }
    }

    private void maskTransactionDto(TransactionDto transactionDto) {
        try {
            String currentUsername = userUtil.getCurrentUsername();
            boolean isAdmin = userUtil.isCurrentUserAdmin();
            boolean isOwner = userUtil.isCardOwner(transactionDto.getToCardId(), currentUsername);

            log.debug("Маскирование номера карты в транзакции ID: {} , пользователь: {}, админ: {}, владелец: {}",
                    transactionDto.getId(), currentUsername, isAdmin, isOwner);

            if (isAdmin) {
                transactionDto.setFromCardCardNumber(encryptionUtil.maskCardNumber(transactionDto.getFromCardCardNumber()));
                transactionDto.setToCardCardNumber(encryptionUtil.maskCardNumber(transactionDto.getToCardCardNumber()));
            }

        } catch (Exception e) {
            log.warn("Ошибка при маскировании номера карты: {}", e.getMessage());
            transactionDto.setFromCardCardNumber("****");
            transactionDto.setToCardCardNumber("****");
        }
    }

    private boolean isAlreadyDecrypted(String cardNumber) {
        return cardNumber.matches("\\d{13,19}");
    }
}