package com.example.bankcards.service;

import com.example.bankcards.dto.users.UserDto;
import com.example.bankcards.dto.mappers.UserMapper;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repositories.UserRepository;
import com.example.bankcards.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository repository;
    private final CardRepository cardRepository;
    private final UserMapper userMapper;


    @Transactional
    public User create(User user) {
        log.info("Создание нового пользователя: {}", user.getFullName());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return repository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.info("Получение списка всех пользователей, страница: {}, размер: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return repository.findAll(pageable)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.info("Поиск пользователя по ID: {}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с ID " + id + " не найден"));

        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto toggleUserStatus(Long id) {
        log.info("Изменение статуса пользователя с ID: {}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с ID " + id + " не найден"));

        user.setEnabled(!user.getEnabled());
        user.setUpdatedAt(Instant.now());

        User updatedUser = repository.save(user);

        log.info("Статус пользователя {} изменен на: {}",
                updatedUser.getFullName(), updatedUser.getEnabled() ? "активен" : "заблокирован");

        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Удаление пользователя с ID: {}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с ID " + id + " не найден"));

        boolean hasActiveCards = cardRepository.findActiveCardsByOwnerId(id).size() > 0;
        if (hasActiveCards) {
            throw new IllegalArgumentException("Нельзя удалить пользователя с активными картами");
        }

        repository.delete(user);
        log.info("Пользователь {} успешно удален", user.getFullName());
    }

    public User getByPhoneNumber(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с номером: " + phoneNumber));
    }

    public UserDetailsService userDetailsService() {
        return this::getByPhoneNumber;
    }


    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        return repository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }
}