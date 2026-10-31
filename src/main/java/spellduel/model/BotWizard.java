package spellduel.model;

import java.util.Random;
import spellduel.typing.Tier;

/**
 * บอท (ขวาจอ) ร่ายผ่าน cast bar: ว่าง → พัก reaction วินาที → ถามสมองว่าร่ายอะไร → ร่ายจนเต็มแถบ
 * ค่าตัวเลขมาจาก BotProfile ส่วนนิสัยมาจาก BotBrain
 */
class BotWizard extends Duelist {

    /** เวลาพักเพิ่มตอนเริ่มดวล ให้ผู้เล่นได้อ่านการ์ดก่อน */
    static final double START_DELAY = 1.5;

    private final BotProfile profile;
    private final BotBrain brain;
    private Cast cast;
    private double idle = -START_DELAY;

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
        cast = new Cast(role, word, profile.castTime(word));
    }

    /**
     * เรียกทุก tick จาก GameWorld
     * @return Cast ที่ร่ายเสร็จใน tick นี้ (GameWorld จะปล่อยคาถา) หรือ null
     */
    Cast think(double dt, DuelView view, Random rnd) {
        if (cast == null) {
            idle += dt;
            if (idle >= profile.reaction()) {
                SpellRole role = brain.decide(view);
                startCast(role, SpellDeck.randomWord(role, rnd));
            }
            return null;
        }
        SpellRole change = brain.duringCast(view, cast);
        if (change != null && change != cast.role()) {
            cast.switchTo(change, SpellDeck.randomWord(change, rnd));
        }
        cast.advance(dt);
        if (cast.isDone()) {
            Cast done = cast;
            cast = null;
            idle = 0;
            return done;
        }
        return null;
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

    /** โดนตัด: cast bar หาย แล้วต้องพักใหม่ */
    @Override
    void interruptCast() {
        cast = null;
        idle = 0;
    }

    @Override
    boolean isCasting() {
        return cast != null;
    }

    @Override
    SpellRole castingRole() {
        return cast == null ? null : cast.role();
    }
}
