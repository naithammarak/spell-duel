package spelldefender.model;

import spelldefender.typing.Tier;

/** ไม่มีกระสุน ผลเกิดทันที (เช่น Protego ใส่โล่ให้ฐาน) */
class SelfTargeting implements Targeting {

    @Override
    public boolean launch(Spell spell, Tier tier, GameWorld world) {
        spell.impact(world, null, tier);
        return true;
    }
}
