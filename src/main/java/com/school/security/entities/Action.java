package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

/**
 * Action élémentaire pouvant être accordée sur un module.
 *
 * <p>Chaque action possède :
 * <ul>
 *   <li>un <strong>code binaire</strong> ({@code code_action}, puissance de 2)
 *       qui permet de représenter un ensemble d'actions par un masque ;</li>
 *   <li>un <strong>nom technique</strong> ({@code name}, ex. {@code INSERT})
 *       utilisé par l'enforcement ({@code hasAction('MANAGE_TASKS','INSERT')}) ;</li>
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

    /** Code binaire de l'action (1, 2, 4, 8, 16, 32). */
    @Column(name = "code_action", nullable = false, unique = true)
    private Integer codeAction;

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

    public Integer getCodeAction() {
        return codeAction;
    }

    public void setCodeAction(Integer codeAction) {
        this.codeAction = codeAction;
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
