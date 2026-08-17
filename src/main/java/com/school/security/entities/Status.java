package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "statuses")
@ToString
@Getter
@Setter
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
}
