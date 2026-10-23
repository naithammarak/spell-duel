package spelldefender.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import spelldefender.model.Assets;
import spelldefender.model.GameWorld;

/**
 * หน้าจอเล่นเกม: รับปุ่ม (KeyListener) แล้วส่งต่อให้ GameWorld และสั่ง world วาดตัวเอง
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

        // เมาส์: ขยับ = หันไม้กายสิทธิ์, คลิก = ปุ่ม/วางนักเวท/ล็อกเป้า
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (world != null && !loop.isPaused()) {
                    world.aimWandAt(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow(); // คลิกแล้วคีย์บอร์ดต้องยังพิมพ์ได้
                if (world != null && !loop.isPaused()) {
                    world.click(e.getX(), e.getY());
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** เริ่มเกมใหม่ (สร้าง world และ thread ใหม่ทุกครั้ง) */
    public void startNewGame() {
        stopGame();
        world = new GameWorld(System.nanoTime());
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
                stopGame();
                window.showMenu();
            }
            return;
        }
        switch (key) {
            case KeyEvent.VK_ENTER -> world.castTyped();
            case KeyEvent.VK_BACK_SPACE -> world.backspace();
            case KeyEvent.VK_TAB -> world.cycleTarget();
            case KeyEvent.VK_LEFT -> world.turnWand(-1);
            case KeyEvent.VK_RIGHT -> world.turnWand(+1);
            case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> world.selectAlly(0);
            case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> world.selectAlly(1);
            case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> world.selectAlly(2);
            case KeyEvent.VK_4, KeyEvent.VK_NUMPAD4 -> world.selectAlly(3);
            default -> {
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (world == null || loop.isPaused()) {
            return;
        }
        char c = e.getKeyChar();
        if (c != '\n' && c != '\b' && c != '\t' && c != KeyEvent.VK_ESCAPE) {
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
