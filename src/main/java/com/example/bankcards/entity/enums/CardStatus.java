package com.example.bankcards.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CardStatus {
    ACTIVE("АКТИВНА"), BLOCKED("ЗАБЛОКИРОВАНА"), EXPIRED("ИСТЕК СРОК");

    private final String description;
}
