package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.transactions.TransactionDto;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final EncryptionUtil encryptionUtil;

    public TransactionDto toDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setFromCardId(transaction.getFromCard().getId());
        dto.setToCardId(transaction.getToCard().getId());

        // Маскируем номера карт для отображения
        dto.setFromCardCardNumber(transaction.getFromCard().getCardNumber());
        dto.setToCardCardNumber(transaction.getToCard().getCardNumber());

        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setStatus(transaction.getStatus());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setProcessedAt(transaction.getProcessedAt());
        dto.setErrorMessage(transaction.getErrorMessage());

        return dto;
    }
}