package spelldefender.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * ควบคุม wave ด้วยตาราง int[][] waveTable
 *   แถว   = wave ที่เท่าไหร่
 *   คอลัมน์ = จำนวนศัตรูแต่ละชนิด: {Goblin, Troll, Dementor, Boss}
 *
 * ระหว่าง wave เกมจะรอให้ผู้เล่นกดเริ่ม (startNextWave) เพื่อให้มีเวลาซื้อกองทัพ
 * ในแต่ละ wave ศัตรูทั้งหมดจะทยอยออกมาภายใน SPAWN_SECONDS วินาที บอสออกตัวสุดท้ายเสมอ
 */
class WaveManager {

    static final int GOBLIN = 0;
    static final int TROLL = 1;
    static final int DEMENTOR = 2;
    static final int BOSS = 3;
    static final int ENEMY_TYPES = 4;

    static final double SPAWN_SECONDS = 30; // ศัตรูทั้ง wave ออกมาครบภายในเวลานี้
    static final double HP_GROWTH = 0.4;    // wave ถัดไปศัตรูอึดขึ้น 40% (ไม่ให้กองทัพอย่างเดียวชนะได้)

    private static final int[][] STANDARD_WAVES = {
            // Goblin, Troll, Dementor, Boss
            {45, 0, 0, 0},
            {80, 6, 0, 0},
            {115, 14, 10, 0},
            {160, 24, 16, 0},
            {210, 34, 24, 1},
    };

    private final int[][] waveTable;
    private final Random rng;
    private final ArrayList<Integer> spawnQueue = new ArrayList<>();
    private int waveIndex = -1;     // wave ล่าสุดที่เริ่มไปแล้ว (-1 = ยังไม่เริ่ม)
    private boolean inWave;         // กำลังสู้อยู่ (false = ช่วงเตรียมตัว)
    private double spawnInterval;
    private double timer;

    WaveManager(int[][] waveTable, Random rng) {
        for (int[] row : waveTable) {
            if (row.length != ENEMY_TYPES) {
                throw new IllegalArgumentException("each wave needs " + ENEMY_TYPES + " columns");
            }
        }
        this.waveTable = waveTable;
        this.rng = rng;
    }

    static WaveManager standard(Random rng) {
        return new WaveManager(STANDARD_WAVES, rng);
    }

    /** ไม่มี wave เลย และไม่มีวันจบ ใช้ในเทสต์ที่วางศัตรูเอง */
    static WaveManager idle() {
        return new WaveManager(new int[0][ENEMY_TYPES], new Random(0));
    }

    int totalWaves() {
        return waveTable.length;
    }

    /** wave ปัจจุบันแบบนับจาก 1 (0 = ยังไม่เริ่ม) */
    int currentWave() {
        return waveIndex + 1;
    }

    boolean inWave() {
        return inWave;
    }

    /** เริ่ม wave ถัดไปได้ไหม (ต้องอยู่ช่วงเตรียมตัว และยังมี wave เหลือ) */
    boolean canStartNextWave() {
        return !inWave && waveIndex < waveTable.length - 1;
    }

    void startNextWave() {
        if (!canStartNextWave()) {
            return;
        }
        waveIndex++;
        inWave = true;
        int[] counts = waveTable[waveIndex];
        for (int type = 0; type < ENEMY_TYPES; type++) {
            if (type == BOSS) {
                continue;
            }
            for (int i = 0; i < counts[type]; i++) {
                spawnQueue.add(type);
            }
        }
        Collections.shuffle(spawnQueue, rng);
        for (int i = 0; i < counts[BOSS]; i++) {
            spawnQueue.add(BOSS); // บอสออกท้ายสุด
        }
        spawnInterval = SPAWN_SECONDS / Math.max(1, spawnQueue.size());
        timer = 0;
    }

    /** ผ่านครบทุก wave แล้ว (โหมด idle ไม่มีวันจบ) */
    boolean allWavesCleared() {
        return waveTable.length > 0 && waveIndex == waveTable.length - 1 && !inWave;
    }

    /**
     * เดินเวลา แล้วคืนศัตรูที่เกิดใหม่ในเฟรมนี้ (GameWorld เอาไปใส่ list เอง)
     * @param fieldEmpty ตอนนี้ไม่มีศัตรูเหลือบนแผนที่ ถ้าปล่อยครบแล้วด้วย = จบ wave
     */
    List<Enemy> update(double dt, boolean fieldEmpty, TileMap map) {
        List<Enemy> spawned = new ArrayList<>();
        if (!inWave) {
            return spawned;
        }
        timer -= dt;
        while (timer <= 0 && !spawnQueue.isEmpty()) { // ฝูงใหญ่: เฟรมเดียวอาจออกหลายตัว
            spawned.add(create(spawnQueue.remove(0), map));
            timer += spawnInterval;
        }
        if (spawnQueue.isEmpty() && fieldEmpty && spawned.isEmpty()) {
            inWave = false; // จบ wave กลับไปช่วงเตรียมตัว
        }
        return spawned;
    }

    /** แปลงเลขคอลัมน์เป็นศัตรูจริง ที่จุดเกิดแบบสุ่ม (ขยับนิดหน่อยให้ดูเป็นฝูง) */
    private Enemy create(int type, TileMap map) {
        List<double[]> points = map.spawnPoints();
        double[] p = points.get(rng.nextInt(points.size()));
        double x = p[0] + rng.nextDouble() * 30;
        double y = p[1] + (rng.nextDouble() - 0.5) * 30;
        Enemy e = switch (type) {
            case GOBLIN -> new Goblin(x, y);
            case TROLL -> new Troll(x, y);
            case DEMENTOR -> new Dementor(x, y);
            case BOSS -> new DarkWizardBoss(x, y);
            default -> throw new IllegalArgumentException("unknown enemy type " + type);
        };
        if (type != BOSS) {
            e.scaleHp(1 + HP_GROWTH * waveIndex);
        }
        return e;
    }
}
