package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.cards.CardDto;
import com.example.bankcards.dto.users.UserDto;
import com.example.bankcards.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final CardMapper cardMapper;

    public UserDto toDto(User user) {
       UserDto dto =  getUserDto(user);

        if (user.getCards() != null && !user.getCards().isEmpty()) {
            List<CardDto> cardDtos = user.getCards().stream()
                    .map(cardMapper::toDto)
                    .collect(Collectors.toList());
            dto.setCards(cardDtos);
        }

        return dto;
    }

    public UserDto toDtoWithoutCards(User user) {
        return getUserDto(user);
    }

    private UserDto getUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(String.valueOf(user.getId()));
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setFirstName(user.getFirstName());
        dto.setMiddleName(user.getMiddleName());
        dto.setLastName(user.getLastName());
        dto.setIsActive(user.getEnabled());
        return dto;
    }
}