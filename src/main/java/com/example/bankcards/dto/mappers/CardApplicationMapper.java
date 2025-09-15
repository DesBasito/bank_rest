package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.cardApplication.CardApplicationDto;
import com.example.bankcards.entity.CardApplication;
import com.example.bankcards.enums.CardRequestStatus;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.CardType;
import com.example.bankcards.enums.EnumInterface;
import org.springframework.stereotype.Component;

@Component
public class CardApplicationMapper {

    public CardApplicationDto mapToDto(CardApplication application) {
        return CardApplicationDto.builder()
                .id(application.getId())
                .userId(application.getUser().getId())
                .userName(String.format("%s %s %s",
                        application.getUser().getFirstName(),
                        application.getUser().getLastName(),
                        application.getUser().getMiddleName()))
                .cardType(EnumInterface.toDescription(CardType.class,application.getCardType()))
                .comment(application.getComment())
                .status(EnumInterface.toDescription(CardRequestStatus.class,application.getStatus()))
                .createdAt(application.getCreatedAt())
                .processedAt(application.getProcessedAt())
                .build();
    }
}
