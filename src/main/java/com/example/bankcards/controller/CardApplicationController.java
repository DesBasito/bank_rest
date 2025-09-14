package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.cardApplication.CardApplicationDto;
import com.example.bankcards.dto.cardApplication.CardApplicationRequest;
import com.example.bankcards.service.CardApplicationService;
import com.example.bankcards.util.AuthenticatedUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/card-applications")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Заявки на карты", description = "Управление заявками на создание карт")
public class CardApplicationController {

    private final CardApplicationService cardApplicationService;
    private final AuthenticatedUserUtil userUtil;

    @Operation(summary = "Создать заявку на карту",
            description = "Создание заявки на новую карту пользователем")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Заявка создана",
                    content = @Content(schema = @Schema(implementation = CardApplicationDto.class)))
    })
    @PostMapping
    public ResponseEntity<CardApplicationDto> createApplication(
            @Valid @RequestBody CardApplicationRequest request,
            HttpServletRequest httpRequest) {

        Long userId = userUtil.getUserIdFromToken(httpRequest);
        String userName = userUtil.getUserNameFromToken(httpRequest);

        log.info("Пользователь {} создает заявку на карту типа {}", userName, request.getCardType());

        CardApplicationDto application = cardApplicationService.createCardApplication(userId, request);

        return ResponseEntity.ok(application);
    }

    @Operation(summary = "Получить мои заявки",
            description = "Получение списка заявок текущего пользователя")
    @GetMapping("/my")
    public ResponseEntity<Page<CardApplicationDto>> getMyApplications(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);

        Page<CardApplicationDto> applications = cardApplicationService.getUserApplications(userId, pageable);

        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "Отменить заявку",
            description = "Отмена заявки пользователем (только в статусе PENDING)")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CardApplicationDto> cancelApplication(
            @Parameter(description = "ID заявки") @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);
        String userName = userUtil.getUserNameFromToken(request);

        log.info("Пользователь {} отменяет заявку с ID: {}", userName, id);

        CardApplicationDto application = cardApplicationService.cancelCardApplication(id, userId);

        return ResponseEntity.ok(application);
    }

    // ======================================================================================================

    @Operation(summary = "Получить все заявки (админ)",
            description = "Получение всех заявок в системе (только для администраторов)")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardApplicationDto>> getAllApplications(
            @Parameter(description = "Статус заявки")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<CardApplicationDto> applications;

        if (status != null) {
            applications = cardApplicationService.getApplicationsByStatus(status, pageable);
        } else {
            applications = cardApplicationService.getAllApplications(pageable);
        }

        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "Одобрить заявку (админ)",
            description = "Одобрение заявки и создание карты")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Заявка одобрена, карта создана",
                    content = @Content(schema = @Schema(implementation = CardDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Заявка уже обработана")
    })
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> approveApplication(
            @Parameter(description = "ID заявки") @PathVariable Long id) {

        log.info("Администратор одобряет заявку с ID: {}", id);

        CardDto card = cardApplicationService.approveCardApplication(id);

        return ResponseEntity.ok(card);
    }

    @Operation(summary = "Отклонить заявку (админ)",
            description = "Отклонение заявки администратором")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardApplicationDto> rejectApplication(
            @Parameter(description = "ID заявки") @PathVariable Long id,
            @Parameter(description = "Причина отклонения")
            @RequestParam(required = false) String reason) {

        log.info("Администратор отклоняет заявку с ID: {}, причина: {}", id, reason);

        CardApplicationDto application = cardApplicationService.rejectCardApplication(id, reason);

        return ResponseEntity.ok(application);
    }
}