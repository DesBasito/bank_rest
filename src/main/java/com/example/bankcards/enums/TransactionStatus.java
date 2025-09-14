package com.example.bankcards.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransactionStatus implements EnumInterface{
    SUCCESS("Успешно"), CANCELLED("Отклонена самим заказчиком 💅🏿"), REFUNDED("Возвращено");

    private final String description;
}
