package spellduel.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** หน้าเมนู: เริ่มเกม / วิธีเล่น / ออก (JButton + ActionListener) */
public class MenuPanel extends JPanel {

    static final String HOW_TO_PLAY = """
            ดวลเวท 1 ต่อ 1 กับบอท 3 ตัว ชนะครบ = จบเกม

            แต่ละรอบมีเวลา 8 วินาที คุณกับบอทพิมพ์พร้อมกัน แล้วเปิดไพ่พร้อมกัน
            บอทล็อกแล้วคุณเหลือเวลาอีก 1 วินาที · ล็อกก่อนบอทเปิดไพ่ = แรง ×1.5
            พิมพ์คำบนการ์ดใบไหนแล้วกด Enter = ล็อกใบนั้น (ใช้แป้นภาษาอังกฤษ)
            ล็อกไม่ทันเวลา = รอบนั้นไม่ได้ทำอะไร

              ATTACK     ชนะ Interrupt · เจอ Attack โดนทั้งคู่
              SHIELD     ชนะ Attack (สะท้อนกลับใส่คนตี) · ยิ่งแม่นยิ่งแรง
              INTERRUPT  ชนะ Shield (ทำลายโล่และดาเมจเต็ม) · เจอ Interrupt เบาทั้งคู่

            cast bar ของบอทจะเปิดว่าร่ายอะไรเมื่อถึงครึ่งแถบ · Dark Wizard อาจหลอกเปลี่ยน!

            พิมพ์ยิ่งถูกและเร็วยิ่งแรง: PERFECT ×1.25 · Great ×1.0 · Good ×0.6 · Weak ×0.3 · Fizzle
            พิมพ์ไม่ถึงครึ่งคำ = Fizzle (แก้ตัวได้ถ้ายังมีเวลา)

            Backspace = ลบ · Esc = หยุดเกม
            """;

    public MenuPanel(GameWindow window) {
        setLayout(new GridBagLayout());
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(Box.createVerticalStrut(160));
        column.add(makeButton("เริ่มเกม", e -> window.startDuel(0)));
        column.add(Box.createVerticalStrut(14));
        column.add(makeButton("วิธีเล่น", e -> {
            JOptionPane.showMessageDialog(this, HOW_TO_PLAY, "วิธีเล่น", JOptionPane.INFORMATION_MESSAGE);
            window.focusCurrent();
        }));
        column.add(Box.createVerticalStrut(14));
        column.add(makeButton("ออก", e -> System.exit(0)));
        add(column);
    }

    static JButton makeButton(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(80, 50, 150));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 170, 255), 2),
                BorderFactory.createEmptyBorder(10, 50, 10, 50)));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.addActionListener(action);
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, new Color(20, 10, 45), 0, getHeight(), new Color(60, 30, 90)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 120));
        for (int i = 0; i < 60; i++) { // ดาว
            int sx = (i * 97) % Math.max(1, getWidth());
            int sy = (i * 53) % Math.max(1, getHeight() / 2);
            g2.fillRect(sx, sy, 2, 2);
        }
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 64));
        String title = "Spell Duel";
        int w = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(title, (getWidth() - w) / 2 + 4, 154);
        g2.setColor(new Color(255, 215, 90));
        g2.drawString(title, (getWidth() - w) / 2, 150);
        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));
        String sub = "อ่านทางบอท เลือกคาถาให้ถูกจังหวะ แล้วร่ายให้เสร็จก่อน";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.setColor(new Color(220, 210, 255));
        g2.drawString(sub, (getWidth() - sw) / 2, 190);
    }
}
