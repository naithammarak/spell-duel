package spellduel.model;

/**
 * อ่านทางผู้เล่น: ส่วนใหญ่ออกการ์ดที่ชนะการ์ดรอบก่อนของผู้เล่น นอกนั้นสุ่ม
 * ผู้เล่นที่จับนิสัยนี้ได้ จะหลอกกลับได้
 */
class AurorBrain implements BotBrain {

    static final int COUNTER_CHANCE = 55;

    @Override
    public SpellRole decide(DuelView view) {
        if (view.lastPlayerRole() != null && view.rnd().nextInt(100) < COUNTER_CHANCE) {
            return view.lastPlayerRole().beatenBy();
        }
        SpellRole[] roles = SpellRole.values();
        return roles[view.rnd().nextInt(roles.length)];
    }
}
