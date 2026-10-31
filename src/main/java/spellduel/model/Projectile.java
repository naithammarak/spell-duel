package spellduel.model;

import java.awt.Color;
import java.awt.Graphics2D;
import spellduel.typing.Tier;

/** ลูกเวท (Attack / Interrupt) ที่บินจากผู้ร่ายไปหาเป้า พอถึงแล้ว GameWorld.resolveHit ตัดสิน */
class Projectile extends Entity {

    private final Duelist caster;
    private final Duelist target;
    private final SpellRole role;
    private final double power;
    private final Tier tier;
    private final int direction; // +1 บินไปขวา, -1 บินไปซ้าย

    Projectile(double x, double y, Duelist caster, Duelist target, SpellRole role, double power, Tier tier) {
        super(x, y);
        this.caster = caster;
        this.target = target;
        this.role = role;
        this.power = power;
        this.tier = tier;
        this.direction = target.x() > x ? 1 : -1;
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

    /** ต้องแม่นถึงจะตัด cast ได้: GREAT/PERFECT เท่านั้น (กันการพิมพ์ครึ่งคำรัว ๆ) */
    boolean canCut() {
        return tier == Tier.GREAT || tier == Tier.PERFECT;
    }

    /** สะท้อน: ลูกใหม่ที่บินกลับไปหาคนร่าย แรงตาม tier ของโล่ที่สะท้อน */
    Projectile reflected(Tier shieldTier) {
        return new Projectile(x, y, target, caster, role, power * shieldTier.multiplier(), shieldTier);
    }

    /** บินถึงตัวเป้าแล้วหรือยัง */
    boolean hasArrived() {
        return direction > 0 ? x >= target.x() : x <= target.x();
    }

    @Override
    public void update(double dt) {
        x += direction * role.projectileSpeed() * dt;
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        int size = role == SpellRole.ATTACK ? 18 : 10;
        Color c = role.color();
        // หางลำแสง
        for (int i = 4; i >= 1; i--) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40 * (5 - i)));
            int tx = (int) (x - direction * i * size * 0.8);
            int s = size - i * 2;
            g.fillOval(tx - s / 2, (int) y - s / 2, s, s);
        }
        g.setColor(c);
        g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        g.setColor(Color.WHITE);
        g.fillOval((int) x - size / 4, (int) y - size / 4, size / 2, size / 2);
    }
}
