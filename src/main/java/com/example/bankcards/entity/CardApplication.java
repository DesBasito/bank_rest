package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "card_applications")
public class CardApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "processed_at")
    Instant processedAt;

    @ColumnDefault("'PENDING'")
    @Column(name = "status", columnDefinition = "card_application_status not null")
    String status;

    @Column(name = "type", columnDefinition = "card_type not null")
    String cardType;

    @Column(name = "comment", columnDefinition = "card_type not null")
    String comment;
}