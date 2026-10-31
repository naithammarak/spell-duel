package spellduel.model;

import spellduel.typing.Tier;

/**
 * โล่บนตัวนักเวท อยู่ได้ DURATION วินาที (เวลาในเกม) และกันการโจมตีได้ครั้งเดียว
 * ทุก tier สะท้อน Attack กลับใส่คนตี ยิ่ง tier สูง ลูกสะท้อนยิ่งแรง (ดู GameWorld.resolveHit)
 */
class Shield implements Updatable {

    static final double DURATION = 3.0;

    private final Tier tier;
    private double remaining = DURATION;

    Shield(Tier tier) {
        this.tier = tier;
    }

    Tier tier() {
        return tier;
    }

    @Override
    public void update(double dt) {
        remaining -= dt;
    }

    boolean isExpired() {
        return remaining <= 0;
    }

    /** เหลือเวลากี่ส่วน (1 = เพิ่งตั้ง, 0 = หมด) ใช้วาดให้จางลง */
    double fraction() {
        return Math.max(0, remaining / DURATION);
    }

    /** โล่ PERFECT วาดเป็นสีทอง */
    boolean isPerfect() {
        return tier == Tier.PERFECT;
    }
}
