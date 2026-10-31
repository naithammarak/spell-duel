package spellduel.model;

/**
 * บอส: ตัดสินใจเหมือน Auror (สืบทอดมา) แต่ชอบ "หลอก"
 * พอร่ายเกินครึ่งทาง มีโอกาส 30% (สุ่มครั้งเดียวต่อ cast) ที่จะเปลี่ยนเป็นหน้าที่อื่น
 */
class DarkWizardBrain extends AurorBrain {

    static final int FEINT_CHANCE = 30;

    private Cast lastRolled; // cast ที่สุ่มไปแล้ว จะได้ไม่สุ่มซ้ำ

    @Override
    public SpellRole duringCast(DuelView view, Cast cast) {
        if (cast == lastRolled || cast.progress() < 0.5) {
            return null;
        }
        lastRolled = cast;
        if (view.rnd().nextInt(100) >= FEINT_CHANCE) {
            return null;
        }
        // เลือกหน้าที่อื่นที่ไม่ใช่หน้าที่เดิม
        SpellRole[] roles = SpellRole.values();
        int shift = 1 + view.rnd().nextInt(roles.length - 1);
        return roles[(cast.role().ordinal() + shift) % roles.length];
    }
}
