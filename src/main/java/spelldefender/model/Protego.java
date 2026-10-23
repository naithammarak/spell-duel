package spelldefender.model;

import java.awt.Color;
import spelldefender.typing.Tier;

/** Self: โล่ป้องกันฐาน ดูดซับดาเมจ อยู่ได้ 8 วินาที */
class Protego extends Spell {

    static final double SHIELD_AMOUNT = 30;
    static final double SHIELD_SECONDS = 8;

    Protego() {
        super("protego", 0, 12.0, new Color(120, 200, 255), new SelfTargeting());
    }

    @Override
    void impact(GameWorld world, Enemy hit, Tier tier) {
        world.base().addShield(SHIELD_AMOUNT * tier.multiplier(), SHIELD_SECONDS);
    }
}
