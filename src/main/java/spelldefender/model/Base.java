package spelldefender.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * ฐานที่ต้องป้องกัน ถ้า HP เหลือ 0 = แพ้
 * โล่ (จาก Protego) ดูดซับดาเมจก่อนถึง HP และหมดอายุตามเวลา
 */
class Base extends Entity {

    static final int MAX_HP = 150;

    private final double frontX; // ขอบขวาของฐาน ศัตรูเดินมาถึงตรงนี้ = โจมตีฐาน
    private final double height;
    private int hp = MAX_HP;
    private double shield;
    private double shieldTimeLeft;

    Base(double centerX, double centerY, double frontX, double height) {
        super(centerX, centerY, 0);
        this.frontX = frontX;
        this.height = height;
    }

    double frontX() {
        return frontX;
    }

    int hp() {
        return hp;
    }

    double shield() {
        return shield;
    }

    boolean isDestroyed() {
        return hp <= 0;
    }

    /** ศัตรูโจมตีฐาน: โล่รับก่อน ที่เหลือหัก HP */
    void takeDamage(int amount) {
        double absorbed = Math.min(shield, amount);
        shield -= absorbed;
        int rest = (int) Math.round(amount - absorbed);
        hp = Math.max(0, hp - rest);
    }

    /** ใส่โล่ (ไม่สะสม เอาค่าที่มากกว่า) */
    void addShield(double amount, double seconds) {
        shield = Math.max(shield, amount);
        shieldTimeLeft = Math.max(shieldTimeLeft, seconds);
    }

    @Override
    public void update(double dt) {
        if (shieldTimeLeft > 0) {
            shieldTimeLeft -= dt;
            if (shieldTimeLeft <= 0) {
                shield = 0;
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        // ตัวปราสาท (ถ้าไม่มีรูปก็วาดเป็นกำแพงหิน)
        int top = (int) (y - height / 2);
        int w = (int) frontX;
        if (assets.get("castle") != null) {
            g.drawImage(assets.get("castle"), 0, top, w, (int) height, null);
        } else {
            g.setColor(new Color(110, 110, 125));
            g.fillRect(0, top, w, (int) height);
            g.setColor(new Color(80, 80, 95));
            for (int by = top; by < top + height; by += 16) {
                for (int bx = ((by / 16) % 2) * 12; bx < w; bx += 24) {
                    g.drawRect(bx, by, 24, 16);
                }
            }
            // เชิงเทิน
            g.setColor(new Color(130, 130, 145));
            for (int by = top; by < top + height; by += 40) {
                g.fillRect(w - 8, by, 12, 20);
            }
        }
        if (shield > 0) {
            g.setColor(new Color(120, 200, 255, 110));
            g.setStroke(new BasicStroke(6));
            g.drawArc((int) (frontX - 60), top - 10, 120, (int) height + 20, -80, 160);
            g.setStroke(new BasicStroke(1));
        }
    }
}
