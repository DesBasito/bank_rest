package com.example.bankcards.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CardType {
    ACTIVE("АКТИВНА"), BLOCKED("ЗАБЛОКИРОВАНА"), EXPIRED("ИСТЕК СРОК");

    private final String description;
}
