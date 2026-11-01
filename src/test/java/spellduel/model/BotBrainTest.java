package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class BotBrainTest {

    private static DuelView view(SpellRole last, Random rnd) {
        return new DuelView(100, 100, false, false, null, last, rnd);
    }

    private static Cast castAt(double progress) {
        Cast cast = new Cast(SpellRole.ATTACK, "bombarda maxima", 2.0);
        cast.advance(2.0 * progress);
        return cast;
    }

    private static int count(BotBrain brain, SpellRole last, SpellRole wanted, Random rnd) {
        int n = 0;
        for (int i = 0; i < 1000; i++) {
            if (brain.decide(view(last, rnd)) == wanted) {
                n++;
            }
        }
        return n;
    }

    @Test
    void cardsFormACycle() {
        assertEquals(SpellRole.SHIELD, SpellRole.ATTACK.beatenBy());
        assertEquals(SpellRole.INTERRUPT, SpellRole.SHIELD.beatenBy());
        assertEquals(SpellRole.ATTACK, SpellRole.INTERRUPT.beatenBy());
    }

    @Test
    void apprenticeMostlyAttacks() {
        int attacks = count(new ApprenticeBrain(), null, SpellRole.ATTACK, new Random(1));
        assertTrue(attacks > 650 && attacks < 750, "ประมาณ 70%: " + attacks);
    }

    @Test
    void aurorMostlyCountersLastCard() {
        int shields = count(new AurorBrain(), SpellRole.ATTACK, SpellRole.SHIELD, new Random(1));
        assertTrue(shields > 700 && shields < 850, "ประมาณ 70% + ส่วนที่สุ่มได้: " + shields);
    }

    @Test
    void aurorHasNothingToReadOnFirstRound() {
        int shields = count(new AurorBrain(), null, SpellRole.SHIELD, new Random(1));
        assertTrue(shields > 250 && shields < 420, "ประมาณ 1 ใน 3: " + shields);
    }

    @Test
    void roleIsRevealedAtTwentyPercent() {
        assertFalse(castAt(0.1).isRevealed());
        assertTrue(castAt(0.2).isRevealed());
    }

    @Test
    void darkWizardNeverFeintsBeforeRoleIsShown() {
        DarkWizardBrain brain = new DarkWizardBrain();
        Random rnd = new Random(3);
        for (int i = 0; i < 100; i++) {
            assertNull(brain.duringCast(view(null, rnd), castAt(0.7)));
        }
    }

    @Test
    void darkWizardFeintsSometimesAndOnlyOncePerCast() {
        DarkWizardBrain brain = new DarkWizardBrain();
        Random rnd = new Random(42);
        int feints = 0;
        for (int i = 0; i < 300; i++) {
            Cast cast = castAt(0.8);
            SpellRole change = brain.duringCast(view(null, rnd), cast);
            if (change != null) {
                feints++;
                assertNotEquals(SpellRole.ATTACK, change);
            }
            cast.advance(0.1);
            assertNull(brain.duringCast(view(null, rnd), cast), "สุ่มครั้งเดียวต่อ cast");
        }
        assertTrue(feints > 50 && feints < 130, "ประมาณ 30% ของ 300: " + feints);
    }

    @Test
    void feintKeepsRemainingTime() {
        BotWizard bot = new BotWizard(0, 0, BotProfile.row(2), new DarkWizardBrain());
        bot.startCast(SpellRole.ATTACK, "bombarda maxima");
        Cast cast = bot.cast();
        double duration = cast.duration();
        cast.advance(duration * 0.6);
        cast.switchTo(SpellRole.SHIELD, "protego maxima");
        assertEquals(SpellRole.SHIELD, cast.role());
        assertEquals(duration, cast.duration());
        assertEquals(0.6, cast.progress(), 1e-9);
        assertTrue(cast.isFeinted());
    }

    @Test
    void botAlwaysLocksBeforeRoundEnds() {
        BotWizard bot = new BotWizard(0, 0, BotProfile.row(0), new ApprenticeBrain());
        bot.startCast(SpellRole.ATTACK, "petrificus totalus");
        assertTrue(bot.cast().duration() < GameWorld.ROUND_TIME);
    }
}
