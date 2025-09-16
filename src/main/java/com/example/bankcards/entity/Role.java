package com.example.bankcards.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @NotNull
    @Column(name = "name", nullable = false, length = 50)
    String name;

    @Column(name = "description")
    String description;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    List<User> users = new ArrayList<>();
}