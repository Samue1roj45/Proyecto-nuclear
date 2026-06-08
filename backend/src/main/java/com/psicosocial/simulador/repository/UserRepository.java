package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.AuthProvider;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
    long countByEnabledTrue();
    long countByEnabledFalse();
    long countByRoleAndEnabledTrue(UserRole role);
    long countByRoleAndEnabledFalse(UserRole role);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<User> findByRole(UserRole role);
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}
