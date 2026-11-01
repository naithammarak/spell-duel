# ทำความเข้าใจ Spell Duel

ร่างนี้ให้ใช้อ่านคู่กับโค้ด เมื่ออ่านแต่ละส่วนจบ ให้ปิดไฟล์แล้วอธิบายด้วยคำพูดของตัวเอง และแก้โน้ตนี้ให้เป็นสำนวนของคุณ

## 1. ภาพใหญ่ใน 1 นาที

เกมนี้เหมือนสนามมวยที่มีกล้องถ่ายทอดสด

| ของจริง | ในโค้ด | หน้าที่ |
|---|---|---|
| กรรมการ + สนาม | `GameWorld` | ถือข้อมูลการดวล และตัดสินกติกาทั้งหมด |
| นักมวย | `PlayerWizard`, `BotWizard` | มี HP และโล่ แต่ไม่ตัดสินเอง |
| โค้ชของบอท | `BotBrain` | เลือกว่ารอบนี้จะออกการ์ดอะไร |
| นาฬิกาจับเวลา | `GameLoop` | เรียก `update` ประมาณ 60 ครั้งต่อวินาที |
| กล้องถ่ายทอดสด | Swing (`GamePanel`) | รับปุ่มที่กด และวาดจอ ไม่แก้กติกาเอง |

ลูกศรของ package ไปทางเดียว: `ui → model → typing`
ข้อดีคือกรรมการไม่ต้องมีจอ จึงใช้ JUnit ทดสอบกติกาได้โดยไม่ต้องเปิดเกม

## 2. เดินตามโค้ด 1 รอบ

ตัวอย่าง: ผู้เล่นพิมพ์ `bombarda maxima` ถูกหมดและทันเวลา ก่อนที่บอทจะร่าย Interrupt เสร็จ

1. `GameWorld.startRound()` รีเซ็ตค่าของรอบ แล้วเรียก `bot.startRound(...)`
2. บอทถาม `brain.decide(view)` แล้วสร้าง `Cast` ซึ่งก็คือ cast bar
3. ช่วง `COUNTDOWN` นับ 3-2-1 ก่อน ระหว่างนี้ยังพิมพ์ไม่ได้
4. ช่วง `TYPING` ทุกเฟรม `GameLoop` เรียก `world.update(dt)` และ cast bar ขยับผ่าน `bot.think(...)`
5. ผู้เล่นกดปุ่ม → `GamePanel.keyTyped` → `world.typeChar(c)` → เก็บใน `InputBuffer`
6. ผู้เล่นกด Enter → `world.castTyped()`
   - `SpellDeck.match` ใช้ `Levenshtein` หาการ์ดที่ใกล้ที่สุด
   - `AccuracyGrader.grade` ให้เกรด `Tier.PERFECT`
   - `deck.consume` เปลี่ยนคำบนการ์ด แล้วจำไว้ใน `lockedRole` ว่ารอบนี้ใช้แล้ว
   - `release(...)` สร้าง `Projectile` ทันที
7. สายฟ้าถึงเป้า → `resolveHit(...)` หัก HP บอท 20 × 1.25 = **25**
8. บอทไม่มีโล่ และ Attack ระดับ PERFECT ขัดได้ → `cutCast` ทำให้บอทเสียการร่ายรอบนี้
9. จบช่วงพิมพ์ → ช่วง `REVEAL` รอสายฟ้าถึงเป้า → `startRound()` เริ่มรอบถัดไป
10. ถ้า HP ฝ่ายใดหมด `status` จะเปลี่ยน แล้ว `GameLoop` พาไปหน้า Result ถ้าชนะ `GameWindow.nextDuel` สร้างด่านถัดไปด้วย `stats.nextHp()`

## 3. ลำดับการอ่าน (ประมาณ 3 ชั่วโมง)

| ลำดับ | อ่าน | สิ่งที่ต้องตอบได้ |
|---|---|---|
| 1 | `SpellRole`, `Incantation`, `Tier` | enum คืออะไร ทำไมเก็บตัวคูณไว้ใน enum |
| 2 | `Levenshtein`, `AccuracyGrader`, `InputBuffer` | ตาราง `dp` เติมอย่างไร เกรดมาจากอะไร |
| 3 | `SpellDeck` | `POOLS` เก็บอะไร ทำไม `hand()` คืนสำเนา |
| 4 | `Entity`, `Updatable`, `Drawable`, `Duelist` | ทำไมมีทั้ง interface และ abstract class |
| 5 | `PlayerWizard`, `BotWizard`, `Cast`, `Shield` | `interruptCast()` ต่างกันอย่างไรในแต่ละ class |
| 6 | `BotProfile`, `DuelView`, `BotBrain` + สมอง 3 ตัว | ทำไมบอทรู้ทุกอย่างไม่ได้ (ดูแค่ `DuelView`) |
| 7 | `Projectile`, `Particle`, `FloatingText` | ทำไมใช้ `kill()` แทนการลบออกจาก list ทันที |
| 8 | `GameWorld` | อธิบาย `castTyped`, `updateTyping`, `release`, `resolveHit`, `cutCast` ได้ |
| 9 | `GameLoop`, `GamePanel`, `GameWindow`, `MenuPanel`, `ResultPanel`, `Main` | ข้อมูลข้าม thread อย่างไรให้ปลอดภัย |
| 10 | `GameWorldTest`, `BotBrainTest` | เทสต์เดินเวลาโดยไม่รอเวลาจริงได้อย่างไร |

## 4. สรุปแต่ละ class

### typing: เรื่องการพิมพ์ ไม่รู้จักเกมเลย
- `Levenshtein`: นับว่าต้องแก้กี่ตัวอักษรจึงจะเป็นคำเป้าหมาย เหมือนครูนับจุดที่ต้องแก้ในข้อสอบสะกดคำ
- `Tier`: เกรดของคาถา และตัวคูณแรง (PERFECT 1.25, Great 1.0, Good 0.6, Weak 0.3, Fizzle 0)
- `AccuracyGrader`: แปลงจำนวนจุดผิดและเวลาเป็นเกรด PERFECT ต้องไม่ผิดเลยและทัน `parTime`
- `InputBuffer`: ช่องพิมพ์ จำข้อความและเวลาที่เริ่มพิมพ์

### model: การ์ด
- `SpellRole`: หน้าที่ 3 แบบ เก็บดาเมจ สี (Attack เขียว, Interrupt แดง) ความเร็ว และ `beatenBy()` ที่บอกว่าใบไหนชนะใบนี้
- `Incantation`: การ์ด 1 ใบ มีคำและหน้าที่
- `SpellDeck`: สำรับ 3 ใบ เลือกการ์ดที่ใกล้คำที่พิมพ์ และสุ่มคำใหม่ `guessRole` ใช้ไฮไลต์การ์ดบน HUD

### model: ตัวละครและของบนจอ
- `Updatable`, `Drawable`: สัญญาว่า "ขยับตามเวลาได้" และ "วาดตัวเองได้"
- `Entity`: แม่แบบของทุกอย่างที่มีตำแหน่งบนจอ
- `Duelist`: แม่แบบนักเวท มี HP โล่ และการวาดตัวละครแยกเป็น `drawRobe`, `drawHead`, `drawArm`, `drawShield`
- `PlayerWizard`: ผู้เล่น ถูกขัดหรือจบรอบแล้วล้างช่องพิมพ์ รับ HP เริ่มต้นจากด่านก่อนได้
- `BotWizard`: บอท ถาม `BotBrain` ต้นรอบ แล้วร่ายจนเต็มแถบ ถ้าเป็น Shield ใช้เวลาพิมพ์แค่ ×0.6 (`SHIELD_SPEED`)
- `Cast`: cast bar ถึง 20% ของแถบแล้วเปิดว่าบอทใช้สกิลอะไร
- `Shield`: โล่ เก็บ tier ที่ใช้คูณแรงลูกสะท้อน อยู่จนจบรอบ
- `Projectile`: สายฟ้าที่พุ่งหาเป้า `draw` วาดเส้นหยักซ้อน 3 ชั้น เหมือนหลอดไฟที่แกนขาวและมีแสงเรือง `canCut` บอกว่าแม่นพอจะขัดไหม `reflected` สร้างลูกสะท้อน
- `Particle`, `FloatingText`: เอฟเฟกต์ประกายไฟ และข้อความลอย

### model: บอท
- `BotProfile`: ค่าสถานะบอทจากตาราง `int[][] STATS` และ `forLevel` สร้างบอทด่านเกินตาราง เหมือนตารางคู่ต่อสู้ที่ต่อท้ายได้ไม่รู้จบ
- `DuelView`: แผ่นข้อมูลอ่านอย่างเดียวที่ให้สมองบอทดู
- `BotBrain`: สัญญาว่าสมองบอทต้องเลือกการ์ดได้
- `ApprenticeBrain`: ออก Attack 70% ให้คนเล่นใหม่อ่านทางง่าย
- `AurorBrain`: 55% ออกใบที่ชนะการ์ดรอบก่อนของผู้เล่น
- `DarkWizardBrain`: สืบทอด Auror แล้วเพิ่มการหลอกตอน 3/4 แถบ

### model: ระบบ
- `GameWorld`: กรรมการและสนาม ทุก public method เป็น `synchronized` · รอบเดิน `COUNTDOWN → TYPING → REVEAL`
- `GameStats`: สรุปผลที่แก้ไม่ได้ ส่งข้าม thread ไปหน้า Result ได้ปลอดภัย `nextHp` คำนวณ HP ด่านถัดไป `levelsCleared` นับด่านที่ผ่าน
- `Hud`: วาดฉาก HP bar, cast bar, ช่องพิมพ์ และการ์ด ฉากสนามประลองวาดครั้งเดียวแล้วเก็บเป็นรูปใน `arena` ยกเว้นผู้ชม 2 แถวหน้าที่วาดใหม่ทุกเฟรมเป็นคลื่นเชียร์ใน `drawLiveCrowd` ส่วน `glow` คือแสงฟุ้งที่ลูกเวทกับไม้กายสิทธิ์ใช้ร่วมกัน
- `Assets`: ตัวโหลดรูป ตอนนี้ไม่มีไฟล์รูป เพราะเกมวาดทุกอย่างด้วย `Graphics2D`

### ui
- `Main`: จุดเริ่มโปรแกรม เปิดหน้าต่างผ่าน `invokeLater`
- `GameWindow`: หน้าต่างหลัก สลับหน้าด้วย `CardLayout` เป็นเจ้าของการเล่น 1 รอบ (`startRun`, `nextDuel`) และบันทึกสถิติด้วย `Preferences`
- `MenuPanel`, `ResultPanel`: หน้าเมนูและหน้าผล ใช้ปุ่มกับ `ActionListener` แสดงสถิติสูงสุด · `MenuPanel` ใช้ `GameWorld` ตัวหนึ่งเป็นฉากหลังที่ขยับด้วย Swing `Timer` และปุ่ม `MagicButton` สืบทอด `JButton` เพื่อวาดพื้นปุ่มเอง
- `GamePanel`: รับคีย์บอร์ดด้วย `KeyListener` แล้ววาดจอ โดยวาดลงรูป `frame` ก่อนแล้วแปะขึ้นจอทีเดียว เหมือนวาดบนกระดาษทดแล้วค่อยติดบนกระดาน แทนการเขียนบนกระดานทีละเส้น
- `GameLoop`: game thread ใช้ `volatile paused` สำหรับหยุดเกม

## 5. หัวข้อวิชา → จุดที่ชี้ให้อาจารย์ดู

| หัวข้อ | เปิดตรงไหน | พูดว่า |
|---|---|---|
| 2D array | `SpellDeck.POOLS`, `BotProfile.STATS`, `Levenshtein.dp` | แถวคือหน้าที่หรือบอท คอลัมน์คือข้อมูลย่อย |
| Inheritance | `DarkWizardBrain extends AurorBrain` | ได้การเลือกการ์ดมาฟรี แล้วเพิ่มการหลอก |
| Polymorphism | `brain.decide(view)`, `interruptCast()` | เรียกชื่อเดียว แต่แต่ละ class ทำงานต่างกัน |
| Interface/Abstract | `BotBrain`, `abstract Duelist` | interface คือสัญญา abstract class มีโค้ดร่วมให้ด้วย |
| Thread | `GameLoop`, `synchronized`, `volatile`, `invokeLater` | game thread กับ EDT เข้า `GameWorld` พร้อมกันได้ จึงต้องมีกุญแจ |
| GUI/Event | `GamePanel.keyTyped` | Swing ส่ง event มา แล้วเราส่งต่อให้ `GameWorld` |
| Graphics | `Hud.drawCard`, `Duelist.draw` | วาดด้วย `Graphics2D` |

## 6. คำถามซ้อมก่อนพรีเซนต์

ตอบด้วยคำพูดของตัวเองโดยไม่เปิดโค้ด

1. ทำไม `model` ห้าม import `javax.swing`
2. ถ้าไม่มี `synchronized` จะเกิดอะไรได้บ้าง
3. ทำไม `GameLoop` ต้องดึง `stats()` ก่อนเรียก `invokeLater`
4. ทำไม `updateProjectiles` วน list ด้วย index แทน for-each
5. เพิ่มบอทตัวใหม่ต้องแก้ไฟล์ไหนบ้าง
6. ทำไมเปลี่ยนจาก real-time เป็นรอบละ 1 คาถาที่ทำงานทันที
7. `GRACE_TIME` และ `SHIELD_SPEED` แก้ปัญหาอะไร
8. Levenshtein ของ `cat` กับ `cut` เท่ากับเท่าไร และเติมตารางอย่างไร
9. ทำไม `Duelist` ต้องเป็น abstract
10. เทสต์ใน `GameWorldTest` เดินเวลาโดยไม่ต้องรอจริงได้อย่างไร

## 7. จุดที่อาจโดนถาม

- `DuelView.playerIntent`, `playerShielded`, `botShielded` ยังไม่มีสมองตัวไหนใช้ เป็นของเหลือจากแบบ real-time
- `Shield.DURATION` มีผลกับความจางของโล่บนจอ แต่กติกาจริงคือโล่หายตอนขึ้นรอบใหม่
- โหมดไต่ด่านเพิ่มหลัง feature freeze 30 ต.ค. เพราะ playtest แล้วบอทอ่อน และอยากให้เป็นเกมลูปมากขึ้น
- `Hud` อยู่ใน `model` เพราะต้องอ่าน class แบบ package-private ได้
