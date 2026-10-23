package spelldefender.model;

import java.awt.Color;
import spelldefender.typing.Tier;

/** Lock-on: ดาเมจกลาง + ผลักศัตรูถอยหลัง */
class Expelliarmus extends Spell {

    Expelliarmus() {
        super("expelliarmus", 30, 2.5, new Color(255, 220, 80), new LockOnTargeting());
    }

    @Override
    void impact(GameWorld world, Enemy hit, Tier tier) {
        hit.takeDamage(damage(world, tier), damageType());
        hit.knockBack(90 * tier.multiplier());
    }
}
