package spellduel.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import spellduel.typing.Tier;

/**
 * นักเวท 1 คน (ผู้เล่นหรือบอท) มี HP และโล่
 * นักเวทไม่ตัดสินเองว่าโดนแล้วเกิดอะไร: GameWorld.resolveHit เป็นคนเรียก method เหล่านี้
 */
abstract class Duelist extends Entity {

    private static final double FLASH_TIME = 0.15;
    private static final Color SKIN = new Color(238, 196, 156);
    private static final Color GOLD = new Color(232, 190, 90);

    private final String name;
    private final int maxHp;
    private final int facing; // +1 หันขวา (ผู้เล่น), -1 หันซ้าย (บอท)
    private final Color robe;
    protected int hp;
    protected Shield shield;
    private double flash; // เวลากระพริบขาวหลังโดนตี
    private double time; // เวลาที่ใช้ทำท่าหายใจและแสงกระพริบ

    protected Duelist(String name, int maxHp, double x, double y, int facing, Color robe) {
        super(x, y);
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.facing = facing;
        this.robe = robe;
    }

    String name() {
        return name;
    }

    int hp() {
        return hp;
    }

    int maxHp() {
        return maxHp;
    }

    boolean isDefeated() {
        return hp <= 0;
    }

    /** โล่ที่ยังไม่หมด หรือ null */
    Shield shield() {
        return shield;
    }

    boolean hasShield() {
        return shield != null;
    }

    void raiseShield(Tier tier) {
        shield = new Shield(tier);
    }

    void breakShield() {
        shield = null;
    }

    /** ลด HP (role ไว้เผื่อทำ effect ต่างกันตามชนิดคาถา) */
    void takeHit(SpellRole role, double damage) {
        hp = Math.max(0, hp - (int) Math.round(damage));
        flash = FLASH_TIME;
    }

    /** ตำแหน่งปลายไม้กายสิทธิ์ (projectile ออกจากตรงนี้) */
    double wandX() {
        return x + facing * 44;
    }

    double wandY() {
        return y - 72;
    }

    /** จบรอบ: ผู้เล่นล้างข้อความที่พิมพ์ค้าง / บอททิ้ง cast bar (polymorphism) */
    abstract void interruptCast();

    /** กำลังร่ายอยู่ไหม (ไม้กายสิทธิ์จะเรืองแสง) */
    abstract boolean isCasting();

    @Override
    public void update(double dt) {
        time += dt;
        flash = Math.max(0, flash - dt);
        if (shield != null) {
            shield.update(dt);
            if (shield.isExpired()) {
                shield = null;
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        int feet = (int) y;
        int breath = (int) Math.round(Math.sin(time * 2.5) * 2); // ขยับขึ้นลงเบา ๆ เหมือนหายใจ

        // เงาที่พื้น
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval((int) x - 44, feet - 7, 88, 14);

        drawRobe(g, feet, breath);
        drawHead(g, feet, breath);
        drawArm(g, feet, breath);
        if (isCasting()) {
            Hud.glow(g, wandX(), wandY() + breath, 17 + 4 * Math.sin(time * 9), new Color(255, 245, 190));
        }
        if (shield != null) {
            drawShield(g, feet);
        }
    }

    /** สีถูกแทนด้วยขาวล้วนตอนโดนตี */
    private Color tint(Color c) {
        return flash > 0 ? Color.WHITE : c;
    }

    private void drawRobe(Graphics2D g, int feet, int breath) {
        int top = feet - 74 + breath;
        Path2D.Double robeShape = new Path2D.Double();
        robeShape.moveTo(x - 15, top);
        robeShape.curveTo(x - 20, top + 34, x - 28, feet - 24, x - 36, feet);
        robeShape.quadTo(x - 18, feet - 7, x, feet - 2);
        robeShape.quadTo(x + 18, feet + 3, x + 36, feet);
        robeShape.curveTo(x + 28, feet - 24, x + 20, top + 34, x + 15, top);
        robeShape.closePath();

        // ด้านที่หันเข้ากลางจอสว่างกว่า เหมือนโดนแสงจันทร์
        g.setPaint(new GradientPaint((float) (x - facing * 32), top, tint(robe.darker().darker()),
                (float) (x + facing * 28), top, tint(robe.brighter())));
        g.fill(robeShape);

        // แถบกลางเสื้อ + เข็มขัด
        g.setColor(tint(robe.darker()));
        g.fillPolygon(new int[] {(int) x - 5, (int) x + 5, (int) x + 11, (int) x - 11},
                new int[] {top, top, feet - 3, feet - 3}, 4);
        g.setColor(tint(GOLD));
        g.fillRoundRect((int) x - 18, feet - 45 + breath, 36, 5, 4, 4);
    }

    private void drawHead(Graphics2D g, int feet, int breath) {
        int headY = feet - 92 + breath;
        g.setColor(tint(SKIN));
        g.fillOval((int) x - 13, headY, 26, 26);
        g.setColor(tint(new Color(40, 30, 45)));
        g.fillOval((int) x + facing * 6 - 2, headY + 11, 4, 5); // ตา

        // หมวกปลายงอไปด้านหลัง
        double tipX = x - facing * 30;
        double tipY = headY - 40;
        Path2D.Double hat = new Path2D.Double();
        hat.moveTo(x - 20, headY + 5);
        hat.quadTo(x - facing * 2, headY - 26, tipX, tipY);
        hat.quadTo(x + facing * 10, headY - 18, x + 20, headY + 5);
        hat.closePath();
        g.setPaint(new GradientPaint((float) (x - facing * 20), headY - 20, tint(robe.darker().darker()),
                (float) (x + facing * 20), headY - 20, tint(robe)));
        g.fill(hat);
        g.setColor(tint(robe.darker()));
        g.fillOval((int) x - 32, headY, 64, 12); // ปีกหมวก
        g.setColor(tint(GOLD));
        g.setStroke(new BasicStroke(3));
        g.drawArc((int) x - 20, headY - 8, 40, 16, 180, 180);
    }

    private void drawArm(Graphics2D g, int feet, int breath) {
        int shoulderX = (int) x + facing * 8;
        int shoulderY = feet - 66 + breath;
        int handX = (int) x + facing * 28;
        int handY = feet - 62 + breath;
        g.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(tint(robe));
        g.drawLine(shoulderX, shoulderY, handX, handY);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(tint(new Color(120, 78, 44)));
        g.drawLine(handX, handY, (int) wandX(), (int) wandY() + breath);
        g.setColor(tint(SKIN));
        g.fillOval(handX - 5, handY - 5, 10, 10);
        g.setStroke(new BasicStroke(1));
    }

    private void drawShield(Graphics2D g, int feet) {
        Color c = shield.isPerfect() ? GOLD : SpellRole.SHIELD.color();
        int alpha = (int) (110 + 120 * shield.fraction());
        double r = 84;
        double cx = x + facing * 12;
        double cy = feet - 66;
        Color clear = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        Color edge = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        // โปร่งตรงกลาง เข้มตรงขอบ เหมือนฟองสบู่
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r,
                new float[] {0.45f, 1f}, new Color[] {clear, edge}));
        Ellipse2D.Double bubble = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        g.fill(bubble);
        g.setColor(edge);
        g.setStroke(new BasicStroke(3));
        g.draw(bubble);
        g.setStroke(new BasicStroke(1));
    }
}
