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

/** กติการ่ายทันที (DESIGN §5I): ผลตอนลูกเวทถึงเป้า + วงจรของรอบ */
class GameWorldTest {

    private static final int BOT_HP = BotProfile.row(0).hp();
    private static final int PLAYER_HP = PlayerWizard.MAX_HP;

    private static GameWorld apprenticeWorld() {
        return started(new GameWorld(BotProfile.row(0), new ApprenticeBrain(), SpellDeck.standard(new Random(1))));
    }

    /** ข้ามการนับถอยหลังต้นรอบ */
    private static GameWorld started(GameWorld w) {
        run(w, GameWorld.COUNTDOWN_TIME + 1.0 / 60);
        return w;
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

    /** ร่ายด้วย Great ทั้งคู่ แล้วรอให้ลูกเวท (รวมลูกสะท้อน) ถึงเป้า */
    private static GameWorld duel(SpellRole player, SpellRole bot) {
        return duel(player, Tier.GREAT, bot, Tier.GREAT);
    }

    private static GameWorld duel(SpellRole player, Tier playerTier, SpellRole bot, Tier botTier) {
        GameWorld w = apprenticeWorld();
        if (player != null) {
            w.release(w.player(), w.bot(), player, playerTier);
        }
        w.release(w.bot(), w.player(), bot, botTier);
        run(w, 1.5);
        return w;
    }

    private static void assertHp(GameWorld w, int playerHp, int botHp) {
        assertEquals(playerHp, w.player().hp(), "HP ผู้เล่น");
        assertEquals(botHp, w.bot().hp(), "HP บอท");
    }

    @Test
    void attackBeatsInterrupt() {
        assertHp(duel(SpellRole.ATTACK, SpellRole.INTERRUPT), PLAYER_HP - 5, BOT_HP - 20);
        assertHp(duel(SpellRole.INTERRUPT, SpellRole.ATTACK), PLAYER_HP - 20, BOT_HP - 5);
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
        assertHp(duel(null, null, SpellRole.ATTACK, Tier.GREAT), PLAYER_HP - 20, BOT_HP);
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
        type(w, card(w, SpellRole.SHIELD)); // ใช้ Shield จะได้ไม่ขัดบอท
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

    /** บอทที่ออกการ์ดใบเดียวตลอด ใช้จัดฉากทั้งเส้นทางจริงตั้งแต่พิมพ์จนลูกเวทถึงเป้า */
    private static GameWorld worldAgainst(SpellRole botRole) {
        return started(new GameWorld(BotProfile.row(0), view -> botRole, SpellDeck.standard(new Random(1))));
    }

    @Test
    void countdownBlocksTypingAndBotUntilItEnds() {
        GameWorld w = new GameWorld(BotProfile.row(0), new ApprenticeBrain(), SpellDeck.standard(new Random(1)));
        assertEquals(GameWorld.Phase.COUNTDOWN, w.phase());
        type(w, "abc");
        assertEquals("", w.typed());
        run(w, GameWorld.COUNTDOWN_TIME - 0.1);
        assertEquals(0, w.bot().cast().progress());
        run(w, 0.2);
        assertEquals(GameWorld.Phase.TYPING, w.phase());
        assertTrue(w.bot().cast().progress() > 0);
    }

    @Test
    void castWorksTheMomentEnterIsPressed() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        assertEquals(1, w.projectileCount());
    }

    @Test
    void onlyOneCastPerRound() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        type(w, card(w, SpellRole.INTERRUPT));
        assertNull(w.castTyped());
        assertEquals(1, w.projectileCount());
    }

    @Test
    void attackLandingBeforeShieldIsUpHitsFully() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        run(w, 1.0);
        assertEquals(BOT_HP - 25, w.bot().hp(), "20 × 1.25 (PERFECT)");
    }

    @Test
    void attackAfterShieldIsUpGetsReflected() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        run(w, w.bot().cast().duration() + 0.1);
        assertTrue(w.bot().hasShield());
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        run(w, 1.2); // บินไปกลับ
        assertTrue(w.player().hp() < PLAYER_HP);
    }

    @Test
    void accurateAttackCutsBotCast() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        w.release(w.player(), w.bot(), SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.0);
        assertFalse(w.bot().isCasting());
        run(w, w.bot().profile().reaction() + 6);
        assertFalse(w.bot().hasShield(), "ถูกขัดแล้ว รอบนี้ไม่ได้ร่าย");
    }

    @Test
    void sloppyAttackDoesNotCut() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        w.release(w.player(), w.bot(), SpellRole.ATTACK, Tier.GOOD);
        run(w, 1.0);
        assertTrue(w.bot().isCasting());
    }

    @Test
    void botAttackClearsTypingButPlayerCanRetype() {
        GameWorld w = worldAgainst(SpellRole.SHIELD);
        type(w, "bom");
        w.release(w.bot(), w.player(), SpellRole.ATTACK, Tier.GREAT);
        run(w, 1.0);
        assertEquals("", w.typed());
        type(w, card(w, SpellRole.ATTACK));
        w.castTyped();
        assertEquals(SpellRole.ATTACK, w.lockedRole());
    }

    @Test
    void shieldLastsUntilRoundEnds() {
        GameWorld w = worldAgainst(SpellRole.INTERRUPT);
        w.release(w.player(), w.bot(), SpellRole.SHIELD, Tier.GREAT);
        run(w, w.bot().cast().duration() - 0.1);
        assertTrue(w.player().hasShield());
    }

    @Test
    void timeoutSubmitsWhatWasTyped() {
        GameWorld w = worldAgainst(SpellRole.SHIELD); // ใช้ Shield จะได้ไม่ขัดข้อความที่ค้างอยู่
        type(w, card(w, SpellRole.INTERRUPT));
        while (w.phase() == GameWorld.Phase.TYPING) {
            w.update(1.0 / 60);
        }
        assertEquals(SpellRole.INTERRUPT, w.lockedRole());
    }

    @Test
    void nextRoundStartsCleanAfterReveal() {
        GameWorld w = apprenticeWorld();
        w.release(w.player(), w.bot(), SpellRole.SHIELD, Tier.GREAT);
        assertTrue(w.player().hasShield());
        run(w, w.bot().cast().duration() + GameWorld.GRACE_TIME + GameWorld.REVEAL_TIME
                + GameWorld.COUNTDOWN_TIME + 0.2);
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
    void playerHpCarriesOverAndHealsAfterWin() {
        GameWorld w = new GameWorld(0, 10, 1L);
        assertEquals(10, w.player().hp());
        assertEquals(10 + GameStats.HEAL_PER_WIN, w.stats().nextHp());
        assertEquals(GameWorld.PLAYER_MAX_HP, new GameWorld(0, 90, 1L).stats().nextHp());
    }

    @Test
    void levelsClearedCountsOnlyWins() {
        GameWorld w = new GameWorld(4, 100, 1L);
        assertEquals(4, w.stats().levelsCleared());
        assertEquals(new GameWorld(4, 100, 1L).bot().profile(), BotProfile.forLevel(4));
    }

    @Test
    void wonWhenBotHpZero() {
        GameWorld w = apprenticeWorld();
        for (int i = 0; i < 4; i++) { // 25 × 4 ≥ 80
            w.release(w.player(), w.bot(), SpellRole.ATTACK, Tier.PERFECT);
            run(w, 1.0);
        }
        assertEquals(GameWorld.Status.WON, w.status());
        assertTrue(w.stats().won());
    }

    @Test
    void lostWhenPlayerHpZero() {
        GameWorld w = apprenticeWorld();
        for (int i = 0; i < 4; i++) { // 25 × 4 = 100
            w.release(w.bot(), w.player(), SpellRole.ATTACK, Tier.PERFECT);
            run(w, 1.0);
        }
        assertEquals(GameWorld.Status.LOST, w.status());
        assertFalse(w.stats().won());
    }
}
