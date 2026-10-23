package spelldefender.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** นักเวทสายฟ้า: สายฟ้ากระโดดต่อไปยังศัตรูตัวที่ใกล้ที่สุด สูงสุด 4 ตัว */
class LightningMage extends Ally {

    static final int CHAIN = 4;
    static final double JUMP_RANGE = 90;

    LightningMage(double x, double y) {
        super(x, y, 60, 300, 1.1);
    }

    @Override
    void attack(GameWorld world, Enemy target) {
        List<Enemy> hit = new ArrayList<>();
        Enemy current = target;
        double fromX = x;
        double fromY = y;
        while (current != null && hit.size() < CHAIN) {
            hit.add(current);
            world.damageEnemy(current, 10, DamageType.NORMAL);
            world.addBolt(fromX, fromY, current.x(), current.y(), new Color(255, 255, 120));
            fromX = current.x();
            fromY = current.y();
            current = nextJump(world, current, hit);
        }
    }

    /** ศัตรูที่ใกล้ที่สุดที่ยังไม่โดน และอยู่ในระยะกระโดด */
    private Enemy nextJump(GameWorld world, Enemy from, List<Enemy> alreadyHit) {
        Enemy best = null;
        for (Enemy e : world.enemiesNear(from.x(), from.y(), JUMP_RANGE)) {
            if (!alreadyHit.contains(e) && (best == null || e.distanceTo(from) < best.distanceTo(from))) {
                best = e;
            }
        }
        return best;
    }

    @Override
    protected Color robeColor() {
        return new Color(220, 190, 40);
    }
}
