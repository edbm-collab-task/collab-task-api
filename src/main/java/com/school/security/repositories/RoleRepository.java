package com.school.security.repositories;

import com.school.security.entities.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Optional<Role> findByCodeRole(String codeRole);

    /** Rôles triés par rang hiérarchique, puis par nom pour départager les rangs égaux. */
    List<Role> findAllByOrderBySortOrderAscNameAsc();
}
