package com.example.bankcards.aspects;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.util.AuthenticatedUserUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CardMaskingAspect {
    private final EncryptionUtil encryptionUtil;

    @AfterReturning(
            pointcut = "execution(* com.example.bankcards.service.CardService.*(..)) || " +
                       "execution(* com.example.bankcards.service.CardApplicationService.approveCardApplication(..))",
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
            String currentUsername = getCurrentUsername();
            boolean isAdmin = isCurrentUserAdmin();
            boolean isOwner = isCardOwner(cardDto.getOwnerId(), currentUsername);

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


    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
        }
        return false;
    }

    private boolean isCardOwner(Long cardOwnerId, String username) {
        if (username == null || cardOwnerId == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return cardOwnerId.equals(user.getId());
        }
        return false;
    }
}