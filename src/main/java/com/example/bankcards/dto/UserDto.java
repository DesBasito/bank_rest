package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link com.example.bankcards.entity.User}
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    String id;
    @NotNull
    String phoneNumber;
    @NotNull
    String firstName;
    @NotNull
    String middleName;
    @NotNull
    String lastName;
}