package com.example.bankcards.controller;

import com.example.bankcards.dto.users.AuthResponse;
import com.example.bankcards.dto.users.SignInRequest;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.security.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация")
public class AuthController {
    private final AuthenticationService authenticationService;

    @Operation(summary = "Регистрация пользователя")
    @ApiResponse(description = "Возвращает токен после регистрации")
    @PostMapping("/sign-up")
    @PreAuthorize("hasRole=('ADMIN')")
    public HttpStatus signUp(@io.swagger.v3.oas.annotations.parameters.RequestBody
            @RequestBody @Valid SignUpRequest request) {
        authenticationService.signUp(request);
        return HttpStatus.CREATED;
    }

    @Operation(summary = "Авторизация пользователя")
    @ApiResponse(description = "Возвращает токен после авторизации, рефреш устанавливает в куку")
    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> signIn(@io.swagger.v3.oas.annotations.parameters.RequestBody
                             @RequestBody @Valid SignInRequest signInRequest,
                                               HttpServletResponse response,
                                               HttpServletRequest request) {
        return ResponseEntity.ok(authenticationService.signIn(signInRequest,response, request));
    }

    @Operation(summary = "Обновления access токена для JWT")
    @ApiResponse(description = "Возвращает новый обновленный токен, рефреш устанавливает в куку")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@CookieValue("refreshToken") UUID refreshToken,
                                                     HttpServletResponse response,
                                                     HttpServletRequest request) {
            return ResponseEntity.ok(authenticationService.refreshToken(refreshToken, response, request));
    }
}
