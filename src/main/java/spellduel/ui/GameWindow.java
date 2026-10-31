package spellduel.ui;

import java.awt.CardLayout;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JPanel;
import spellduel.model.GameStats;

/**
 * หน้าต่างหลัก สลับหน้าจอด้วย CardLayout: Menu → Duel → Result
 * จำไว้ว่ากำลังดวลกับบอทเลเวลไหน: ชนะ → Next (level + 1) · แพ้ → Retry (เลเวลเดิม)
 */
public class GameWindow extends JFrame {

    private static final String MENU = "menu";
    private static final String GAME = "game";
    private static final String RESULT = "result";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MenuPanel menuPanel;
    private final GamePanel gamePanel;
    private final ResultPanel resultPanel;
    private Component current;
    private int botLevel;

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

    public void startDuel(int level) {
        botLevel = level;
        gamePanel.startDuel(level);
        show(GAME, gamePanel);
    }

    public void nextDuel() {
        startDuel(botLevel + 1);
    }

    public void retryDuel() {
        startDuel(botLevel);
    }

    public void showMenu() {
        gamePanel.stopGame();
        show(MENU, menuPanel);
    }

    public void showResult(GameStats stats) {
        resultPanel.setStats(stats);
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
