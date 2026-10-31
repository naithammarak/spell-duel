package spellduel.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import spellduel.typing.AccuracyGrader;
import spellduel.typing.InputBuffer;
import spellduel.typing.Tier;

/**
 * "กรรมการ + สนาม" ของการดวล 1 ครั้ง: ถือข้อมูลทุกอย่างและรู้กติกาทั้งหมด
 * ui คุยกับ model ผ่าน class นี้ทางเดียว
 *
 * Thread: public method ทุกตัวเป็น synchronized เพราะ EDT (ปุ่ม/วาดจอ) กับ game thread (update)
 * เข้ามาพร้อมกันได้ ใครจะเข้าต้องถือกุญแจ และเข้าได้ทีละคน
 */
public class GameWorld {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;
    public static final int BOT_COUNT = 3;

    static final double GROUND_Y = 400;
    static final double PLAYER_X = 200;
    static final double BOT_X = 760;

    public enum Status { PLAYING, WON, LOST }

    /** ผลของการกด Enter 1 ครั้ง */
    public record CastResult(String spell, Tier tier) {
    }

    private final int botLevel;
    private final InputBuffer input = new InputBuffer();
    private final AccuracyGrader grader = new AccuracyGrader();
    private final SpellDeck deck;
    private final PlayerWizard player;
    private final BotWizard bot;
    private final Random rnd;
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Entity> effects = new ArrayList<>();
    private final int[] tierCounts = new int[Tier.values().length];
    private double accuracySum;
    private double clock; // เวลาในเกม (วินาที) ไม่นับตอน pause
    private double thaiWarning; // เหลือเวลาแสดงคำเตือนแป้นไทยอีกกี่วินาที
    private Status status = Status.PLAYING;

    /** ดวล 1 ครั้งกับบอทเลเวลนี้ (0 = Apprentice, 1 = Auror, 2 = Dark Wizard) */
    public GameWorld(int botLevel, long seed) {
        this(botLevel, BotProfile.row(botLevel), brainFor(botLevel),
                SpellDeck.standard(new Random(seed)), new Random(seed + 1));
    }

    /** สำหรับเทสต์: เลือกบอท สมอง และสำรับเองได้ */
    GameWorld(BotProfile profile, BotBrain brain, SpellDeck deck) {
        this(Arrays.asList(BotProfile.NAMES).indexOf(profile.name()), profile, brain, deck, new Random(0));
    }

    private GameWorld(int botLevel, BotProfile profile, BotBrain brain, SpellDeck deck, Random rnd) {
        this.botLevel = botLevel;
        this.deck = deck;
        this.rnd = rnd;
        this.player = new PlayerWizard(PLAYER_X, GROUND_Y, input, deck);
        this.bot = new BotWizard(BOT_X, GROUND_Y, profile, brain);
    }

    /** บอทแต่ละเลเวลได้สมองคนละแบบ (polymorphism) */
    static BotBrain brainFor(int level) {
        return switch (level) {
            case 0 -> new ApprenticeBrain();
            case 1 -> new AurorBrain();
            default -> new DarkWizardBrain();
        };
    }

    // ---------------------------------------------------------------- input (EDT)

    public synchronized void typeChar(char c) {
        if (status != Status.PLAYING) {
            return;
        }
        if (c >= '฀' && c <= '๿') { // ตัวอักษรไทย = ลืมเปลี่ยนแป้น
            thaiWarning = 2.0;
            return;
        }
        input.add(c, clock);
    }

    public synchronized void backspace() {
        input.backspace();
    }

    /**
     * กด Enter: หาการ์ดที่ใกล้ที่สุด → ให้เกรด → ถ้าไม่ Fizzle ใช้การ์ดแล้วร่าย
     * @return ผลการร่าย หรือ null ถ้ายังไม่ได้พิมพ์อะไร
     */
    public synchronized CastResult castTyped() {
        String typed = input.text().trim();
        double seconds = input.elapsed(clock);
        input.clear();
        if (status != Status.PLAYING || typed.isEmpty()) {
            return null;
        }
        SpellDeck.Match match = deck.match(typed);
        Incantation card = match.card();
        Tier tier = grader.grade(match.distance(), card.text().length(), seconds);
        tierCounts[tier.ordinal()]++;
        accuracySum += AccuracyGrader.accuracy(match.distance(), card.text().length());

        if (tier == Tier.FIZZLE) {
            popText(player, tier.label(), Hud.tierColor(tier)); // การ์ดยังอยู่ ไม่ถูกใช้
        } else {
            deck.consume(card);
            playerCasts(card.role(), tier);
        }
        return new CastResult(card.text(), tier);
    }

    // ---------------------------------------------------------------- game thread

    public synchronized void update(double dt) {
        for (Entity e : effects) {
            e.update(dt); // effect ขยับต่อแม้เกมจบแล้ว จะได้เห็นหมัดสุดท้าย
        }
        if (status == Status.PLAYING) {
            clock += dt;
            thaiWarning = Math.max(0, thaiWarning - dt);
            player.update(dt);
            bot.update(dt);

            Cast finished = bot.think(dt, view(), rnd);
            if (finished != null) {
                botCasts(finished.role(), bot.rollTier(rnd));
            }

            // ใช้ index เพราะ resolveHit อาจเพิ่มลูกสะท้อนเข้ามาใน list ระหว่างวน
            for (int i = 0; i < projectiles.size(); i++) {
                Projectile p = projectiles.get(i);
                p.update(dt);
                if (!p.isDead() && p.hasArrived()) {
                    p.kill();
                    resolveHit(p, p.target());
                }
            }

            if (bot.isDefeated()) {
                status = Status.WON;
            } else if (player.isDefeated()) {
                status = Status.LOST;
            }
        }
        sweepDead();
    }

    private void sweepDead() {
        projectiles.removeIf(Entity::isDead);
        effects.removeIf(Entity::isDead);
    }

    /** สิ่งที่สมองบอทเห็น */
    private DuelView view() {
        return new DuelView(player.hp(), bot.hp(), player.hasShield(), bot.hasShield(),
                deck.guessRole(input.text()), rnd);
    }

    // ---------------------------------------------------------------- กติกา

    /** ผู้เล่นร่ายหน้าที่นี้ออกไป (เทสต์เรียกตรง ๆ เพื่อจัดฉากได้) */
    void playerCasts(SpellRole role, Tier tier) {
        release(player, bot, role, tier);
    }

    /** บอทร่ายหน้าที่นี้ออกไป */
    void botCasts(SpellRole role, Tier tier) {
        release(bot, player, role, tier);
    }

    private void release(Duelist caster, Duelist target, SpellRole role, Tier tier) {
        popText(caster, tier.label() + " " + role.label(), Hud.tierColor(tier));
        if (role == SpellRole.SHIELD) {
            caster.raiseShield(tier); // โล่ขึ้นทันที
        } else {
            projectiles.add(Projectile.fire(caster, target, role, tier));
        }
    }

    /**
     * กติกาเดียวของเกม: ลูกเวทไปถึงเป้าแล้วเกิดอะไร (DESIGN §5.3)
     * "ร่ายเสร็จก่อนชนะ" เกิดจากตรงนี้: โดน Attack ที่แม่นพอแล้ว cast ของเป้าถูกตัด
     * วงจร: Attack ชนะ Interrupt · Interrupt ชนะ Shield · Shield ชนะ Attack
     */
    void resolveHit(Projectile p, Duelist target) {
        burst(target, p.role().color());

        if (p.role() == SpellRole.INTERRUPT) {
            boolean hadShield = target.hasShield();
            // Interrupt ตัดได้แค่การร่ายโล่ ตัด Attack ไม่ได้
            boolean cut = p.canCut() && target.castingRole() == SpellRole.SHIELD;
            target.breakShield(); // ทำลายโล่ได้ทุก tier
            if (cut) {
                target.interruptCast();
            }
            target.takeHit(SpellRole.INTERRUPT, p.power());
            String text = hadShield ? "Shield Broken!" : cut ? "Interrupted!" : "";
            popText(target, (text + " -" + Math.round(p.power())).trim(), SpellRole.INTERRUPT.color());
            return;
        }

        Shield shield = target.shield();
        if (shield == null) {
            boolean cut = p.canCut() && target.isCasting();
            target.takeHit(SpellRole.ATTACK, p.power());
            if (cut) {
                target.interruptCast();
            }
            popText(target, (cut ? "Interrupted! " : "") + "-" + Math.round(p.power()), Color.RED);
            return;
        }

        target.breakShield(); // โล่ใช้ได้ครั้งเดียว
        projectiles.add(p.reflected(shield.tier())); // Shield ชนะ Attack: ตีโล่ = ตีตัวเอง
        popText(target, "Reflect!", new Color(255, 215, 90));
    }

    private void popText(Duelist who, String text, Color color) {
        effects.add(new FloatingText(who.x(), who.y() - 150, text, color));
    }

    private void burst(Duelist who, Color color) {
        for (int i = 0; i < 18; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double speed = 60 + rnd.nextDouble() * 160;
            effects.add(new Particle(who.x(), who.y() - 60, Math.cos(angle) * speed,
                    Math.sin(angle) * speed, 6, 0.5, color));
        }
    }

    // ---------------------------------------------------------------- วาด / สถานะ

    public synchronized void draw(Graphics2D g, Assets assets) {
        Hud.drawArena(g);
        player.draw(g, assets);
        bot.draw(g, assets);
        for (Projectile p : projectiles) {
            p.draw(g, assets);
        }
        for (Entity e : effects) {
            e.draw(g, assets);
        }
        String typed = input.text();
        Hud.drawBars(g, player, bot, botLevel);
        Hud.drawCastBar(g, bot);
        Hud.drawInput(g, typed, thaiWarning > 0);
        Hud.drawCards(g, deck.hand(), typed, deck.guessRole(typed));
    }

    public synchronized Status status() {
        return status;
    }

    public synchronized GameStats stats() {
        int casts = Arrays.stream(tierCounts).sum();
        return new GameStats(status == Status.WON, botLevel, bot.name(), player.hp(), clock,
                casts == 0 ? 0 : accuracySum / casts, tierCounts);
    }

    // ---------------------------------------------------------------- สำหรับเทสต์

    PlayerWizard player() {
        return player;
    }

    BotWizard bot() {
        return bot;
    }

    SpellDeck deck() {
        return deck;
    }

    String typed() {
        return input.text();
    }

    int projectileCount() {
        return projectiles.size();
    }
}
