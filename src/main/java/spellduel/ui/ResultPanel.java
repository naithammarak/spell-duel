package spellduel.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import spellduel.model.GameStats;
import spellduel.typing.Tier;

/** หน้าสรุปผล: ชนะ → ด่านถัดไป · แพ้ → เริ่มใหม่ตั้งแต่ด่าน 1 */
public class ResultPanel extends JPanel {

    private final JButton nextButton;
    private final JButton retryButton;
    private GameStats stats;
    private int best;

    public ResultPanel(GameWindow window) {
        setLayout(new GridBagLayout());
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        nextButton = MenuPanel.makeButton("ด่านถัดไป", e -> window.nextDuel());
        retryButton = MenuPanel.makeButton("เริ่มใหม่ด่าน 1", e -> window.startRun());
        column.add(Box.createVerticalStrut(320));
        column.add(nextButton);
        column.add(retryButton);
        column.add(Box.createVerticalStrut(12));
        column.add(MenuPanel.makeButton("กลับเมนู", e -> window.showMenu()));
        add(column);
    }

    public void setStats(GameStats stats, int best) {
        this.stats = stats;
        this.best = best;
        nextButton.setText("ด่านถัดไป (HP " + stats.nextHp() + ")");
        nextButton.setVisible(stats.won());
        retryButton.setVisible(!stats.won());
        revalidate();
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
        String title = (stats.won() ? "ชนะ " : "แพ้ ") + stats.botName() + (stats.won() ? "!" : "...");
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 52));
        g2.setColor(stats.won() ? new Color(255, 215, 90) : new Color(230, 80, 80));
        center(g2, title, 120);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        g2.setColor(Color.WHITE);
        center(g2, String.format("HP เหลือ %d   ·   ใช้เวลา %.0f วินาที", stats.hpLeft(), stats.seconds()), 175);
        center(g2, String.format("ร่ายทั้งหมด %d ครั้ง   ·   ความแม่นยำเฉลี่ย %.0f%%",
                stats.totalCasts(), stats.averageAccuracy() * 100), 207);

        StringBuilder tiers = new StringBuilder();
        for (Tier t : Tier.values()) {
            tiers.append(t.label()).append(" ").append(stats.count(t)).append("    ");
        }
        g2.setColor(new Color(200, 190, 255));
        center(g2, tiers.toString().trim(), 245);
        g2.setColor(new Color(255, 215, 90));
        center(g2, String.format("ผ่านแล้ว %d ด่าน   ·   สถิติสูงสุด %d ด่าน", stats.levelsCleared(), best), 283);
    }

    private void center(Graphics2D g, String text, int y) {
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (getWidth() - w) / 2, y);
    }
}
