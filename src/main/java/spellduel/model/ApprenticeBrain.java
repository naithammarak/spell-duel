package spellduel.model;

/** บอทตัวสอนเล่น: โจมตีอย่างเดียว */
class ApprenticeBrain implements BotBrain {

    @Override
    public SpellRole decide(DuelView view) {
        return SpellRole.ATTACK;
    }
}
