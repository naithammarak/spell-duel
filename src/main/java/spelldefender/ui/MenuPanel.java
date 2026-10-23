package spelldefender.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** หน้าเมนู: เริ่มเกม / วิธีเล่น / ออก (JButton + ActionListener) */
public class MenuPanel extends JPanel {

    static final String HOW_TO_PLAY = """
            พิมพ์ชื่อคาถาแล้วกด Enter เพื่อร่าย (ใช้แป้นภาษาอังกฤษ)
            พิมพ์ยิ่งถูกและเร็ว คาถายิ่งแรง:
              PERFECT ×1.25 · Great ×1.0 · Good ×0.6 · Weak ×0.3 · Fizzle ×0

            คาถา
              stupefy           ดาเมจน้อย + ทำให้มึน (ล็อกเป้า)
              incendio          ลูกไฟระเบิดโดนหลายตัว (ยิงตามทิศไม้)
              protego           โล่ป้องกันฐาน
              expelliarmus      ดาเมจกลาง + ผลักถอย (ล็อกเป้า)
              expecto patronum  แรงมาก ใช้กับ Dementor (ล็อกเป้า)

            Combo: ได้ PERFECT ติดกัน ดาเมจเพิ่มครั้งละ 10% (สูงสุด +50%)

            ควบคุม
              ขยับเมาส์ = เล็งไม้กายสิทธิ์ (ใช้กับ incendio)
              คลิกศัตรู หรือ Tab = เลือกเป้า (ใช้กับคาถาล็อกเป้า)
              Backspace = ลบ     Esc = หยุดเกม

            Dementor (ผีดำ) แทบไม่โดนคาถาอื่น ต้องใช้ expecto patronum
            รอดครบ 5 wave และชนะบอส = ชนะ
            """;

    public MenuPanel(GameWindow window) {
        setLayout(new GridBagLayout());
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(Box.createVerticalStrut(160));
        column.add(makeButton("เริ่มเกม", e -> window.startGame()));
        column.add(Box.createVerticalStrut(14));
        column.add(makeButton("วิธีเล่น", e -> {
            JOptionPane.showMessageDialog(this, HOW_TO_PLAY, "วิธีเล่น", JOptionPane.INFORMATION_MESSAGE);
            window.focusCurrent();
        }));
        column.add(Box.createVerticalStrut(14));
        column.add(makeButton("ออก", e -> System.exit(0)));
        add(column);
    }

    static JButton makeButton(String text, java.awt.event.ActionListener action) {
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
        // ดาว
        g2.setColor(new Color(255, 255, 255, 120));
        for (int i = 0; i < 60; i++) {
            int sx = (i * 97) % Math.max(1, getWidth());
            int sy = (i * 53) % Math.max(1, getHeight() / 2);
            g2.fillRect(sx, sy, 2, 2);
        }
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 64));
        String title = "Spell Defender";
        int w = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(title, (getWidth() - w) / 2 + 4, 154);
        g2.setColor(new Color(255, 215, 90));
        g2.drawString(title, (getWidth() - w) / 2, 150);
        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));
        String sub = "พิมพ์คาถาให้ถูกและเร็ว เพื่อปกป้องปราสาท";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.setColor(new Color(220, 210, 255));
        g2.drawString(sub, (getWidth() - sw) / 2, 190);
    }
}
