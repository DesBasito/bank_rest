package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.cards.CardBlockRequestCreateDto;
import com.example.bankcards.dto.cards.CardBlockRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.enums.CardRequestStatus;
import com.example.bankcards.enums.EnumInterface;
import org.springframework.stereotype.Component;

@Component
public class CardBlockRequestMapper {
    public CardBlockRequestDto mapToDto(CardBlockRequest request) {
        return CardBlockRequestDto.builder()
                .id(request.getId())
                .cardId(request.getCard().getId())
                .cardNumber(request.getCard().getCardNumber())
                .userId(request.getUser().getId())
                .userName(String.format("%s %s %s",
                        request.getUser().getFirstName(),
                        request.getUser().getLastName(),
                        request.getUser().getMiddleName()))
                .reason(request.getReason())
                .status(EnumInterface.toDescription(CardRequestStatus.class,request.getStatus()))
                .adminComment(request.getAdminComment())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    public CardBlockRequest toEntity(Card card, CardBlockRequestCreateDto request) {
        return CardBlockRequest.builder()
                .card(card)
                .reason(request.getReason())
                .status(CardRequestStatus.PENDING.name())
                .build();
    }
}