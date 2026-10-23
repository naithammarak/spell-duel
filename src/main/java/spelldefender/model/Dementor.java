package spelldefender.model;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * ผีที่คาถาทั่วไปแทบทำอะไรไม่ได้
 * กฎ "แพ้แค่ Expecto Patronum" อยู่ที่นี่ที่เดียว คาถาแค่บอกว่าดาเมจชนิดไหน
 */
class Dementor extends Enemy {

    /** สัดส่วนดาเมจที่ผ่านเข้ามาได้ ถ้าไม่ใช่ Patronum */
    static final double RESISTED_FRACTION = 0.2;

    Dementor(double x, double y) {
        super(x, y, 16, 45, 35, 10);
    }

    @Override
    void takeDamage(double amount, DamageType type) {
        double real = (type == DamageType.PATRONUS) ? amount : amount * RESISTED_FRACTION;
        super.takeDamage(real, type);
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f)); // โปร่งแสง
        super.draw(g, assets);
        g.setComposite(old);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        // ผ้าคลุมดำทรงหยดน้ำ
        int r = (int) radius;
        g.setColor(bodyColor());
        g.fillOval((int) x - r, (int) y - r, r * 2, r * 2);
        g.fillPolygon(new int[] {(int) x - r, (int) x + r, (int) x + r + 6, (int) x - r + 4},
                new int[] {(int) y, (int) y, (int) y + r + 10, (int) y + r + 6}, 4);
        g.setColor(new Color(150, 200, 255));
        g.fillOval((int) x - 7, (int) y - 4, 4, 4);
        g.fillOval((int) x - 1, (int) y - 4, 4, 4);
    }

    @Override
    protected String spriteName() {
        return "dementor";
    }

    @Override
    protected Color bodyColor() {
        return new Color(35, 35, 50);
    }
}
