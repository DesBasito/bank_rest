package com.example.bankcards.components;

import com.example.bankcards.entity.User;
import com.example.bankcards.util.AuthenticatedUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAuditorAwareImpl implements AuditorAware<User> {
    private final AuthenticatedUserUtil userUtil;

    @Override
    public Optional<User> getCurrentAuditor() {
        return Optional.of(userUtil.getCurrentUser());
    }
}
