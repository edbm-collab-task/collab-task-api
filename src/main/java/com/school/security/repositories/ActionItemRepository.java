package com.school.security.repositories;

import com.school.security.entities.ActionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByProjectProjectId(Long projectId);
}