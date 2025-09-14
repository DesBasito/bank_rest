package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardMapper {

    /**
     * Преобразование Entity в DTO без маскирования
     * (маскирование будет выполнено аспектом)
     */
    public CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());

        // Оставляем номер карты как есть - аспект его обработает
        dto.setCardNumber(card.getCardNumber());

        dto.setOwnerName(String.format("%s %s %s",
                card.getOwner().getFirstName(),
                card.getOwner().getLastName(),
                card.getOwner().getMiddleName()));
        dto.setOwnerId(card.getOwner().getId());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(CardStatus.valueOf(card.getStatus()).getDescription());
        dto.setBalance(card.getBalance());
        dto.setCreatedAt(card.getCreatedAt());
        dto.setUpdatedAt(card.getUpdatedAt());

        return dto;
    }
}