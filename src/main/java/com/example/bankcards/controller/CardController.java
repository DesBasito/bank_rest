package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.AuthenticatedUserUtil;
import com.example.bankcards.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cards")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Tag(name = "Карты", description = "Операции управления банковскими картами")
public class CardController {
    private final AuthenticatedUserUtil userUtil;
    private final CardService cardService;

    @Operation(summary = "Получить все карты пользователя",
            description = "Получение списка карт текущего пользователя с пагинацией")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Список карт получен",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<Page<CardDto>> getMyCards(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest request) {

        Long userId = userUtil.getUserIdFromToken(request);

        Page<CardDto> cards = cardService.getUserCards(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Получить активные карты пользователя",
            description = "Получение списка активных карт текущего пользователя")
    @GetMapping("/my/active")
    public ResponseEntity<List<CardDto>> getMyActiveCards(HttpServletRequest request) {
        Long userId = userUtil.getUserIdFromToken(request);

        List<CardDto> cards = cardService.getUserActiveCards(userId);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Получить карту по ID",
            description = "Получение подробной информации о карте")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Информация о карте получена",
                    content = @Content(schema = @Schema(implementation = CardDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к карте")
    })
    @GetMapping("/{id}")
    @PreAuthorize("@cardService.isCardOwner(#id, authentication.name) or hasRole('ADMIN')")
    public ResponseEntity<CardDto> getCard(
            @Parameter(description = "ID карты") @PathVariable Long id) {

        CardDto card = cardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

}