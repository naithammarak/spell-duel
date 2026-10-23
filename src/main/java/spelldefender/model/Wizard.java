package spelldefender.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * พ่อมดยืนนิ่งหน้าฐาน เป็นเจ้าของ 2 อย่าง:
 *   - ทิศไม้กายสิทธิ์ (wandAngle) ใช้กับคาถาแบบ Directional
 *   - เป้าที่ผู้เล่นเลือกด้วย Tab (manualLock) ถ้า null = เล็งตัวที่ใกล้ฐานที่สุดอัตโนมัติ
 */
class Wizard extends Entity {

    static final double TURN_STEP = Math.toRadians(10);
    static final double MAX_ANGLE = Math.toRadians(75);

    private double wandAngle; // 0 = ชี้ไปทางขวา, ค่าบวก = ชี้ลง
    private Enemy manualLock;
    private double castFlash; // เวลาที่เหลือของแสงตอนร่าย

    Wizard(double x, double y) {
        super(x, y, 16);
    }

    double wandAngle() {
        return wandAngle;
    }

    void turn(int direction) {
        wandAngle += direction * TURN_STEP;
        wandAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, wandAngle));
    }

    /** หันไม้ไปทางจุด (px, py) เช่นตำแหน่งเมาส์ */
    void aimAt(double px, double py) {
        double angle = Math.atan2(py - y, px - x);
        wandAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, angle));
    }

    Enemy manualLock() {
        return manualLock;
    }

    void setManualLock(Enemy enemy) {
        manualLock = enemy;
    }

    void flash() {
        castFlash = 0.25;
    }

    /** ปลายไม้กายสิทธิ์ (จุดที่กระสุนออก) */
    double wandTipX() {
        return x + Math.cos(wandAngle) * 26;
    }

    double wandTipY() {
        return y + Math.sin(wandAngle) * 26;
    }

    @Override
    public void update(double dt) {
        castFlash = Math.max(0, castFlash - dt);
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        // เส้นเล็ง (ประ) ตามทิศไม้กายสิทธิ์
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {6, 8}, 0));
        g.drawLine((int) x, (int) y, (int) (x + Math.cos(wandAngle) * 260), (int) (y + Math.sin(wandAngle) * 260));
        g.setStroke(new BasicStroke(1));

        if (!drawSprite(g, assets, "wizard", 48)) {
            // เสื้อคลุม + หมวกแหลม
            g.setColor(new Color(70, 50, 140));
            g.fillOval((int) x - 14, (int) y - 10, 28, 28);
            g.setColor(new Color(245, 215, 180));
            g.fillOval((int) x - 8, (int) y - 18, 16, 16);
            g.setColor(new Color(50, 35, 110));
            g.fillPolygon(new int[] {(int) x - 12, (int) x + 12, (int) x + 2},
                    new int[] {(int) y - 14, (int) y - 14, (int) y - 40}, 3);
        }
        // ไม้กายสิทธิ์
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(120, 80, 40));
        g.drawLine((int) x, (int) y, (int) wandTipX(), (int) wandTipY());
        g.setStroke(new BasicStroke(1));
        if (castFlash > 0) {
            int r = (int) (castFlash * 60);
            g.setColor(new Color(255, 255, 200, 180));
            g.fillOval((int) wandTipX() - r / 2, (int) wandTipY() - r / 2, r, r);
        }
    }
}
