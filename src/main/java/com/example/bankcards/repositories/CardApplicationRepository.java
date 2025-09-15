package com.example.bankcards.repositories;

import com.example.bankcards.entity.CardApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CardApplicationRepository extends JpaRepository<CardApplication, Long> {

    Page<CardApplication> findByUserId(Long userId, Pageable pageable);
    Page<CardApplication> findByStatus(String status, Pageable pageable);
}