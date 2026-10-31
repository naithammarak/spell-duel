package spellduel.model;

/** บอทตัวสอนเล่น: ส่วนใหญ่โจมตี นาน ๆ ทีถึงออกใบอื่น เพื่อให้ผู้เล่นได้ลองทุกการ์ด */
class ApprenticeBrain implements BotBrain {

    static final int ATTACK_CHANCE = 70;

    @Override
    public SpellRole decide(DuelView view) {
        if (view.rnd().nextInt(100) < ATTACK_CHANCE) {
            return SpellRole.ATTACK;
        }
        return view.rnd().nextBoolean() ? SpellRole.SHIELD : SpellRole.INTERRUPT;
    }
}
