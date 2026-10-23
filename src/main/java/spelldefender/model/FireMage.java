package spelldefender.model;

import java.awt.Color;

/** นักเวทไฟ: ลูกไฟระเบิดโดนทุกตัวในรัศมี ยิงช้า */
class FireMage extends Ally {

    static final double SPLASH = 55;

    FireMage(double x, double y) {
        super(x, y, 60, 320, 1.6);
    }

    @Override
    void attack(GameWorld world, Enemy target) {
        for (Enemy e : world.enemiesNear(target.x(), target.y(), SPLASH)) {
            world.damageEnemy(e, 14, DamageType.NORMAL);
        }
        world.addBolt(x, y, target.x(), target.y(), new Color(255, 140, 30));
        world.burstAt(target.x(), target.y(), new Color(255, 160, 40), 10);
    }

    @Override
    protected Color robeColor() {
        return new Color(200, 60, 30);
    }
}
