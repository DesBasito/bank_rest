package com.example.bankcards.enums;

public interface EnumInterface {
    String getDescription();

    static <E extends Enum<E> & EnumInterface> String toDescription(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value).getDescription();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static <E extends Enum<E> & EnumInterface> Boolean isExists(Class<E> enumClass,String value) {
        for (E type : enumClass.getEnumConstants()) {
            if (type.name().equalsIgnoreCase(value.strip())) {
                return true;
            }
        }
        return false;
    }

    static  <E extends Enum<E> & EnumInterface> String getEnumDescription(Class<E> enumClass) {
        StringBuilder str = new StringBuilder();
        str.append("Указан неверный тип. Укажите одну из: ");
        for (E enumValue : enumClass.getEnumConstants()) {
            str.append(String.format("-> %s(%s) ",enumValue.name(), enumValue.getDescription()));
        }
        return str.toString();
    }
}