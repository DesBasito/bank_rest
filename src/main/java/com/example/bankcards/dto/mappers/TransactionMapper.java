package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.transactions.TransactionDto;
import com.example.bankcards.dto.transactions.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.enums.EnumInterface;
import com.example.bankcards.enums.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransactionMapper {
    public TransactionDto toDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setFromCardId(transaction.getFromCard().getId());
        dto.setToCardId(transaction.getToCard().getId());

        dto.setFromCardCardNumber(transaction.getFromCard().getCardNumber());
        dto.setToCardCardNumber(transaction.getToCard().getCardNumber());

        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setStatus(EnumInterface.toDescription(TransactionStatus.class,transaction.getStatus()));
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setProcessedAt(transaction.getProcessedAt());
        dto.setErrorMessage(transaction.getErrorMessage());

        return dto;
    }

    public Transaction toEntity(Card toCard, Card fromCard, TransferRequest request) {
        String description = request.getToCardId().equals(request.getFromCardId())
                ? "Пополнение счета через терминал"
                : request.getDescription();
        return Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.getAmount())
                .description(description)
                .status(TransactionStatus.SUCCESS.name())
                .build();
    }

    public Transaction toEntityWithError(Card toCard, Card fromCard, TransferRequest request, String err) {
        String description = request.getToCardId().equals(request.getFromCardId())
                ? "Пополнение счета через терминал"
                : request.getDescription();
        return Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.getAmount())
                .description(description)
                .status(TransactionStatus.FAILED.name())
                .errorMessage(err)
                .build();
    }
}