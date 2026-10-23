package spelldefender.model;

import java.awt.Color;
import spelldefender.typing.Tier;

/** Lock-on: ดาเมจแรง cooldown ยาว เป็นคาถาเดียวที่ทำร้าย Dementor ได้เต็มที่ */
class ExpectoPatronum extends Spell {

    ExpectoPatronum() {
        super("expecto patronum", 45, 8.0, new Color(200, 240, 255), new LockOnTargeting());
    }

    @Override
    DamageType damageType() {
        return DamageType.PATRONUS;
    }

    @Override
    void impact(GameWorld world, Enemy hit, Tier tier) {
        hit.takeDamage(damage(world, tier), damageType());
    }
}
