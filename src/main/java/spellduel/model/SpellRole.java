package spellduel.model;

import java.awt.Color;

/**
 * หน้าที่ของคาถา 3 แบบ (วงจรเป่ายิ้งฉุบ):
 * Attack ชนะ Interrupt · Interrupt ชนะ Shield · Shield ชนะ Attack
 */
enum SpellRole {
    ATTACK("Attack", 20, 1000, new Color(60, 235, 110)),
    SHIELD("Shield", 0, 0, new Color(90, 180, 255)),
    INTERRUPT("Interrupt", 20, 1600, new Color(255, 55, 65));

    private final String label;
    private final int base;
    private final double projectileSpeed; // pixel ต่อวินาที (0 = ไม่มี projectile)
    private final Color color;

    SpellRole(String label, int base, double projectileSpeed, Color color) {
        this.label = label;
        this.base = base;
        this.projectileSpeed = projectileSpeed;
        this.color = color;
    }

    /** การ์ดที่ชนะการ์ดใบนี้ */
    SpellRole beatenBy() {
        return switch (this) {
            case ATTACK -> SHIELD;
            case SHIELD -> INTERRUPT;
            case INTERRUPT -> ATTACK;
        };
    }

    String label() {
        return label;
    }

    int base() {
        return base;
    }

    double projectileSpeed() {
        return projectileSpeed;
    }

    Color color() {
        return color;
    }
}
