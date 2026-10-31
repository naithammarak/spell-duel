# DESIGN: Spell Duel

เอกสารออกแบบสถาปัตยกรรมที่อนุมัติแล้ว (27 ต.ค. 2026 แทนฉบับ tower defense เมื่อ 11 ต.ค. · 31 ต.ค. เปลี่ยนกติกาเป็นแบบรอบ) ใช้คู่กับ `SPEC.md`: SPEC บอกว่าเกมต้อง**ทำอะไร** ส่วนเอกสารนี้บอกว่าโค้ด**จัดโครงอย่างไร**
ถ้าตอนเขียนโค้ดต้องเบี่ยงจากเอกสารนี้ ให้แก้เอกสารก่อน แล้วค่อยแก้โค้ด

## 1. ภาพรวม

เปรียบเกมเป็นสนามมวย:
- **`GameWorld` = กรรมการ + สนาม** รู้กติกาทั้งหมด (ใครโดนตี โล่กันได้ไหม ใครชนะ) และถือข้อมูลทุกอย่างของการดวล 1 ครั้ง
- **`PlayerWizard` / `BotWizard` = นักมวยสองคน** มี HP, โล่ และคาถาที่กำลังร่าย นักมวยไม่ตัดสินเอง ทุกอย่างผ่านกรรมการ
- **`BotBrain` = โค้ชของบอท** ดูสถานการณ์แล้วบอกบอทว่าจะร่ายอะไรต่อ เปลี่ยนโค้ชก็ได้บอทนิสัยใหม่
- **Swing = กล้องถ่ายทอดสด** รับปุ่มที่ผู้เล่นกดแล้วส่งให้กรรมการ และวาดภาพสนาม ห้ามแก้ข้อมูลในเกมเอง
- **`GameLoop` = นาฬิกาจับเวลา** มี thread เดียว ทุก ~1/60 วินาทีจะเรียก `world.update(dt)` แล้วเรียก `panel.repaint()`

ข้อดี: กรรมการไม่ต้องมีจอ จึงทดสอบกติกาทั้งหมดด้วย JUnit ได้โดยไม่ต้องเปิดเกม

## 2. Package

dependency ชี้ไปทางเดียว ห้ามย้อนกลับ: `ui ──▶ model ──▶ typing`

| Package | มีอะไร | กฎ |
|---|---|---|
| `spellduel.typing` | `Levenshtein`, `Tier`, `AccuracyGrader`, `InputBuffer` (**ของเดิม** เปลี่ยนชื่อ package และเกณฑ์ `Tier`: Good ≥ 75%, Weak ≥ 50%) | logic ล้วน ใช้แค่ JDK |
| `spellduel.model` | `GameWorld`, นักเวท, การ์ด, สมองบอท, projectile, effect, `Hud`, `Assets`, `GameStats` | ใช้ `java.awt` ได้ แต่**ห้าม import `javax.swing`** (`ArchitectureTest` ตรวจ) · ทุก class เป็น package-private ยกเว้น `GameWorld`, `GameStats`, `Assets` |
| `spellduel.ui` | `GameWindow`, `MenuPanel`, `GamePanel`, `ResultPanel`, `GameLoop` | Swing อยู่ที่นี่ที่เดียว คุยกับ model ผ่าน `GameWorld` เท่านั้น |
| `spellduel` | `Main` | เปิดหน้าต่างผ่าน `SwingUtilities.invokeLater` |

`InputBuffer.MAX_LENGTH = 24` พอสำหรับคาถาที่ยาวที่สุด (`petrificus totalus` / `protego horribilis` = 18 ตัว)

## 3. Thread (ยกกฎเดิมมาทั้งหมด)

- **กฎข้อเดียว:** public method **ทุกตัว**ของ `GameWorld` เป็น `synchronized` ใครจะเข้าต้องถือกุญแจ และเข้าได้ทีละคน
- **ลบทีหลัง:** ระหว่าง `update` ของบนจอเรียกแค่ `kill()` แล้ว `sweepDead()` ลบทีเดียวตอนท้าย tick
- **ห้ามเรียก Swing ระหว่างถือกุญแจ:** ตอนจบเกม `GameLoop` ดึง `world.stats()` ออกมาก่อน แล้วค่อย `SwingUtilities.invokeLater(...)` ไปหน้า Result
- **Pause** ใช้ `volatile boolean paused` ใน `GameLoop` ห้ามใช้ `Thread.suspend()`
- **dt** วัดจาก `System.nanoTime()` และจำกัดด้วย `MAX_DT` · **เวลาในเกม** `GameWorld.clock` = ผลรวม dt ใช้กับทุกอย่าง (parTime, cast bar, โล่, พักของบอท) ตอน pause จึงไม่นับเวลา และเทสต์คุมเวลาได้ด้วย `update(dt)`
- **Focus:** หลังสลับ card ต้องเรียก `requestFocusInWindow()` และ `GamePanel` ต้องเรียก `setFocusTraversalKeysEnabled(false)`

## 4. Public API ของ `GameWorld`

```java
public GameWorld(int botLevel, long seed)           // ดวล 1 ครั้ง (0 = Apprentice, 1 = Auror, 2 = Dark Wizard) · Retry = new GameWorld(เลเวลเดิม)
public synchronized void typeChar(char c)           // EDT
public synchronized void backspace()                // EDT
public synchronized CastResult castTyped()          // EDT: Enter
public synchronized void update(double dt)          // game thread
public synchronized void draw(Graphics2D g, Assets assets) // EDT
public synchronized Status status()                 // PLAYING / WON / LOST
public synchronized GameStats stats()               // record ที่แก้ไม่ได้ ใช้ในหน้า Result
public static final int BOT_COUNT = 3;
```

เทสต์ใช้ constructor แบบ package-private: `new GameWorld(BotProfile.row(i), brain, SpellDeck.standard(new Random(1)))` และ `resolveRound(SpellRole, Tier, SpellRole, Tier)` เพื่อจัดฉากเปิดไพ่ได้ตรง ๆ

## 5. กติกาการดวล

เปรียบเหมือนเป่ายิ้งฉุบที่ต้องพิมพ์ก่อนออกมือ: ทั้งสองฝ่ายเตรียมมือพร้อมกัน แล้วกรรมการสั่งเปิดพร้อมกัน

### 5.1 วงจรของรอบ
`GameWorld.phase` มี 2 ช่วง: `TYPING → REVEAL → TYPING รอบถัดไป`

ค่าคงที่อยู่ใน `GameWorld`: `ROUND_TIME` 8 วินาที · `REVEAL_TIME` 1.6 วินาที · `GRACE_TIME` 1 วินาที · `EARLY_BONUS` ×1.5 · `CLASH_SCALE` ×0.25

**TYPING จบเมื่อ**เกิดข้อใดข้อหนึ่งก่อน
1. ล็อกครบทั้งสองฝ่าย
2. บอทล็อกแล้วผ่านไป `GRACE_TIME` (กันการรอดูไพ่แล้วสวนสบาย ๆ)
3. ครบ `ROUND_TIME` · ถ้าผู้เล่นยังไม่ล็อก ระบบเรียก `castTyped()` ส่งข้อความที่ค้างให้

จากนั้น `resolveRound` เปิดไพ่และเข้า REVEAL · REVEAL จบเมื่อผ่าน `REVEAL_TIME` และไม่มีลูกเวทบินอยู่ · `startRound` ล้างโล่ทั้งสองฝ่าย (**โล่อยู่รอบเดียว**) แล้วถามสมองบอท

### 5.2 ผู้เล่นร่าย (`castTyped()` ที่เดียว)
ตัวอย่าง: การ์ด Attack คือ `bombarda maxima` แล้วผู้เล่นพิมพ์ `bombarda maxma` กด Enter
1. `InputBuffer` ให้ข้อความที่พิมพ์ และเวลาที่ใช้ (นับจากกดตัวแรก) แล้วล้าง buffer
2. `SpellDeck.match(typed)` เทียบกับการ์ด **3 ใบที่อยู่บนมือ** ด้วย Levenshtein → การ์ด Attack, distance 1 (ถ้าเสมอกันเลือกคำที่สั้นกว่า)
3. `AccuracyGrader.grade(distance, length, seconds)` → Great
4. ถ้าได้ FIZZLE จะขึ้นข้อความ "Fizzle!" การ์ดไม่ถูกใช้ และพิมพ์ใหม่ได้ถ้ายังมีเวลา
5. ถ้าไม่ใช่ FIZZLE: `deck.consume(card)` สุ่มคำใหม่ให้การ์ดใบนั้น แล้ว**ล็อก**หน้าที่กับ tier ไว้รอเปิดไพ่ พิมพ์ต่อไม่ได้แล้ว · ถ้าบอทยังไม่เปิดไพ่ จะจำว่า `lockedEarly`

### 5.3 บอทร่าย
- ต้นรอบ `BotWizard.startRound` ถาม `brain.decide(view)` แล้วสร้าง `Cast` (หน้าที่, คำ, `duration = reaction + length / cps` ไม่เกิน `MAX_CAST`, `elapsed`) ซึ่งก็คือ cast bar บนจอ
- `Cast.isRevealed()`: ถึงครึ่งแถบ (`REVEAL_AT`) จึงแสดงหน้าที่ ก่อนหน้านั้น HUD แสดง "???"
- ทุก tick เรียก `brain.duringCast(view, cast)` ซึ่งเป็นช่องที่ Dark Wizard ใช้หลอก
- แถบเต็ม = บอทล็อก · Tier สุ่มตอนเปิดไพ่ด้วย `Random` จาก `profile.accuracy`

### 5.4 ผลของการเปิดไพ่ (`resolveRound` + `resolveHit` ตัดสิน · `power = base × tier.multiplier() × scale`)

| ผู้เล่น \ บอท | Attack | Shield | Interrupt |
|---|---|---|---|
| **Attack** | โดนทั้งคู่ | ผู้เล่นโดนลูกสะท้อน | บอทโดน · Interrupt แตก |
| **Shield** | บอทโดนลูกสะท้อน | ไม่เกิดอะไร | โล่แตก ผู้เล่นโดนเต็ม |
| **Interrupt** | Interrupt แตก · ผู้เล่นโดน | โล่แตก บอทโดนเต็ม | เบาทั้งคู่ (`CLASH_SCALE`) |

- **Shield:** ตั้งโล่ทันทีตอนเปิดไพ่ · แรงลูกสะท้อน = power ของ Attack × ตัวคูณ tier ของโล่ × `bonus` ของโล่
- **Interrupt แตก:** `release` ไม่สร้าง `Projectile` เลย
- **ไม่ล็อก:** หน้าที่เป็น `null` ไม่ปล่อยอะไร จึงโดนของอีกฝ่ายเต็ม ๆ
- **กล้าเดา:** ถ้า `lockedEarly` คูณ scale ฝั่งผู้เล่นด้วย `EARLY_BONUS` (Attack และ Interrupt ที่ไปถึงเป้า และ `Shield.bonus`) เพื่อให้การเดาคุ้มกว่าการรอดูไพ่

เหตุผล (31 ต.ค. playtest): แบบ real-time พิมพ์ใบเดียวรัวก็ชนะ และ Interrupt แทบไม่ได้ใช้ เพราะต้องสวนให้ทันตอนบอทกำลังร่าย แบบรอบทำให้ทุกใบมีทางชนะจริง ผู้เล่นจำลองที่รัวใบเดียวที่ 4 ตัว/วินาที ชนะ Auror ไม่เกิน 7% และ Dark Wizard ไม่เกิน 14%

### 5.5 จบเกม
HP ฝั่งใดถึง 0 → `status()` เป็น WON หรือ LOST · `GameLoop` เห็นแล้วพาไปหน้า Result
`GameWindow` เก็บ `botLevel` ไว้: ชนะ → Next (`level + 1`) · แพ้ → Retry (เลเวลเดิม) · ชนะเลเวล 2 → หน้า "จบเกม"

## 6. บอท

### 6.1 `BotProfile`: ตาราง `int[][] STATS` (แถว = บอท)

| บอท | HP | cps×10 (ความเร็วพิมพ์) | accuracy % | reaction ms (พักก่อนร่ายต่อ) |
|---|---|---|---|---|
| 0 Apprentice | 60 | 25 | 70 | 1500 |
| 1 Auror | 90 | 35 | 85 | 700 |
| 2 Dark Wizard | 120 | 45 | 95 | 400 |

ผู้เล่นมี HP 100 · ค่า base: Attack 20, Interrupt 20 · `reaction` บวกเข้าไปในเวลาร่ายของทุกรอบ
`BotProfile.row(i)` แปลงแถวเป็น record (`cps = cps×10 / 10.0`) และ constructor ไม่ยอมรับแถวที่ความกว้างผิด

### 6.2 `interface BotBrain`
```java
interface BotBrain {
    SpellRole decide(DuelView view);                     // ต้นรอบ: จะร่ายอะไร
    default SpellRole duringCast(DuelView view, Cast c) { return null; } // null = ไม่เปลี่ยน
}
```
`DuelView` เป็น record ที่อ่านได้อย่างเดียว ส่งให้สมองบอทดู: HP ทั้งสองฝั่ง, สถานะโล่, `playerIntent`, `lastPlayerRole` (การ์ดรอบก่อนของผู้เล่น) และ `Random`

| Brain | นิสัย |
|---|---|
| `ApprenticeBrain` | ATTACK 70% · ที่เหลือสุ่ม SHIELD หรือ INTERRUPT |
| `AurorBrain` | ถ้ามี `lastPlayerRole`: 70% ออก `lastPlayerRole.beatenBy()` · นอกนั้นสุ่ม 3 ใบ |
| `DarkWizardBrain` | ตอนตัดสินใจใช้กฎเดียวกับ Auror (สืบทอด) · ใน `duringCast` เมื่อถึง 3/4 แถบ มีโอกาส 30% (ครั้งเดียวต่อ cast) ที่จะเปลี่ยนหน้าที่ ป้ายบน cast bar เปลี่ยนแต่เวลาที่เหลือเท่าเดิม |

ตอนนี้ไม่มีบอทตัวไหนใช้ `playerIntent` · `SpellDeck.guessRole` ยังใช้จริงกับ HUD เพื่อไฮไลต์การ์ดที่ผู้เล่นกำลังพิมพ์

## 7. การ์ด: `SpellDeck`

- `enum SpellRole { ATTACK, SHIELD, INTERRUPT }` เก็บ `base`, `label`, สี, ความเร็ว projectile และ `beatenBy()` (ใบที่ชนะใบนี้) ไว้ใน enum
- `static final String[][] POOLS` (แถว = role ตามลำดับ `ordinal()`, คอลัมน์ = 5 คาถาใน SPEC §2) เป็น **2D array**
- `Incantation[] hand` (3 ใบ, ใบละ role) · `consume(card)` สุ่มคำใหม่จากแถวเดิม โดยห้ามซ้ำกับคำปัจจุบัน
- บอทก็สุ่มคำจาก `POOLS` เหมือนกัน (เพื่อเอามาแสดงบน cast bar และคำนวณ duration) แต่ไม่ได้ใช้มือของผู้เล่น

## 8. Class diagram

```mermaid
classDiagram
    direction TB
    class GameWindow {
        -CardLayout cards
        -int botLevel
        +startDuel(int)
        +showResult(GameStats)
    }
    class GamePanel {
        +keyPressed(KeyEvent)
        +keyTyped(KeyEvent)
        #paintComponent(Graphics)
    }
    class GameLoop {
        -volatile boolean paused
        +run()
        +togglePause()
    }
    class Runnable {
        <<interface>>
    }
    Runnable <|.. GameLoop
    GameWindow *-- MenuPanel
    GameWindow *-- GamePanel
    GameWindow *-- ResultPanel
    GamePanel --> GameWorld
    GameLoop --> GameWorld

    class GameWorld {
        -PlayerWizard player
        -BotWizard bot
        -SpellDeck deck
        -InputBuffer input
        -ArrayList~Projectile~ projectiles
        -ArrayList~Entity~ effects
        -double clock
        -Phase phase
        -SpellRole lockedRole
        +typeChar(char)
        +backspace()
        +castTyped() CastResult
        +update(double)
        +draw(Graphics2D, Assets)
        +status() Status
        +stats() GameStats
        ~resolveRound(SpellRole, Tier, SpellRole, Tier)
        ~resolveHit(Projectile, Duelist)
    }
    class Updatable {
        <<interface>>
        +update(double)
    }
    class Drawable {
        <<interface>>
        +draw(Graphics2D, Assets)
    }
    class Entity {
        <<abstract>>
        #double x
        #double y
        +kill()
        +isDead()
    }
    Updatable <|.. Entity
    Drawable <|.. Entity
    Entity <|-- Duelist
    Entity <|-- Projectile
    Entity <|-- Particle
    Entity <|-- FloatingText

    class Duelist {
        <<abstract>>
        #int hp
        #Shield shield
        +takeHit(SpellRole, double)
        +raiseShield(Tier, double)
        +interruptCast()*
    }
    Duelist <|-- PlayerWizard
    Duelist <|-- BotWizard
    class PlayerWizard {
        +interruptCast()
    }
    class BotWizard {
        -Cast cast
        -BotProfile profile
        -BotBrain brain
        +startRound(DuelView, Random)
        +think(double, DuelView, Random)
        +interruptCast()
    }
    Duelist o-- Shield
    BotWizard o-- Cast
    BotWizard --> BotBrain
    BotWizard --> BotProfile

    class BotBrain {
        <<interface>>
        +decide(DuelView) SpellRole
        +duringCast(DuelView, Cast) SpellRole
    }
    BotBrain <|.. ApprenticeBrain
    BotBrain <|.. AurorBrain
    AurorBrain <|-- DarkWizardBrain
    class BotProfile {
        <<record>>
        +STATS int[][]$
        +row(int)$ BotProfile
    }

    class SpellRole {
        <<enumeration>>
        ATTACK
        SHIELD
        INTERRUPT
    }
    class Incantation {
        <<record>>
        String text
        SpellRole role
    }
    class SpellDeck {
        -POOLS String[][]$
        -Incantation[] hand
        +match(String) Match
        +consume(Incantation)
        +guessRole(String) SpellRole
    }
    SpellDeck o-- Incantation
    Projectile --> SpellRole

    GameWorld *-- PlayerWizard
    GameWorld *-- BotWizard
    GameWorld *-- SpellDeck
    GameWorld *-- InputBuffer
    GameWorld *-- AccuracyGrader
    GameWorld o-- Projectile
    GameWorld ..> Hud
    GameWorld ..> GameStats

    class Levenshtein {
        +distance(String, String)$ int
    }
    class AccuracyGrader {
        +grade(int, int, double) Tier
    }
    class Tier {
        <<enumeration>>
        PERFECT
        GREAT
        GOOD
        WEAK
        FIZZLE
    }
    SpellDeck ..> Levenshtein
    AccuracyGrader ..> Tier
```

## 9. แต่ละเรื่องมีเจ้าของคนเดียว

| ข้อมูล / กฎ | เจ้าของ | คนอื่นทำได้แค่ |
|---|---|---|
| ข้อความที่พิมพ์ + เวลาเริ่ม | `InputBuffer` | `GameWorld` ส่ง `clock` เข้าไป · `PlayerWizard.interruptCast()` สั่งล้าง |
| ตัวคูณและค่าตัดของ Tier | `Tier` | |
| คำบนการ์ด 3 ใบ + การสุ่มใหม่ | `SpellDeck` | HUD อ่าน `hand()` |
| "ผู้เล่นกำลังพิมพ์หน้าที่อะไร" | `SpellDeck.guessRole` | HUD ใช้ไฮไลต์การ์ด |
| ช่วงของรอบ + การ์ดที่ผู้เล่นล็อก | `GameWorld` | HUD อ่านผ่าน `draw` |
| ใบไหนชนะใบไหน | `SpellRole.beatenBy` | Auror ใช้เลือกการ์ด |
| cast bar ของบอท | `BotWizard.cast` | HUD อ่าน `progress()` และ `role()` |
| HP + โล่ | `Duelist` | `GameWorld.resolveHit` เป็นคนเรียก `takeHit` |
| กติกาว่าเปิดไพ่แล้วเกิดอะไร (§5.4) | `GameWorld.resolveRound` + `resolveHit` **ที่เดียว** | |
| stat ของบอท | `BotProfile.STATS` | |
| นิสัยบอท | `BotBrain` แต่ละตัว | |

## 10. หัวข้อที่เรียน → class ที่ใช้ (ใช้ในรายงานและสไลด์)

| หัวข้อ | ใช้ที่ไหน |
|---|---|
| Class/Object | `Duelist`, `Cast`, `Shield`, `SpellDeck`, `Incantation` |
| Method | `Levenshtein.distance`, `SpellDeck.match/guessRole`, `AccuracyGrader.grade`, `GameWorld.resolveHit` |
| Array | `Incantation[] hand`, `int[] tierCounts` ใน `GameStats` |
| 2D array | `String[][] POOLS`, `int[][] STATS`, `int[][] dp` ใน `Levenshtein` |
| ArrayList | `projectiles`, `effects` |
| Inheritance | `Entity → Duelist → PlayerWizard/BotWizard`, `AurorBrain → DarkWizardBrain` |
| Polymorphism | `brain.decide(...)` (บอท 3 นิสัย), `duelist.interruptCast()` (จบรอบ: ผู้เล่นล้างช่องพิมพ์ / บอททิ้ง cast bar), `entity.draw(...)` |
| Interface/Abstract | `BotBrain`, `Updatable`, `Drawable`, `Runnable` · `abstract Entity/Duelist` |
| GUI/Event | `GameWindow` (CardLayout), `MenuPanel`/`ResultPanel` (ActionListener), `GamePanel` (KeyListener) |
| Graphics | `draw(Graphics2D)` ของแต่ละ entity, `Hud` (การ์ด, cast bar, HP bar), `Assets` |
| Thread | `GameLoop implements Runnable`, `volatile`, `synchronized`, `invokeLater` |

## 11. Test (JUnit 5, เทสต์เฉพาะ model)

| Test class | ตรวจอะไร | วัน |
|---|---|---|
| `LevenshteinTest`, `AccuracyGraderTest`, `InputBufferTest`, `ArchitectureTest` | ของเดิม (เปลี่ยน package และเกณฑ์ Tier) | D1 |
| `SpellDeckTest` | match ได้การ์ดที่ถูก, consume แล้วได้คำใหม่ที่ไม่ซ้ำ, แต่ละแถวใน POOLS มีอย่างน้อย 3 คำ, `guessRole` คืน null ถ้าพิมพ์ไม่ถึง 3 ตัว | D2 |
| `GameWorldTest` | ทุกช่องในตาราง §5.4, Fizzle แล้วการ์ดยังอยู่, วงจรรอบ, `GRACE_TIME`, `EARLY_BONUS`, WON/LOST | D2 |
| `BotBrainTest` | วงจร `beatenBy`, Apprentice ออก ATTACK ส่วนใหญ่, Auror สวนการ์ดรอบก่อน, เปิดหน้าที่ตอนครึ่งแถบ, Dark Wizard หลอกตามเงื่อนไข, บอทล็อกก่อนหมดรอบเสมอ (seed คงที่) | D3 |
| `BotProfileTest` | ทุกแถวกว้างเท่ากัน, ค่าบอทตัวหลังเก่งกว่าตัวก่อน | D3 |

## 12. ถ้าเวลาไม่พอ ให้ตัดตามลำดับนี้
1. การหลอกของ Dark Wizard → ให้เป็น Auror ที่เร็วขึ้นแทน (ยังมี subclass อยู่)
2. แรงลูกสะท้อนตาม tier ของโล่ → สะท้อนแรงเท่าเดิมทุก tier (ห้ามตัดการสะท้อนทิ้ง เพราะเป็นทางที่ Shield ชนะ Attack)
3. sprite → ใช้รูปทรง + สี (`Assets.none()`)

## 13. สิ่งที่ตั้งใจไม่ทำ
- ไม่มี thread แยกต่อตัวละคร · ไม่มี `reset()` (Retry = สร้าง `GameWorld` ใหม่)
- ไม่มี cooldown หรือ mana · ไม่มี event bus หรือ design pattern ที่ซับซ้อนเกินกว่าจะอธิบายได้

## 14. ทางเลือกที่พิจารณาแล้วไม่เลือก

| ทางเลือก | ทำไมไม่เลือก |
|---|---|
| ปรับ tower defense ต่อ | playtest แล้วไม่สนุก ปัญหา 4 ข้อใน SPEC เป็นปัญหาที่ตัวแนวเกม ไม่ใช่ตัวเลข |
| แข่งพิมพ์ล้วน (ใครเสร็จก่อนได้ตี) | ไม่มีการตัดสินใจ กลายเป็นเกมฝึกพิมพ์ |
| ผลัดกันเดิน (turn-based) | ช้า และทำให้หัวข้อ Thread ดูไม่เด่น (แบบรอบที่ใช้อยู่พิมพ์พร้อมกันแข่งกับเวลา จึงยังมี game thread เดินตลอด) |
| Real-time ร่ายแข่งกัน | ลองทำแล้ว (branch `duel`) พิมพ์รัวใบเดียวชนะได้ และ Interrupt แทบไม่ได้ใช้ แม้ปรับกติกาและความยาวคำหลายรอบ |
| Beam Clash | ต้องเพิ่มโหมดย่อยอีก 1 โหมด ไม่ทันใน 4 วัน |
| บอทต่างกันแค่ตัวเลข | บอททั้ง 3 ตัวจะรู้สึกเหมือนกัน และเสีย polymorphism ที่จะโชว์ในรายงาน |
| คาถา 3 คำตายตัว | ผู้เล่นพิมพ์คำเดิมซ้ำจนเบื่อ (ปัญหาเดียวกับ tower defense) |
| เขียน `typing` ใหม่ | ของเดิมใช้กับเกมแนวไหนก็ได้ และมี test ผ่านอยู่แล้ว |
