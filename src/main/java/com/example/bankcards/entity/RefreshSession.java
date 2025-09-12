package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "refresh_sessions")
public class RefreshSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "refresh_token", nullable = false)
    private UUID refreshToken;

    @NotNull
    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @NotNull
    @Column(name = "ua", nullable = false)
    private String ua;

    @NotNull
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @NotNull
    @Column(name = "expires_in", nullable = false)
    private Long expiresIn;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

}