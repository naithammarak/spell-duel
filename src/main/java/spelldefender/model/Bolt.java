package spelldefender.model;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/** เส้นแสงสั้น ๆ จากนักเวทไปหาเป้า (ใช้แสดงการโจมตีที่โดนทันที) */
class Bolt extends Entity {

    private static final double LIFETIME = 0.15;

    private final double toX;
    private final double toY;
    private final Color color;
    private double age;

    Bolt(double fromX, double fromY, double toX, double toY, Color color) {
        super(fromX, fromY, 0);
        this.toX = toX;
        this.toY = toY;
        this.color = color;
    }

    @Override
    public void update(double dt) {
        age += dt;
        if (age >= LIFETIME) {
            kill();
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, 1 - age / LIFETIME)));
        g.setColor(color);
        g.setStroke(new BasicStroke(3));
        g.drawLine((int) x, (int) y, (int) toX, (int) toY);
        g.setStroke(new BasicStroke(1));
        g.setComposite(old);
    }
}
