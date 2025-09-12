package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @Column(name = "personal_id", nullable = false)
    String id;

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
    Instant createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    Instant updatedAt;

    @OneToMany(mappedBy = "owner")
    @BatchSize(size = 3)
    @Builder.Default
    transient Set<Card> cards = new LinkedHashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    transient Set<Role> roles = new LinkedHashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(e -> new SimpleGrantedAuthority("ROLE_"+e.getName())).toList();
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