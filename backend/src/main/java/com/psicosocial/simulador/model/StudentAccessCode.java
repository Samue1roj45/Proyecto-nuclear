package com.psicosocial.simulador.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_acceso_estudiante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAccessCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private User student;

    @Column(unique = true, length = 10)
    private String codigo;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessCodeStatus estado;

    @Builder.Default
    @Column(nullable = false)
    private boolean utilizado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private User approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime usedAt;

    @Column(length = 64)
    private String sessionToken;

    private LocalDateTime sessionExpiresAt;
}
