package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link com.example.bankcards.entity.Transaction}
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    Long id;
    Long fromCardId;
    String fromCardCardNumber;
    Long toCardId;
    String toCardCardNumber;
    @NotNull
    BigDecimal amount;
    String description;
    @NotNull
    String status;
    @NotNull
    Instant createdAt;
    Instant processedAt;
    String errorMessage;
}