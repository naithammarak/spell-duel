package spelldefender.model;

import java.awt.Color;
import spelldefender.typing.Tier;

/** Lock-on: ดาเมจน้อย + ทำให้มึน (หยุดเดิน) */
class Stupefy extends Spell {

    Stupefy() {
        super("stupefy", 20, 1.5, new Color(230, 60, 60), new LockOnTargeting());
    }

    @Override
    void impact(GameWorld world, Enemy hit, Tier tier) {
        hit.takeDamage(damage(world, tier), damageType());
        hit.stun(1.6 * tier.multiplier());
    }
}
