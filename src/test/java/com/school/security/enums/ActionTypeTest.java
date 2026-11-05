package com.school.security.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Verrouille la correspondance action → code binaire.
 *
 * <p>L'ordre de déclaration de {@link ActionType} est la source de vérité des
 * masques stockés en base ({@code roles_permissions.actions},
 * {@code permissions.total_actions}). Si quelqu'un réordonne l'énumération ou
 * modifie un index, ce test casse immédiatement — au lieu de corrompre
 * silencieusement les masques déjà persistés.
 */
class ActionTypeTest {

    @Test
    void codesShouldMatchTheFrozenMapping() {
        assertEquals(1, ActionType.INSERT.getCode());
        assertEquals(2, ActionType.SELECT.getCode());
        assertEquals(4, ActionType.UPDATE.getCode());
        assertEquals(8, ActionType.DELETE.getCode());
        assertEquals(16, ActionType.EXPORT.getCode());
        assertEquals(32, ActionType.DOWNLOAD.getCode());
        assertEquals(64, ActionType.ARCHIVE.getCode());
    }

    @Test
    void bitIndexShouldBeThePosition() {
        assertEquals(0, ActionType.INSERT.getBitIndex());
        assertEquals(1, ActionType.SELECT.getBitIndex());
        assertEquals(3, ActionType.DELETE.getBitIndex());
        assertEquals(4, ActionType.EXPORT.getBitIndex());
        assertEquals(5, ActionType.DOWNLOAD.getBitIndex());
        assertEquals(6, ActionType.ARCHIVE.getBitIndex());
    }

    @Test
    void codeForShouldComputePowerOfTwo() {
        assertEquals(1, ActionType.codeFor(0));
        assertEquals(2, ActionType.codeFor(1));
        assertEquals(16, ActionType.codeFor(4));
        assertEquals(32, ActionType.codeFor(5));
        assertEquals(64, ActionType.codeFor(6));
    }

    @Test
    void fromCodeShouldResolveEachAction() {
        for (ActionType action : ActionType.values()) {
            assertEquals(action, ActionType.fromCode(action.getCode()));
        }
    }

    @Test
    void applySelectPrerequisiteShouldNeutralizeMaskWithoutSelect() {
        int withoutSelect = ActionType.INSERT.getCode() | ActionType.UPDATE.getCode();
        assertEquals(0, ActionType.applySelectPrerequisite(withoutSelect));

        int withSelect = withoutSelect | ActionType.SELECT.getCode();
        assertEquals(withSelect, ActionType.applySelectPrerequisite(withSelect));
    }
}
