package spelldefender.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * ช่องวางนักเวทหน้าปราสาท เป็นตาราง Ally[][] slots (แถว = เลนที่ศัตรูเดิน, คอลัมน์ = ระยะจากปราสาท)
 * null = ช่องว่าง นักเวทที่ตายจะถูกเคลียร์ออกจากช่องใน sweep()
 */
class ArmyGrid implements Drawable {

    static final int COLS = 4;
    static final double SPACING = 54;
    static final double SLOT_SIZE = 40;

    private final Ally[][] slots;
    private final double[] laneY;
    private final double firstX;

    ArmyGrid(double[] laneY, double firstX) {
        this.laneY = laneY.clone();
        this.firstX = firstX;
        this.slots = new Ally[laneY.length][COLS];
    }

    double slotX(int col) {
        return firstX + col * SPACING;
    }

    double slotY(int row) {
        return laneY[row];
    }

    /** ช่องที่ตำแหน่ง (x, y) อยู่ข้างใน คืน {row, col} หรือ null */
    int[] slotAt(double x, double y) {
        for (int row = 0; row < slots.length; row++) {
            for (int col = 0; col < COLS; col++) {
                if (Math.abs(x - slotX(col)) <= SLOT_SIZE / 2 && Math.abs(y - slotY(row)) <= SLOT_SIZE / 2) {
                    return new int[] {row, col};
                }
            }
        }
        return null;
    }

    boolean isEmpty(int row, int col) {
        return slots[row][col] == null;
    }

    Ally place(int row, int col, AllyType type) {
        Ally ally = type.create(slotX(col), slotY(row));
        slots[row][col] = ally;
        return ally;
    }

    /** นักเวทที่ยังมีชีวิตทั้งหมด */
    List<Ally> allies() {
        List<Ally> list = new ArrayList<>();
        for (Ally[] row : slots) {
            for (Ally a : row) {
                if (a != null && !a.isDead()) {
                    list.add(a);
                }
            }
        }
        return list;
    }

    /** เคลียร์ช่องของนักเวทที่ตายแล้ว */
    void sweep() {
        for (int row = 0; row < slots.length; row++) {
            for (int col = 0; col < COLS; col++) {
                if (slots[row][col] != null && slots[row][col].isDead()) {
                    slots[row][col] = null;
                }
            }
        }
    }

    /** นักเวทที่ขวางหน้าศัตรูตัวนี้อยู่ (ชนกันและอยู่เลนเดียวกัน) หรือ null */
    Ally blockerFor(Enemy e) {
        Ally best = null;
        for (Ally a : allies()) {
            boolean inFront = a.x() < e.x() && e.x() - a.x() <= e.radius() + a.radius() + 2;
            boolean sameLane = Math.abs(a.y() - e.y()) < 30;
            if (inFront && sameLane && (best == null || a.x() > best.x())) {
                best = a;
            }
        }
        return best;
    }

    /** วาดกรอบช่อง (ชัดขึ้นตอนกำลังเลือกซื้อ) และนักเวท */
    void draw(Graphics2D g, Assets assets, boolean highlight) {
        g.setStroke(new BasicStroke(highlight ? 2 : 1));
        for (int row = 0; row < slots.length; row++) {
            for (int col = 0; col < COLS; col++) {
                int sx = (int) (slotX(col) - SLOT_SIZE / 2);
                int sy = (int) (slotY(row) - SLOT_SIZE / 2);
                if (slots[row][col] == null) {
                    g.setColor(highlight ? new Color(255, 255, 160, 200) : new Color(255, 255, 255, 50));
                    g.drawRoundRect(sx, sy, (int) SLOT_SIZE, (int) SLOT_SIZE, 8, 8);
                }
            }
        }
        g.setStroke(new BasicStroke(1));
        for (Ally a : allies()) {
            a.draw(g, assets);
        }
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        draw(g, assets, false);
    }
}
