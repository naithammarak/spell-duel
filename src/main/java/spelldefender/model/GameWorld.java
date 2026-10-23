package spelldefender.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import spelldefender.typing.AccuracyGrader;
import spelldefender.typing.InputBuffer;
import spelldefender.typing.Tier;

/**
 * เกม 1 เกมทั้งหมด (ห้องครัว) ฝั่ง ui เรียกได้แค่ public method ข้างล่าง
 * list และกฎทั้งหมดซ่อนไว้ข้างใน
 *
 * Thread safety: public method ทุกตัวเป็น synchronized
 *   game thread เรียก update() ส่วน Swing (EDT) เรียกรับปุ่มและ draw()
 *   จึงไม่มีทางที่สองฝั่งจะแตะ list พร้อมกัน
 *
 * การลบทีหลัง: ระหว่าง update entity แค่ kill() ตัวเอง
 * แล้ว sweepDead() ลบทีเดียวตอนท้าย
 *
 * 1 object = 1 เกม ถ้าจะเริ่มใหม่ให้สร้าง GameWorld ใหม่
 */
public final class GameWorld {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 640;

    public enum Status { PLAYING, WON, LOST }

    static final int START_GOLD = 120;

    /** ผลของการกด Enter (ฝั่ง ui ไม่จำเป็นต้องใช้ แต่เทสต์ใช้) */
    public record CastResult(Outcome outcome, String spellName, Tier tier) {
        public enum Outcome { CAST, FIZZLE, ON_COOLDOWN, EMPTY }
    }

    private final SpellBook spellBook;
    private final WaveManager waves;
    private final TileMap map;
    private final AccuracyGrader grader = new AccuracyGrader();
    private final InputBuffer input = new InputBuffer();
    private final Base base;
    private final Wizard wizard;
    private final ArmyGrid army;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    /** เอฟเฟกต์ (ข้อความลอย + เศษประกาย) เก็บรวมเป็น Entity เพราะทุกตัว update/draw ได้เหมือนกัน */
    private final ArrayList<Entity> effects = new ArrayList<>();
    private final Random fxRandom = new Random();

    private final int[] tierCounts = new int[Tier.values().length]; // นับจำนวนครั้งของแต่ละ Tier
    private double accuracySum;
    private int enemiesDefeated;
    private double clock;           // เวลาในเกม (วินาที) ไม่นับตอน pause
    private double layoutWarningCooldown;
    private int lastAnnouncedWave;
    private int textSlot;           // เลื่อนข้อความที่ขึ้นติดกันไม่ให้ซ้อนกัน
    private int combo;              // จำนวน PERFECT ติดกัน
    private int bestCombo;
    private double shakeTime;       // จอสั่นตอนฐานโดนตี
    private int gold = START_GOLD;
    private AllyType selectedAlly;  // สายนักเวทที่กำลังจะวาง (null = ไม่ได้เลือก)
    private boolean wasInWave;
    private Status status = Status.PLAYING;

    /** เกมจริง: คาถา 5 อัน, 5 wave, แผนที่มาตรฐาน */
    public GameWorld(long seed) {
        this(SpellBook.standard(), WaveManager.standard(new Random(seed)), TileMap.standard());
    }

    /** สำหรับเทสต์: เลือกส่วนประกอบเองได้ */
    GameWorld(SpellBook spellBook, WaveManager waves, TileMap map) {
        this.spellBook = spellBook;
        this.waves = waves;
        this.map = map;
        this.base = map.createBase();
        this.wizard = new Wizard(base.frontX() - 30, base.y()); // ยืนบนกำแพงปราสาท
        this.army = new ArmyGrid(laneYs(map), base.frontX() + 44);
    }

    /** เลนที่ศัตรูเดิน = แถวของจุดเกิด (เรียงจากบนลงล่าง) */
    private static double[] laneYs(TileMap map) {
        List<double[]> spawns = map.spawnPoints();
        double[] ys = new double[spawns.size()];
        for (int i = 0; i < ys.length; i++) {
            ys[i] = spawns.get(i)[1];
        }
        java.util.Arrays.sort(ys);
        return ys;
    }

    // ===================== input (เรียกจาก EDT) =====================

    /** พิมพ์ตัวอักษร (a-z / ช่องว่าง) ถ้าเป็นภาษาไทยจะเตือนให้เปลี่ยนแป้น */
    public synchronized void typeChar(char c) {
        if (status != Status.PLAYING) {
            return;
        }
        if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.THAI) {
            if (layoutWarningCooldown <= 0) {
                addText("เปลี่ยนเป็นแป้นภาษาอังกฤษ (EN)", new Color(255, 120, 120));
                layoutWarningCooldown = 2;
            }
            return;
        }
        input.add(c, clock);
    }

    public synchronized void backspace() {
        input.backspace();
    }

    /**
     * Enter: ขั้นตอนร่ายคาถาทั้งหมดอยู่ที่นี่
     * InputBuffer → SpellBook.match → AccuracyGrader.grade → Spell.cast
     */
    public synchronized CastResult castTyped() {
        if (status != Status.PLAYING || input.isEmpty()) {
            input.clear();
            if (status == Status.PLAYING && waves.canStartNextWave()) {
                waves.startNextWave(); // Enter ตอนช่องพิมพ์ว่าง = เริ่ม wave
            }
            return new CastResult(CastResult.Outcome.EMPTY, "", null);
        }
        String typed = input.text().trim();
        double seconds = input.elapsed(clock);
        input.clear();

        SpellBook.Match match = spellBook.match(typed);
        Spell spell = match.spell();
        if (!spell.isReady()) {
            addText(spell.name() + ": Cooldown!", Color.LIGHT_GRAY);
            return new CastResult(CastResult.Outcome.ON_COOLDOWN, spell.name(), null);
        }

        Tier tier = grader.grade(match.distance(), spell.length(), seconds);
        if (tier != Tier.FIZZLE && !spell.cast(this, tier)) {
            tier = Tier.FIZZLE; // หาเป้าไม่เจอ
        }

        tierCounts[tier.ordinal()]++;
        accuracySum += AccuracyGrader.accuracy(match.distance(), spell.length());
        updateCombo(tier);
        addText(tier.label(), colorFor(tier));
        if (combo >= 2) {
            addText("Combo x" + combo + "!", new Color(255, 120, 220));
        }
        if (tier != Tier.FIZZLE) {
            wizard.flash();
            return new CastResult(CastResult.Outcome.CAST, spell.name(), tier);
        }
        return new CastResult(CastResult.Outcome.FIZZLE, spell.name(), tier);
    }

    /** Tab: ล็อกเป้าตัวถัดไป (เรียงจากใกล้ฐานไปไกล) */
    public synchronized void cycleTarget() {
        List<Enemy> alive = aliveEnemiesByDistance();
        if (alive.isEmpty()) {
            return;
        }
        int index = alive.indexOf(currentLockTarget());
        wizard.setManualLock(alive.get((index + 1) % alive.size()));
    }

    /** ←/→: หมุนไม้กายสิทธิ์ (-1 = ขึ้น, +1 = ลง) */
    public synchronized void turnWand(int direction) {
        wizard.turn(direction);
    }

    /** ขยับเมาส์: หันไม้กายสิทธิ์ไปทางเมาส์ */
    public synchronized void aimWandAt(double x, double y) {
        wizard.aimAt(x, y);
    }

    /** กด 1–4: เลือกสายนักเวทที่จะซื้อ (กดซ้ำ = ยกเลิก) */
    public synchronized void selectAlly(int index) {
        AllyType[] types = AllyType.values();
        if (index < 0 || index >= types.length) {
            return;
        }
        selectedAlly = (selectedAlly == types[index]) ? null : types[index];
    }

    /** เริ่ม wave ถัดไป (ช่วงเตรียมตัวเท่านั้น) */
    public synchronized void startWave() {
        if (status == Status.PLAYING) {
            waves.startNextWave();
        }
    }

    /**
     * คลิกเมาส์ ตามลำดับความสำคัญ:
     * ปุ่มเริ่ม Wave → ปุ่มร้านค้า → วางนักเวทในช่องว่าง → ล็อกเป้าศัตรูใกล้จุดคลิก
     */
    public synchronized void click(double x, double y) {
        if (status != Status.PLAYING) {
            return;
        }
        if (waves.canStartNextWave() && Hud.startButtonContains(x, y)) {
            waves.startNextWave();
            return;
        }
        int chip = Hud.shopChipAt(x, y);
        if (chip >= 0) {
            selectAlly(chip);
            return;
        }
        int[] slot = army.slotAt(x, y);
        if (slot != null && selectedAlly != null) {
            buyAlly(slot[0], slot[1]);
            return;
        }
        Enemy e = nearestEnemyTo(x, y);
        if (e != null && e.distanceTo(x, y) <= e.radius() + 60) {
            wizard.setManualLock(e);
        }
    }

    // ===================== game thread =====================

    /** เดินเวลาไป dt วินาที */
    public synchronized void update(double dt) {
        if (status != Status.PLAYING) {
            return;
        }
        clock += dt;
        layoutWarningCooldown -= dt;
        shakeTime = Math.max(0, shakeTime - dt);
        spellBook.tickCooldowns(dt);
        base.update(dt);
        wizard.update(dt);

        // 1. ศัตรูเดิน (ถ้ามีนักเวทขวางก็หยุดตี) ถ้าถึงฐานก็โจมตีแล้วหายไป
        for (Enemy e : enemies) {
            e.setBlocker(army.blockerFor(e));
            e.update(dt);
            if (!e.isDead() && e.reachedBase(base.frontX())) {
                base.takeDamage(e.attack());
                e.kill();
                shakeTime = 0.3;
                burstAt(base.frontX(), e.y(), new Color(255, 80, 60), 14);
            }
        }

        // 2. กองทัพของเรายิงศัตรูในระยะ (Polymorphism: แต่ละสาย attack ต่างกัน)
        for (Ally a : army.allies()) {
            a.update(dt);
            if (a.isReady()) {
                Enemy target = nearestEnemyWithin(a, a.range());
                if (target != null) {
                    a.attack(this, target);
                    a.startCooldown();
                }
            }
        }

        // 3. กระสุนบินและชน
        for (Projectile p : projectiles) {
            if (p.isHoming() && p.target().isDead()) {
                Enemy next = nearestEnemyTo(p.x(), p.y());
                if (next == null) {
                    p.kill(); // ไม่มีศัตรูเหลือ กระสุนหายไป
                    continue;
                }
                p.retarget(next);
            }
            p.update(dt);
            if (p.isDead()) {
                continue;
            }
            Enemy hit = findHit(p);
            if (hit != null) {
                resolveImpact(p, hit);
                p.kill();
            }
        }

        // 4. เอฟเฟกต์ (Polymorphism: FloatingText กับ Particle update ต่างกัน)
        for (Entity fx : effects) {
            fx.update(dt);
        }

        // 5. ลบตัวที่ตายทีเดียว แล้วค่อยเพิ่มศัตรูใหม่
        sweepDead();
        enemies.addAll(waves.update(dt, enemies.isEmpty(), map));
        announceWave();
        if (wasInWave && !waves.inWave()) {
            int bonus = 30 + 10 * waves.currentWave();
            gold += bonus;
            effects.add(new FloatingText(WIDTH / 2.0, HEIGHT / 2.0, "จบ Wave! +" + bonus + " ทอง",
                    new Color(255, 215, 0), 32));
        }
        wasInWave = waves.inWave();

        // 6. แพ้ / ชนะ
        if (base.isDestroyed()) {
            status = Status.LOST;
        } else if (waves.allWavesCleared() && enemies.isEmpty()) {
            status = Status.WON;
        }
    }

    // ===================== วาด (EDT) =====================

    /** วาดทุกอย่างจากหลังไปหน้า แต่ละ entity วาดตัวเอง (Polymorphism) */
    public synchronized void draw(Graphics2D g, Assets assets) {
        // จอสั่น: เลื่อนทั้งฉากไปแบบสุ่มนิดหน่อย
        int shakeX = 0;
        int shakeY = 0;
        if (shakeTime > 0) {
            int amount = (int) (shakeTime * 20);
            shakeX = fxRandom.nextInt(amount * 2 + 1) - amount;
            shakeY = fxRandom.nextInt(amount * 2 + 1) - amount;
        }
        g.translate(shakeX, shakeY);
        map.draw(g, assets);
        base.draw(g, assets);
        army.draw(g, assets, selectedAlly != null);
        for (Enemy e : enemies) {
            e.draw(g, assets);
        }
        drawLockMarker(g);
        for (Projectile p : projectiles) {
            p.draw(g, assets);
        }
        wizard.draw(g, assets);
        for (Entity fx : effects) {
            fx.draw(g, assets);
        }
        g.translate(-shakeX, -shakeY);
        Hud.draw(g, this);
    }

    public synchronized Status status() {
        return status;
    }

    public synchronized GameStats stats() {
        int casts = 0;
        for (int c : tierCounts) {
            casts += c;
        }
        double avg = casts == 0 ? 0 : accuracySum / casts;
        return new GameStats(status == Status.WON, Math.max(1, waves.currentWave()), waves.totalWaves(),
                enemiesDefeated, avg, tierCounts, bestCombo);
    }

    // ===================== สำหรับคาถา / HUD / เทสต์ (package-private) =====================

    Base base() {
        return base;
    }

    Wizard wizard() {
        return wizard;
    }

    SpellBook spellBook() {
        return spellBook;
    }

    WaveManager waves() {
        return waves;
    }

    int combo() {
        return combo;
    }

    int gold() {
        return gold;
    }

    AllyType selectedAlly() {
        return selectedAlly;
    }

    ArmyGrid army() {
        return army;
    }

    /** ซื้อนักเวทลงช่อง (row, col) ถ้าช่องว่างและทองพอ */
    boolean buyAlly(int row, int col) {
        if (selectedAlly == null || !army.isEmpty(row, col)) {
            return false;
        }
        if (gold < selectedAlly.cost()) {
            addText("ทองไม่พอ", new Color(255, 120, 120));
            return false;
        }
        gold -= selectedAlly.cost();
        Ally a = army.place(row, col, selectedAlly);
        burstAt(a.x(), a.y(), new Color(255, 255, 180), 12);
        return true;
    }

    /** ใช้ตอนนักเวทโจมตี (ไม่แสดงตัวเลข เพราะศัตรูเป็นร้อยจะรกจอ) */
    void damageEnemy(Enemy e, double amount, DamageType type) {
        if (!e.isDead()) {
            e.takeDamage(amount, type);
        }
    }

    /** ศัตรูที่ยังไม่ตายในรัศมี (list ใหม่ วนแล้วทำดาเมจได้ปลอดภัย) */
    List<Enemy> enemiesNear(double x, double y, double radius) {
        List<Enemy> list = new ArrayList<>();
        for (Enemy e : enemies) {
            if (!e.isDead() && e.distanceTo(x, y) <= radius + e.radius()) {
                list.add(e);
            }
        }
        return list;
    }

    void addBolt(double x1, double y1, double x2, double y2, Color color) {
        effects.add(new Bolt(x1, y1, x2, y2, color));
    }

    /** โบนัสดาเมจจาก Combo: PERFECT ติดกัน 2 ครั้ง = +10% ... สูงสุด +50% */
    double comboMultiplier() {
        return 1 + 0.1 * Math.min(Math.max(combo - 1, 0), 5);
    }

    String typedText() {
        return input.text();
    }

    List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    int projectileCount() {
        return projectiles.size();
    }

    /** ใส่กระสุน (เรียกจาก Targeting ระหว่าง castTyped ไม่ใช่ระหว่างวน loop ใน update) */
    void addProjectile(Projectile p) {
        projectiles.add(p);
    }

    /** เทสต์ใช้วางศัตรูเอง */
    void addEnemy(Enemy e) {
        enemies.add(e);
    }

    /**
     * "ตอนนี้เล็งตัวไหน" นิยามที่นี่ที่เดียว (ใช้ทั้งตอนร่ายและตอนวาด marker)
     * ถ้าเป้าที่ล็อกด้วย Tab ยังไม่ตาย ใช้ตัวนั้น ไม่งั้นใช้ตัวที่ใกล้ฐานที่สุด ไม่มีศัตรูเลยคืน null
     */
    Enemy currentLockTarget() {
        Enemy locked = wizard.manualLock();
        if (locked != null && !locked.isDead() && enemies.contains(locked)) {
            return locked;
        }
        wizard.setManualLock(null);
        List<Enemy> alive = aliveEnemiesByDistance();
        return alive.isEmpty() ? null : alive.get(0);
    }

    // ===================== private =====================

    private List<Enemy> aliveEnemiesByDistance() {
        List<Enemy> alive = new ArrayList<>();
        for (Enemy e : enemies) {
            if (!e.isDead()) {
                alive.add(e);
            }
        }
        alive.sort(Comparator.comparingDouble(e -> e.distanceTo(base.frontX(), base.y())));
        return alive;
    }

    private Enemy nearestEnemyWithin(Entity from, double range) {
        Enemy best = nearestEnemyTo(from.x(), from.y());
        return (best != null && best.distanceTo(from) <= range) ? best : null;
    }

    private Enemy nearestEnemyTo(double x, double y) {
        Enemy best = null;
        for (Enemy e : enemies) {
            if (!e.isDead() && (best == null || e.distanceTo(x, y) < best.distanceTo(x, y))) {
                best = e;
            }
        }
        return best;
    }

    /** กระสุนติดตามชนได้แค่เป้าตัวเอง ส่วนกระสุนพุ่งตรงชนตัวแรกที่เจอ */
    private Enemy findHit(Projectile p) {
        if (p.isHoming()) {
            return p.overlaps(p.target()) ? p.target() : null;
        }
        for (Enemy e : enemies) {
            if (!e.isDead() && p.overlaps(e)) {
                return e;
            }
        }
        return null;
    }

    /** ใช้ผลคาถากับตัวที่โดน หรือทุกตัวในรัศมีระเบิด */
    private void resolveImpact(Projectile p, Enemy struck) {
        burstAt(p.x(), p.y(), p.spell().color(), 8 + (int) (8 * p.tier().multiplier()));
        if (p.splashRadius() <= 0) {
            applySpell(p, struck);
            return;
        }
        for (Enemy e : enemies) {
            if (!e.isDead() && e.distanceTo(p) <= p.splashRadius() + e.radius()) {
                applySpell(p, e);
            }
        }
        burstAt(p.x(), p.y(), new Color(255, 220, 120), 24); // ระเบิดใหญ่
    }

    /** ใช้ผลคาถากับศัตรู 1 ตัว แล้วแสดงตัวเลขดาเมจที่ทำได้จริง */
    private void applySpell(Projectile p, Enemy e) {
        double before = e.hp();
        p.spell().impact(this, e, p.tier());
        int dealt = (int) Math.round(before - e.hp());
        if (dealt > 0) {
            effects.add(new FloatingText(e.x() + fxRandom.nextInt(21) - 10, e.y() - e.radius() - 12,
                    "-" + dealt, new Color(255, 240, 200), 16));
        }
    }

    /** เศษประกายพุ่งออกรอบจุด (x, y) */
    void burstAt(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = fxRandom.nextDouble() * Math.PI * 2;
            double speed = 60 + fxRandom.nextDouble() * 160;
            effects.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    3 + fxRandom.nextDouble() * 4, 0.3 + fxRandom.nextDouble() * 0.4, color));
        }
    }

    private void updateCombo(Tier tier) {
        if (tier == Tier.PERFECT) {
            combo++;
            bestCombo = Math.max(bestCombo, combo);
        } else {
            combo = 0;
        }
    }

    private void sweepDead() {
        for (Enemy e : enemies) {
            if (e.isDead() && e.hp() <= 0) {
                enemiesDefeated++;
                gold += e.bounty();
                burstAt(e.x(), e.y(), e.bodyColor(), 8); // ศัตรูแตกกระจาย
            }
        }
        army.sweep();
        enemies.removeIf(Entity::isDead);
        projectiles.removeIf(Entity::isDead);
        effects.removeIf(Entity::isDead);
    }

    private void announceWave() {
        int wave = waves.currentWave();
        if (wave != lastAnnouncedWave && wave > 0) {
            lastAnnouncedWave = wave;
            String text = (wave == waves.totalWaves()) ? "Wave " + wave + " — BOSS!" : "Wave " + wave;
            effects.add(new FloatingText(WIDTH / 2.0, HEIGHT / 2.0, text, new Color(255, 230, 120), 36));
        }
    }

    private void addText(String text, Color color) {
        textSlot = (textSlot + 1) % 3;
        effects.add(new FloatingText(wizard.x() + 60, wizard.y() - 40 - textSlot * 26, text, color));
    }

    private void drawLockMarker(Graphics2D g) {
        Enemy t = currentLockTarget();
        if (t == null) {
            return;
        }
        int r = (int) t.radius() + 8;
        g.setColor(wizard.manualLock() != null ? new Color(255, 80, 80) : new Color(255, 255, 255, 170));
        g.setStroke(new BasicStroke(2));
        int cx = (int) t.x();
        int cy = (int) t.y();
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.drawLine(cx - r - 6, cy, cx - r + 4, cy);
        g.drawLine(cx + r - 4, cy, cx + r + 6, cy);
        g.drawLine(cx, cy - r - 6, cx, cy - r + 4);
        g.drawLine(cx, cy + r - 4, cx, cy + r + 6);
        g.setStroke(new BasicStroke(1));
    }

    private static Color colorFor(Tier tier) {
        return switch (tier) {
            case PERFECT -> new Color(255, 215, 0);
            case GREAT -> new Color(120, 255, 120);
            case GOOD -> new Color(150, 200, 255);
            case WEAK -> new Color(255, 170, 90);
            case FIZZLE -> new Color(170, 170, 170);
        };
    }
}
