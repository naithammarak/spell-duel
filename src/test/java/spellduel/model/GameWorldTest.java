package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import spellduel.typing.Tier;

/** กติกาใน DESIGN §5.3 ทุกแถว (เวลาเดินด้วย update(dt) เอง ไม่มี thread) */
class GameWorldTest {

    private static GameWorld apprenticeWorld() {
        return new GameWorld(BotProfile.row(0), new ApprenticeBrain(), SpellDeck.standard(new Random(1)));
    }

    /** เดินเวลาในเกมไป seconds วินาที ทีละ 1/60 */
    private static void run(GameWorld w, double seconds) {
        for (double t = 0; t < seconds; t += 1.0 / 60) {
            w.update(1.0 / 60);
        }
    }

    private static void type(GameWorld w, String text) {
        for (char c : text.toCharArray()) {
            w.typeChar(c);
        }
    }

    @Test
    void attackLowersHpAndCancelsBotCast() {
        GameWorld w = apprenticeWorld();
        w.bot().startCast(SpellRole.ATTACK, "bombarda maxima");
        w.playerCasts(SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.0);
        assertEquals(60 - 20, w.bot().hp());
        assertNull(w.bot().cast());
    }

    @Test
    void attackOnShieldReflectsBackToCaster() {
        GameWorld w = apprenticeWorld();
        w.botCasts(SpellRole.SHIELD, Tier.GREAT);
        w.playerCasts(SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.5);
        assertEquals(60, w.bot().hp());
        assertEquals(PlayerWizard.MAX_HP - 20, w.player().hp());
        assertFalse(w.bot().hasShield(), "โล่ใช้ได้ครั้งเดียว");
    }

    @Test
    void perfectShieldReflectsStronger() {
        GameWorld w = apprenticeWorld();
        w.playerCasts(SpellRole.SHIELD, Tier.PERFECT);
        w.botCasts(SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.5);
        assertEquals(PlayerWizard.MAX_HP, w.player().hp());
        assertEquals(60 - 25, w.bot().hp());
    }

    @Test
    void weakShieldStillReflectsButWeakly() {
        GameWorld w = apprenticeWorld();
        w.playerCasts(SpellRole.SHIELD, Tier.WEAK);
        w.botCasts(SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.5);
        assertEquals(PlayerWizard.MAX_HP, w.player().hp());
        assertEquals(60 - 6, w.bot().hp());
    }

    @Test
    void interruptBreaksShield() {
        GameWorld w = apprenticeWorld();
        w.botCasts(SpellRole.SHIELD, Tier.PERFECT);
        w.playerCasts(SpellRole.INTERRUPT, Tier.GREAT);
        run(w, 1.0);
        assertFalse(w.bot().hasShield());
        assertEquals(60 - 5, w.bot().hp());
    }

    @Test
    void weakAttackDamagesButDoesNotCut() {
        GameWorld w = apprenticeWorld();
        w.bot().startCast(SpellRole.ATTACK, "bombarda maxima");
        w.playerCasts(SpellRole.ATTACK, Tier.WEAK);
        run(w, 1.0);
        assertEquals(60 - 6, w.bot().hp());
        assertNotNull(w.bot().cast(), "พิมพ์ครึ่งคำ ตัดไม่ได้");
    }

    @Test
    void interruptCannotCutAttackCast() {
        GameWorld w = apprenticeWorld();
        w.bot().startCast(SpellRole.ATTACK, "bombarda maxima");
        w.playerCasts(SpellRole.INTERRUPT, Tier.PERFECT);
        run(w, 1.0);
        assertEquals(SpellRole.ATTACK, w.bot().cast().role());
        assertEquals(60 - 6, w.bot().hp());
    }

    @Test
    void interruptCutsShieldCast() {
        GameWorld w = apprenticeWorld();
        w.bot().startCast(SpellRole.SHIELD, "protego maxima");
        w.playerCasts(SpellRole.INTERRUPT, Tier.GREAT);
        run(w, 1.0);
        assertNull(w.bot().cast());
    }

    @Test
    void weakInterruptStillBreaksShieldButCannotCut() {
        GameWorld w = apprenticeWorld();
        w.botCasts(SpellRole.SHIELD, Tier.GREAT);
        w.bot().startCast(SpellRole.SHIELD, "protego maxima");
        w.playerCasts(SpellRole.INTERRUPT, Tier.WEAK);
        run(w, 1.0);
        assertFalse(w.bot().hasShield());
        assertNotNull(w.bot().cast());
    }

    @Test
    void botInterruptOnlyClearsPlayerTypingShield() {
        GameWorld w = apprenticeWorld();
        String attack = w.deck().hand()[SpellRole.ATTACK.ordinal()].text().substring(0, 4);
        type(w, attack);
        w.botCasts(SpellRole.INTERRUPT, Tier.GREAT);
        run(w, 1.0);
        assertEquals(attack, w.typed(), "กำลังพิมพ์ Attack อยู่ Interrupt ตัดไม่ได้");

        GameWorld w2 = apprenticeWorld();
        String shield = w2.deck().hand()[SpellRole.SHIELD.ordinal()].text().substring(0, 4);
        type(w2, shield);
        w2.botCasts(SpellRole.INTERRUPT, Tier.GREAT);
        run(w2, 1.0);
        assertEquals("", w2.typed());
    }

    @Test
    void shieldExpiresAfterDuration() {
        GameWorld w = apprenticeWorld();
        w.playerCasts(SpellRole.SHIELD, Tier.GREAT);
        run(w, Shield.DURATION - 0.1);
        assertTrue(w.player().hasShield());
        run(w, 0.2);
        assertFalse(w.player().hasShield());
    }

    @Test
    void fizzleKeepsCard() {
        GameWorld w = apprenticeWorld();
        Incantation[] before = w.deck().hand();
        type(w, "zzzzzzzzzzzzzzz");
        assertEquals(Tier.FIZZLE, w.castTyped().tier());
        assertArrayEquals(before, w.deck().hand());
        assertEquals(0, w.projectileCount());
    }

    @Test
    void castConsumesCardAndGivesNewWord() {
        GameWorld w = apprenticeWorld();
        Incantation attack = w.deck().hand()[SpellRole.ATTACK.ordinal()];
        type(w, attack.text());
        assertEquals(Tier.PERFECT, w.castTyped().tier());
        Incantation next = w.deck().hand()[SpellRole.ATTACK.ordinal()];
        assertNotEquals(attack.text(), next.text());
        assertEquals(SpellRole.ATTACK, next.role());
        assertEquals(1, w.projectileCount());
        assertEquals("", w.typed(), "Enter แล้วช่องพิมพ์ว่าง");
    }

    @Test
    void typoMakesSpellWeaker() {
        GameWorld w = apprenticeWorld();
        String word = w.deck().hand()[SpellRole.ATTACK.ordinal()].text();
        type(w, word.substring(0, word.length() - 1)); // ขาดตัวสุดท้าย
        assertEquals(Tier.GREAT, w.castTyped().tier());
    }

    @Test
    void lessThanHalfAWordFizzles() {
        GameWorld w = apprenticeWorld();
        Incantation[] before = w.deck().hand();
        String word = before[SpellRole.ATTACK.ordinal()].text();
        type(w, word.substring(0, word.length() / 2 - 1));
        assertEquals(Tier.FIZZLE, w.castTyped().tier());
        assertArrayEquals(before, w.deck().hand());
    }

    @Test
    void getHitClearsTypedText() {
        GameWorld w = apprenticeWorld();
        type(w, "bom");
        w.botCasts(SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.0);
        assertEquals("", w.typed());
        assertEquals(PlayerWizard.MAX_HP - 20, w.player().hp());
    }

    @Test
    void thaiKeysAreIgnored() {
        GameWorld w = apprenticeWorld();
        w.typeChar('ก');
        assertEquals("", w.typed());
    }

    @Test
    void wonWhenBotHpZero() {
        GameWorld w = apprenticeWorld();
        for (int i = 0; i < 3; i++) { // 25 × 3 ≥ 60
            w.playerCasts(SpellRole.ATTACK, Tier.PERFECT);
            run(w, 1.0);
        }
        assertEquals(GameWorld.Status.WON, w.status());
        assertTrue(w.stats().won());
    }

    @Test
    void lostWhenPlayerHpZero() {
        GameWorld w = apprenticeWorld();
        for (int i = 0; i < 4; i++) { // 25 × 4 = 100
            w.botCasts(SpellRole.ATTACK, Tier.PERFECT);
            run(w, 1.0);
        }
        assertEquals(GameWorld.Status.LOST, w.status());
        assertFalse(w.stats().won());
    }

    @Test
    void aurorShieldsWhenPlayerStartsTypingAttack() {
        GameWorld w = new GameWorld(BotProfile.row(1), new AurorBrain(), SpellDeck.standard(new Random(1)));
        String attack = w.deck().hand()[SpellRole.ATTACK.ordinal()].text();
        type(w, attack.substring(0, 3));
        run(w, BotWizard.START_DELAY + BotProfile.row(1).reaction() + 0.1);
        assertEquals(SpellRole.SHIELD, w.bot().cast().role());
    }

    @Test
    void botCastFinishesAndHitsPlayer() {
        GameWorld w = apprenticeWorld();
        BotProfile p = BotProfile.row(0);
        // พักตอนเริ่ม + reaction + ร่ายคำที่ยาวที่สุด + projectile บิน
        run(w, BotWizard.START_DELAY + p.reaction() + 18 / p.cps() + 1.0);
        assertTrue(w.player().hp() < PlayerWizard.MAX_HP);
    }
}
