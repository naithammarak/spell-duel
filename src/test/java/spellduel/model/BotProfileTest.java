package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import org.junit.jupiter.api.Test;

class BotProfileTest {

    @Test
    void everyRowHasSameWidth() {
        for (int[] row : BotProfile.STATS) {
            assertEquals(BotProfile.COLUMNS, row.length);
        }
    }

    @Test
    void laterBotsAreStronger() {
        for (int i = 1; i < BotProfile.STATS.length; i++) {
            BotProfile prev = BotProfile.row(i - 1);
            BotProfile next = BotProfile.row(i);
            assertTrue(next.hp() > prev.hp());
            assertTrue(next.cps() > prev.cps());
            assertTrue(next.accuracy() > prev.accuracy());
            assertTrue(next.reaction() < prev.reaction());
        }
    }

    @Test
    void rowConvertsUnits() {
        BotProfile apprentice = BotProfile.row(0);
        assertEquals(3.0, apprentice.cps(), 1e-9);
        assertEquals(1.0, apprentice.reaction(), 1e-9);
        assertEquals(5.0, apprentice.castTime("bombarda maxima"), 1e-9);
    }

    @Test
    void levelsPastTheTableKeepGettingStronger() {
        BotProfile boss = BotProfile.forLevel(2);
        assertEquals(BotProfile.row(2).name(), boss.name());
        BotProfile next = BotProfile.forLevel(3);
        assertEquals("Dark Wizard +1", next.name());
        assertTrue(next.hp() > boss.hp());
        assertTrue(next.cps() > boss.cps());
        BotProfile far = BotProfile.forLevel(100);
        assertEquals(BotProfile.MAX_CPS, far.cps(), 1e-9);
        assertEquals(BotProfile.MAX_ACCURACY, far.accuracy());
        assertEquals(BotProfile.MIN_REACTION, far.reaction(), 1e-9);
    }

    @Test
    void rejectsRowWithWrongWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> BotProfile.fromRow("Broken", new int[] {1, 2, 3}, Color.RED));
    }
}
