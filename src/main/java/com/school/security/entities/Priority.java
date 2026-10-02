package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "priorities")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Priority implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "priority_id")
    protected Long priorityId;

    @Column(nullable = false)
    private String name;

    /**
     * Ordre métier de la priorité (1 = Urgente, 2 = Haute, 3 = Moyenne,
     * 4 = Basse). Distinct de {@link #priorityId} : permet de trier les tâches
     * sans présumer que l'identifiant d'une priorité représente son ordre.
     */
    private Integer sortOrder;

    public Long getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(Long priorityId) {
        this.priorityId = priorityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}