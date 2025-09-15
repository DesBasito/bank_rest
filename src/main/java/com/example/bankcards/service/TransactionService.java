package com.example.bankcards.service;

import com.example.bankcards.dto.mappers.TransactionMapper;
import com.example.bankcards.dto.transactions.TransactionDto;
import com.example.bankcards.dto.transactions.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.enums.TransactionStatus;
import com.example.bankcards.repositories.CardRepository;
import com.example.bankcards.repositories.TransactionRepo;
import com.example.bankcards.util.AuthenticatedUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepo transactionRepository;
    private final AuthenticatedUserUtil userUtil;
    private final CardRepository cardRepository;
    private final CardService cardService;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionDto transferBetweenUserCards(TransferRequest request) {
        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new NoSuchElementException("Карта отправителя не найдена"));

        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new NoSuchElementException("Карта получателя не найдена"));
        String description = request.getToCardId().equals(request.getFromCardId())
                ? "Пополнение счета через терминал"
                : request.getDescription();
        try {
            Transaction transaction = Transaction.builder()
                    .fromCard(fromCard)
                    .toCard(toCard)
                    .amount(request.getAmount())
                    .description(description)
                    .status(TransactionStatus.SUCCESS.name())
                    .build();
            if (!request.getFromCardId().equals(request.getToCardId())) {
                cardService.deductBalance(request.getFromCardId(), request.getAmount());
            }
            cardService.addBalance(request.getToCardId(), request.getAmount());

            log.info("Перевод пользователя {} с карты {} на карту {} на сумму {}",
                    fromCard.getOwner().getFullName(), request.getFromCardId(), request.getToCardId(), request.getAmount());

            Transaction savedTransaction = transactionRepository.save(transaction);

            log.info("Перевод выполнен успешно. ID транзакции: {}", savedTransaction.getId());

            return transactionMapper.toDto(savedTransaction);

        } catch (Exception e) {
            log.error("Ошибка при выполнении перевода: {}", e.getMessage(), e);
            Transaction failedTransaction = Transaction.builder()
                    .fromCard(fromCard)
                    .toCard(toCard)
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .status(TransactionStatus.CANCELLED.name())
                    .createdAt(Instant.now())
                    .processedAt(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();

            transactionRepository.save(failedTransaction);
            throw new RuntimeException("Ошибка при выполнении перевода: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getUserTransactions(Long userId, Long cardId, Pageable pageable) {
        log.info("Получение транзакций для пользователя {}, карта: {}", userId, cardId);

        Page<Transaction> transactions;

        if (cardId != null) {
            transactions = transactionRepository.findByCardId(cardId, pageable);
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));

            if (!Objects.equals(card.getOwner().getId(), userId)) {
                throw new IllegalArgumentException("Карта не принадлежит пользователю");
            }
        } else {
            transactions = transactionRepository.findByUserId(userId, pageable);
        }

        return transactions.map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long transactionId) {
        Long userId = userUtil.getCurrentUserId();

        log.info("Получение транзакции {} для пользователя {}", transactionId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Транзакция не найдена"));

        boolean hasAccess = Objects.equals(transaction.getFromCard().getOwner().getId(), userId) &&
                            Objects.equals(transaction.getToCard().getOwner().getId(), userId);

        boolean admin = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            admin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
        }


        if (!hasAccess || admin) {
            throw new IllegalArgumentException("Нет доступа к данной транзакции");
        }

        return transactionMapper.toDto(transaction);
    }


}