package spelldefender.model;

import spelldefender.typing.Tier;

/** ยิงตรงตามทิศไม้กายสิทธิ์ (←/→) ถ้า splashRadius &gt; 0 จะระเบิดโดนหลายตัว */
class DirectionalTargeting implements Targeting {

    private final double splashRadius;

    DirectionalTargeting(double splashRadius) {
        this.splashRadius = splashRadius;
    }

    @Override
    public boolean launch(Spell spell, Tier tier, GameWorld world) {
        Wizard w = world.wizard();
        world.addProjectile(new Projectile(spell, tier, w.wandTipX(), w.wandTipY(), w.wandAngle(), null, splashRadius));
        return true; // ยิงได้เสมอ แม้ไม่มีศัตรู
    }
}
