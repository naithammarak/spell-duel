package spellduel.ui;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JPanel;
import spellduel.model.GameStats;
import spellduel.model.GameWorld;

/**
 * หน้าต่างหลัก สลับหน้าจอด้วย CardLayout: Menu → Duel → Result
 * เจ้าของการเล่น 1 รอบแบบเอาตัวรอด: ชนะ → ด่านถัดไปพร้อม HP ที่เหลือ · แพ้ → เริ่มด่าน 1 ใหม่
 * สถิติสูงสุดเก็บใน Preferences ปิดเกมแล้วยังอยู่
 */
public class GameWindow extends JFrame {

    private static final String MENU = "menu";
    private static final String GAME = "game";
    private static final String RESULT = "result";
    private static final String BEST_KEY = "bestLevels";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MenuPanel menuPanel;
    private final GamePanel gamePanel;
    private final ResultPanel resultPanel;
    private Component current;
    private final Preferences prefs = Preferences.userNodeForPackage(GameWindow.class);
    private int botLevel;
    private GameStats lastStats;

    public GameWindow() {
        super("Spell Duel");
        menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel(this);
        resultPanel = new ResultPanel(this);
        root.add(menuPanel, MENU);
        root.add(gamePanel, GAME);
        root.add(resultPanel, RESULT);
        setContentPane(root);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        show(MENU, menuPanel);
    }

    /** เริ่มการเล่นใหม่ตั้งแต่ด่านแรก */
    public void startRun() {
        startDuel(0, GameWorld.PLAYER_MAX_HP);
    }

    public void nextDuel() {
        startDuel(botLevel + 1, lastStats.nextHp());
    }

    private void startDuel(int level, int playerHp) {
        botLevel = level;
        gamePanel.startDuel(level, playerHp);
        show(GAME, gamePanel);
    }

    int bestLevels() {
        return prefs.getInt(BEST_KEY, 0);
    }

    public void showMenu() {
        gamePanel.stopGame();
        show(MENU, menuPanel);
    }

    public void showResult(GameStats stats) {
        lastStats = stats;
        if (stats.levelsCleared() > bestLevels()) {
            prefs.putInt(BEST_KEY, stats.levelsCleared());
        }
        resultPanel.setStats(stats, bestLevels());
        show(RESULT, resultPanel);
    }

    /** คืน focus ให้หน้าที่แสดงอยู่ (เรียกหลังปิด dialog) */
    void focusCurrent() {
        current.requestFocusInWindow();
    }

    private void show(String name, Component panel) {
        cards.show(root, name);
        current = panel;
        // สำคัญ: ไม่งั้น KeyListener ของ GamePanel จะไม่ได้รับปุ่ม
        panel.requestFocusInWindow();
    }
}
