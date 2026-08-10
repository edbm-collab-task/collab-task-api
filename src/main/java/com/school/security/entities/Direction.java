package com.school.security.entities;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "direction")
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Direction implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "direction_id")
    protected Long directionId;

    private String name;
}
