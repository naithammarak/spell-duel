package spellduel.model;

/**
 * ทุกอย่างที่มีตำแหน่งบนจอ (นักเวท, projectile, particle, ข้อความลอย)
 *
 * การลบแบบ "ทีหลัง": ห้ามลบออกจาก list ระหว่างวน loop ให้เรียก kill()
 * แล้ว GameWorld จะลบตัวที่ตายทั้งหมดตอนท้าย update (sweepDead)
 */
abstract class Entity implements Updatable, Drawable {

    protected double x;
    protected double y;
    private boolean dead;

    protected Entity(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    boolean isDead() {
        return dead;
    }

    void kill() {
        dead = true;
    }
}
