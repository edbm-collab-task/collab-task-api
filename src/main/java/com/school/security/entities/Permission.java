package com.school.security.entities;

import com.school.security.enums.PermissionType;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Permission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PermissionType name;

    private String description;
}
