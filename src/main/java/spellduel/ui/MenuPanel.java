package spellduel.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import spellduel.model.Assets;
import spellduel.model.GameWorld;

/** หน้าเมนู: เริ่มเกม / วิธีเล่น / ออก (JButton + ActionListener) */
public class MenuPanel extends JPanel {

    static final String HOW_TO_PLAY = """
            ดวลเวท 1 ต่อ 1 ไต่ด่านไปเรื่อย ๆ บอทเก่งขึ้นทุกด่าน แพ้ = เริ่มด่าน 1 ใหม่
            ชนะแล้ว HP ที่เหลือติดตัวไปด่านถัดไป และฟื้นอีก 75

            ทุกรอบนับ 3-2-1 สั้น ๆ แล้วคุณกับบอทพิมพ์พร้อมกัน ใช้คาถาได้ฝ่ายละ 1 ครั้ง
            พิมพ์คำบนการ์ดแล้วกด Enter = คาถาทำงานทันที (ใช้แป้นภาษาอังกฤษ)
            บอทร่ายเสร็จแล้วคุณเหลือเวลาอีก 1 วินาที · ไม่ทัน = รอบนั้นไม่ได้ทำอะไร

              ATTACK     โดนตัว = ดาเมจเต็ม และ Great ขึ้นไปขัดคาถาที่ยังร่ายไม่เสร็จ · เจอโล่ = สะท้อน
              SHIELD     อยู่จนจบรอบ ต้องขึ้นก่อน Attack ถึงตัว · ยิ่งแม่นยิ่งสะท้อนแรง
              INTERRUPT  เจอโล่ = ทำลายโล่และดาเมจเต็ม · ไม่เจอโล่ = ดาเมจเบา

            cast bar ของบอทจะเปิดว่าร่ายอะไรเมื่อถึง 20% ของแถบ · Dark Wizard อาจหลอกเปลี่ยน!

            พิมพ์ยิ่งถูกและเร็วยิ่งแรง: PERFECT ×1.25 · Great ×1.0 · Good ×0.6 · Weak ×0.3 · Fizzle
            พิมพ์ไม่ถึงครึ่งคำ = Fizzle (แก้ตัวได้ถ้ายังมีเวลา)

            Backspace = ลบ · Esc = หยุดเกม
            """;

    private static final Color GOLD = new Color(255, 215, 90);
    private static final int TICK_MS = 33;
    private static final Dimension BUTTON_SIZE = new Dimension(220, 58);

    private final GameWindow window;
    private final GameWorld scene = new GameWorld(0, GameWorld.PLAYER_MAX_HP, 1L); // ใช้แค่เป็นฉากหลัง ไม่ได้ดวลจริง
    private final Assets assets = Assets.none();
    private BufferedImage frame;

    public MenuPanel(GameWindow window) {
        this.window = window;
        setLayout(new BorderLayout());

        JButton start = makeButton("เริ่มเกม", e -> window.startRun());
        start.setBackground(new Color(120, 60, 200));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 44, 0));
        row.add(sized(start));
        row.add(sized(makeButton("วิธีเล่น", e -> {
            JOptionPane.showMessageDialog(this, HOW_TO_PLAY, "วิธีเล่น", JOptionPane.INFORMATION_MESSAGE);
            window.focusCurrent();
        })));
        row.add(sized(makeButton("ออก", e -> System.exit(0))));
        add(row, BorderLayout.SOUTH);

        // ฉากขยับเฉพาะตอนหน้านี้แสดงอยู่ ใช้ Swing Timer จึงทำงานบน EDT ไม่ต้องมี thread เพิ่ม
        new Timer(TICK_MS, e -> {
            if (isShowing()) {
                scene.idle(TICK_MS / 1000.0);
                repaint();
            }
        }).start();
    }

    private static JButton sized(JButton b) {
        b.setPreferredSize(BUTTON_SIZE);
        return b;
    }

    static JButton makeButton(String text, ActionListener action) {
        JButton b = new MagicButton(text);
        b.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(44, 30, 86));
        b.setBorder(BorderFactory.createEmptyBorder(12, 50, 12, 50));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.addActionListener(action);
        return b;
    }

    /** ปุ่มมุมมนขอบทอง ชี้แล้วสว่างขึ้น วาดพื้นปุ่มเอง แล้วให้ JButton วาดตัวหนังสือ */
    private static class MagicButton extends JButton {

        MagicButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            Shape shape = new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, 18, 18);
            Color fill = hover ? getBackground().brighter() : getBackground();
            g2.setPaint(new GradientPaint(0, 0, fill, 0, getHeight(), fill.darker()));
            g2.fill(shape);
            g2.setColor(hover ? Color.WHITE : GOLD);
            g2.setStroke(new BasicStroke(2));
            g2.draw(shape);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (frame == null) {
            frame = new BufferedImage(GameWorld.WIDTH, GameWorld.HEIGHT, BufferedImage.TYPE_INT_RGB);
        }
        // วาดลงรูปก่อนแล้วค่อยแปะขึ้นจอ เหมือน GamePanel เพราะไล่สีตรงลงจอช้า
        Graphics2D g2 = frame.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        scene.drawMenuScene(g2, assets);
        // เงาด้านบนและล่าง ให้ตัวหนังสือกับปุ่มอ่านง่าย
        g2.setPaint(new GradientPaint(0, 0, new Color(8, 6, 24, 215), 0, 250, new Color(8, 6, 24, 0)));
        g2.fillRect(0, 0, GameWorld.WIDTH, 250);
        g2.setPaint(new GradientPaint(0, 420, new Color(8, 6, 24, 0), 0, GameWorld.HEIGHT, new Color(8, 6, 24, 225)));
        g2.fillRect(0, 420, GameWorld.WIDTH, GameWorld.HEIGHT - 420);
        drawTitle(g2);
        g2.dispose();
        g.drawImage(frame, 0, 0, null);
    }

    private void drawTitle(Graphics2D g) {
        Font font = new Font(Font.SERIF, Font.BOLD, 92);
        FontRenderContext context = g.getFontRenderContext();
        String title = "Spell Duel";
        int x = (GameWorld.WIDTH - (int) font.getStringBounds(title, context).getWidth()) / 2;
        Shape outline = font.createGlyphVector(context, title).getOutline(x, 122);
        g.setColor(new Color(0, 0, 0, 170));
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);
        g.setPaint(new GradientPaint(0, 50, new Color(255, 245, 190), 0, 125, new Color(235, 150, 40)));
        g.fill(outline);

        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));
        centerText(g, "อ่านทางบอท เลือกคาถาให้ถูกจังหวะ แล้วร่ายให้เสร็จก่อน", 160, new Color(230, 222, 255));
        if (window.bestLevels() > 0) {
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
            centerText(g, "สถิติสูงสุด " + window.bestLevels() + " ด่าน", 192, GOLD);
        }
    }

    private void centerText(Graphics2D g, String text, int y, Color color) {
        int x = (GameWorld.WIDTH - g.getFontMetrics().stringWidth(text)) / 2;
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(text, x + 1, y + 2);
        g.setColor(color);
        g.drawString(text, x, y);
    }
}
