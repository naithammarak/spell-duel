package spelldefender.model;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import spelldefender.typing.Tier;

/**
 * กระสุนคาถา มี 2 แบบ:
 *   - ติดตามเป้า (target != null) จากคาถา Lock-on
 *   - พุ่งตรงตามทิศ (target == null) จากคาถา Directional ถ้ามี splashRadius จะระเบิดโดนหลายตัว
 * ขนาดและความสว่างขึ้นกับ Tier
 */
class Projectile extends Entity {

    static final double SPEED = 420;
    static final double MAX_RANGE = 900;

    private final Spell spell;
    private final Tier tier;
    private final double splashRadius;
    private Enemy target;
    private double vx;
    private double vy;
    private double traveled;

    Projectile(Spell spell, Tier tier, double x, double y, double angle, Enemy target, double splashRadius) {
        super(x, y, 5 + 4 * tier.multiplier());
        this.spell = spell;
        this.tier = tier;
        this.target = target;
        this.splashRadius = splashRadius;
        this.vx = Math.cos(angle) * SPEED;
        this.vy = Math.sin(angle) * SPEED;
    }

    Spell spell() {
        return spell;
    }

    Tier tier() {
        return tier;
    }

    Enemy target() {
        return target;
    }

    boolean isHoming() {
        return target != null;
    }

    double splashRadius() {
        return splashRadius;
    }

    /** เปลี่ยนเป้า (ใช้ตอนเป้าเดิมตายก่อนกระสุนถึง) */
    void retarget(Enemy newTarget) {
        target = newTarget;
    }

    @Override
    public void update(double dt) {
        if (target != null) {
            // หันไปหาเป้าทุกเฟรม
            double dx = target.x() - x;
            double dy = target.y() - y;
            double len = Math.max(1e-6, Math.hypot(dx, dy));
            vx = dx / len * SPEED;
            vy = dy / len * SPEED;
        }
        x += vx * dt;
        y += vy * dt;
        traveled += SPEED * dt;
        if (traveled > MAX_RANGE || x < 0 || x > GameWorld.WIDTH || y < 0 || y > GameWorld.HEIGHT) {
            kill();
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        Color c = spell.color();
        Composite old = g.getComposite();
        // แสงเรือง: PERFECT สว่างสุด
        float glow = (float) Math.min(1, 0.25 + 0.4 * tier.multiplier());
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glow * 0.5f));
        g.setColor(c);
        int gr = (int) (radius * 2.2);
        g.fillOval((int) x - gr, (int) y - gr, gr * 2, gr * 2);
        g.setComposite(old);
        int r = (int) radius;
        g.setColor(c);
        g.fillOval((int) x - r, (int) y - r, r * 2, r * 2);
        g.setColor(Color.WHITE);
        g.fillOval((int) x - r / 2, (int) y - r / 2, r, r);
    }
}
