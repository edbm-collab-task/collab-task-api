package com.school.security.entities;

import com.school.security.enums.PermissionType;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "user_project_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "project_id", "permission_name"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class UserProjectPermission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_name", nullable = false)
    private PermissionType permissionName;
}
