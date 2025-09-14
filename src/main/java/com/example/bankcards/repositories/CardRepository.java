package com.example.bankcards.repositories;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Поиск карты по номеру
     */
    Optional<Card> findByCardNumber(String cardNumber);

    /**
     * Проверка существования карты по номеру
     */
    boolean existsByCardNumber(String cardNumber);

    /**
     * Поиск всех карт пользователя
     */
    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Поиск активных карт пользователя
     */
    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId AND c.status = 'ACTIVE'")
    List<Card> findActiveCardsByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Поиск истекших карт
     */
    @Query("SELECT c FROM Card c WHERE c.expiryDate < :currentDate")
    List<Card> findExpiredCards(@Param("currentDate") LocalDate currentDate);


    /**
     * Поиск карт по владельцу (username или имени)
     */
    @Query("SELECT c FROM Card c WHERE " +
           "LOWER(c.owner.middleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.owner.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.owner.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Card> findByOwnerNameContaining(@Param("search") String search, Pageable pageable);

//    /**
//     * Получение статистики по картам пользователя
//     */
//    @Query("SELECT c.status, COUNT(c) FROM Card c WHERE c.owner.id = :ownerId GROUP BY c.status")
//    List<Object[]> getCardStatisticsByOwnerId(@Param("ownerId") Long ownerId);
//
//    /**
//     * Получение общей статистики по картам
//     */
//    @Query("SELECT c.status, COUNT(c) FROM Card c GROUP BY c.status")
//    List<Object[]> getCardStatistics();
}