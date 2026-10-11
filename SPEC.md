# SPEC: Spell Defender

## 1. Goal
เกม 2D pixel top-down แนว tower defense ผู้เล่นเป็นพ่อมดยืนป้องกันฐาน ร่ายคาถาสไตล์ Harry Potter ด้วยการ **พิมพ์**
ยิ่งพิมพ์ถูกและเร็ว คาถายิ่งแรง ถ้าพิมพ์ผิดมาก คาถาจะอ่อนลงหรือไม่ออกเลย (Fizzle)
เป็น mini project วิชา OOP (ทำคนเดียว) ใช้ Java 17 + Swing ต้องใช้ทุกหัวข้อที่เรียนอย่างมีเหตุผล และอธิบายได้ทุก class ตอนพรีเซนต์

## 2. Features
### Must-have
- **การพิมพ์**: A–Z/Space พิมพ์, Backspace ลบ, Enter ร่าย, Esc หยุดเกม
- **คาถา 5 อัน** แต่ละอันมี cooldown ของตัวเอง และเล็งคนละแบบ

  | คาถา | การเล็ง | ผล |
  |---|---|---|
  | `stupefy` | Lock-on | ดาเมจน้อย + stun |
  | `incendio` | Directional | ลูกไฟพุ่งตามทิศ ระเบิดโดนหลายตัว (AoE) |
  | `protego` | Self | โล่ป้องกันฐาน ดูดซับดาเมจ |
  | `expelliarmus` | Lock-on | ดาเมจกลาง + knockback |
  | `expecto patronum` | Lock-on | แรงมากกับ Dementor, cooldown ยาว |
- **การเล็ง**: พ่อมดยืนนิ่งที่ฐาน ใช้ ←/→ หมุนทิศไม้กายสิทธิ์ (สำหรับ Directional) ส่วน Tab ใช้สลับเป้า Lock-on (ค่าเริ่มต้นคือตัวที่ใกล้ฐานที่สุด) และวาด marker บนตัวที่ถูกล็อก
- **ระบบความแม่นยำ** (หัวใจของเกม)
  1. หาคาถาที่ใกล้กับคำที่พิมพ์ที่สุดด้วย **Levenshtein distance** (DP บน 2D array)
  2. คิด `accuracy = 1 - distance / spellLength` แล้วแบ่งระดับ: PERFECT ×1.25 · Great ×1.0 · Good ×0.6 · Weak ×0.3 · Fizzle ×0 (ค่าตัดแต่ละระดับไว้ปรับตอน balance)
  3. **PERFECT** = พิมพ์ถูกหมด **และ** เสร็จภายใน `parTime = spellLength × k + 1s` (เริ่มจับเวลาตั้งแต่กดตัวแรก) ถ้าถูกหมดแต่ช้ากว่านั้นได้แค่ Great
  4. แสดงผลระดับลอยขึ้นบนจอ (เช่น "Weak!") และขนาด/ความสว่างของ projectile เปลี่ยนตามระดับ
- **ศัตรู 3 แบบ + boss**: Goblin (เร็ว, HP น้อย), Troll (ช้า, HP เยอะ), Dementor (ดาเมจจากคาถาอื่นลดลงมาก), Boss: Dark Wizard (wave 5)
- **5 wave**: ฐานมี HP ถ้าศัตรูเดินถึงฐานจะเสีย HP ฐาน HP หมดคือแพ้ ชนะ boss ใน wave 5 คือชนะ
- **หน้าจอ**: Menu (Start / How to play / Exit) → Game (HUD: HP ฐาน, wave, cooldown, ช่องพิมพ์) → Result (Win/Lose + สถิติ accuracy) และหน้า Pause
- **1 map** ใช้ tilemap + sprite จาก free asset pack (itch.io / OpenGameArt) ใส่ credit ใน README/รายงาน
- **Unit test** เฉพาะ logic ล้วนด้วย **JUnit 5** (library นอกตัวแรกที่ได้ลองใช้)
- GitHub repo + GitHub Actions (build + test) + Release `SpellDefender.jar`

### Nice-to-have (ทำเมื่อ must-have เสร็จก่อน 28 ต.ค.)
- Sound effect / BGM (`javax.sound.sampled`)
- Sprite animation (walk / cast / hit) และ particle ตอนระเบิด
- ให้พ่อมดเดินได้
- บันทึก high score / สถิติลงไฟล์
- ลองใช้ library เพิ่ม เช่น FlatLaf (ทำให้ปุ่มเมนูดูทันสมัย) หรือ Gson (โหลด wave/spell config จาก JSON)

### Out of scope
หลาย map, multiplayer, ระบบอัปเกรด/shop, level editor, รองรับแป้นภาษาไทย (แสดงคำเตือนให้เปลี่ยนเป็นแป้นอังกฤษแทน)

## 3. Design overview
แยก **logic (model)** ออกจาก **Swing (view)** เพื่อให้ test ได้และอธิบาย OOP ได้ชัด

```
GameWindow (JFrame, CardLayout)
 ├─ MenuPanel / ResultPanel (JPanel + JButton, ActionListener)
 └─ GamePanel (JPanel, paintComponent, KeyListener) ──draws──▶ GameWorld
GameLoop implements Runnable ── Thread ~60 FPS: world.update(dt) → panel.repaint()
GameWorld: Base, Wizard, ArrayList<Enemy>, ArrayList<Projectile>, WaveManager, TileMap
```

| Class / Interface | หน้าที่ | หัวข้อ OOP |
|---|---|---|
| `interface Updatable`, `interface Drawable` | `update(double dt)`, `draw(Graphics2D)` | Interface |
| `abstract Entity` | x, y, hitbox, sprite | Abstract, Inheritance |
| `abstract Enemy extends Entity` → `Goblin`, `Troll`, `Dementor`, `DarkWizardBoss` | `takeDamage(Spell, power)` override ได้ (Dementor ต้านคาถาอื่น) | Inheritance, Polymorphism |
| `abstract Spell` → `Stupefy`, `Incendio`, `Protego`, `Expelliarmus`, `ExpectoPatronum` | ชื่อ, baseDamage, cooldown, `cast(world, power)` | Polymorphism |
| `interface Targeting` → `LockOnTargeting`, `DirectionalTargeting`, `SelfTargeting` | Spell แต่ละตัวถือ Targeting ของตัวเอง | Interface, composition |
| `Projectile extends Entity` | วิ่งไปหาเป้าหรือวิ่งตามทิศ แล้วชน | Graphics |
| `SpellBook` | เก็บ Spell[], `match(String typed)` → Spell + distance | Array, Method |
| `Levenshtein` (static) | `distance(a, b)` ด้วย `int[][] dp` | Multidimensional array |
| `AccuracyGrader` + `enum Tier` | (distance, length, timeMs) → Tier + multiplier | Algorithm |
| `InputBuffer` | ตัวอักษรที่พิมพ์ + เวลาเริ่ม | |
| `WaveManager` | `int[][] waveTable` (แถว = wave, คอลัมน์ = จำนวนศัตรูแต่ละชนิด), spawn ตามเวลา | 2D array, ArrayList |
| `TileMap` | `int[][] tiles` + tileset | 2D array, Graphics |
| `Assets` | โหลด/cache `BufferedImage` จาก resources | |

**จุดยากที่ต้องระวัง**
- **Thread safety**: game thread แก้ list ขณะที่ EDT วาด → `ConcurrentModificationException` ให้ sync ด้วย lock ตัวเดียว หรือวาดจาก snapshot และไม่แก้ list ระหว่าง loop (เก็บตัวที่ตายไว้ลบทีหลัง)
- **Pause**: ใช้ `volatile boolean paused` ห้ามใช้ `Thread.suspend()` (deprecated แบบในโค้ดตัวอย่าง)
- **Keyboard focus**: หลังกดปุ่มหรือสลับ card ต้องเรียก `requestFocusInWindow()` ไม่งั้น KeyListener จะไม่ทำงาน
- **dt-based movement**: ความเร็วต้องไม่ขึ้นกับ FPS (`pos += speed * dt`) และทำให้ test เวลา cooldown ได้
- Lock-on แล้วเป้าตายก่อนกด Enter → เลือกตัวใกล้สุดใหม่อัตโนมัติ, ถ้าไม่มีศัตรูเลย → Fizzle
- คำที่พิมพ์ใกล้คาถาสองอันพอกัน → เลือกอันที่ distance ต่ำสุด เสมอกันให้เลือกอันที่สั้นกว่า

## 4. Milestones (วันนี้ 11 ต.ค. · feature freeze 28 ต.ค.)
| # | ช่วง | ผลลัพธ์ที่รันหรือทดสอบได้ |
|---|---|---|
| M0 | 11–12 ต.ค. | git repo + GitHub, โครง `src/`, `test.sh`/`run.sh`/`build.sh`, JUnit 5 รันได้, Actions เขียว, JFrame เปล่าเปิดได้ |
| M1 | 13–15 ต.ค. | Core typing logic: `Levenshtein`, `AccuracyGrader`, `SpellBook.match` + unit test ครบ (ยังไม่มีกราฟิก) |
| M2 | 16–18 ต.ค. | `GameLoop` thread + `GamePanel` วาดรูปทรงง่ายๆ: ฐาน, Goblin เดินเข้าหา, พิมพ์ `stupefy` + Enter แล้วยิงโดน เห็น Tier บนจอ |
| M3 | 19–22 ต.ค. | ครบ 5 คาถา + 3 Targeting + cooldown + Tab/←→ + HUD cooldown |
| M4 | 23–25 ต.ค. | ครบ 3 ศัตรู + boss, `WaveManager` 5 wave, Win/Lose, Menu/Result/Pause |
| M5 | 26–28 ต.ค. | ใส่ asset จริง: tilemap, sprite, HUD สวยขึ้น (ถ้าเหลือเวลาทำ animation/sound) |
| M6 | 29–31 ต.ค. | Balance, แก้บั๊ก, playtest กับเพื่อน 2–3 คน, Release `v1.0` .jar |
| M7 | 1–3 พ.ย. | รายงาน (บทที่ 1–3: Class Diagram, GUI, Event handling, Algorithm, ปัญหา/จุดเด่น), สไลด์ 10 นาที, ซ้อมพรีเซนต์ + demo script |

## 5. Verification
- **M0**: `./test.sh` ผ่าน (test ตัวอย่าง 1 ตัว), `./run.sh` เปิดหน้าต่างได้, badge Actions เขียว
- **M1**: JUnit เช่น `distance("incendio","incendoi")==2`, `match("stupify")` ได้ Stupefy, พิมพ์ถูกหมดภายใน parTime ได้ PERFECT แต่ช้ากว่านั้นได้ Great, `"xyz"` ได้ Fizzle, input ว่างไม่ crash
- **M2**: เล่นด้วยมือ: Goblin เดินถึงฐานแล้ว HP ลด, พิมพ์ถูกหรือผิดแล้วดาเมจต่างกันตามที่เห็นบนจอ, กด Esc แล้วทุกอย่างหยุดจริง
- **M3**: test cooldown (cast ซ้ำก่อนหมด cooldown ไม่ออก), test Dementor รับดาเมจ patronum มากกว่า stupefy; เล่นด้วยมือ: Tab วน marker ได้ครบ, ←→ หมุนทิศแล้ว incendio ไปตามทิศ
- **M4**: test `WaveManager` spawn จำนวนตาม `waveTable`; เล่นด้วยมือจนชนะและแพ้อย่างละ 1 รอบ, Restart จาก Result แล้ว state รีเซ็ตครบ
- **M5–M6**: เปิด .jar จาก Release บนเครื่องอื่น (หรือ Windows) แล้ว asset โหลดครบ, เล่น 10 นาทีแล้วไม่ lag ไม่ crash
- **End-to-end**: ดาวน์โหลด `SpellDefender.jar` จาก GitHub Release บนเครื่องที่มีแค่ Java 17 → `java -jar` → Menu → เล่นผ่านทั้ง 5 wave จนชนะ boss ระหว่างนั้นต้องสาธิตได้ว่า (1) พิมพ์ผิดแล้วคาถาอ่อนลง/Fizzle (2) พิมพ์เร็วแล้วได้ PERFECT (3) Dementor แพ้ patronum (4) protego กันดาเมจฐานได้ (5) Pause/Resume ทำงาน และ `./test.sh` ผ่านทั้งหมด

## 6. Constraints and rules
- ส่งงาน **3 พ.ย.** และพรีเซนต์ **4 พ.ย.** (10 นาที) เป้าคือ 10/10
- ทำคนเดียว ใช้ AI ได้แต่ **ต้องอธิบายโค้ดได้ทุก class**: ทุก milestone ต้องอ่านและสรุปเองว่าแต่ละ class ทำอะไร และทำตาราง "หัวข้อที่เรียน → class ที่ใช้" ไว้ในรายงานและสไลด์
- ต้องครอบคลุมทุกหัวข้อ: Class/Object, Method, Array, 2D array/ArrayList, Inheritance, Polymorphism, Interface/Abstract, GUI/Event, Graphics, Thread
- Java 17, Swing/AWT/Java2D ใช้ library นอกได้ถ้ามีเหตุผล (เริ่มจาก JUnit 5)
- Asset ต้องมี license ที่ใช้ฟรีได้ และใส่ credit
- รายงานใช้โครงเดียวกับตัวอย่างใน `docs/example/`

## 7. Open questions
- Build tool: ใช้ shell script + JUnit standalone jar (เรียบง่ายตาม README) หรือย้ายไป **Gradle** (มาตรฐานงานจริง จัดการ library ให้เอง) ต้องตัดสินใจใน M0
- จะใช้ asset pack ไหน (ต้องมีทั้งพ่อมด, Goblin, Troll, Dementor-like ghost, boss และ tileset ในสไตล์เดียวกัน)
- ค่า `k` ใน parTime และค่าตัดของแต่ละ Tier จะหาจากการ playtest ใน M2–M3
- ไฟล์ใน `docs/` ที่ README อ้างถึง (GDD, ARCHITECTURE, ROADMAP ฯลฯ) ยังไม่มี: จะเขียนจริงหรือลดเหลือ SPEC.md + รายงาน
- ขนาดหน้าต่าง และขนาด tile (เช่น 960×640, tile 32px)
