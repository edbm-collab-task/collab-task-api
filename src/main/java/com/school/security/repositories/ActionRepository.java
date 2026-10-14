package com.school.security.repositories;

import com.school.security.entities.Action;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    /** Retrouve une action par son nom technique (ex. {@code "INSERT"}). */
    Optional<Action> findByName(String name);

    /** Toutes les actions, triées par code binaire croissant. */
    List<Action> findAllByOrderByCodeActionAsc();
}
