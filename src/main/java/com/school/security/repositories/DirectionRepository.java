package com.school.security.repositories;
import com.school.security.entities.Direction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Long> {
    Optional<Direction> findByName(String name);
}