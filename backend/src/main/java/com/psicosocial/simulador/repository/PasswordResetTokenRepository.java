package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.PasswordResetToken;
import com.psicosocial.simulador.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    void deleteByUser(User user);
}
