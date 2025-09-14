package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.cardApplication.CardApplicationDto;
import com.example.bankcards.entity.CardApplication;
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
                .cardType(application.getCardType())
                .comment(application.getComment())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .processedAt(application.getProcessedAt())
                .build();
    }
}
