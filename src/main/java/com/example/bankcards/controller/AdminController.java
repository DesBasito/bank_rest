package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Управление пользователями", description = "Административные операции с пользователями")
public class AdminController {
    private final UserService userService;

    @Operation(summary = "Получить всех пользователей",
            description = "Получение списка всех пользователей с пагинацией")
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<UserDto> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Получить пользователя по ID",
            description = "Получение подробной информации о пользователе")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Информация о пользователе получена",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "ID пользователя") @PathVariable Long id) {

        UserDto user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Создать пользователя",
            description = "Создание нового пользователя администратором")
    @PostMapping
    public ResponseEntity<String> createUser(
            @Valid @RequestBody SignUpRequest userDto,
            @Parameter(description = "Пароль пользователя") @RequestParam String password) {

        UserDto createdUser = userService.createUser(userDto, password);

        return ResponseEntity.ok("Пользователь "+userDto.getFullName()+" создан");
    }


    @Operation(summary = "Заблокировать/разблокировать пользователя",
            description = "Изменение статуса активности пользователя")
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<String> toggleUserStatus(
            @Parameter(description = "ID пользователя") @PathVariable Long id) {

        UserDto user = userService.toggleUserStatus(id);

        String message = user.getIsActive() ? "Пользователь активирован: "+user.getFullName() : "Пользователь "+user.getFullName()+" заблокирован";

        return ResponseEntity.ok(message);
    }


    @Operation(summary = "Удалить пользователя",
            description = "Удаление пользователя из системы")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Пользователь удален"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Нельзя удалить пользователя с активными картами")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "ID пользователя") @PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.ok("Пользователь удален"));
    }
}