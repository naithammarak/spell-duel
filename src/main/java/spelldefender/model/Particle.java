package spelldefender.model;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/** เศษประกายเล็ก ๆ ตอนคาถาโดนหรือศัตรูตาย พุ่งออกแล้วจางหาย */
class Particle extends Entity {

    private final Color color;
    private final double vx;
    private final double vy;
    private final double lifetime;
    private double age;

    Particle(double x, double y, double vx, double vy, double size, double lifetime, Color color) {
        super(x, y, size);
        this.vx = vx;
        this.vy = vy;
        this.lifetime = lifetime;
        this.color = color;
    }

    @Override
    public void update(double dt) {
        age += dt;
        x += vx * dt;
        y += vy * dt;
        if (age >= lifetime) {
            kill();
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        float alpha = (float) Math.max(0, 1 - age / lifetime);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);
        int s = (int) Math.max(1, radius * alpha + 1);
        g.fillRect((int) x - s / 2, (int) y - s / 2, s, s); // สี่เหลี่ยมเล็ก ๆ ให้ดูเป็น pixel
        g.setComposite(old);
    }
}
