package com.example.bankcards.service;

import com.example.bankcards.dto.cards.CardDto;
import com.example.bankcards.dto.mappers.CardMapper;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.repositories.CardRepository;
import com.example.bankcards.repositories.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final CardMapper cardMapper;
    @Value("${app.expiry_date}")
    private Integer expiryDate;
    private static final String CARD_NOT_FOUND = "Карта не найдена!";


    public CardDto createCard(Long ownerId, String cardType) {
        log.info("Создание карты для пользователя с ID: {}", ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с номером телефоном " + ownerId + " не найден"));

        String cardNumber = encryptionUtil.generateCardNumber();
        System.out.println(cardNumber);
        Card card = new Card();
        card.setCardNumber(encryptionUtil.encryptCardNumber(cardNumber));
        card.setOwner(owner);
        card.setType(cardType);
        card.setExpiryDate(LocalDate.now().plusYears(this.expiryDate));
        card.setStatus(CardStatus.ACTIVE.name());
        card.setBalance(BigDecimal.ZERO);

        Card savedCard = cardRepository.save(card);
        log.info("Карта создана с ID: {}", savedCard.getId());

        return cardMapper.toDto(savedCard);
    }

    @Transactional(readOnly = true)
    public CardDto getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CARD_NOT_FOUND));
        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public CardDto getCardByNumber(String cardNumber) {
        String encryptedNumber = encryptionUtil.encryptCardNumber(cardNumber);
        Card card = cardRepository.findByCardNumber(encryptedNumber)
                .orElseThrow(() -> new NoSuchElementException("Карта с указанным номером не найдена"));
        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public Page<CardDto> getUserCards(Long userId, Pageable pageable) {
        return cardRepository.findByOwnerId(userId, pageable)
                .map(cardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<CardDto> getUserActiveCards(Long userId) {
        return cardRepository.findActiveCardsByOwnerId(userId)
                .stream()
                .map(cardMapper::toDto)
                .toList();
    }



    public void blockCard(Long cardId, String reason) {
        log.info("Блокировка карты с ID: {}, причина: {}", cardId, reason);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException(CARD_NOT_FOUND));

        if (Objects.equals(card.getStatus(), CardStatus.BLOCKED.getDescription())) {
            throw new ValidationException("Карта уже заблокирована");
        }

        card.setStatus(CardStatus.BLOCKED.name());
        Card updatedCard = cardRepository.save(card);

        log.info("Карта {} заблокирована", card);
    }

    public CardDto unblockCard(Long cardId) {
        log.info("Разблокировка карты с ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException(CARD_NOT_FOUND));

        if (!Objects.equals(card.getStatus(), CardStatus.BLOCKED.name())) {
            throw new ValidationException("Карта не заблокирована");
        }

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Нельзя разблокировать истекшую карту");
        }

        card.setStatus(CardStatus.ACTIVE.name());
        Card updatedCard = cardRepository.save(card);

        log.info("Карта {} разблокирована", card);

        return cardMapper.toDto(updatedCard);
    }

    public void addBalance(Long cardId, BigDecimal amount) {
        log.info("Пополнение карты с ID: {} на сумму: {}", cardId, amount);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException(CARD_NOT_FOUND));

        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);

        log.info("Баланс карты {} пополнен на {}", card, amount);
    }

    public void deductBalance(Long cardId, BigDecimal amount) {
        log.info("Списание с карты с ID: {} суммы: {}", cardId, amount);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));

        card.setBalance(card.getBalance().subtract(amount));
        cardRepository.save(card);

        log.info("С карты {} списано {}", card, amount);
    }


    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void updateExpiredCards() {
        log.info("Обновление статуса истекших карт");

        List<Card> expiredCards = cardRepository.findExpiredCards(LocalDate.now());

        for (Card card : expiredCards) {
            if (!Objects.equals(card.getStatus(), CardStatus.EXPIRED.name())) {
                card.setStatus(CardStatus.EXPIRED.name());
                cardRepository.save(card);
                log.info("Карта {} помечена как истекшая", card);
            }
        }

        log.info("Обновлено {} истекших карт", expiredCards.size());
    }

    public void deleteCard(Long cardId) {
        log.info("Удаление карты с ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));

        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ValidationException("Нельзя удалить карту с положительным балансом");
        }

        cardRepository.delete(card);
        log.info("Карта {} удалена", card);
    }


    public Page<CardDto> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(cardMapper::toDto);
    }
}