package spelldefender.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/** วาดข้อมูลบนจอ: HP ฐาน, wave, ช่องพิมพ์, แถบ cooldown ของแต่ละคาถา */
final class Hud {

    private static final Font SMALL = new Font(Font.DIALOG, Font.BOLD, 13);
    private static final Font TYPING = new Font(Font.MONOSPACED, Font.BOLD, 24);
    private static final Color PANEL = new Color(15, 10, 30, 200);

    private Hud() {
    }

    static void draw(Graphics2D g, GameWorld world) {
        drawTopBar(g, world);
        drawTypingBox(g, world);
        drawSpellSlots(g, world);
    }

    private static void drawTopBar(Graphics2D g, GameWorld world) {
        g.setColor(PANEL);
        g.fillRect(0, 0, GameWorld.WIDTH, 30);
        g.setFont(SMALL);

        // HP ฐาน
        Base base = world.base();
        g.setColor(Color.WHITE);
        g.drawString("HP ฐาน", 10, 20);
        g.setColor(new Color(60, 0, 0));
        g.fillRect(70, 8, 200, 14);
        g.setColor(new Color(220, 60, 60));
        g.fillRect(70, 8, 200 * base.hp() / Base.MAX_HP, 14);
        if (base.shield() > 0) {
            g.setColor(new Color(120, 200, 255, 200));
            g.fillRect(70, 8, (int) Math.min(200, base.shield() * 200 / Base.MAX_HP), 5);
        }
        g.setColor(Color.WHITE);
        g.drawString(base.hp() + "/" + Base.MAX_HP, 280, 20);

        // wave
        WaveManager waves = world.waves();
        String waveText = "Wave " + Math.max(1, waves.currentWave()) + "/" + waves.totalWaves();
        if (waves.isBetweenWaves()) {
            waveText += String.format("  (เริ่มใน %.0f)", Math.ceil(waves.breakTimeLeft()));
        }
        g.drawString(waveText, 420, 20);

        g.setColor(new Color(200, 200, 220));
        g.drawString("เมาส์เล็ง · คลิก/Tab เลือกเป้า · Esc หยุด", 660, 20);
    }

    private static void drawTypingBox(Graphics2D g, GameWorld world) {
        int w = 420;
        int x = (GameWorld.WIDTH - w) / 2;
        int y = GameWorld.HEIGHT - 100;
        g.setColor(PANEL);
        g.fillRoundRect(x, y, w, 40, 12, 12);
        g.setColor(new Color(180, 150, 255));
        g.drawRoundRect(x, y, w, 40, 12, 12);
        g.setFont(TYPING);
        String typed = world.typedText();
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        g.setColor(Color.WHITE);
        g.drawString("> " + typed + (blink ? "_" : ""), x + 12, y + 28);

        // Combo อยู่ขวาของช่องพิมพ์
        int combo = world.combo();
        if (combo >= 2) {
            int bonus = (int) Math.round((world.comboMultiplier() - 1) * 100);
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            g.setColor(new Color(255, 120, 220));
            g.drawString("Combo x" + combo + "  +" + bonus + "%", x + w + 14, y + 27);
        }
    }

    private static void drawSpellSlots(Graphics2D g, GameWorld world) {
        List<Spell> spells = world.spellBook().spells();
        int slotW = 170;
        int gap = 8;
        int total = spells.size() * slotW + (spells.size() - 1) * gap;
        int x = (GameWorld.WIDTH - total) / 2;
        int y = GameWorld.HEIGHT - 50;
        g.setFont(SMALL);
        for (Spell s : spells) {
            g.setColor(PANEL);
            g.fillRoundRect(x, y, slotW, 40, 10, 10);
            // แถบ cooldown: เต็ม = พร้อมใช้
            double ready = 1 - s.cooldownFraction();
            g.setColor(s.isReady() ? s.color() : s.color().darker().darker());
            g.fillRoundRect(x + 4, y + 28, (int) ((slotW - 8) * ready), 7, 6, 6);
            g.setColor(s.isReady() ? Color.WHITE : Color.GRAY);
            g.drawString(s.name(), x + 8, y + 20);
            x += slotW + gap;
        }
    }
}
