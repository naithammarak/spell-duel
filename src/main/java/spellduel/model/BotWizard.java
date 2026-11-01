package spellduel.model;

import java.util.Random;
import spellduel.typing.Tier;

/**
 * บอท (ขวาจอ) ต้นรอบถามสมองว่าจะออกอะไร แล้วร่ายจนเต็มแถบ = คาถาทำงานทันที
 * ค่าตัวเลขมาจาก BotProfile ส่วนนิสัยมาจาก BotBrain
 */
class BotWizard extends Duelist {

    /** บอทต้องล็อกก่อนหมดรอบเสมอ คำยาวของบอทช้าจึงถูกจำกัดเวลาไว้ */
    static final double MAX_CAST = GameWorld.ROUND_TIME - 0.3;
    /** โล่ร่ายเร็วกว่าใบอื่น ไม่งั้น Attack ของคนพิมพ์เร็วจะถึงก่อนโล่ขึ้นทุกรอบ */
    static final double SHIELD_SPEED = 0.6;

    private final BotProfile profile;
    private final BotBrain brain;
    private Cast cast;

    BotWizard(double x, double y, BotProfile profile, BotBrain brain) {
        super(profile.name(), profile.hp(), x, y, -1, profile.robe());
        this.profile = profile;
        this.brain = brain;
    }

    BotProfile profile() {
        return profile;
    }

    /** cast bar ปัจจุบัน หรือ null ถ้าว่างอยู่ */
    Cast cast() {
        return cast;
    }

    void startCast(SpellRole role, String word) {
        double typing = profile.castTime(word) * (role == SpellRole.SHIELD ? SHIELD_SPEED : 1);
        double duration = Math.min(profile.reaction() + typing, MAX_CAST);
        cast = new Cast(role, word, duration);
    }

    /** ต้นรอบ: ถามสมองแล้วเริ่มร่าย */
    void startRound(DuelView view, Random rnd) {
        SpellRole role = brain.decide(view);
        startCast(role, SpellDeck.randomWord(role, rnd));
    }

    /** เรียกทุก tick ระหว่างรอบพิมพ์ */
    void think(double dt, DuelView view, Random rnd) {
        if (cast == null || cast.isDone()) {
            return;
        }
        SpellRole change = brain.duringCast(view, cast);
        if (change != null && change != cast.role()) {
            cast.switchTo(change, SpellDeck.randomWord(change, rnd));
        }
        cast.advance(dt);
    }

    /** ร่ายเต็มแถบแล้ว */
    boolean isLocked() {
        return cast != null && cast.isDone();
    }

    /**
     * สุ่ม Tier จากความแม่นยำ: ติด accuracy% → GREAT (1 ใน 4 เป็น PERFECT)
     * พลาด → GOOD หรือ WEAK อย่างละครึ่ง
     */
    Tier rollTier(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < profile.accuracy()) {
            return r < profile.accuracy() / 4 ? Tier.PERFECT : Tier.GREAT;
        }
        return rnd.nextBoolean() ? Tier.GOOD : Tier.WEAK;
    }

    /** จบรอบ: cast bar หาย */
    @Override
    void interruptCast() {
        cast = null;
    }

    @Override
    boolean isCasting() {
        return cast != null;
    }
}
