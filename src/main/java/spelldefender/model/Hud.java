package spelldefender.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * วาดข้อมูลบนจอ: HP ฐาน, ทอง, wave, ร้านค้านักเวท, ปุ่มเริ่ม Wave, ช่องพิมพ์, แถบ cooldown
 * Hud เป็นเจ้าของตำแหน่งปุ่ม GameWorld จึงถาม Hud ว่าคลิกโดนปุ่มไหน (shopChipAt / startButtonContains)
 */
final class Hud {

    private static final Font SMALL = new Font(Font.DIALOG, Font.BOLD, 13);
    private static final Font TYPING = new Font(Font.MONOSPACED, Font.BOLD, 24);
    private static final Color PANEL = new Color(15, 10, 30, 200);

    // ร้านค้า: ปุ่มเรียงแนวนอนใต้แถบบน
    private static final int CHIP_X = 8;
    private static final int CHIP_Y = 34;
    private static final int CHIP_W = 150;
    private static final int CHIP_H = 30;
    private static final int CHIP_GAP = 6;
    // ปุ่มเริ่ม Wave
    private static final int START_X = 680;
    private static final int START_Y = 34;
    private static final int START_W = 272;
    private static final int START_H = 30;

    private Hud() {
    }

    /** ปุ่มร้านค้าที่ (x, y) คืน index ของ AllyType หรือ -1 */
    static int shopChipAt(double x, double y) {
        for (int i = 0; i < AllyType.values().length; i++) {
            int cx = CHIP_X + i * (CHIP_W + CHIP_GAP);
            if (x >= cx && x <= cx + CHIP_W && y >= CHIP_Y && y <= CHIP_Y + CHIP_H) {
                return i;
            }
        }
        return -1;
    }

    static boolean startButtonContains(double x, double y) {
        return x >= START_X && x <= START_X + START_W && y >= START_Y && y <= START_Y + START_H;
    }

    static void draw(Graphics2D g, GameWorld world) {
        drawTopBar(g, world);
        drawShop(g, world);
        drawStartButton(g, world);
        drawTypingBox(g, world);
        drawSpellSlots(g, world);
    }

    private static void drawTopBar(Graphics2D g, GameWorld world) {
        g.setColor(PANEL);
        g.fillRect(0, 0, GameWorld.WIDTH, 30);
        g.setFont(SMALL);

        Base base = world.base();
        g.setColor(Color.WHITE);
        g.drawString("HP ปราสาท", 10, 20);
        g.setColor(new Color(60, 0, 0));
        g.fillRect(90, 8, 180, 14);
        g.setColor(new Color(220, 60, 60));
        g.fillRect(90, 8, 180 * base.hp() / Base.MAX_HP, 14);
        if (base.shield() > 0) {
            g.setColor(new Color(120, 200, 255, 200));
            g.fillRect(90, 8, (int) Math.min(180, base.shield() * 180 / Base.MAX_HP), 5);
        }
        g.setColor(Color.WHITE);
        g.drawString(base.hp() + "/" + Base.MAX_HP, 278, 20);

        g.setColor(new Color(255, 215, 0));
        g.drawString("ทอง " + world.gold(), 370, 20);

        WaveManager waves = world.waves();
        g.setColor(Color.WHITE);
        g.drawString("Wave " + Math.max(1, waves.currentWave()) + "/" + waves.totalWaves()
                + "   ศัตรูบนสนาม " + world.enemies().size(), 470, 20);

        g.setColor(new Color(200, 200, 220));
        g.drawString("1-4 เลือกนักเวท · คลิกช่องเพื่อวาง · Esc หยุด", 700, 20);
    }

    private static void drawShop(Graphics2D g, GameWorld world) {
        AllyType[] types = AllyType.values();
        g.setFont(SMALL);
        for (int i = 0; i < types.length; i++) {
            AllyType t = types[i];
            int x = CHIP_X + i * (CHIP_W + CHIP_GAP);
            boolean selected = world.selectedAlly() == t;
            boolean affordable = world.gold() >= t.cost();
            g.setColor(selected ? new Color(110, 80, 30, 230) : PANEL);
            g.fillRoundRect(x, CHIP_Y, CHIP_W, CHIP_H, 8, 8);
            g.setColor(selected ? new Color(255, 215, 0) : new Color(140, 120, 200));
            g.drawRoundRect(x, CHIP_Y, CHIP_W, CHIP_H, 8, 8);
            g.setColor(affordable ? Color.WHITE : Color.GRAY);
            g.drawString("[" + (i + 1) + "] " + t.label(), x + 8, CHIP_Y + 20);
            g.setColor(affordable ? new Color(255, 215, 0) : Color.GRAY);
            String cost = String.valueOf(t.cost());
            g.drawString(cost, x + CHIP_W - 8 - g.getFontMetrics().stringWidth(cost), CHIP_Y + 20);
        }
    }

    private static void drawStartButton(Graphics2D g, GameWorld world) {
        WaveManager waves = world.waves();
        if (!waves.canStartNextWave()) {
            return;
        }
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        g.setColor(blink ? new Color(40, 120, 40, 230) : new Color(30, 90, 30, 230));
        g.fillRoundRect(START_X, START_Y, START_W, START_H, 8, 8);
        g.setColor(new Color(160, 255, 160));
        g.drawRoundRect(START_X, START_Y, START_W, START_H, 8, 8);
        g.setFont(SMALL);
        g.setColor(Color.WHITE);
        g.drawString("▶ เริ่ม Wave " + (waves.currentWave() + 1) + "  (Enter หรือคลิก)", START_X + 14, START_Y + 20);
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
            double ready = 1 - s.cooldownFraction();
            g.setColor(s.isReady() ? s.color() : s.color().darker().darker());
            g.fillRoundRect(x + 4, y + 28, (int) ((slotW - 8) * ready), 7, 6, 6);
            g.setColor(s.isReady() ? Color.WHITE : Color.GRAY);
            g.drawString(s.name(), x + 8, y + 20);
            x += slotW + gap;
        }
    }
}
