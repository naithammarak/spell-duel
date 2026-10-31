package spellduel.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import spellduel.typing.Tier;

/**
 * วาดฉากหลังและข้อมูลบนจอ: HP bar, cast bar ของบอท, ช่องพิมพ์, การ์ด 3 ใบ
 * อ่านข้อมูลอย่างเดียว ไม่แก้อะไรในเกม
 */
final class Hud {

    private static final Font LABEL = new Font(Font.DIALOG, Font.BOLD, 16);
    private static final Font SMALL = new Font(Font.DIALOG, Font.PLAIN, 13);
    private static final Font WORD = new Font(Font.MONOSPACED, Font.BOLD, 20);
    private static final Font TYPED = new Font(Font.MONOSPACED, Font.BOLD, 24);

    private static final int CARD_W = 280;
    private static final int CARD_H = 112;
    private static final int CARD_GAP = 20;
    private static final int CARD_Y = 474;

    private Hud() {
    }

    static Color tierColor(Tier tier) {
        return switch (tier) {
            case PERFECT -> new Color(255, 215, 90);
            case GREAT -> new Color(120, 230, 120);
            case GOOD -> Color.WHITE;
            case WEAK -> new Color(255, 160, 80);
            case FIZZLE -> Color.GRAY;
        };
    }

    /** คำอธิบายสั้น ๆ ใต้ชื่อหน้าที่บนการ์ด */
    private static String hint(SpellRole role) {
        return switch (role) {
            case ATTACK -> "ชนะ Interrupt · แพ้ Shield";
            case SHIELD -> "ชนะ Attack (สะท้อน) · แพ้ Interrupt";
            case INTERRUPT -> "ชนะ Shield (ทำลายโล่) · แพ้ Attack";
        };
    }

    static void drawArena(Graphics2D g) {
        int ground = (int) GameWorld.GROUND_Y;
        g.setPaint(new GradientPaint(0, 0, new Color(20, 12, 45), 0, ground, new Color(70, 40, 100)));
        g.fillRect(0, 0, GameWorld.WIDTH, ground);
        g.setColor(new Color(255, 255, 255, 110));
        for (int i = 0; i < 50; i++) { // ดาว (ตำแหน่งคงที่)
            g.fillRect((i * 97) % GameWorld.WIDTH, 60 + (i * 53) % (ground - 160), 2, 2);
        }
        g.setColor(new Color(240, 235, 200));
        g.fillOval(GameWorld.WIDTH / 2 - 40, 80, 80, 80); // พระจันทร์
        g.setColor(new Color(45, 38, 55));
        g.fillRect(0, ground, GameWorld.WIDTH, GameWorld.HEIGHT - ground);
        g.setColor(new Color(90, 75, 105));
        g.fillRect(0, ground, GameWorld.WIDTH, 4);
    }

    static void drawBars(Graphics2D g, Duelist player, Duelist bot, int botLevel) {
        drawHp(g, player, 24, false);
        drawHp(g, bot, GameWorld.WIDTH - 24 - 380, true);
        g.setFont(LABEL);
        g.setColor(new Color(220, 210, 255));
        centered(g, "บอท " + (botLevel + 1) + "/" + GameWorld.BOT_COUNT, GameWorld.WIDTH / 2, 40);
    }

    /** เลขรอบ + แถบเวลาที่เหลือ ใต้กลางจอ */
    static void drawRound(Graphics2D g, int round, double timeLeft, boolean revealing) {
        int w = 110;
        int x = (GameWorld.WIDTH - w) / 2;
        g.setFont(SMALL);
        g.setColor(Color.WHITE);
        centered(g, revealing ? "เปิดไพ่!" : "รอบ " + round, GameWorld.WIDTH / 2, 60);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x, 66, w, 8);
        g.setColor(timeLeft > 0.3 ? new Color(120, 230, 120) : new Color(255, 90, 90));
        g.fillRect(x, 66, (int) (w * timeLeft), 8);
    }

    private static void drawHp(Graphics2D g, Duelist d, int x, boolean rightToLeft) {
        int w = 380;
        int h = 20;
        int y = 30;
        double frac = (double) d.hp() / d.maxHp();
        int fill = (int) (w * frac);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x, y, w, h);
        g.setColor(frac > 0.3 ? new Color(80, 200, 100) : new Color(220, 70, 60));
        g.fillRect(rightToLeft ? x + w - fill : x, y, fill, h);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, w, h);
        g.setFont(LABEL);
        String label = d.name() + "  " + d.hp() + "/" + d.maxHp();
        int lw = g.getFontMetrics().stringWidth(label);
        g.drawString(label, rightToLeft ? x + w - lw : x, y - 6);
        if (d.hasShield()) {
            g.setFont(SMALL);
            g.setColor(d.shield().isPerfect() ? tierColor(Tier.PERFECT) : SpellRole.SHIELD.color());
            String s = "โล่ " + d.shield().tier().label();
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, rightToLeft ? x + w - sw : x, y + h + 16);
        }
    }

    /** cast bar เหนือหัวบอท: ซ่อนหน้าที่ไว้จนถึงครึ่งแถบ */
    static void drawCastBar(Graphics2D g, BotWizard bot) {
        int w = 240;
        int h = 16;
        int cx = (int) bot.x();
        int y = (int) GameWorld.GROUND_Y - 200;
        Cast cast = bot.cast();
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(cx - w / 2, y, w, h);
        if (cast != null && !cast.isRevealed()) {
            g.setColor(Color.GRAY);
            g.fillRect(cx - w / 2, y, (int) (w * cast.progress()), h);
            g.setFont(LABEL);
            g.setColor(Color.WHITE);
            centered(g, cast.isDone() ? "ล็อกแล้ว" : "???", cx, y - 8);
        } else if (cast != null) {
            Color c = cast.role().color();
            g.setColor(c);
            g.fillRect(cx - w / 2, y, (int) (w * cast.progress()), h);
            g.setFont(LABEL);
            String title = cast.role().label().toUpperCase() + (cast.isFeinted() ? "  ?!" : "");
            centered(g, title, cx, y - 26);
            g.setFont(SMALL);
            g.setColor(Color.WHITE);
            double left = cast.duration() * (1 - cast.progress());
            centered(g, "\"" + cast.word() + "\"  " + String.format("%.1f", left) + "s", cx, y - 8);
        } else {
            g.setFont(SMALL);
            g.setColor(Color.LIGHT_GRAY);
            centered(g, "...", cx, y - 8);
        }
        g.setColor(Color.WHITE);
        g.drawRect(cx - w / 2, y, w, h);
    }

    /** ช่องพิมพ์ของผู้เล่น */
    static void drawInput(Graphics2D g, String typed, String locked, boolean thaiWarning) {
        int w = 480;
        int h = 40;
        int x = (GameWorld.WIDTH - w) / 2;
        int y = (int) GameWorld.GROUND_Y + 18;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(160, 140, 220));
        g.drawRoundRect(x, y, w, h, 10, 10);
        if (locked != null) {
            g.setFont(LABEL);
            g.setColor(new Color(255, 215, 90));
            centered(g, locked, GameWorld.WIDTH / 2, y + 26);
        } else if (typed.isEmpty()) {
            g.setFont(SMALL);
            g.setColor(Color.GRAY);
            centered(g, "พิมพ์คาถาจากการ์ด แล้วกด Enter", GameWorld.WIDTH / 2, y + 25);
        } else {
            g.setFont(TYPED);
            g.setColor(Color.WHITE);
            boolean caret = (System.nanoTime() / 400_000_000L) % 2 == 0;
            centered(g, typed + (caret ? "_" : " "), GameWorld.WIDTH / 2, y + 28);
        }
        if (thaiWarning) {
            g.setFont(LABEL);
            g.setColor(new Color(255, 90, 90));
            centered(g, "เปลี่ยนเป็นแป้นภาษาอังกฤษก่อนนะ", GameWorld.WIDTH / 2, y - 8);
        }
    }

    /** การ์ด 3 ใบ: ใบที่ผู้เล่นน่าจะกำลังพิมพ์จะมีกรอบหนา และระบายสีตัวที่พิมพ์ถูก/ผิด */
    static void drawCards(Graphics2D g, Incantation[] hand, String typed, SpellRole guessed) {
        int total = hand.length * CARD_W + (hand.length - 1) * CARD_GAP;
        int x = (GameWorld.WIDTH - total) / 2;
        for (Incantation card : hand) {
            boolean active = card.role() == guessed;
            drawCard(g, card, x, active ? typed : "", active);
            x += CARD_W + CARD_GAP;
        }
    }

    private static void drawCard(Graphics2D g, Incantation card, int x, String typed, boolean active) {
        Color c = card.role().color();
        g.setColor(new Color(25, 20, 40, 230));
        g.fillRoundRect(x, CARD_Y, CARD_W, CARD_H, 14, 14);
        g.setColor(active ? c : c.darker().darker());
        g.setStroke(new BasicStroke(active ? 4 : 2));
        g.drawRoundRect(x, CARD_Y, CARD_W, CARD_H, 14, 14);
        g.setStroke(new BasicStroke(1));

        g.setFont(LABEL);
        g.setColor(c);
        g.drawString(card.role().label().toUpperCase(), x + 14, CARD_Y + 24);
        g.setFont(SMALL);
        g.setColor(new Color(190, 185, 210));
        g.drawString(hint(card.role()), x + 14, CARD_Y + 44);

        // คำที่ต้องพิมพ์ ทีละตัว: เขียว = พิมพ์ถูก, แดง = พิมพ์ผิด, ขาว = ยังไม่พิมพ์
        g.setFont(WORD);
        FontMetrics fm = g.getFontMetrics();
        String word = card.text();
        int wx = x + (CARD_W - fm.stringWidth(word)) / 2;
        int wy = CARD_Y + 84;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (i < typed.length()) {
                g.setColor(typed.charAt(i) == ch ? new Color(120, 230, 120) : new Color(255, 90, 90));
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString(String.valueOf(ch), wx, wy);
            wx += fm.charWidth(ch);
        }
    }

    private static void centered(Graphics2D g, String text, int cx, int y) {
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - w / 2, y);
    }
}
