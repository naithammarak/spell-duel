package spelldefender.ui;

import java.awt.CardLayout;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JPanel;
import spelldefender.model.GameStats;

/** หน้าต่างหลัก สลับหน้าจอด้วย CardLayout: Menu → Game → Result */
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

    public GameWindow() {
        super("Spell Defender");
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

    public void startGame() {
        gamePanel.startNewGame();
        show(GAME, gamePanel);
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
