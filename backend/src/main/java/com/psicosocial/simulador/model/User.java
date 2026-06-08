package com.psicosocial.simulador.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(length = 1000)
    private String avatarUrl;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int maxAttempts = 3;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    private String providerId;
}
