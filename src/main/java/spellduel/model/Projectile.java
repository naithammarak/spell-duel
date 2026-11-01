package spellduel.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.Random;
import spellduel.typing.Tier;

/** สายฟ้า (Attack / Interrupt) ที่พุ่งจากผู้ร่ายไปหาเป้า พอถึงแล้ว GameWorld.resolveHit ตัดสิน */
class Projectile extends Entity {

    // รูปร่างสายฟ้า
    private static final double SEGMENT = 26;
    private static final double AMPLITUDE = 17;
    private static final int FLICKER_PER_SECOND = 24;

    private final Duelist caster;
    private final Duelist target;
    private final SpellRole role;
    private final double power;
    private final Tier tier;
    private final int direction; // +1 บินไปขวา, -1 บินไปซ้าย
    private final double startX; // ต้นสายฟ้า
    private double age;

    Projectile(double x, double y, Duelist caster, Duelist target, SpellRole role, double power, Tier tier) {
        super(x, y);
        this.caster = caster;
        this.target = target;
        this.role = role;
        this.power = power;
        this.tier = tier;
        this.direction = target.x() > x ? 1 : -1;
        this.startX = x;
    }

    /** ยิงจากปลายไม้ของผู้ร่าย */
    static Projectile fire(Duelist caster, Duelist target, SpellRole role, Tier tier) {
        double power = role.base() * tier.multiplier();
        return new Projectile(caster.wandX(), caster.wandY(), caster, target, role, power, tier);
    }

    Duelist caster() {
        return caster;
    }

    Duelist target() {
        return target;
    }

    SpellRole role() {
        return role;
    }

    double power() {
        return power;
    }

    /** ต้องแม่นถึงจะขัดได้ กันการพิมพ์ครึ่งคำแล้วรีบกด Enter */
    boolean canCut() {
        return tier == Tier.PERFECT || tier == Tier.GREAT;
    }

    /** สะท้อน: ลูกใหม่ที่บินกลับไปหาคนร่าย แรงตาม tier ของโล่ที่สะท้อน */
    Projectile reflected(Shield shield) {
        double reflectedPower = power * shield.tier().multiplier();
        return new Projectile(x, y, target, caster, role, reflectedPower, shield.tier());
    }

    /** บินถึงตัวเป้าแล้วหรือยัง */
    boolean hasArrived() {
        return direction > 0 ? x >= target.x() : x <= target.x();
    }

    @Override
    public void update(double dt) {
        x += direction * role.projectileSpeed() * dt;
        age += dt;
    }

    /** สายฟ้าจากจุดเริ่มถึงหัวลูกเวท: วาดเส้นหยัก 3 ชั้น นอกจาง กลางสี ในขาว */
    @Override
    public void draw(Graphics2D g, Assets assets) {
        int width = role == SpellRole.ATTACK ? 7 : 5;
        Color c = role.color();
        // seed เปลี่ยนตามเวลา สายฟ้าเลยกระพริบ ใช้ Random ของตัวเองเพื่อไม่กระทบผลเกม
        Random jitter = new Random((long) (age * FLICKER_PER_SECOND) + (long) startX);
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(startX, y);
        int steps = Math.max(2, (int) (Math.abs(x - startX) / SEGMENT));
        for (int i = 1; i < steps; i++) {
            double px = startX + (x - startX) * i / steps;
            double py = y + (jitter.nextDouble() - 0.5) * 2 * AMPLITUDE;
            bolt.lineTo(px, py);
            if (i % 3 == 0) {
                // แขนงเล็ก ๆ แตกออกจากเส้นหลัก
                bolt.lineTo(px + direction * SEGMENT, py + (jitter.nextDouble() - 0.5) * 3 * AMPLITUDE);
                bolt.moveTo(px, py);
            }
        }
        bolt.lineTo(x, y);

        Stroke before = g.getStroke();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
        g.setStroke(new BasicStroke(width * 2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(bolt);
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(bolt);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1, width / 3f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(bolt);
        g.setStroke(before);
        Hud.glow(g, x, y, width * 4, c);
    }
}
