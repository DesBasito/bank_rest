package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.channels.FileChannel;
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


//    /**
//     * Поиск карт по статусу
//     */
//    Page<Card> findByStatus(Card.CardStatus status, Pageable pageable);
//
//    /**
//     * Поиск карт пользователя по статусу
//     */
//    Page<Card> findByOwnerIdAndStatus(Long ownerId, Card.CardStatus status, Pageable pageable);
//
    /**
     * Поиск истекших карт
     */
    @Query("SELECT c FROM Card c WHERE c.expiryDate < :currentDate")
    List<Card> findExpiredCards(@Param("currentDate") LocalDate currentDate);

//    /**
//     * Поиск карт, срок которых истекает скоро
//     */
//    @Query("SELECT c FROM Card c WHERE c.expiryDate BETWEEN :startDate AND :endDate")
//    List<Card> findCardsExpiringBetween(@Param("startDate") LocalDate startDate,
//                                        @Param("endDate") LocalDate endDate);

//    /**
//     * Комплексный поиск карт с фильтрами
//     */
//    @Query("SELECT c FROM Card c WHERE " +
//           "(:ownerId IS NULL OR c.owner.id = :ownerId) AND " +
//           "(:status IS NULL OR c.status = :status) AND " +
//           "(:cardType IS NULL OR c.cardType = :cardType) AND " +
//           "(:expiryDateFrom IS NULL OR c.expiryDate >= :expiryDateFrom) AND " +
//           "(:expiryDateTo IS NULL OR c.expiryDate <= :expiryDateTo)")
//    Page<Card> findCardsWithFilters(@Param("ownerId") Long ownerId,
//                                    @Param("status") Card.CardStatus status,
//                                    @Param("cardType") String cardType,
//                                    @Param("expiryDateFrom") LocalDate expiryDateFrom,
//                                    @Param("expiryDateTo") LocalDate expiryDateTo,
//                                    Pageable pageable);
//
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