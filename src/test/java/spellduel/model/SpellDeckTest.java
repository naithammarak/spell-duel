package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import spellduel.typing.InputBuffer;

class SpellDeckTest {

    private final SpellDeck deck = SpellDeck.standard(new Random(7));

    @Test
    void handHasOneCardPerRole() {
        Incantation[] hand = deck.hand();
        assertEquals(SpellRole.values().length, hand.length);
        for (SpellRole role : SpellRole.values()) {
            assertEquals(role, hand[role.ordinal()].role());
        }
    }

    @Test
    void matchFindsClosestCard() {
        Incantation shield = deck.hand()[SpellRole.SHIELD.ordinal()];
        String typo = "x" + shield.text().substring(1); // ผิดตัวแรก 1 ตัว
        SpellDeck.Match m = deck.match(typo);
        assertEquals(shield, m.card());
        assertEquals(1, m.distance());
    }

    @Test
    void consumeGivesNewWordOfSameRole() {
        for (int i = 0; i < 50; i++) {
            Incantation old = deck.hand()[SpellRole.INTERRUPT.ordinal()];
            deck.consume(old);
            Incantation next = deck.hand()[SpellRole.INTERRUPT.ordinal()];
            assertNotEquals(old.text(), next.text());
            assertEquals(SpellRole.INTERRUPT, next.role());
        }
    }

    @Test
    void poolsHaveARowPerRoleAndEnoughWords() {
        assertEquals(SpellRole.values().length, SpellDeck.POOLS.length);
        for (String[] row : SpellDeck.POOLS) {
            assertTrue(row.length >= 3);
            for (String word : row) {
                assertTrue(word.length() <= InputBuffer.MAX_LENGTH, word);
            }
        }
    }

    @Test
    void guessRoleNeedsThreeChars() {
        String attack = deck.hand()[SpellRole.ATTACK.ordinal()].text();
        assertNull(deck.guessRole(""));
        assertNull(deck.guessRole(attack.substring(0, 2)));
        assertEquals(SpellRole.ATTACK, deck.guessRole(attack.substring(0, 3)));
    }

    @Test
    void guessRoleFollowsWhatIsTyped() {
        for (Incantation card : deck.hand()) {
            assertEquals(card.role(), deck.guessRole(card.text().substring(0, 5)), card.text());
        }
    }
}
