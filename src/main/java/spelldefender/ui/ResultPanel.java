package spelldefender.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import spelldefender.model.GameStats;
import spelldefender.typing.Tier;

/** หน้าสรุปผล: ชนะ/แพ้ + สถิติความแม่นยำ */
public class ResultPanel extends JPanel {

    private GameStats stats;

    public ResultPanel(GameWindow window) {
        setLayout(new GridBagLayout());
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(Box.createVerticalStrut(360));
        column.add(MenuPanel.makeButton("เล่นอีกครั้ง", e -> window.startGame()));
        column.add(Box.createVerticalStrut(12));
        column.add(MenuPanel.makeButton("กลับเมนู", e -> window.showMenu()));
        add(column);
    }

    public void setStats(GameStats stats) {
        this.stats = stats;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(20, 12, 40));
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (stats == null) {
            return;
        }
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 56));
        g2.setColor(stats.won() ? new Color(255, 215, 90) : new Color(230, 80, 80));
        center(g2, stats.won() ? "ชนะแล้ว!" : "ปราสาทแตก...", 110);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        g2.setColor(Color.WHITE);
        center(g2, "ถึง Wave " + stats.waveReached() + "/" + stats.totalWaves()
                + "   ·   กำจัดศัตรู " + stats.enemiesDefeated() + " ตัว", 160);
        center(g2, String.format("ร่ายทั้งหมด %d ครั้ง   ·   ความแม่นยำเฉลี่ย %.0f%%",
                stats.totalCasts(), stats.averageAccuracy() * 100), 192);

        StringBuilder tiers = new StringBuilder();
        for (Tier t : Tier.values()) {
            tiers.append(t.label()).append(" ").append(stats.count(t)).append("    ");
        }
        g2.setColor(new Color(200, 190, 255));
        center(g2, tiers.toString().trim(), 230);
        g2.setColor(new Color(255, 120, 220));
        center(g2, "Combo สูงสุด x" + stats.bestCombo(), 262);
    }

    private void center(Graphics2D g, String text, int y) {
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (getWidth() - w) / 2, y);
    }
}
