package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class BotBrainTest {

    private static DuelView view(boolean playerShielded, SpellRole intent, Random rnd) {
        return new DuelView(100, 100, playerShielded, false, intent, rnd);
    }

    @Test
    void apprenticeOnlyAttacks() {
        BotBrain brain = new ApprenticeBrain();
        Random rnd = new Random(1);
        for (SpellRole intent : new SpellRole[] {null, SpellRole.ATTACK, SpellRole.SHIELD}) {
            assertEquals(SpellRole.ATTACK, brain.decide(view(true, intent, rnd)));
            assertEquals(SpellRole.ATTACK, brain.decide(view(false, intent, rnd)));
        }
    }

    @Test
    void aurorReadsThePlayer() {
        BotBrain brain = new AurorBrain();
        Random rnd = new Random(1);
        assertEquals(SpellRole.SHIELD, brain.decide(view(false, SpellRole.ATTACK, rnd)));
        assertEquals(SpellRole.INTERRUPT, brain.decide(view(true, null, rnd)));
        assertEquals(SpellRole.ATTACK, brain.decide(view(false, null, rnd)));
    }

    @Test
    void darkWizardDecidesLikeAuror() {
        BotBrain brain = new DarkWizardBrain();
        assertEquals(SpellRole.SHIELD, brain.decide(view(false, SpellRole.ATTACK, new Random(1))));
    }

    @Test
    void darkWizardNeverFeintsBeforeHalfway() {
        DarkWizardBrain brain = new DarkWizardBrain();
        Random rnd = new Random(3);
        for (int i = 0; i < 100; i++) {
            Cast cast = new Cast(SpellRole.ATTACK, "bombarda maxima", 2.0);
            cast.advance(0.9); // 45%
            assertNull(brain.duringCast(view(false, null, rnd), cast));
        }
    }

    @Test
    void darkWizardFeintsSometimesAndOnlyOncePerCast() {
        DarkWizardBrain brain = new DarkWizardBrain();
        Random rnd = new Random(42);
        int feints = 0;
        for (int i = 0; i < 300; i++) {
            Cast cast = new Cast(SpellRole.ATTACK, "bombarda maxima", 2.0);
            cast.advance(1.2);
            SpellRole change = brain.duringCast(view(false, null, rnd), cast);
            if (change != null) {
                feints++;
                assertNotEquals(SpellRole.ATTACK, change);
            }
            cast.advance(0.1);
            assertNull(brain.duringCast(view(false, null, rnd), cast), "สุ่มครั้งเดียวต่อ cast");
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
}
