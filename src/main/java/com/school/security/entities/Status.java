package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "statuses")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Status implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    protected Long statusId;

    @Column(nullable = false)
    private String name;

    private Integer sortOrder;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}