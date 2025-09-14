package com.example.bankcards.repositories;

import com.example.bankcards.entity.RefreshSession;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Integer> {
    Optional<RefreshSession> findByRefreshToken(UUID refreshToken);
    void deleteByRefreshToken(UUID refreshToken);
    List<RefreshSession> findByUserOrderByCreatedAtAsc(User user, Pageable pageable);
}
