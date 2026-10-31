package spellduel.model;

/**
 * อ่านทางผู้เล่น: เห็นผู้เล่นพิมพ์ Attack → ตั้งโล่ · ผู้เล่นมีโล่ → Interrupt · นอกนั้นโจมตี
 */
class AurorBrain implements BotBrain {

    @Override
    public SpellRole decide(DuelView view) {
        if (view.playerIntent() == SpellRole.ATTACK && !view.botShielded()) {
            return SpellRole.SHIELD;
        }
        if (view.playerShielded()) {
            return SpellRole.INTERRUPT;
        }
        return SpellRole.ATTACK;
    }
}
