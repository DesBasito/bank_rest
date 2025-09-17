package com.example.bankcards.dto.mappers;

import com.example.bankcards.dto.cards.CardDto;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.dto.users.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final CardMapper cardMapper;
    private final PasswordEncoder encoder;

    public UserDto toDto(User user) {
        UserDto dto = getUserDto(user);

        if (user.getCards() != null && !user.getCards().isEmpty()) {
            List<CardDto> cardDtos = user.getCards().stream()
                    .map(cardMapper::toDto)
                    .collect(Collectors.toList());
            dto.setCards(cardDtos);
        }

        return dto;
    }

    public User toEntity(SignUpRequest request) {
        return User.builder()
                .firstName(request.getName())
                .lastName(request.getSurname())
                .middleName(request.getMiddleName())
                .phoneNumber(request.getPhoneNumber())
                .role(new Role().setId(1L))
                .password(encoder.encode(request.getPassword()))
                .enabled(true)
                .build();
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