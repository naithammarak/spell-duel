package spelldefender.model;

import java.awt.Color;
import spelldefender.typing.Tier;

/**
 * คาถา 1 อัน แบ่งงานเป็น 2 คำถาม:
 *   - "ไปถึงเป้ายังไง"  → Targeting ที่คาถาถืออยู่ (LockOn / Directional / Self)
 *   - "ถึงแล้วทำอะไร"   → impact() ที่แต่ละคาถา override เอง
 *
 * cast() เป็น final: กฎ cooldown อยู่ที่นี่ที่เดียว subclass ลืมไม่ได้
 * cooldown เริ่มนับเฉพาะตอนร่ายออกจริง (Fizzle / ไม่มีเป้า ไม่เสีย cooldown)
 */
abstract class Spell {

    private final String name;
    private final double baseDamage;
    private final double cooldownSeconds;
    private final Color color;
    private final Targeting targeting;
    private double cooldownLeft;

    protected Spell(String name, double baseDamage, double cooldownSeconds, Color color, Targeting targeting) {
        this.name = name;
        this.baseDamage = baseDamage;
        this.cooldownSeconds = cooldownSeconds;
        this.color = color;
        this.targeting = targeting;
    }

    /** สิ่งที่ผู้เล่นต้องพิมพ์ เช่น "expecto patronum" */
    String name() {
        return name;
    }

    int length() {
        return name.length();
    }

    Color color() {
        return color;
    }

    double cooldownSeconds() {
        return cooldownSeconds;
    }

    boolean isReady() {
        return cooldownLeft <= 0;
    }

    /** 0 = พร้อม, 1 = เพิ่งร่าย (ใช้วาดแถบ cooldown) */
    double cooldownFraction() {
        return cooldownSeconds <= 0 ? 0 : cooldownLeft / cooldownSeconds;
    }

    void tick(double dt) {
        cooldownLeft = Math.max(0, cooldownLeft - dt);
    }

    /** ชนิดดาเมจ (ExpectoPatronum override เป็น PATRONUS) */
    DamageType damageType() {
        return DamageType.NORMAL;
    }

    /** ดาเมจจริง = ดาเมจพื้นฐาน × ตัวคูณของ Tier × โบนัส Combo */
    protected double damage(GameWorld world, Tier tier) {
        return baseDamage * tier.multiplier() * world.comboMultiplier();
    }

    /**
     * ร่ายคาถา
     * @return false ถ้ายังติด cooldown หรือหาเป้าไม่เจอ (เช่น Lock-on แต่ไม่มีศัตรู)
     */
    final boolean cast(GameWorld world, Tier tier) {
        if (tier == Tier.FIZZLE) {
            throw new IllegalArgumentException("FIZZLE cannot be cast");
        }
        if (!isReady()) {
            return false;
        }
        if (!targeting.launch(this, tier, world)) {
            return false;
        }
        cooldownLeft = cooldownSeconds;
        return true;
    }

    /**
     * ผลเมื่อคาถาถึงเป้า
     * @param hit ศัตรูที่โดน หรือ null สำหรับคาถาที่ใช้กับตัวเอง (Self)
     */
    abstract void impact(GameWorld world, Enemy hit, Tier tier);
}
