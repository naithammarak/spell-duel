package spellduel.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;
import spellduel.model.Assets;
import spellduel.model.GameWorld;

/**
 * หน้าจอดวล: รับปุ่ม (KeyListener) แล้วส่งต่อให้ GameWorld และสั่ง world วาดตัวเอง
 * ไม่มี logic เกมอยู่ที่นี่
 */
public class GamePanel extends JPanel implements KeyListener {

    private final GameWindow window;
    private final Assets assets = Assets.load();
    private GameWorld world;
    private GameLoop loop;

    public GamePanel(GameWindow window) {
        this.window = window;
        setPreferredSize(new Dimension(GameWorld.WIDTH, GameWorld.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // ไม่งั้น Swing จะเอา Tab ไปใช้ย้าย focus
        addKeyListener(this);
    }

    /** เริ่มดวลใหม่กับบอทเลเวลนี้ (สร้าง world และ thread ใหม่ทุกครั้ง = Retry ได้ state สะอาด) */
    public void startDuel(int botLevel) {
        stopGame();
        world = new GameWorld(botLevel, System.nanoTime());
        loop = new GameLoop(world, this, window::showResult);
        loop.start();
    }

    public void stopGame() {
        if (loop != null) {
            loop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (world == null) {
            return;
        }
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) {
            loop.togglePause();
            repaint();
            return;
        }
        if (loop.isPaused()) {
            if (key == KeyEvent.VK_Q) {
                window.showMenu();
            }
            return;
        }
        if (key == KeyEvent.VK_ENTER) {
            world.castTyped();
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            world.backspace();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (world == null || loop.isPaused()) {
            return;
        }
        char c = e.getKeyChar();
        if (c != '\n' && c != '\b' && c != KeyEvent.VK_ESCAPE) {
            world.typeChar(c);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (world == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        world.draw(g2, assets);
        if (loop.isPaused()) {
            drawPauseOverlay(g2);
        }
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 40));
        drawCentered(g, "หยุดชั่วคราว", getHeight() / 2 - 20);
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        drawCentered(g, "Esc = เล่นต่อ    Q = กลับเมนู", getHeight() / 2 + 25);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (getWidth() - w) / 2, y);
    }
}
