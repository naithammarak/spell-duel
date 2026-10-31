package spellduel.model;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;

/** ข้อความที่ลอยขึ้นแล้วจางหาย เช่น "PERFECT!" หรือ "Blocked!" */
class FloatingText extends Entity {

    private static final double LIFETIME = 1.2;
    private static final double RISE_SPEED = 35;
    private final String text;
    private final Color color;
    private final Font font;
    private double age;

    FloatingText(double x, double y, String text, Color color) {
        this(x, y, text, color, 22);
    }

    FloatingText(double x, double y, String text, Color color, int fontSize) {
        super(x, y);
        this.text = text;
        this.color = color;
        this.font = new Font(Font.DIALOG, Font.BOLD, fontSize);
    }

    String text() {
        return text;
    }

    @Override
    public void update(double dt) {
        age += dt;
        y -= RISE_SPEED * dt;
        if (age >= LIFETIME) {
            kill();
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        float alpha = (float) Math.max(0, 1 - age / LIFETIME);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text);
        g.setColor(Color.BLACK);
        g.drawString(text, (int) x - w / 2 + 2, (int) y + 2);
        g.setColor(color);
        g.drawString(text, (int) x - w / 2, (int) y);
        g.setComposite(old);
    }
}
