package com.example.bankcards.controller;

import com.example.bankcards.dto.transactions.TransactionDto;
import com.example.bankcards.dto.transactions.TransferRequest;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/transactions")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Транзакции", description = "Операции с переводами между картами")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthenticatedUserUtil userUtil;

    @Operation(summary = "Перевод между своими картами",
            description = "Перевод средств между картами текущего пользователя")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Перевод успешно выполнен",
                    content = @Content(schema = @Schema(implementation = TransactionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные для перевода"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Карта не принадлежит пользователю")
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferBetweenMyCards(
            @Valid @RequestBody TransferRequest request,
            HttpServletRequest httpRequest) {

        String userName = userUtil.getUserNameFromToken(httpRequest);
        log.info("Пользователь {} инициирует перевод с карты {} на карту {} на сумму {}",
                userName, request.getFromCardId(), request.getToCardId(), request.getAmount());
        TransactionDto transaction = transactionService.transferBetweenUserCards(request);
        return ResponseEntity.ok(transaction);
    }

    @Operation(summary = "Получить историю моих транзакций",
            description = "Получение истории транзакций по картам пользователя")
    @GetMapping("/my")
    public ResponseEntity<Page<TransactionDto>> getMyTransactions(
            @Parameter(description = "ID карты для фильтрации (необязательно)")
            @RequestParam(required = false) Long cardId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);
        Page<TransactionDto> transactions = transactionService.getUserTransactions(userId, cardId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Получить транзакцию по ID",
            description = "Получение подробной информации о транзакции")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Информация о транзакции получена",
                    content = @Content(schema = @Schema(implementation = TransactionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Транзакция не найдена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к транзакции")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(
            @Parameter(description = "ID транзакции") @PathVariable Long id) {
        TransactionDto transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @Operation(summary = "Получить историю транзакций",
            description = "Получение истории транзакций")
    @GetMapping("/all")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }
}