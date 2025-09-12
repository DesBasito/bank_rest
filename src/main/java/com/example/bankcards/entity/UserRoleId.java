package com.example.bankcards.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class UserRoleId implements Serializable {
    @Serial
    private static final long serialVersionUID = 798504902068411700L;
    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserRoleId entity = (UserRoleId) o;
        return Objects.equals(this.roleId, entity.roleId) &&
               Objects.equals(this.userId, entity.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, userId);
    }

}