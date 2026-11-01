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
 * เกมเดินเป็นรอบ: COUNTDOWN (นับ 3-2-1) → TYPING (พิมพ์พร้อมกัน ใครร่ายเสร็จคาถาทำงานทันที คนละ 1 ครั้ง) → REVEAL (รอลูกเวทถึงเป้า) → รอบใหม่
 *
 * Thread: public method ทุกตัวเป็น synchronized เพราะ EDT (ปุ่ม/วาดจอ) กับ game thread (update)
 * เข้ามาพร้อมกันได้ ใครจะเข้าต้องถือกุญแจ และเข้าได้ทีละคน
 */
public class GameWorld {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 600;
    public static final int PLAYER_MAX_HP = PlayerWizard.MAX_HP;

    static final double GROUND_Y = 400;
    static final double PLAYER_X = 200;
    static final double BOT_X = 760;

    /** นับ 3-2-1 ก่อนเริ่มรอบ เลขละเท่านี้วินาที ระหว่างนี้ยังพิมพ์ไม่ได้และบอทยังไม่ร่าย */
    static final double COUNT_STEP = 0.3;
    static final int COUNT_FROM = 3;
    static final double COUNTDOWN_TIME = COUNT_STEP * COUNT_FROM;
    /** "เริ่ม!" ค้างบนจอเท่านี้วินาที (ไม่บังการเล่น) */
    static final double GO_TIME = 0.4;

    /** เวลาพิมพ์ต่อรอบ และเวลาขั้นต่ำของช่วงพักท้ายรอบ (วินาที) */
    static final double ROUND_TIME = 8.0;
    static final double REVEAL_TIME = 1.6;
    /** Interrupt ที่ไม่เจอโล่แรงเหลือเท่านี้ */
    static final double NO_SHIELD_SCALE = 0.25;
    /** บอทล็อกแล้ว ผู้เล่นเหลือเวลาอีกเท่านี้ (ไม่งั้นรอดูแล้วสวนได้สบาย ๆ) */
    static final double GRACE_TIME = 1.0;

    public enum Status { PLAYING, WON, LOST }

    enum Phase { COUNTDOWN, TYPING, REVEAL }

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
    private SpellRole lockedRole; // การ์ดที่ผู้เล่นใช้ในรอบนี้ (null = ยังไม่ได้ใช้)
    private Tier lockedTier;
    private boolean botActed; // บอทปล่อยคาถาของรอบนี้ไปแล้ว
    private double graceTime; // เวลาที่ผ่านไปหลังบอทปล่อยคาถา
    private SpellRole lastPlayerRole; // การ์ดรอบก่อน ให้บอทใช้อ่านทาง

    /** ดวล 1 ด่าน (0 = Apprentice, 1 = Auror, 2 ขึ้นไป = Dark Wizard ที่เก่งขึ้นเรื่อย ๆ) playerHp = HP ที่เหลือจากด่านก่อน */
    public GameWorld(int botLevel, int playerHp, long seed) {
        this(botLevel, playerHp, BotProfile.forLevel(botLevel), brainFor(botLevel),
                SpellDeck.standard(new Random(seed)), new Random(seed + 1));
    }

    /** สำหรับเทสต์: เลือกบอท สมอง และสำรับเองได้ */
    GameWorld(BotProfile profile, BotBrain brain, SpellDeck deck) {
        this(Arrays.asList(BotProfile.NAMES).indexOf(profile.name()), PlayerWizard.MAX_HP, profile, brain, deck,
                new Random(0));
    }

    private GameWorld(int botLevel, int playerHp, BotProfile profile, BotBrain brain, SpellDeck deck, Random rnd) {
        this.botLevel = botLevel;
        this.deck = deck;
        this.rnd = rnd;
        this.player = new PlayerWizard(PLAYER_X, GROUND_Y, input, playerHp);
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
     * กด Enter: หาการ์ดที่ใกล้ที่สุด → ให้เกรด → ถ้าไม่ Fizzle คาถาทำงานทันที (ใช้ได้รอบละครั้ง)
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
            lastPlayerRole = card.role();
            release(player, bot, card.role(), tier);
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
            if (phase == Phase.COUNTDOWN && phaseTime >= COUNTDOWN_TIME) {
                phase = Phase.TYPING;
                phaseTime = 0;
            }
            if (phase == Phase.TYPING) {
                updateTyping(dt);
            }
            updateProjectiles(dt);
            updateRoundEnd();
        }
        sweepDead();
    }

    private void updateTyping(double dt) {
        bot.think(dt, view(), rnd);
        if (bot.isLocked()) {
            if (!botActed) {
                botActed = true;
                release(bot, player, bot.cast().role(), bot.rollTier(rnd));
            }
            graceTime += dt;
        }
        boolean bothActed = lockedRole != null && botActed;
        if (bothActed || graceTime >= GRACE_TIME || phaseTime >= ROUND_TIME) {
            if (lockedRole == null) {
                castTyped(); // หมดเวลา: ส่งเท่าที่พิมพ์ค้างไว้
            }
            endTyping();
        }
    }

    private void endTyping() {
        phase = Phase.REVEAL;
        phaseTime = 0;
        player.interruptCast();
        bot.interruptCast();
        if (lockedRole == null) {
            popText(player, "ไม่ทัน!", Color.GRAY);
        }
    }

    private void updateProjectiles(double dt) {
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
    }

    private void updateRoundEnd() {
        if (bot.isDefeated()) {
            status = Status.WON;
        } else if (player.isDefeated()) {
            status = Status.LOST;
        } else if (phase == Phase.REVEAL && phaseTime >= REVEAL_TIME && projectiles.isEmpty()) {
            startRound();
        }
    }

    private void startRound() {
        round++;
        phase = Phase.COUNTDOWN;
        phaseTime = 0;
        lockedRole = null;
        lockedTier = null;
        botActed = false;
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
     * ปล่อยคาถาทันที ผลขึ้นกับสถานะตอนลูกเวทถึงเป้า:
     * Attack เจอโล่ = สะท้อน · Interrupt เจอโล่ = ทำลายโล่ · Interrupt ไม่เจอโล่ = เบามาก
     */
    void release(Duelist caster, Duelist target, SpellRole role, Tier tier) {
        popText(caster, tier.label() + " " + role.label(), Hud.tierColor(tier));
        if (role == SpellRole.SHIELD) {
            caster.raiseShield(tier); // โล่ขึ้นทันที และอยู่จนจบรอบ
        } else {
            projectiles.add(Projectile.fire(caster, target, role, tier));
        }
    }

    /** ลูกเวทไปถึงเป้าแล้วเกิดอะไร (DESIGN §5.4) */
    void resolveHit(Projectile p, Duelist target) {
        burst(target, p.role().color());

        if (p.role() == SpellRole.INTERRUPT) {
            boolean hadShield = target.hasShield();
            target.breakShield();
            double damage = hadShield ? p.power() : p.power() * NO_SHIELD_SCALE;
            target.takeHit(SpellRole.INTERRUPT, damage);
            String text = hadShield ? "Shield Broken! " : "";
            popText(target, text + "-" + Math.round(damage), SpellRole.INTERRUPT.color());
            return;
        }

        Shield shield = target.shield();
        if (shield == null) {
            target.takeHit(SpellRole.ATTACK, p.power());
            popText(target, "-" + Math.round(p.power()), Color.RED);
            if (p.canCut()) {
                cutCast(target);
            }
            return;
        }

        target.breakShield(); // โล่ใช้ได้ครั้งเดียว
        projectiles.add(p.reflected(shield)); // Shield ชนะ Attack: ตีโล่ = ตีตัวเอง
        popText(target, "Reflect!", new Color(255, 215, 90));
    }

    /** Attack ที่โดนตัวขัดคาถาที่ยังร่ายไม่เสร็จ: บอทเสียการร่ายรอบนี้ · ผู้เล่นข้อความหาย แต่พิมพ์ใหม่ได้ */
    private void cutCast(Duelist target) {
        boolean botCut = target == bot && !botActed;
        boolean playerCut = target == player && lockedRole == null && player.isCasting();
        if (botCut) {
            botActed = true;
        }
        if (botCut || playerCut) {
            target.interruptCast();
            popText(target, "ถูกขัด!", Color.WHITE);
        }
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
        Hud.drawArena(g, clock);
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
        double timeLeft = switch (phase) {
            case COUNTDOWN -> 1;
            case TYPING -> Math.max(0, 1 - phaseTime / ROUND_TIME);
            case REVEAL -> 0;
        };
        Hud.drawBars(g, player, bot, botLevel);
        Hud.drawRound(g, round, timeLeft, revealing);
        Hud.drawCastBar(g, bot);
        Hud.drawInput(g, typed, lockedText(), thaiWarning > 0);
        Hud.drawCards(g, deck.hand(), typed, deck.guessRole(typed));
        if (status == Status.PLAYING) {
            drawCountdown(g);
        }
    }

    private void drawCountdown(Graphics2D g) {
        if (phase == Phase.COUNTDOWN) {
            int step = Math.min((int) (phaseTime / COUNT_STEP), COUNT_FROM - 1);
            Hud.drawCountdown(g, String.valueOf(COUNT_FROM - step), phaseTime / COUNT_STEP - step);
        } else if (phase == Phase.TYPING && phaseTime < GO_TIME) {
            Hud.drawCountdown(g, "เริ่ม!", phaseTime / GO_TIME);
        }
    }

    private String lockedText() {
        if (lockedRole == null) {
            return null;
        }
        return "ใช้ " + lockedRole.label() + " แล้ว (" + lockedTier.label() + ")";
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

    SpellRole lockedRole() {
        return lockedRole;
    }

    int projectileCount() {
        return projectiles.size();
    }
}
