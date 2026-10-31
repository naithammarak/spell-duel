package spellduel.typing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccuracyGraderTest {

    private final AccuracyGrader grader = new AccuracyGrader(0.25);

    @Test
    void parTimeIsLengthTimesKPlusOneSecond() {
        assertEquals(7 * 0.25 + 1, grader.parTime(7), 1e-9);
    }

    @Test
    void perfectWhenExactAndFast() {
        assertEquals(Tier.PERFECT, grader.grade(0, 7, 1.0));
        assertEquals(Tier.PERFECT, grader.grade(0, 7, grader.parTime(7))); // ตรงเส้นพอดียังนับ
    }

    @Test
    void greatWhenExactButSlow() {
        assertEquals(Tier.GREAT, grader.grade(0, 7, grader.parTime(7) + 0.01));
    }

    @Test
    void tiersFollowAccuracyCutoffs() {
        // 20 ตัวอักษร: ผิด 3 = 0.85, ผิด 5 = 0.75, ผิด 10 = 0.50
        assertEquals(Tier.GREAT, grader.grade(3, 20, 0));
        assertEquals(Tier.GOOD, grader.grade(4, 20, 0));
        assertEquals(Tier.GOOD, grader.grade(5, 20, 0));
        assertEquals(Tier.WEAK, grader.grade(6, 20, 0));
        assertEquals(Tier.WEAK, grader.grade(10, 20, 0));
        assertEquals(Tier.FIZZLE, grader.grade(11, 20, 0));
    }

    @Test
    void oneTypoInSevenLettersIsGreat() {
        // stupify → stupefy: accuracy 6/7 ≈ 0.857
        assertEquals(Tier.GREAT, grader.grade(1, 7, 0));
    }

    @Test
    void accuracyNeverNegative() {
        assertEquals(0, AccuracyGrader.accuracy(20, 7), 1e-9);
        assertEquals(0, AccuracyGrader.accuracy(0, 0), 1e-9);
    }
}
