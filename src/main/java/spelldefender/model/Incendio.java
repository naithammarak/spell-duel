package spelldefender.model;

import java.awt.Color;
import spelldefender.typing.Tier;

/** Directional: ลูกไฟพุ่งตามทิศไม้กายสิทธิ์ ระเบิดโดนทุกตัวในรัศมี (AoE) */
class Incendio extends Spell {

    Incendio() {
        super("incendio", 30, 3.5, new Color(255, 140, 30), new DirectionalTargeting(70));
    }

    @Override
    void impact(GameWorld world, Enemy hit, Tier tier) {
        hit.takeDamage(damage(world, tier), damageType());
    }
}
