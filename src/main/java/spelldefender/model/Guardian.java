package spelldefender.model;

import java.awt.Color;
import java.awt.Graphics2D;

/** ผู้พิทักษ์: HP สูง ขวางทางศัตรู ตีระยะประชิด */
class Guardian extends Ally {

    Guardian(double x, double y) {
        super(x, y, 220, 40, 0.6);
    }

    @Override
    void attack(GameWorld world, Enemy target) {
        world.damageEnemy(target, 6, DamageType.NORMAL);
        world.addBolt(x, y, target.x(), target.y(), new Color(220, 220, 220));
    }

    @Override
    protected Color robeColor() {
        return new Color(150, 150, 165);
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        super.draw(g, assets);
        // โล่
        g.setColor(new Color(190, 160, 70));
        g.fillRoundRect((int) x + 6, (int) y - 8, 12, 20, 6, 6);
    }
}
