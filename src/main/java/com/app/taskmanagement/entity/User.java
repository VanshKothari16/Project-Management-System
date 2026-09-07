package com.app.taskmanagement.entity;

import com.app.taskmanagement.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,unique = true,updatable = false)
    private String email;

    @Column(nullable = false,updatable = false)
    private String password;

    @Column(nullable = false,unique = true)
    private String mobileNo;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Role role;

}
