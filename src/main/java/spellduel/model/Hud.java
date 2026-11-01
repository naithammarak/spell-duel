package spellduel.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;
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

    private static final Color PANEL = new Color(14, 10, 28, 215);
    private static final Color FRAME = new Color(150, 130, 200);

    /** ฉากไม่เปลี่ยนตลอดเกม จึงวาดครั้งเดียวแล้วเก็บเป็นรูป ไม่ต้องวาดใหม่ 60 ครั้งต่อวินาที */
    private static BufferedImage arena;

    private static final int STAND_ROWS = 6;
    private static final int ROW_HEIGHT = 32;
    private static final int LIVE_ROW = STAND_ROWS - 2; // แถวหน้า 2 แถวขยับได้ จึงไม่อยู่ในรูปที่เก็บไว้
    private static final int SEAT_GAP = 30;
    private static final Color[] CROWD_CLOTHES = {
            new Color(196, 70, 80), new Color(70, 120, 200), new Color(220, 170, 70),
            new Color(80, 160, 110), new Color(160, 90, 180), new Color(205, 205, 215),
    };

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
            case ATTACK -> "โดนตัว = ดาเมจเต็ม + ขัด · เจอโล่ = สะท้อน";
            case SHIELD -> "ต้องขึ้นก่อน Attack ถึง · แพ้ Interrupt";
            case INTERRUPT -> "เจอโล่ = ทำลาย + เต็ม · ไม่เจอ = เบา";
        };
    }

    /** แสงฟุ้ง: สว่างตรงกลางแล้วจางหายที่ขอบ (ใช้กับลูกเวท ไม้กายสิทธิ์ และพระจันทร์) */
    static void glow(Graphics2D g, double x, double y, double radius, Color c) {
        Color center = new Color(c.getRed(), c.getGreen(), c.getBlue(), 170);
        Color edge = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        g.setPaint(new RadialGradientPaint((float) x, (float) y, (float) radius,
                new float[] {0f, 1f}, new Color[] {center, edge}));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    static void drawArena(Graphics2D g, double time) {
        if (arena == null) {
            arena = new BufferedImage(GameWorld.WIDTH, GameWorld.HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D painter = arena.createGraphics();
            painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintSky(painter);
            paintStands(painter);
            paintWall(painter);
            paintGround(painter);
            painter.dispose();
        }
        g.drawImage(arena, 0, 0, null);
        drawLiveCrowd(g, time);
    }

    private static void paintSky(Graphics2D g) {
        int ground = (int) GameWorld.GROUND_Y;
        g.setPaint(new LinearGradientPaint(0, 0, 0, ground, new float[] {0f, 0.6f, 1f},
                new Color[] {new Color(11, 9, 38), new Color(45, 28, 92), new Color(132, 62, 122)}));
        g.fillRect(0, 0, GameWorld.WIDTH, ground);

        Random stars = new Random(42); // seed คงที่ ดาวจึงอยู่ที่เดิมทุกครั้ง
        for (int i = 0; i < 90; i++) {
            int size = 1 + stars.nextInt(2);
            g.setColor(new Color(255, 255, 255, 70 + stars.nextInt(160)));
            g.fillRect(stars.nextInt(GameWorld.WIDTH), stars.nextInt(ground - 130), size, size);
        }

        int moonX = 250;
        int moonY = 105;
        glow(g, moonX, moonY, 95, new Color(255, 225, 190));
        g.setColor(new Color(245, 235, 205));
        g.fillOval(moonX - 36, moonY - 36, 72, 72);
        g.setColor(new Color(222, 208, 180));
        g.fillOval(moonX - 20, moonY - 16, 15, 15);
        g.fillOval(moonX + 9, moonY - 4, 11, 11);
        g.fillOval(moonX - 8, moonY + 14, 12, 12);
    }

    /** ขอบบนของแถวอัฒจันทร์ ด้านข้างจอสูงกว่ากลางจอ จึงดูเป็นสนามทรงโค้ง */
    private static int rowY(int row, int x) {
        double fromCenter = (x - GameWorld.WIDTH / 2.0) / (GameWorld.WIDTH / 2.0);
        return 168 + row * ROW_HEIGHT - (int) (46 * fromCenter * fromCenter);
    }

    /** ระบายสีตั้งแต่ขอบบนของแถวนี้ลงไปจนถึงพื้น */
    private static void fillFromRow(Graphics2D g, int row, Color color) {
        Path2D.Double band = new Path2D.Double();
        band.moveTo(0, GameWorld.GROUND_Y);
        for (int x = 0; x <= GameWorld.WIDTH; x += 20) {
            band.lineTo(x, rowY(row, x));
        }
        band.lineTo(GameWorld.WIDTH, GameWorld.GROUND_Y);
        band.closePath();
        g.setColor(color);
        g.fill(band);
    }

    private static void paintStands(Graphics2D g) {
        Random seats = new Random(7); // seed คงที่ ผู้ชมจึงนั่งที่เดิมทุกครั้ง
        for (int row = 0; row < LIVE_ROW; row++) {
            int shade = 34 + row * 7;
            fillFromRow(g, row, new Color(shade, shade - 8, shade + 26));
            for (int x = 8 + row * 9; x < GameWorld.WIDTH; x += SEAT_GAP) {
                Color clothes = CROWD_CLOTHES[seats.nextInt(CROWD_CLOTHES.length)];
                if (seats.nextInt(10) > 0) { // เว้นที่นั่งว่างไว้บ้าง
                    drawSpectator(g, x, rowY(row, x), faded(clothes, 0.82 - row * 0.05), 0);
                }
            }
        }
        // พื้นแถวหน้าที่ผู้ชมแบบขยับได้จะมานั่ง
        fillFromRow(g, LIVE_ROW, new Color(62, 54, 90));
        fillFromRow(g, LIVE_ROW + 1, new Color(69, 61, 97));
    }

    /** ผสมสีเข้าหาสีกลางคืน ยิ่งแถวไกลยิ่งกลืนไปกับฉากหลัง */
    private static Color faded(Color c, double amount) {
        Color night = new Color(30, 22, 60);
        return new Color((int) (c.getRed() + (night.getRed() - c.getRed()) * amount),
                (int) (c.getGreen() + (night.getGreen() - c.getGreen()) * amount),
                (int) (c.getBlue() + (night.getBlue() - c.getBlue()) * amount));
    }

    /** ผู้ชม 1 คน: ไหล่ + หัว · lift = ยกตัวขึ้นกี่ pixel ตอนเชียร์ */
    private static void drawSpectator(Graphics2D g, int x, int y, Color clothes, int lift) {
        int top = y + 3 - lift;
        g.setColor(clothes);
        g.fillRoundRect(x - 10, top + 9, 20, 18 + lift, 10, 10);
        g.setColor(faded(new Color(226, 186, 150), 0.55));
        g.fillOval(x - 6, top, 12, 12);
    }

    /** แถวหน้าวาดใหม่ทุกเฟรม ไล่จังหวะทีละคนจึงดูเป็นคลื่นเชียร์ */
    private static void drawLiveCrowd(Graphics2D g, double time) {
        for (int row = LIVE_ROW; row < STAND_ROWS; row++) {
            int seat = 0;
            for (int x = 8 + row * 9; x < GameWorld.WIDTH; x += SEAT_GAP) {
                seat++;
                int lift = (int) (Math.max(0, Math.sin(time * 3 + seat * 0.45)) * 5);
                Color clothes = CROWD_CLOTHES[(seat * 5 + row) % CROWD_CLOTHES.length];
                drawSpectator(g, x, rowY(row, x), faded(clothes, 0.55), lift);
            }
        }
    }

    /** กำแพงรอบลาน มีธงของทั้งสองฝ่ายและคบเพลิง */
    private static void paintWall(Graphics2D g) {
        fillFromRow(g, STAND_ROWS, new Color(56, 46, 74));
        g.setColor(new Color(0, 0, 0, 60));
        for (int y = 340; y < GameWorld.GROUND_Y; y += 18) {
            g.drawLine(0, y, GameWorld.WIDTH, y);
        }
        g.setColor(new Color(232, 190, 90));
        for (int x = 0; x < GameWorld.WIDTH; x += 20) {
            g.drawLine(x, rowY(STAND_ROWS, x), x + 20, rowY(STAND_ROWS, x + 20));
        }
        // {x, ชนิดธง}: 0 = ฝั่งผู้เล่น, 1 = กลาง, 2 = ฝั่งบอท
        int[][] banners = {{70, 0}, {330, 0}, {480, 1}, {630, 2}, {890, 2}};
        Color[] colors = {new Color(50, 90, 190), new Color(190, 150, 60), new Color(165, 40, 60)};
        for (int[] banner : banners) {
            paintBanner(g, banner[0], colors[banner[1]]);
        }
        for (int x : new int[] {270, 400, 560, 690}) {
            paintTorch(g, x, rowY(STAND_ROWS, x) + 22);
        }
    }

    private static void paintBanner(Graphics2D g, int x, Color color) {
        int top = rowY(STAND_ROWS, x);
        int[] xs = {x - 15, x + 15, x + 15, x, x - 15};
        int[] ys = {top, top, top + 46, top + 36, top + 46}; // ปลายเว้าเป็นหางธง
        g.setColor(color);
        g.fillPolygon(xs, ys, 5);
        g.setColor(new Color(240, 225, 180));
        g.fillOval(x - 5, top + 13, 10, 10);
    }

    private static void paintTorch(Graphics2D g, int x, int y) {
        glow(g, x, y - 8, 38, new Color(255, 150, 60));
        g.setColor(new Color(70, 50, 40));
        g.fillRect(x - 2, y, 4, 18);
        g.setColor(new Color(255, 190, 80));
        g.fillOval(x - 5, y - 13, 10, 15);
    }

    private static void paintGround(Graphics2D g) {
        int ground = (int) GameWorld.GROUND_Y;
        g.setPaint(new GradientPaint(0, ground, new Color(54, 44, 76), 0, GameWorld.HEIGHT, new Color(18, 14, 32)));
        g.fillRect(0, ground, GameWorld.WIDTH, GameWorld.HEIGHT - ground);
        g.setColor(new Color(150, 125, 180));
        g.fillRect(0, ground, GameWorld.WIDTH, 2);
        // ร่องหินบนลาน
        g.setColor(new Color(0, 0, 0, 70));
        for (int row = 0; row < 3; row++) {
            int y = ground + 12 + row * 14;
            g.drawLine(0, y, GameWorld.WIDTH, y);
            for (int x = row * 34; x < GameWorld.WIDTH; x += 96) {
                g.drawLine(x, y, x, y + 14);
            }
        }
        paintDuelCircle(g, (int) GameWorld.PLAYER_X, new Color(110, 170, 255));
        paintDuelCircle(g, (int) GameWorld.BOT_X, new Color(235, 95, 120));
    }

    /** วงเวทใต้เท้านักเวท */
    private static void paintDuelCircle(Graphics2D g, int cx, Color c) {
        int y = (int) GameWorld.GROUND_Y + 2;
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
        g.drawOval(cx - 66, y - 9, 132, 20);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
        g.drawOval(cx - 50, y - 5, 100, 12);
        g.setStroke(new BasicStroke(1));
    }

    static void drawBars(Graphics2D g, Duelist player, Duelist bot, int botLevel) {
        drawHp(g, player, 24, false);
        drawHp(g, bot, GameWorld.WIDTH - 24 - 380, true);
        g.setFont(LABEL);
        g.setColor(new Color(220, 210, 255));
        centered(g, "ด่าน " + (botLevel + 1), GameWorld.WIDTH / 2, 40);
    }

    /** เลขรอบ + แถบเวลาที่เหลือ ใต้กลางจอ */
    static void drawRound(Graphics2D g, int round, double timeLeft, boolean revealing) {
        int w = 110;
        int x = (GameWorld.WIDTH - w) / 2;
        g.setFont(SMALL);
        g.setColor(Color.WHITE);
        centered(g, revealing ? "จบรอบ" : "รอบ " + round, GameWorld.WIDTH / 2, 60);
        g.setColor(PANEL);
        g.fillRoundRect(x, 66, w, 8, 8, 8);
        g.setColor(timeLeft > 0.3 ? new Color(120, 230, 120) : new Color(255, 90, 90));
        g.fillRoundRect(x, 66, (int) (w * timeLeft), 8, 8, 8);
    }

    private static void drawHp(Graphics2D g, Duelist d, int x, boolean rightToLeft) {
        int w = 380;
        int h = 20;
        int y = 30;
        double frac = (double) d.hp() / d.maxHp();
        int fill = (int) (w * frac);
        boolean healthy = frac > 0.3;
        g.setColor(PANEL);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setPaint(new GradientPaint(0, y, healthy ? new Color(130, 235, 130) : new Color(255, 120, 95),
                0, y + h, healthy ? new Color(40, 150, 85) : new Color(175, 40, 45)));
        g.fillRoundRect(rightToLeft ? x + w - fill : x, y, fill, h, 10, 10);
        g.setColor(FRAME);
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setColor(Color.WHITE);
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

    /** cast bar เหนือหัวบอท: ซ่อนหน้าที่ไว้จนถึง 20% ของแถบ */
    static void drawCastBar(Graphics2D g, BotWizard bot) {
        int w = 240;
        int h = 16;
        int cx = (int) bot.x();
        int y = (int) GameWorld.GROUND_Y - 200;
        Cast cast = bot.cast();
        g.setColor(PANEL);
        g.fillRoundRect(cx - w / 2 - 10, y - 46, w + 20, h + 54, 12, 12); // พื้นหลังให้อ่านออกบนฝูงชน
        if (cast != null && !cast.isRevealed()) {
            g.setColor(Color.GRAY);
            g.fillRoundRect(cx - w / 2, y, (int) (w * cast.progress()), h, 8, 8);
            g.setFont(LABEL);
            g.setColor(Color.WHITE);
            centered(g, "???", cx, y - 8);
        } else if (cast != null) {
            Color c = cast.role().color();
            g.setColor(c);
            g.fillRoundRect(cx - w / 2, y, (int) (w * cast.progress()), h, 8, 8);
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
        g.setColor(FRAME);
        g.drawRoundRect(cx - w / 2, y, w, h, 8, 8);
    }

    /** ช่องพิมพ์ของผู้เล่น */
    static void drawInput(Graphics2D g, String typed, String locked, boolean thaiWarning) {
        int w = 480;
        int h = 40;
        int x = (GameWorld.WIDTH - w) / 2;
        int y = (int) GameWorld.GROUND_Y + 18;
        g.setColor(PANEL);
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(FRAME);
        g.drawRoundRect(x, y, w, h, 12, 12);
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
        g.setPaint(new GradientPaint(0, CARD_Y, new Color(46, 34, 72), 0, CARD_Y + CARD_H, new Color(20, 15, 36)));
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

    /** เลขนับถอยหลังกลางจอ: ปรากฏตัวใหญ่แล้วหดลง (progress = 0..1 ของเลขตัวนั้น) */
    static void drawCountdown(Graphics2D g, String text, double progress) {
        g.setFont(new Font(Font.DIALOG, Font.BOLD, (int) (110 - 40 * progress)));
        int y = 290;
        g.setColor(new Color(0, 0, 0, 190));
        centered(g, text, GameWorld.WIDTH / 2 + 4, y + 4);
        g.setColor(tierColor(Tier.PERFECT));
        centered(g, text, GameWorld.WIDTH / 2, y);
    }

    private static void centered(Graphics2D g, String text, int cx, int y) {
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - w / 2, y);
    }
}
