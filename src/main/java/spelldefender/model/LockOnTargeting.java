package spelldefender.model;

import spelldefender.typing.Tier;

/** ยิงกระสุนติดตามไปหาเป้าที่ล็อกอยู่ (เลือกด้วย Tab หรือตัวที่ใกล้ฐานที่สุด) */
class LockOnTargeting implements Targeting {

    @Override
    public boolean launch(Spell spell, Tier tier, GameWorld world) {
        Enemy target = world.currentLockTarget();
        if (target == null) {
            return false; // ไม่มีศัตรู → Fizzle
        }
        Wizard w = world.wizard();
        double angle = Math.atan2(target.y() - w.y(), target.x() - w.x());
        world.addProjectile(new Projectile(spell, tier, w.wandTipX(), w.wandTipY(), angle, target, 0));
        return true;
    }
}
