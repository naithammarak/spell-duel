package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import org.junit.jupiter.api.Test;

class BotProfileTest {

    @Test
    void everyRowHasSameWidth() {
        assertEquals(GameWorld.BOT_COUNT, BotProfile.STATS.length);
        for (int[] row : BotProfile.STATS) {
            assertEquals(BotProfile.COLUMNS, row.length);
        }
    }

    @Test
    void laterBotsAreStronger() {
        for (int i = 1; i < GameWorld.BOT_COUNT; i++) {
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
        assertEquals(2.5, apprentice.cps(), 1e-9);
        assertEquals(1.5, apprentice.reaction(), 1e-9);
        assertEquals(6.0, apprentice.castTime("bombarda maxima"), 1e-9);
    }

    @Test
    void rejectsRowWithWrongWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> BotProfile.fromRow("Broken", new int[] {1, 2, 3}, Color.RED));
    }
}
