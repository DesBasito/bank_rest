package com.example.bankcards.aspects;

import com.example.bankcards.dto.cards.CardDto;
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
            pointcut = "execution(* com.example.bankcards.service.CardService.*(..)) || " +
                       "execution(* com.example.bankcards.dto.mappers.CardMapper.*(..))",
            returning = "result")
    public void maskSingleCardDto(Object result) {
        if (result instanceof CardDto cardDto) {
            maskCardDto(cardDto);
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.service.CardService.getUserActiveCards(..))",
            returning = "result")
    public void maskCardDtoList(Object result) {
        if (result instanceof List<?> list) {
            list.forEach(item -> {
                if (item instanceof CardDto cardDto) {
                    maskCardDto(cardDto);
                }
            });
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.service.CardService.getUserCards(..))",
            returning = "result")
    public void maskCardDtoPage(Object result) {
        if (result instanceof Page<?> page) {
            page.getContent().forEach(item -> {
                if (item instanceof CardDto cardDto) {
                    maskCardDto(cardDto);
                }
            });
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
}