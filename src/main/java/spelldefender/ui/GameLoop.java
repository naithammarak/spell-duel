package spelldefender.ui;

import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import spelldefender.model.GameStats;
import spelldefender.model.GameWorld;

/**
 * Game thread ตัวเดียวของเกม วนประมาณ 60 รอบต่อวินาที:
 *   world.update(dt) → panel.repaint() → พักจนครบเฟรม
 *
 * Pause ใช้ volatile boolean (ห้ามใช้ Thread.suspend ที่เลิกใช้แล้ว)
 * volatile = ให้ thread อื่นเห็นค่าล่าสุดเสมอ
 */
public class GameLoop implements Runnable {

    private static final int FPS = 60;
    private static final long FRAME_NANOS = 1_000_000_000L / FPS;
    private static final double MAX_DT = 0.05; // กันเกมกระโดดถ้าเครื่องค้าง

    private final GameWorld world;
    private final GamePanel panel;
    private final Consumer<GameStats> onGameOver;
    private volatile boolean running;
    private volatile boolean paused;

    public GameLoop(GameWorld world, GamePanel panel, Consumer<GameStats> onGameOver) {
        this.world = world;
        this.panel = panel;
        this.onGameOver = onGameOver;
    }

    public void start() {
        running = true;
        Thread thread = new Thread(this, "game-loop");
        thread.setDaemon(true); // ปิดหน้าต่างแล้ว thread ตายตาม
        thread.start();
    }

    public void stop() {
        running = false;
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            double dt = Math.min((now - last) / 1e9, MAX_DT);
            last = now; // เดินต่อแม้ตอน pause จะได้ไม่กระโดดตอนกลับมาเล่น

            try {
                if (!paused) {
                    world.update(dt);
                }
                panel.repaint();
                if (world.status() != GameWorld.Status.PLAYING) {
                    running = false;
                    GameStats stats = world.stats();
                    SwingUtilities.invokeLater(() -> onGameOver.accept(stats));
                }
            } catch (RuntimeException e) {
                e.printStackTrace(); // ไม่ให้ thread ตายเงียบ ๆ
            }

            long sleepMs = (FRAME_NANOS - (System.nanoTime() - now)) / 1_000_000;
            try {
                Thread.sleep(Math.max(1, sleepMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
