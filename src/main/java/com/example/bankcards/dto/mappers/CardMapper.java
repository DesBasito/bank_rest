package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.cards.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.CardType;
import com.example.bankcards.enums.EnumInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CardMapper {
    public CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setCardNumber(card.getCardNumber());

        dto.setOwnerName(String.format("%s %s %s",
                card.getOwner().getFirstName(),
                card.getOwner().getLastName(),
                card.getOwner().getMiddleName()));
        dto.setOwnerId(card.getOwner().getId());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(EnumInterface.toDescription(CardStatus.class,card.getStatus()));
        dto.setType(EnumInterface.toDescription(CardType.class, card.getType()));
        dto.setBalance(card.getBalance());
        dto.setCreatedAt(card.getCreatedAt());
        dto.setUpdatedAt(card.getUpdatedAt());

        return dto;
    }
}