package spelldefender.model;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * บอส wave 5: HP เยอะมาก ผลักไม่ได้ มึนได้แค่ครึ่งเวลา
 * ถ้า HP ต่ำกว่าครึ่ง จะโกรธและเดินเร็วขึ้น
 */
class DarkWizardBoss extends Enemy {

    DarkWizardBoss(double x, double y) {
        super(x, y, 28, 800, 16, 50);
    }

    boolean isEnraged() {
        return hp < maxHp / 2;
    }

    @Override
    protected double currentSpeed() {
        return isEnraged() ? super.currentSpeed() * 1.6 : super.currentSpeed();
    }

    @Override
    void knockBack(double distance) {
        // บอสไม่ขยับ
    }

    @Override
    void stun(double seconds) {
        super.stun(seconds / 2);
    }

    @Override
    int bounty() {
        return 100;
    }

    @Override
    protected String spriteName() {
        return isEnraged() ? "boss_enraged" : "boss";
    }

    @Override
    protected Color bodyColor() {
        return isEnraged() ? new Color(150, 20, 40) : new Color(60, 20, 70);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        super.drawBody(g);
        // มงกุฎ
        int r = (int) radius;
        g.setColor(new Color(220, 190, 60));
        g.fillPolygon(new int[] {(int) x - 14, (int) x - 8, (int) x, (int) x + 8, (int) x + 14},
                new int[] {(int) y - r + 2, (int) y - r - 12, (int) y - r - 2, (int) y - r - 12, (int) y - r + 2}, 5);
    }
}
