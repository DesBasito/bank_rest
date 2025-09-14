package com.example.bankcards.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransactionStatus {
    SUCCESS("Успешно"), CANCELLED("Отклонена самим заказчиком"), REFUNDED("Возвращено");

    private final String description;
}
