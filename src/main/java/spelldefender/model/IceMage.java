package spelldefender.model;

import java.awt.Color;

/** นักเวทน้ำแข็ง: ดาเมจน้อย แต่ทำให้ศัตรูรอบเป้าเดินช้าลงครึ่งหนึ่ง */
class IceMage extends Ally {

    IceMage(double x, double y) {
        super(x, y, 60, 280, 0.9);
    }

    @Override
    void attack(GameWorld world, Enemy target) {
        for (Enemy e : world.enemiesNear(target.x(), target.y(), 40)) {
            world.damageEnemy(e, 4, DamageType.NORMAL);
            e.slow(2.0, 0.5);
        }
        world.addBolt(x, y, target.x(), target.y(), new Color(150, 220, 255));
    }

    @Override
    protected Color robeColor() {
        return new Color(60, 140, 220);
    }
}
