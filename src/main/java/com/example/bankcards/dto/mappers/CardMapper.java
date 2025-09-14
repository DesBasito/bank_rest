package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardMapper {
    private final EncryptionUtil encryptionUtil;
    /**
     * Преобразование Entity в DTO
     */
    public CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());

        try {
            String decryptedNumber = encryptionUtil.decryptCardNumber(card.getCardNumber());
            dto.setCardNumber(encryptionUtil.maskCardNumber(decryptedNumber));
        } catch (Exception e) {
            log.error("Ошибка при расшифровке номера карты", e);
            dto.setCardNumber("****");
        }

        dto.setOwnerName(String.format("%s %s %s%n",card.getOwner().getFirstName(), card.getOwner().getLastName(), card.getOwner().getMiddleName()));
        dto.setOwnerId(card.getOwner().getId());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(CardStatus.valueOf(card.getStatus()).getDescription());
        dto.setBalance(card.getBalance());
        dto.setCreatedAt(card.getCreatedAt());
        dto.setUpdatedAt(card.getUpdatedAt());

        return dto;
    }
}
