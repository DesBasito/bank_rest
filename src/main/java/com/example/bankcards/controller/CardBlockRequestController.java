package com.example.bankcards.controller;

import com.example.bankcards.dto.cards.CardBlockRequestCreateDto;
import com.example.bankcards.dto.cards.CardBlockRequestDto;
import com.example.bankcards.service.CardBlockRequestService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/card-block-requests")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Запросы на блокировку карт", description = "Управление запросами на блокировку банковских карт")
public class CardBlockRequestController {
    private final CardBlockRequestService cardBlockRequestService;
    private final AuthenticatedUserUtil userUtil;


    @Operation(summary = "Создать запрос на блокировку карты",
            description = "Создание запроса на блокировку собственной карты пользователем")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Запрос на блокировку создан",
                    content = @Content(schema = @Schema(implementation = CardBlockRequestDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные или карта уже заблокирована"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к данной карте")
    })
    @PostMapping
    public ResponseEntity<CardBlockRequestDto> createBlockRequest(
            @Valid @RequestBody CardBlockRequestCreateDto request,
            HttpServletRequest httpRequest) {

        Long userId = userUtil.getUserIdFromToken(httpRequest);
        String userName = userUtil.getUserNameFromToken(httpRequest);

        log.info("Пользователь {} создает запрос на блокировку карты с ID: {}", userName, request.getCardId());

        CardBlockRequestDto blockRequest = cardBlockRequestService.createBlockRequest(userId, request);

        return ResponseEntity.ok(blockRequest);
    }

    @Operation(summary = "Получить мои запросы на блокировку",
            description = "Получение списка запросов на блокировку текущего пользователя")
    @GetMapping("/my")
    public ResponseEntity<Page<CardBlockRequestDto>> getMyBlockRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);

        Page<CardBlockRequestDto> blockRequests = cardBlockRequestService.getUserBlockRequests(userId, pageable);

        return ResponseEntity.ok(blockRequests);
    }

    @Operation(summary = "Отменить запрос на блокировку",
            description = "Отмена запроса на блокировку пользователем (только в статусе PENDING)")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authenticatedUserUtil.isCardOwner(#id, authentication.name)")
    public ResponseEntity<HttpStatus> cancelBlockRequest(
            @Parameter(description = "ID запроса на блокировку") @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);
        String userName = userUtil.getUserNameFromToken(request);

        log.info("Пользователь {} отменяет запрос на блокировку с ID: {}", userName, id);

        cardBlockRequestService.cancelBlockRequest(id, userId);

        return ResponseEntity.ok().body(HttpStatus.OK);
    }

    @Operation(summary = "Получить запрос на блокировку по ID",
            description = "Получение подробной информации о запросе на блокировку")
    @GetMapping("/{id}")
    @PreAuthorize("@authenticatedUserUtil.isCardOwner(#id, authentication.name) or hasRole('ADMIN')")
    public ResponseEntity<CardBlockRequestDto> getBlockRequest(
            @Parameter(description = "ID запроса на блокировку") @PathVariable Long id) {

        CardBlockRequestDto blockRequest = cardBlockRequestService.getBlockRequestById(id);

        return ResponseEntity.ok(blockRequest);
    }


    // ================================================================

    @Operation(summary = "Получить все запросы на блокировку (админ)",
            description = "Получение всех запросов на блокировку в системе (только для администраторов)")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardBlockRequestDto>> getAllBlockRequests(
            @Parameter(description = "Статус запроса")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<CardBlockRequestDto> blockRequests;

        if (status != null) {
            blockRequests = cardBlockRequestService.getBlockRequestsByStatus(status, pageable);
        } else {
            blockRequests = cardBlockRequestService.getAllBlockRequests(pageable);
        }

        return ResponseEntity.ok(blockRequests);
    }

    @Operation(summary = "Одобрить запрос на блокировку (админ)",
            description = "Одобрение запроса на блокировку и блокировка карты")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Запрос одобрен, карта заблокирована",
                    content = @Content(schema = @Schema(implementation = CardBlockRequestDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Запрос уже обработан")
    })
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardBlockRequestDto> approveBlockRequest(
            @Parameter(description = "ID запроса на блокировку") @PathVariable Long id,
            @Parameter(description = "Комментарий администратора")
            @RequestParam(required = false) String adminComment,
            HttpServletRequest request) {

        Long adminId = userUtil.getUserIdFromToken(request);
        String adminName = userUtil.getUserNameFromToken(request);

        log.info("Администратор {} одобряет запрос на блокировку с ID: {}", adminName, id);

        CardBlockRequestDto blockRequest = cardBlockRequestService.approveBlockRequest(id, adminId, adminComment);

        return ResponseEntity.ok(blockRequest);
    }

    @Operation(summary = "Отклонить запрос на блокировку (админ)",
            description = "Отклонение запроса на блокировку администратором")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardBlockRequestDto> rejectBlockRequest(
            @Parameter(description = "ID запроса на блокировку") @PathVariable Long id,
            @Parameter(description = "Комментарий администратора (причина отклонения)")
            @RequestParam(required = false) String adminComment,
            HttpServletRequest request) {

        Long adminId = userUtil.getUserIdFromToken(request);
        String adminName = userUtil.getUserNameFromToken(request);

        log.info("Администратор {} отклоняет запрос на блокировку с ID: {}, причина: {}", adminName, id, adminComment);

        CardBlockRequestDto blockRequest = cardBlockRequestService.rejectBlockRequest(id, adminId, adminComment);

        return ResponseEntity.ok(blockRequest);
    }
}