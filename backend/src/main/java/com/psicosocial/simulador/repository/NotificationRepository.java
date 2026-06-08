package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.Notification;
import com.psicosocial.simulador.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);
    long countByUserAndReadFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllRead(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
