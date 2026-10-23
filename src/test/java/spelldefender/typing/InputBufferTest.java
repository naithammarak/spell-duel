package spelldefender.typing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InputBufferTest {

    @Test
    void acceptsLettersAndSpaceOnly() {
        InputBuffer b = new InputBuffer();
        assertTrue(b.add('E', 0));
        assertTrue(b.add(' ', 0));
        assertFalse(b.add('1', 0));
        assertFalse(b.add('ก', 0));
        assertEquals("e ", b.text());
    }

    @Test
    void timerStartsAtFirstLetter() {
        InputBuffer b = new InputBuffer();
        b.add('a', 5.0);
        b.add('b', 6.0);
        assertEquals(2.5, b.elapsed(7.5), 1e-9);
    }

    @Test
    void backspaceOnEmptyDoesNothing() {
        InputBuffer b = new InputBuffer();
        b.backspace();
        assertTrue(b.isEmpty());
    }
}
