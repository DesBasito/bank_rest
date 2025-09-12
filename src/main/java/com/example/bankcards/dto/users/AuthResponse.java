package com.example.bankcards.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(chain = true)
@Schema(description = "Ответ после аутентификации")
@AllArgsConstructor
public class AuthResponse {
    @Schema(description = "Access token")
    String accessToken;
}
