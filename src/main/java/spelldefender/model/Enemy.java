package spelldefender.model;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * ศัตรูที่เดินจากขวาไปซ้ายเข้าหาฐาน
 * subclass กำหนดค่าพลัง และ override วิธีรับผลจากคาถาได้ (Polymorphism)
 *
 * คาถาต้องเรียก takeDamage / stun / knockBack เท่านั้น ห้ามแก้ hp ตรง ๆ
 * เพื่อให้ subclass อย่าง Dementor หรือบอสได้ตัดสินใจเอง
 */
abstract class Enemy extends Entity {

    protected final double maxHp;
    protected double hp;
    protected final double speed;  // พิกเซลต่อวินาที
    protected final int attack;    // HP ฐานที่เสียเมื่อเดินถึงฐาน
    protected double stunLeft;     // วินาทีที่ยังมึนอยู่
    private double hitFlash;       // กระพริบขาวตอนโดนตี

    protected Enemy(double x, double y, double radius, double maxHp, double speed, int attack) {
        super(x, y, radius);
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.speed = speed;
        this.attack = attack;
    }

    /** ชื่อไฟล์รูปใน /sprites/ (ไม่มีนามสกุล) */
    protected abstract String spriteName();

    /** สีของรูปทรงตอนไม่มีรูป */
    protected abstract Color bodyColor();

    double hp() {
        return hp;
    }

    double maxHp() {
        return maxHp;
    }

    int attack() {
        return attack;
    }

    boolean isStunned() {
        return stunLeft > 0;
    }

    /** ความเร็วตอนนี้ (บอส override ให้เร็วขึ้นตอนเลือดน้อย) */
    protected double currentSpeed() {
        return speed;
    }

    @Override
    public void update(double dt) {
        hitFlash = Math.max(0, hitFlash - dt);
        if (stunLeft > 0) {
            stunLeft = Math.max(0, stunLeft - dt);
            return;
        }
        x -= currentSpeed() * dt;
    }

    /** รับดาเมจ (Dementor override ตรงนี้) */
    void takeDamage(double amount, DamageType type) {
        hp = Math.max(0, hp - amount);
        hitFlash = 0.12;
        if (hp <= 0) {
            kill();
        }
    }

    /** มึน ไม่ขยับ ไม่สะสม เอาค่าที่นานกว่า */
    void stun(double seconds) {
        stunLeft = Math.max(stunLeft, seconds);
    }

    /** ถูกผลักกลับไปทางขวา */
    void knockBack(double distance) {
        x = Math.min(GameWorld.WIDTH - radius, x + distance);
    }

    boolean reachedBase(double baseFrontX) {
        return x - radius <= baseFrontX;
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        if (!drawSprite(g, assets, spriteName(), radius * 2.4)) {
            drawBody(g);
        }
        if (hitFlash > 0) {
            g.setColor(new Color(255, 255, 255, 160));
            g.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
        }
        if (isStunned()) {
            g.setColor(Color.YELLOW);
            for (int i = 0; i < 3; i++) {
                double a = stunLeft * 6 + i * 2.1;
                g.fillOval((int) (x + Math.cos(a) * radius) - 3, (int) (y - radius - 6 + Math.sin(a) * 4) - 3, 6, 6);
            }
        }
        drawHpBar(g);
    }

    /** รูปทรงพื้นฐาน: ตัวกลม + ตา (subclass override เพื่อเปลี่ยนหน้าตาได้) */
    protected void drawBody(Graphics2D g) {
        int r = (int) radius;
        g.setColor(bodyColor());
        g.fillOval((int) x - r, (int) y - r, r * 2, r * 2);
        g.setColor(bodyColor().darker());
        g.drawOval((int) x - r, (int) y - r, r * 2, r * 2);
        g.setColor(Color.WHITE);
        g.fillOval((int) x - r / 2 - 3, (int) y - r / 3, 6, 6);
        g.fillOval((int) x - r / 2 + 5, (int) y - r / 3, 6, 6);
        g.setColor(Color.BLACK);
        g.fillOval((int) x - r / 2 - 3, (int) y - r / 3 + 1, 3, 3);
        g.fillOval((int) x - r / 2 + 5, (int) y - r / 3 + 1, 3, 3);
    }

    private void drawHpBar(Graphics2D g) {
        int w = (int) (radius * 2);
        int bx = (int) (x - radius);
        int by = (int) (y - radius - 10);
        g.setColor(new Color(40, 0, 0));
        g.fillRect(bx, by, w, 4);
        g.setColor(new Color(220, 50, 50));
        g.fillRect(bx, by, (int) (w * hp / maxHp), 4);
    }
}
