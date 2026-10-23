package spelldefender.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WaveManagerTest {

    private final TileMap map = TileMap.standard();

    /** เดินเวลาโดยถือว่าศัตรูตายทันที (fieldEmpty = true) แล้วเก็บทุกตัวที่เกิด */
    private List<Enemy> spawnAll(WaveManager waves, double seconds) {
        List<Enemy> all = new ArrayList<>();
        for (double t = 0; t < seconds; t += 0.05) {
            all.addAll(waves.update(0.05, true, map));
        }
        return all;
    }

    @Test
    void spawnsExactlyWhatTheTableSays() {
        WaveManager waves = new WaveManager(new int[][] {{3, 2, 1, 0}}, new Random(1));
        List<Enemy> spawned = spawnAll(waves, 30);
        assertEquals(3, spawned.stream().filter(e -> e instanceof Goblin).count());
        assertEquals(2, spawned.stream().filter(e -> e instanceof Troll).count());
        assertEquals(1, spawned.stream().filter(e -> e instanceof Dementor).count());
        assertTrue(waves.allWavesCleared());
    }

    @Test
    void bossComesLast() {
        WaveManager waves = new WaveManager(new int[][] {{4, 1, 1, 1}}, new Random(2));
        List<Enemy> spawned = spawnAll(waves, 30);
        assertInstanceOf(DarkWizardBoss.class, spawned.get(spawned.size() - 1));
    }

    @Test
    void standardGameHasFiveWavesWithBossInLast() {
        WaveManager waves = WaveManager.standard(new Random(3));
        assertEquals(5, waves.totalWaves());
        List<Enemy> spawned = spawnAll(waves, 300);
        assertEquals(1, spawned.stream().filter(e -> e instanceof DarkWizardBoss).count());
        assertEquals(5, waves.currentWave());
    }

    @Test
    void nextWaveWaitsUntilFieldIsClear() {
        WaveManager waves = new WaveManager(new int[][] {{1, 0, 0, 0}, {1, 0, 0, 0}}, new Random(4));
        for (int i = 0; i < 200; i++) {
            waves.update(0.05, false, map); // ยังมีศัตรูอยู่ตลอด
        }
        assertEquals(1, waves.currentWave());
        assertFalse(waves.allWavesCleared());
    }

    @Test
    void rejectsTableWithWrongWidth() {
        assertThrows(IllegalArgumentException.class, () -> new WaveManager(new int[][] {{1, 2, 3}}, new Random()));
    }
}
