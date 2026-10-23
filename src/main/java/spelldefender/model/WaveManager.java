package spelldefender.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * ควบคุม wave ด้วยตาราง int[][] waveTable
 *   แถว   = wave ที่เท่าไหร่
 *   คอลัมน์ = จำนวนศัตรูแต่ละชนิด: {Goblin, Troll, Dementor, Boss}
 * ศัตรูในแต่ละ wave ออกทีละตัวแบบสุ่มลำดับ บอสออกตัวสุดท้ายเสมอ
 * wave ถัดไปเริ่มเมื่อศัตรูของ wave ก่อนตายหมดแล้ว
 */
class WaveManager {

    static final int GOBLIN = 0;
    static final int TROLL = 1;
    static final int DEMENTOR = 2;
    static final int BOSS = 3;
    static final int ENEMY_TYPES = 4;

    static final double SPAWN_INTERVAL = 2.5;  // วินาทีระหว่างศัตรูแต่ละตัว
    static final double BREAK_SECONDS = 3.0;   // พักก่อนเริ่ม wave ใหม่

    private static final int[][] STANDARD_WAVES = {
            // Goblin, Troll, Dementor, Boss
            {6, 0, 0, 0},
            {8, 2, 0, 0},
            {7, 2, 2, 0},
            {8, 3, 3, 0},
            {5, 2, 3, 1},
    };

    private final int[][] waveTable;
    private final Random rng;
    private final ArrayList<Integer> spawnQueue = new ArrayList<>();
    private int waveIndex = -1;     // wave ที่กำลังเล่น (-1 = ยังไม่เริ่ม)
    private double timer = BREAK_SECONDS;
    private boolean betweenWaves = true;

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

    boolean isBetweenWaves() {
        return betweenWaves;
    }

    double breakTimeLeft() {
        return betweenWaves ? timer : 0;
    }

    /** ผ่านครบทุก wave แล้ว (โหมด idle ไม่มีวันจบ) */
    boolean allWavesCleared() {
        return waveTable.length > 0 && waveIndex >= waveTable.length - 1 && spawnQueue.isEmpty() && !betweenWaves;
    }

    /**
     * เดินเวลา แล้วคืนศัตรูที่เกิดใหม่ในเฟรมนี้ (GameWorld เอาไปใส่ list เอง)
     * @param fieldEmpty ตอนนี้ไม่มีศัตรูเหลือบนแผนที่
     */
    List<Enemy> update(double dt, boolean fieldEmpty, TileMap map) {
        List<Enemy> spawned = new ArrayList<>();
        if (waveTable.length == 0) {
            return spawned;
        }
        timer -= dt;

        if (betweenWaves) {
            if (timer <= 0) {
                startNextWave();
            }
            return spawned;
        }

        if (!spawnQueue.isEmpty()) {
            if (timer <= 0) {
                spawned.add(create(spawnQueue.remove(0), map));
                timer = SPAWN_INTERVAL;
            }
        } else if (fieldEmpty && waveIndex < waveTable.length - 1) {
            // wave นี้จบแล้ว พักก่อนเริ่ม wave ถัดไป
            betweenWaves = true;
            timer = BREAK_SECONDS;
        }
        return spawned;
    }

    private void startNextWave() {
        waveIndex++;
        betweenWaves = false;
        timer = 0;
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
    }

    /** แปลงเลขคอลัมน์เป็นศัตรูจริง ที่จุดเกิดแบบสุ่ม */
    private Enemy create(int type, TileMap map) {
        List<double[]> points = map.spawnPoints();
        double[] p = points.get(rng.nextInt(points.size()));
        return switch (type) {
            case GOBLIN -> new Goblin(p[0], p[1]);
            case TROLL -> new Troll(p[0], p[1]);
            case DEMENTOR -> new Dementor(p[0], p[1]);
            case BOSS -> new DarkWizardBoss(p[0], p[1]);
            default -> throw new IllegalArgumentException("unknown enemy type " + type);
        };
    }
}
