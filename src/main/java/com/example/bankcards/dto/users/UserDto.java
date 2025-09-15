package com.example.bankcards.dto.users;

import com.example.bankcards.dto.cards.CardDto;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Getter@Setter
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
    Boolean isActive;
    List<CardDto> cards;

    public String getFullName(){
        return String.format("%s %s %s%n", firstName, middleName != null ? middleName : "", lastName);
    }
}