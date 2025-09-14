package com.example.bankcards.controller;

import com.example.bankcards.dto.transactions.TransactionDto;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.AuthenticatedUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/transactions")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Транзакции", description = "Операции с переводами между картами")
public class TransactionController {
    private final TransactionService transactionService;
    private final AuthenticatedUserUtil userUtil;

    @Operation(summary = "Выполнить перевод",
            description = "Перевод средств между картами одного пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Перевод выполнен успешно",
                    content = @Content(schema = @Schema(implementation = TransactionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации или недостаток средств"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к картам")
    })

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferMoney(
            @Parameter(description = "ID карты отправителя") @RequestParam Long fromCardId,
            @Parameter(description = "ID карты получателя") @RequestParam Long toCardId,
            @Parameter(description = "Сумма перевода") @RequestParam BigDecimal amount,
            @Parameter(description = "Описание перевода")
            @RequestParam(required = false) String description,
            HttpServletRequest request) {

        String name = userUtil.getUserNameFromToken(request);
        log.info("Пользователь {} инициирует перевод с карты {} на карту {} суммы {}",
                name, fromCardId, toCardId, amount);

        TransactionDto transaction = transactionService.transferMoney(
                fromCardId, toCardId, amount, description);

        return ResponseEntity.ok(transaction);
    }

    @Operation(summary = "Получить мои транзакции",
            description = "Получение списка транзакций текущего пользователя")
    @GetMapping("/my")
    public ResponseEntity<Page<TransactionDto>> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);

        Page<TransactionDto> transactions = transactionService.getUserTransactions(userId, pageable);

        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Получить транзакции по карте",
            description = "Получение всех транзакций конкретной карты")
    @GetMapping("/card/{cardId}")
    @PreAuthorize("@cardService.getCardById(#cardId).ownerId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getCardTransactions(
            @Parameter(description = "ID карты") @PathVariable Long cardId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TransactionDto> transactions = transactionService.getCardTransactions(cardId, pageable);

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }


    @Operation(summary = "Отменить транзакцию",
            description = "Отмена транзакции в статусе 'Ожидает обработки'")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Транзакция отменена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Нельзя отменить обработанную транзакцию")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TransactionDto>> cancelTransaction(
            @Parameter(description = "ID транзакции") @PathVariable Long id,
            Authentication authentication) {

        TransactionDto transaction = transactionService.cancelTransaction(id);

        return ResponseEntity.ok(ApiResponse.success("Транзакция отменена", transaction));
    }

    @Operation(summary = "Получить статистику транзакций",
            description = "Получение статистики транзакций пользователя")
    @GetMapping("/my/statistics")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMyTransactionStatistics(
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        List<Object[]> statistics = transactionService.getUserTransactionStatistics(userId);

        return ResponseEntity.ok(ApiResponse.success("Статистика транзакций", statistics));
    }

    @Operation(summary = "Получить транзакции между картами",
            description = "Получение истории переводов между двумя конкретными картами")
    @GetMapping("/between-cards")
    @PreAuthorize("(@cardService.getCardById(#card1Id).ownerId == authentication.principal.id and " +
                  "@cardService.getCardById(#card2Id).ownerId == authentication.principal.id) or " +
                  "hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsBetweenCards(
            @Parameter(description = "ID первой карты") @RequestParam Long card1Id,
            @Parameter(description = "ID второй карты") @RequestParam Long card2Id) {

        List<TransactionDto> transactions = transactionService.getTransactionsBetweenCards(card1Id, card2Id);

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @Operation(summary = "Получить все транзакции (админ)",
            description = "Получение всех транзакций в системе (только для администраторов)")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @Parameter(description = "ID пользователя") @RequestParam(required = false) Long userId,
            @Parameter(description = "Статус транзакции")
            @RequestParam(required = false) Transaction.TransactionStatus status,
            @Parameter(description = "Дата начала периода")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Дата окончания периода")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TransactionDto> transactions = transactionService.searchTransactions(
                userId, status, startDate, endDate, pageable);

        return ResponseEntity.ok(transactions);
    }
}