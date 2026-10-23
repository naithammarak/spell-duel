package spelldefender.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * นักเวทในกองทัพของเรา ยืนประจำช่องหน้าปราสาท ยิงศัตรูในระยะเองอัตโนมัติ
 * แต่ละสาย override attack() (Polymorphism) ศัตรูที่เดินมาชนจะหยุดตีจนนักเวทตาย
 */
abstract class Ally extends Entity {

    private final double maxHp;
    private double hp;
    private final double range;
    private final double attackInterval; // วินาทีต่อการโจมตี 1 ครั้ง
    private double cooldownLeft;
    private double hitFlash;

    protected Ally(double x, double y, double maxHp, double range, double attackInterval) {
        super(x, y, 15);
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.range = range;
        this.attackInterval = attackInterval;
    }

    /** โจมตีเป้า (เรียกเมื่อ cooldown หมดและมีศัตรูในระยะ) */
    abstract void attack(GameWorld world, Enemy target);

    /** สีเสื้อคลุม */
    protected abstract Color robeColor();

    double range() {
        return range;
    }

    double hp() {
        return hp;
    }

    boolean isReady() {
        return cooldownLeft <= 0;
    }

    void startCooldown() {
        cooldownLeft = attackInterval;
    }

    void takeDamage(double amount) {
        hp = Math.max(0, hp - amount);
        hitFlash = 0.1;
        if (hp <= 0) {
            kill();
        }
    }

    @Override
    public void update(double dt) {
        cooldownLeft = Math.max(0, cooldownLeft - dt);
        hitFlash = Math.max(0, hitFlash - dt);
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        int cx = (int) x;
        int cy = (int) y;
        if (!drawSprite(g, assets, getClass().getSimpleName().toLowerCase(), 40)) {
            g.setColor(robeColor());
            g.fillOval(cx - 12, cy - 6, 24, 22);
            g.setColor(new Color(245, 215, 180));
            g.fillOval(cx - 7, cy - 14, 14, 14);
            g.setColor(robeColor().darker());
            g.fillPolygon(new int[] {cx - 10, cx + 10, cx + 3}, new int[] {cy - 10, cy - 10, cy - 30}, 3);
        }
        if (hitFlash > 0) {
            g.setColor(new Color(255, 60, 60, 150));
            g.fillOval(cx - 14, cy - 14, 28, 28);
        }
        if (hp < maxHp) {
            g.setColor(new Color(0, 40, 0));
            g.fillRect(cx - 14, cy + 18, 28, 4);
            g.setColor(new Color(80, 220, 80));
            g.fillRect(cx - 14, cy + 18, (int) (28 * hp / maxHp), 4);
        }
        g.setStroke(new BasicStroke(1));
    }
}
