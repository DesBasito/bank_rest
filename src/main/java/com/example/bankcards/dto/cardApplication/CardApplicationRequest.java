package com.example.bankcards.dto.cardApplication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание карты")
public class CardApplicationRequest {

    @Schema(description = "Тип карты", example = "DEBIT", allowableValues = {"DEBIT", "CREDIT", "VIRTUAL", "PREPAID"})
    @NotBlank(message = "Тип карты обязателен")
    String cardType;

    @Schema(description = "Комментарий к заявке", example = "Основная карта для зарплаты")
    String comment;
}