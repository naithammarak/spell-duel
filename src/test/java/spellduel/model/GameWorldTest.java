package spellduel.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import spellduel.typing.Tier;

/** กติกาแบบรอบ (DESIGN §5R): ตารางเป่ายิ้งฉุบทุกช่อง + วงจรของรอบ */
class GameWorldTest {

    private static final int BOT_HP = 60;
    private static final int PLAYER_HP = PlayerWizard.MAX_HP;

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

    private static String card(GameWorld w, SpellRole role) {
        return w.deck().hand()[role.ordinal()].text();
    }

    /** เปิดไพ่ด้วย Great ทั้งคู่ แล้วรอให้ลูกเวท (รวมลูกสะท้อน) ถึงเป้า */
    private static GameWorld duel(SpellRole player, SpellRole bot) {
        return duel(player, Tier.GREAT, bot, Tier.GREAT);
    }

    private static GameWorld duel(SpellRole player, Tier playerTier, SpellRole bot, Tier botTier) {
        GameWorld w = apprenticeWorld();
        w.resolveRound(player, playerTier, bot, botTier);
        run(w, 1.5);
        return w;
    }

    private static void assertHp(GameWorld w, int playerHp, int botHp) {
        assertEquals(playerHp, w.player().hp(), "HP ผู้เล่น");
        assertEquals(botHp, w.bot().hp(), "HP บอท");
    }

    @Test
    void attackBeatsInterrupt() {
        assertHp(duel(SpellRole.ATTACK, SpellRole.INTERRUPT), PLAYER_HP, BOT_HP - 20);
        assertHp(duel(SpellRole.INTERRUPT, SpellRole.ATTACK), PLAYER_HP - 20, BOT_HP);
    }

    @Test
    void shieldBeatsAttackByReflecting() {
        GameWorld w = duel(SpellRole.ATTACK, SpellRole.SHIELD);
        assertHp(w, PLAYER_HP - 20, BOT_HP);
        assertFalse(w.bot().hasShield(), "โล่ใช้ได้ครั้งเดียว");
        assertHp(duel(SpellRole.SHIELD, SpellRole.ATTACK), PLAYER_HP, BOT_HP - 20);
    }

    @Test
    void interruptBeatsShield() {
        GameWorld w = duel(SpellRole.INTERRUPT, SpellRole.SHIELD);
        assertHp(w, PLAYER_HP, BOT_HP - 20);
        assertFalse(w.bot().hasShield());
        assertHp(duel(SpellRole.SHIELD, SpellRole.INTERRUPT), PLAYER_HP - 20, BOT_HP);
    }

    @Test
    void attackVsAttackHitsBoth() {
        assertHp(duel(SpellRole.ATTACK, SpellRole.ATTACK), PLAYER_HP - 20, BOT_HP - 20);
    }

    @Test
    void shieldVsShieldDoesNothing() {
        assertHp(duel(SpellRole.SHIELD, SpellRole.SHIELD), PLAYER_HP, BOT_HP);
    }

    @Test
    void interruptVsInterruptIsLightOnBoth() {
        assertHp(duel(SpellRole.INTERRUPT, SpellRole.INTERRUPT), PLAYER_HP - 5, BOT_HP - 5);
    }

    @Test
    void reflectStrengthFollowsShieldTier() {
        assertHp(duel(SpellRole.SHIELD, Tier.PERFECT, SpellRole.ATTACK, Tier.GREAT), PLAYER_HP, BOT_HP - 25);
        assertHp(duel(SpellRole.SHIELD, Tier.WEAK, SpellRole.ATTACK, Tier.GREAT), PLAYER_HP, BOT_HP - 6);
    }

    @Test
    void noLockMeansYouJustGetHit() {
        assertHp(duel(null, null, SpellRole.INTERRUPT, Tier.GREAT), PLAYER_HP - 20, BOT_HP);
    }

    @Test
    void fizzleKeepsCardAndAllowsRetry() {
        GameWorld w = apprenticeWorld();
        Incantation[] before = w.deck().hand();
        type(w, "zzzzzzzzzzzzzzz");
        assertEquals(Tier.FIZZLE, w.castTyped().tier());
        assertArrayEquals(before, w.deck().hand());
        assertNull(w.lockedRole());
        type(w, card(w, SpellRole.SHIELD));
        w.castTyped();
        assertEquals(SpellRole.SHIELD, w.lockedRole());
    }

    @Test
    void lessThanHalfAWordFizzles() {
        GameWorld w = apprenticeWorld();
        String word = card(w, SpellRole.ATTACK);
        type(w, word.substring(0, word.length() / 2 - 1));
        assertEquals(Tier.FIZZLE, w.castTyped().tier());
    }

    @Test
    void castLocksCardAndGivesNewWord() {
        GameWorld w = apprenticeWorld();
        String attack = card(w, SpellRole.ATTACK);
        type(w, attack);
        assertEquals(Tier.PERFECT, w.castTyped().tier());
        assertEquals(SpellRole.ATTACK, w.lockedRole());
        assertNotEquals(attack, card(w, SpellRole.ATTACK));
        assertEquals("", w.typed(), "Enter แล้วช่องพิมพ์ว่าง");
    }

    @Test
    void cannotTypeAfterLocking() {
        GameWorld w = apprenticeWorld();
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        type(w, "abc");
        assertEquals("", w.typed());
    }

    @Test
    void roundEndsEarlyWhenBothLocked() {
        GameWorld w = apprenticeWorld();
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        run(w, w.bot().cast().duration() + 0.1);
        assertEquals(GameWorld.Phase.REVEAL, w.phase());
    }

    @Test
    void roundEndsShortlyAfterBotLocks() {
        GameWorld w = apprenticeWorld();
        run(w, w.bot().cast().duration() + 0.1);
        assertEquals(GameWorld.Phase.TYPING, w.phase(), "บอทล็อกแล้ว แต่ยังเหลือเวลาอีกนิด");
        run(w, GameWorld.GRACE_TIME);
        assertEquals(GameWorld.Phase.REVEAL, w.phase());
    }

    /** บอทที่ออกการ์ดใบเดียวตลอด ใช้จัดฉากทั้งเส้นทางจริงตั้งแต่พิมพ์จนเปิดไพ่ */
    private static GameWorld worldAgainst(SpellRole botRole) {
        return new GameWorld(BotProfile.row(0), view -> botRole, SpellDeck.standard(new Random(1)));
    }

    private static void runUntilReveal(GameWorld w) {
        while (w.phase() == GameWorld.Phase.TYPING) {
            w.update(1.0 / 60);
        }
    }

    @Test
    void lockingBeforeBotRevealsHitsHarder() {
        GameWorld w = worldAgainst(SpellRole.INTERRUPT);
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        assertTrue(w.lockedEarly());
        runUntilReveal(w);
        run(w, 1.0);
        assertEquals(BOT_HP - 38, w.bot().hp(), "20 × 1.25 (PERFECT) × 1.5");
    }

    @Test
    void lockingAfterBotRevealsGetsNoBonus() {
        GameWorld w = worldAgainst(SpellRole.INTERRUPT);
        run(w, w.bot().cast().duration() * 0.6);
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        assertFalse(w.lockedEarly());
        runUntilReveal(w);
        run(w, 1.0);
        assertEquals(BOT_HP - 25, w.bot().hp());
    }

    @Test
    void earlyShieldReflectsHarder() {
        GameWorld w = worldAgainst(SpellRole.ATTACK);
        type(w, card(w, SpellRole.SHIELD));
        w.castTyped();
        runUntilReveal(w);
        assertEquals(GameWorld.EARLY_BONUS, w.player().shield().bonus());
    }

    @Test
    void timeoutSubmitsWhatWasTyped() {
        GameWorld w = apprenticeWorld();
        type(w, card(w, SpellRole.INTERRUPT));
        run(w, GameWorld.ROUND_TIME + 0.1);
        assertEquals(GameWorld.Phase.REVEAL, w.phase());
        assertEquals(SpellRole.INTERRUPT, w.lockedRole());
    }

    @Test
    void nextRoundStartsCleanAfterReveal() {
        GameWorld w = apprenticeWorld();
        w.resolveRound(SpellRole.SHIELD, Tier.GREAT, SpellRole.SHIELD, Tier.GREAT);
        assertTrue(w.player().hasShield());
        run(w, GameWorld.REVEAL_TIME + 0.1);
        assertEquals(GameWorld.Phase.TYPING, w.phase());
        assertEquals(2, w.round());
        assertFalse(w.player().hasShield(), "โล่อยู่ได้รอบเดียว");
        assertTrue(w.bot().isCasting());
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
            w.resolveRound(SpellRole.ATTACK, Tier.PERFECT, SpellRole.INTERRUPT, Tier.GREAT);
            run(w, 2.0);
        }
        assertEquals(GameWorld.Status.WON, w.status());
        assertTrue(w.stats().won());
    }

    @Test
    void lostWhenPlayerHpZero() {
        GameWorld w = apprenticeWorld();
        for (int i = 0; i < 4; i++) { // 25 × 4 = 100
            w.resolveRound(null, null, SpellRole.ATTACK, Tier.PERFECT);
            run(w, 2.0);
        }
        assertEquals(GameWorld.Status.LOST, w.status());
        assertFalse(w.stats().won());
    }
}
