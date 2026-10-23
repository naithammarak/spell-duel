package spelldefender.typing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LevenshteinTest {

    @Test
    void identicalStringsHaveDistanceZero() {
        assertEquals(0, Levenshtein.distance("stupefy", "stupefy"));
    }

    @Test
    void swappedLettersCostTwo() {
        assertEquals(2, Levenshtein.distance("incendio", "incendoi"));
    }

    @Test
    void oneWrongLetterCostsOne() {
        assertEquals(1, Levenshtein.distance("stupify", "stupefy"));
    }

    @Test
    void missingLetterCostsOne() {
        assertEquals(1, Levenshtein.distance("stupfy", "stupefy"));
    }

    @Test
    void emptyStringCostsFullLength() {
        assertEquals(7, Levenshtein.distance("", "protego"));
        assertEquals(7, Levenshtein.distance("protego", ""));
        assertEquals(0, Levenshtein.distance("", ""));
    }

    @Test
    void classicExample() {
        assertEquals(3, Levenshtein.distance("kitten", "sitting"));
    }
}
