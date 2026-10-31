package spellduel.model;

import java.awt.Color;

/**
 * ค่าสถานะของบอท 1 ตัว (1 แถวของตาราง STATS)
 * cps = ตัวอักษรต่อวินาที, accuracy = %, reaction = วินาทีที่พักก่อนร่ายต่อ
 */
record BotProfile(String name, int hp, double cps, int accuracy, double reaction, Color robe) {

    // คอลัมน์ของตาราง STATS
    static final int HP = 0;
    static final int CPS_X10 = 1;
    static final int ACCURACY = 2;
    static final int REACTION_MS = 3;
    static final int COLUMNS = 4;

    /** 2D array: แถว = บอท (0 Apprentice, 1 Auror, 2 Dark Wizard) */
    static final int[][] STATS = {
            {60, 25, 70, 1500},
            {90, 35, 85, 700},
            {120, 45, 95, 400},
    };
    static final String[] NAMES = {"Apprentice", "Auror", "Dark Wizard"};
    static final Color[] ROBES = {new Color(60, 150, 80), new Color(170, 40, 50), new Color(110, 50, 140)};

    static BotProfile row(int level) {
        return fromRow(NAMES[level], STATS[level], ROBES[level]);
    }

    /** แปลงแถวของตารางเป็น record ไม่ยอมรับแถวที่ความกว้างผิด */
    static BotProfile fromRow(String name, int[] row, Color robe) {
        if (row.length != COLUMNS) {
            throw new IllegalArgumentException("STATS row must have " + COLUMNS + " columns: " + name);
        }
        return new BotProfile(name, row[HP], row[CPS_X10] / 10.0, row[ACCURACY],
                row[REACTION_MS] / 1000.0, robe);
    }

    /** เวลาที่บอทใช้ร่ายคำนี้ (วินาที) */
    double castTime(String word) {
        return word.length() / cps;
    }
}
