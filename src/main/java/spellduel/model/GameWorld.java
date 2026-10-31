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
 * เกมเดินเป็นรอบ: TYPING (พิมพ์พร้อมกัน) → REVEAL (เปิดไพ่ ลูกเวทบิน) → รอบใหม่
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

    /** เวลาพิมพ์ต่อรอบ และเวลาขั้นต่ำของช่วงเปิดไพ่ (วินาที) */
    static final double ROUND_TIME = 8.0;
    static final double REVEAL_TIME = 1.6;
    /** Interrupt ชน Interrupt: เบาทั้งคู่ */
    static final double CLASH_SCALE = 0.25;
    /** บอทล็อกแล้ว ผู้เล่นเหลือเวลาอีกเท่านี้ (ไม่งั้นรอดูแล้วสวนได้สบาย ๆ) */
    static final double GRACE_TIME = 1.0;
    /** ล็อกก่อนบอทเปิดไพ่ = กล้าเดา ได้แรงเพิ่ม */
    static final double EARLY_BONUS = 1.5;

    public enum Status { PLAYING, WON, LOST }

    enum Phase { TYPING, REVEAL }

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

    private Phase phase = Phase.TYPING;
    private double phaseTime; // เวลาที่ผ่านไปในช่วงปัจจุบัน
    private int round;
    private SpellRole lockedRole; // การ์ดที่ผู้เล่นล็อกในรอบนี้ (null = ยังไม่ล็อก)
    private Tier lockedTier;
    private boolean lockedEarly; // ล็อกก่อนบอทเปิดไพ่
    private double graceTime; // เวลาที่ผ่านไปหลังบอทล็อก
    private SpellRole lastPlayerRole; // การ์ดรอบก่อน ให้บอทใช้อ่านทาง

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
        this.player = new PlayerWizard(PLAYER_X, GROUND_Y, input);
        this.bot = new BotWizard(BOT_X, GROUND_Y, profile, brain);
        startRound();
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
        if (!canType()) {
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
     * กด Enter: หาการ์ดที่ใกล้ที่สุด → ให้เกรด → ถ้าไม่ Fizzle ล็อกการ์ดไว้รอเปิดไพ่
     * Fizzle แล้วการ์ดยังอยู่ และพิมพ์ใหม่ได้ถ้ายังมีเวลา
     * @return ผลการร่าย หรือ null ถ้ายังไม่ได้พิมพ์อะไร
     */
    public synchronized CastResult castTyped() {
        String typed = input.text().trim();
        double seconds = input.elapsed(clock);
        input.clear();
        if (!canType() || typed.isEmpty()) {
            return null;
        }
        SpellDeck.Match match = deck.match(typed);
        Incantation card = match.card();
        Tier tier = grader.grade(match.distance(), card.text().length(), seconds);
        tierCounts[tier.ordinal()]++;
        accuracySum += AccuracyGrader.accuracy(match.distance(), card.text().length());

        if (tier == Tier.FIZZLE) {
            popText(player, tier.label(), Hud.tierColor(tier));
        } else {
            deck.consume(card);
            lockedRole = card.role();
            lockedTier = tier;
            lockedEarly = !bot.cast().isRevealed();
        }
        return new CastResult(card.text(), tier);
    }

    private boolean canType() {
        return status == Status.PLAYING && phase == Phase.TYPING && lockedRole == null;
    }

    // ---------------------------------------------------------------- game thread

    public synchronized void update(double dt) {
        for (Entity e : effects) {
            e.update(dt); // effect ขยับต่อแม้เกมจบแล้ว จะได้เห็นหมัดสุดท้าย
        }
        if (status == Status.PLAYING) {
            clock += dt;
            phaseTime += dt;
            thaiWarning = Math.max(0, thaiWarning - dt);
            player.update(dt);
            bot.update(dt);
            if (phase == Phase.TYPING) {
                updateTyping(dt);
            } else {
                updateReveal(dt);
            }
        }
        sweepDead();
    }

    private void updateTyping(double dt) {
        bot.think(dt, view(), rnd);
        if (bot.isLocked()) {
            graceTime += dt;
        }
        boolean bothLocked = lockedRole != null && bot.isLocked();
        if (bothLocked || graceTime >= GRACE_TIME || phaseTime >= ROUND_TIME) {
            if (lockedRole == null) {
                castTyped(); // หมดเวลา: ส่งเท่าที่พิมพ์ค้างไว้
            }
            Cast botCast = bot.cast();
            resolveRound(lockedRole, lockedTier, botCast.role(), bot.rollTier(rnd));
        }
    }

    private void updateReveal(double dt) {
        // ใช้ index เพราะ resolveHit อาจเพิ่มลูกสะท้อนเข้ามาใน list ระหว่างวน
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            p.update(dt);
            if (!p.isDead() && p.hasArrived()) {
                p.kill();
                resolveHit(p, p.target());
            }
        }
        projectiles.removeIf(Entity::isDead);

        if (bot.isDefeated()) {
            status = Status.WON;
        } else if (player.isDefeated()) {
            status = Status.LOST;
        } else if (phaseTime >= REVEAL_TIME && projectiles.isEmpty()) {
            startRound();
        }
    }

    private void startRound() {
        round++;
        phase = Phase.TYPING;
        phaseTime = 0;
        lockedRole = null;
        lockedTier = null;
        lockedEarly = false;
        graceTime = 0;
        player.breakShield(); // โล่อยู่ได้แค่รอบเดียว
        bot.breakShield();
        bot.startRound(view(), rnd);
    }

    private void sweepDead() {
        projectiles.removeIf(Entity::isDead);
        effects.removeIf(Entity::isDead);
    }

    /** สิ่งที่สมองบอทเห็น */
    private DuelView view() {
        return new DuelView(player.hp(), bot.hp(), player.hasShield(), bot.hasShield(),
                deck.guessRole(input.text()), lastPlayerRole, rnd);
    }

    // ---------------------------------------------------------------- กติกา

    /**
     * เปิดไพ่: ทั้งสองฝั่งปล่อยคาถาพร้อมกัน (playerRole = null คือผู้เล่นไม่ได้ล็อก)
     * วงจร: Attack ชนะ Interrupt · Interrupt ชนะ Shield · Shield ชนะ Attack
     */
    void resolveRound(SpellRole playerRole, Tier playerTier, SpellRole botRole, Tier botTier) {
        phase = Phase.REVEAL;
        phaseTime = 0;
        player.interruptCast();
        bot.interruptCast();
        if (playerRole != null) {
            lastPlayerRole = playerRole;
        } else {
            popText(player, "ไม่ทัน!", Color.GRAY);
        }
        boolean clash = playerRole == SpellRole.INTERRUPT && botRole == SpellRole.INTERRUPT;
        double scale = clash ? CLASH_SCALE : 1;
        double playerScale = lockedEarly ? scale * EARLY_BONUS : scale;
        release(player, bot, playerRole, playerTier, botRole, playerScale);
        release(bot, player, botRole, botTier, playerRole, scale);
    }

    private void release(Duelist caster, Duelist target, SpellRole role, Tier tier,
                         SpellRole enemyRole, double scale) {
        if (role == null) {
            return;
        }
        popText(caster, tier.label() + " " + role.label(), Hud.tierColor(tier));
        if (role == SpellRole.SHIELD) {
            caster.raiseShield(tier, scale); // โล่ขึ้นทันที
        } else if (role == SpellRole.INTERRUPT && enemyRole == SpellRole.ATTACK) {
            burst(caster, role.color()); // Attack ชนะ Interrupt: คาถาแตกตั้งแต่ยังไม่ออกจากไม้
        } else {
            projectiles.add(Projectile.fire(caster, target, role, tier, scale));
        }
    }

    /** ลูกเวทไปถึงเป้าแล้วเกิดอะไร (DESIGN §5R) */
    void resolveHit(Projectile p, Duelist target) {
        burst(target, p.role().color());

        if (p.role() == SpellRole.INTERRUPT) {
            boolean hadShield = target.hasShield();
            target.breakShield();
            target.takeHit(SpellRole.INTERRUPT, p.power());
            String text = hadShield ? "Shield Broken! " : "";
            popText(target, text + "-" + Math.round(p.power()), SpellRole.INTERRUPT.color());
            return;
        }

        Shield shield = target.shield();
        if (shield == null) {
            target.takeHit(SpellRole.ATTACK, p.power());
            popText(target, "-" + Math.round(p.power()), Color.RED);
            return;
        }

        target.breakShield(); // โล่ใช้ได้ครั้งเดียว
        projectiles.add(p.reflected(shield)); // Shield ชนะ Attack: ตีโล่ = ตีตัวเอง
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
        boolean revealing = phase == Phase.REVEAL;
        double timeLeft = revealing ? 0 : Math.max(0, 1 - phaseTime / ROUND_TIME);
        Hud.drawBars(g, player, bot, botLevel);
        Hud.drawRound(g, round, timeLeft, revealing);
        Hud.drawCastBar(g, bot);
        Hud.drawInput(g, typed, lockedText(), thaiWarning > 0);
        Hud.drawCards(g, deck.hand(), typed, deck.guessRole(typed));
    }

    private String lockedText() {
        if (lockedRole == null) {
            return null;
        }
        String bonus = lockedEarly ? "  กล้าเดา ×" + EARLY_BONUS : "";
        return "ล็อก " + lockedRole.label() + " (" + lockedTier.label() + ")" + bonus;
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

    Phase phase() {
        return phase;
    }

    int round() {
        return round;
    }

    boolean lockedEarly() {
        return lockedEarly;
    }

    SpellRole lockedRole() {
        return lockedRole;
    }

    int projectileCount() {
        return projectiles.size();
    }
}
