package spellduel.model;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import spellduel.typing.Tier;

/**
 * นักเวท 1 คน (ผู้เล่นหรือบอท) มี HP และโล่
 * นักเวทไม่ตัดสินเองว่าโดนแล้วเกิดอะไร: GameWorld.resolveHit เป็นคนเรียก method เหล่านี้
 */
abstract class Duelist extends Entity {

    private static final double FLASH_TIME = 0.15;

    private final String name;
    private final int maxHp;
    private final int facing; // +1 หันขวา (ผู้เล่น), -1 หันซ้าย (บอท)
    private final Color robe;
    protected int hp;
    protected Shield shield;
    private double flash; // เวลากระพริบขาวหลังโดนตี

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

    /** ตัด cast: ผู้เล่นเสียข้อความที่พิมพ์ค้าง / บอทเสีย cast bar (polymorphism) */
    abstract void interruptCast();

    /** กำลังร่ายอยู่ไหม (ไม้กายสิทธิ์จะเรืองแสง) */
    abstract boolean isCasting();

    /** กำลังร่ายหน้าที่อะไร หรือ null ถ้าไม่รู้/ไม่ได้ร่าย (Interrupt ตัดได้เฉพาะ SHIELD) */
    abstract SpellRole castingRole();

    @Override
    public void update(double dt) {
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
        int cx = (int) x;
        int feet = (int) y;
        Color body = flash > 0 ? Color.WHITE : robe;

        // เงาที่พื้น
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(cx - 34, feet - 6, 68, 12);
        // เสื้อคลุม
        g.setColor(body);
        g.fillPolygon(new int[] {cx - 28, cx + 28, cx + 14, cx - 14},
                new int[] {feet, feet, feet - 70, feet - 70}, 4);
        // หัว
        g.setColor(flash > 0 ? Color.WHITE : new Color(240, 200, 160));
        g.fillOval(cx - 14, feet - 96, 28, 28);
        // หมวกปลายแหลม เอียงไปทางที่หัน
        g.setColor(body.darker());
        g.fillPolygon(new int[] {cx - 22, cx + 22, cx - facing * 10},
                new int[] {feet - 88, feet - 88, feet - 138}, 3);
        g.fillRect(cx - 26, feet - 92, 52, 6);
        // แขน + ไม้กายสิทธิ์
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(6));
        g.setColor(body);
        g.drawLine(cx + facing * 10, feet - 55, cx + facing * 30, feet - 62);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(110, 70, 40));
        g.drawLine(cx + facing * 30, feet - 62, (int) wandX(), (int) wandY());
        g.setStroke(oldStroke);
        if (isCasting()) {
            int pulse = (int) (6 + 3 * Math.sin(System.nanoTime() / 1e8));
            g.setColor(new Color(255, 255, 220, 200));
            g.fillOval((int) wandX() - pulse, (int) wandY() - pulse, pulse * 2, pulse * 2);
        }

        if (shield != null) {
            drawShield(g, cx, feet);
        }
    }

    private void drawShield(Graphics2D g, int cx, int feet) {
        Color c = shield.isPerfect() ? new Color(255, 215, 90) : SpellRole.SHIELD.color();
        float alpha = (float) (0.25 + 0.5 * shield.fraction());
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(c);
        int w = 120;
        int h = 160;
        int left = cx - w / 2 + facing * 14;
        g.setStroke(new BasicStroke(4));
        g.drawOval(left, feet - h + 10, w, h);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.3f));
        g.fillOval(left, feet - h + 10, w, h);
        g.setComposite(old);
        g.setStroke(new BasicStroke(1));
    }
}
