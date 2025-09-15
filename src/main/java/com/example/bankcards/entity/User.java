package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @NotNull
    @Column(name = "phone_number", nullable = false)
    String phoneNumber;

    @NotNull
    @Column(name = "password", nullable = false)
    String password;

    @NotNull
    @Column(name = "first_name", nullable = false, length = 100)
    String firstName;

    @NotNull
    @Column(name = "middle_name", nullable = false, length = 100)
    String middleName;

    @NotNull
    @Column(name = "last_name", nullable = false, length = 100)
    String lastName;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    Boolean enabled = true;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    @CreatedDate
    Instant createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    @LastModifiedDate
    Instant updatedAt;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    @BatchSize(size = 3)
    @Builder.Default
    Set<Card> cards = new LinkedHashSet<>();

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    Set<Role> roles = new LinkedHashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }

        return roles.stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());
    }

    public String getFullName() {
        return String.format("%s %s %s", lastName, firstName, middleName);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}