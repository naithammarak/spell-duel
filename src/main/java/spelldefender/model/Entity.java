package spelldefender.model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * ทุกอย่างที่มีตำแหน่งบนแผนที่ (ศัตรู ฐาน พ่อมด กระสุน ข้อความลอย)
 * hitbox เป็นวงกลม (x, y, radius) เพื่อให้เช็กการชนด้วยระยะห่างอย่างเดียว
 *
 * การลบแบบ "ทีหลัง": ห้ามลบออกจาก list ระหว่างวน loop ให้เรียก kill()
 * แล้ว GameWorld จะลบตัวที่ตายทั้งหมดตอนท้าย update
 */
abstract class Entity implements Updatable, Drawable {

    protected double x;
    protected double y;
    protected final double radius;
    private boolean dead;

    protected Entity(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double radius() {
        return radius;
    }

    boolean isDead() {
        return dead;
    }

    void kill() {
        dead = true;
    }

    double distanceTo(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    double distanceTo(Entity other) {
        return distanceTo(other.x, other.y);
    }

    boolean overlaps(Entity other) {
        return distanceTo(other) <= radius + other.radius;
    }

    /** วาดรูปให้อยู่กลาง (x, y) ขนาด size×size ถ้ามีรูป คืน true */
    protected boolean drawSprite(Graphics2D g, Assets assets, String name, double size) {
        BufferedImage img = assets.get(name);
        if (img == null) {
            return false;
        }
        int s = (int) size;
        g.drawImage(img, (int) (x - size / 2), (int) (y - size / 2), s, s, null);
        return true;
    }
}
