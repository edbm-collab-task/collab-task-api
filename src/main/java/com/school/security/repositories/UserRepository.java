package com.school.security.repositories;

import com.school.security.entities.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByIsActiveTrue();
    List<User> findByIsActiveFalse();

    long countByDirectionDirectionId(Long directionId);

    /**
     * Comptes actifs avec leurs rôles chargés en une seule requête.
     *
     * <p>Équivalent de {@link #findByIsActiveTrue()} mais avec un
     * {@code LEFT JOIN FETCH} sur {@code roles}. Comme {@code User.roles} est
     * en {@code FetchType.EAGER}, l'ancienne méthode déclenchait une requête
     * par utilisateur (N+1) alors que le mapping DTO n'a besoin que d'un rôle
     * représentatif.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.isActive = true")
    List<User> findActiveWithRoles();
}
