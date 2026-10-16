package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

/**
 * Action élémentaire pouvant être accordée sur un module.
 *
 * <p>Chaque action possède :
 * <ul>
 *   <li>un <strong>nom technique</strong> ({@code name}, ex. {@code INSERT})
 *       qui fait le lien avec {@link com.school.security.enums.ActionType} :
 *       c'est là que le <strong>code binaire</strong> de l'action est calculé
 *       ({@code 1 << bitIndex}), il n'est plus stocké en base ;</li>
 *   <li>un <strong>libellé</strong> affichable ({@code label}, ex. « Insérer »).</li>
 * </ul>
 *
 * <p>La table {@code actions} est reliée en plusieurs-à-plusieurs à
 * {@code permissions} ({@link Permission#getActions()}) : cet ensemble définit
 * quelles actions existent pour chaque module et peut être ajusté par
 * l'administration.
 */
@Entity
@Table(name = "actions")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Action implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actions_id")
    private Long actionsId;

    /** Nom technique de l'action (valeur de {@code ActionType}). */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /** Libellé affichable de l'action. */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    public Long getActionsId() {
        return actionsId;
    }

    public void setActionsId(Long actionsId) {
        this.actionsId = actionsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
