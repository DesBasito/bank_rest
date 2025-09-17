package com.example.bankcards.service;

import com.example.bankcards.dto.cardApplication.CardApplicationDto;
import com.example.bankcards.dto.cardApplication.CardApplicationRequest;
import com.example.bankcards.dto.cards.CardDto;
import com.example.bankcards.dto.mappers.CardApplicationMapper;
import com.example.bankcards.entity.CardApplication;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardRequestStatus;
import com.example.bankcards.repositories.CardApplicationRepository;
import com.example.bankcards.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardApplicationService {
    private final CardApplicationRepository cardApplicationRepository;
    private final CardApplicationMapper mapper;
    private final UserRepository userRepository;
    private final CardService cardService;


    public CardApplicationDto createCardApplication(Long userId, CardApplicationRequest request) {
        log.info("Создание заявки на карту для пользователя с ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));
        CardApplication application = mapper.toEntity(user, request);
        CardApplication savedApplication = cardApplicationRepository.save(application);

        log.info("Заявка на карту создана с ID: {}", savedApplication.getId());

        return mapper.mapToDto(savedApplication);
    }

    public CardDto approveCardApplication(Long applicationId) {
        log.info("Одобрение заявки на карту с ID: {}", applicationId);

        CardApplication application = cardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Заявка не найдена"));

        if (!CardRequestStatus.PENDING.name().equals(application.getStatus())) {
            throw new IllegalArgumentException("Заявка уже обработана");
        }

        CardDto savedCard = cardService.createCard(application.getUser().getId(), application.getCardType());

       
        application.setStatus(CardRequestStatus.APPROVED.name());
        application.setProcessedAt(Instant.now());
        cardApplicationRepository.save(application);

        log.info("Заявка одобрена, карта создана с ID: {}", savedCard.getId());

        return savedCard;
    }

    public CardApplicationDto rejectCardApplication(Long applicationId, String reason) {
        log.info("Отклонение заявки на карту с ID: {}, причина: {}", applicationId, reason);

        CardApplication application = cardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Заявка не найдена"));

        if (!CardRequestStatus.PENDING.name().equals(application.getStatus())) {
            throw new IllegalArgumentException("Заявка уже обработана");
        }

        application.setStatus(CardRequestStatus.REJECTED.name());
        application.setProcessedAt(Instant.now());
        if (reason != null) {
            application.setComment(application.getComment() + " | Причина отклонения: " + reason);
        }

        CardApplication savedApplication = cardApplicationRepository.save(application);

        log.info("Заявка отклонена");

        return mapper.mapToDto(savedApplication);
    }


    public CardApplicationDto cancelCardApplication(Long applicationId, Long userId) {
        log.info("Отмена заявки на карту с ID: {} пользователем с ID: {}", applicationId, userId);

        CardApplication application = cardApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Заявка не найдена"));

        if (!application.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет доступа к этой заявке");
        }
        if (!CardRequestStatus.PENDING.name().equals(application.getStatus())) {
            throw new IllegalArgumentException("Можно отменить только заявки в статусе 'Ожидание'");
        }
        application.setStatus(CardRequestStatus.CANCELLED.name());
        application.setProcessedAt(Instant.now());

        CardApplication savedApplication = cardApplicationRepository.save(application);

        log.info("Заявка отменена пользователем");
        return mapper.mapToDto(savedApplication);
    }


    @Transactional(readOnly = true)
    public Page<CardApplicationDto> getAllApplications(Pageable pageable) {
        return cardApplicationRepository.findAll(pageable)
                .map(mapper::mapToDto);
    }


    @Transactional(readOnly = true)
    public Page<CardApplicationDto> getUserApplications(Long userId, Pageable pageable) {
        return cardApplicationRepository.findByUserId(userId, pageable)
                .map(mapper::mapToDto);
    }


    @Transactional(readOnly = true)
    public Page<CardApplicationDto> getApplicationsByStatus(String status, Pageable pageable) {
        return cardApplicationRepository.findByStatus(status, pageable)
                .map(mapper::mapToDto);
    }
}
