package com.example.bankcards.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService{

//    private final UserRepository repository;
//
//
//    public User create(User user) {
//        return repository.save(user);
//    }
//
//
//    public User getByPhoneNumber(String phoneNumber) {
//        return repository.findByPhoneNumber(phoneNumber)
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с номером: " + phoneNumber));
//    }
//
    public UserDetailsService userDetailsService() {
//        return this::getByPhoneNumber;
        return null;
    }
//
//
//    public User getCurrentUser() {
//        // Получение имени пользователя из контекста Spring Security
//        var username = SecurityContextHolder.getContext().getAuthentication().getName();
//        return getByPhoneNumber(username);
//    }
//
//
//    /**
//     * Выдача прав администратора текущему пользователю
//     * <p>
//     * Нужен для демонстрации
//     */
//    @Deprecated
//    public void getAdmin() {
//        var user = getCurrentUser();
//        user.setRole(Role.ADMIN.toString());
//        repository.save(user);
//    }
//
    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
//        return repository.findByPhoneNumber(phoneNumber)
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        return null;
    }
}