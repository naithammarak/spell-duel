# SPEC: Spell Duel

> เปลี่ยนแนวเมื่อ 27 ต.ค. 2026 จาก tower defense (เก็บไว้ที่ tag `td-prototype`) และโหมดกองทัพ (tag `army-prototype`, ดู `SPEC-ARMY.md`) มาเป็นเกมดวลเวท 1v1
> เหตุผล: prototype เดิมพิมพ์คาถาเดิมซ้ำจนเบื่อ ศัตรูเยอะจนมองจอไม่ทัน ไม่มีการตัดสินใจ และระบบ (เงิน/ซื้อพ่อมด/grid) ซับซ้อนเกินไป
> เปลี่ยนจาก real-time เป็นแบบรอบเมื่อ 31 ต.ค. 2026 (branch `duel-rounds`) เพราะ playtest แล้ว Interrupt แทบไม่ได้ใช้ และพิมพ์รัวใบเดียวชนะได้

## 1. Goal
เกม 2D pixel มุมมองด้านข้าง ผู้เล่น (ซ้าย) ดวลเวทกับบอท (ขวา) เป็น**รอบ** ทั้งสองฝ่าย**พิมพ์คาถาพร้อมกัน** แล้วเปิดไพ่
หัวใจของเกมคือ**อ่านทางบอท แล้วเลือกการ์ดให้ชนะ** (เป่ายิ้งฉุบ) ยิ่งพิมพ์แม่น คาถายิ่งแรง และยิ่งกล้าเดาก่อนบอทเปิดไพ่ ยิ่งได้แรงเพิ่ม
เป็น mini project วิชา OOP (ทำคนเดียว) ใช้ Java 17 + Swing ต้องใช้ทุกหัวข้อที่เรียน และอธิบายได้ทุก class

## 2. Features
### Must-have
- **การ์ดคาถา 3 ใบ** บนจอ ใบละ 1 หน้าที่ และแต่ละใบแสดงคาถา (ยาว) ที่ต้องพิมพ์ในตอนนั้น
  - ใช้การ์ดใบไหนไปแล้ว ใบนั้นจะสุ่มคาถาใหม่จากชุดของหน้าที่เดียวกัน (ไม่ซ้ำกับคำเดิม) ผู้เล่นจะได้ไม่ต้องพิมพ์คำเดิมซ้ำ
  - พิมพ์ A–Z/Space, Backspace ลบ, Enter ร่าย, Esc หยุดเกม

  | หน้าที่ | ชุดคาถา (5 บทต่อหน้าที่) | ผล |
  |---|---|---|
  | ⚔ **Attack** | bombarda maxima, petrificus totalus, incendio maxima, confringo maxima, sectumsempra | ดาเมจเต็ม ชนะ Interrupt (Interrupt ของอีกฝ่ายแตกไม่ออกจากไม้) |
  | 🛡 **Shield** | protego maxima, protego totalum, protego horribilis, salvio hexia, cave inimicum | **สะท้อน** Attack กลับใส่คนตี ยิ่งร่ายแม่น ลูกสะท้อนยิ่งแรง โล่อยู่ได้รอบเดียว |
  | ✋ **Interrupt** | expelliarmus, impedimenta, depulso maxima, reducto maxima, diffindo maxima | **ทำลายโล่** และทำดาเมจเต็ม ถ้าไม่เจอโล่ก็ยังทำดาเมจ แต่จะแตกถ้าเจอ Attack |

  **วงจรเป่ายิ้งฉุบ:** Attack ชนะ Interrupt / Interrupt ชนะ Shield / Shield ชนะ Attack
  **ออกใบเดียวกัน:** Attack โดนทั้งคู่ · Shield ไม่เกิดอะไร · Interrupt เบาทั้งคู่ (×0.25)
- **รอบ**
  - รอบละไม่เกิน 8 วินาที พิมพ์พร้อมกับบอท กด Enter = ล็อกการ์ด (Fizzle แล้วพิมพ์ใหม่ได้ถ้ายังมีเวลา)
  - ล็อกครบทั้งคู่ หรือบอทล็อกแล้วผ่านไป 1 วินาที หรือหมด 8 วินาที → เปิดไพ่ ลูกเวทบิน แล้วเริ่มรอบใหม่
  - ตอนหมดเวลา ข้อความที่ค้างในช่องพิมพ์จะถูกส่งให้อัตโนมัติ ถ้าไม่ได้ล็อกเลย = รอบนั้นไม่ได้ทำอะไร
  - **กล้าเดา:** ล็อกก่อนบอทเปิดไพ่ ได้แรง ×1.5 (รวมลูกสะท้อนของโล่)
- **ระบบความแม่นยำ** (ใช้ package `typing` เดิม)
  1. หาการ์ดที่ใกล้กับคำที่พิมพ์ที่สุดด้วย Levenshtein
  2. แบ่งเป็น Tier: PERFECT ×1.25 · Great ×1.0 (แม่น ≥ 85%) · Good ×0.6 (≥ 75%) · Weak ×0.3 (≥ 50%) · Fizzle ×0 (ต่ำกว่า 50% เช่นพิมพ์ไม่ถึงครึ่งคำ)
  3. PERFECT ต้องพิมพ์ถูกหมด และเสร็จภายใน `parTime = length × k + 1s`
  4. Fizzle ไม่เกิดผลอะไร และการ์ดไม่ถูกใช้
- **ฝั่งบอทมี cast bar** เวลาร่ายคิดจาก `พักก่อนร่าย + ความยาวคาถา / ความเร็วพิมพ์ของบอท` (ไม่เกิน 7.7 วินาที) ซ่อนหน้าที่เป็น "???" จนถึง**ครึ่งแถบ**จึงเปิดว่าเป็นการ์ดอะไร ส่วน Tier ของบอทสุ่มจากค่าความแม่นยำของบอทตัวนั้น
- **บอท 3 ตัว ไต่ทีละตัว** นิสัยและความเร็วต่างกัน
  1. **Apprentice**: พิมพ์ช้า ออก Attack 70% นอกนั้นสุ่ม ใช้เป็นตัวสอนเล่น
  2. **Auror**: ดูการ์ดรอบก่อนของผู้เล่น แล้วออกใบที่ชนะใบนั้น 70% นอกนั้นสุ่ม ผู้เล่นที่จับนิสัยได้จะหลอกกลับได้
  3. **Dark Wizard** (boss): เร็ว เลือกการ์ดแบบ Auror และบางครั้ง**หลอก** (ตอนร่ายถึง 3/4 แถบ มีโอกาส 30% เปลี่ยนการ์ดหลังเปิดไพ่ไปแล้ว)
- **แพ้** (HP ผู้เล่นหมด) → Retry ที่บอทตัวเดิม · **ชนะ** → ไปบอทตัวถัดไป · ชนะ Dark Wizard → จบเกม
- **หน้าจอ**: Menu (Start / How to play / Exit) → Duel → Result (Next / Retry / Menu) และมีหน้า Pause
  - หน้าจอ Duel มี HP bar ของทั้งสองฝั่ง, เลขรอบ + แถบเวลา, cast bar ของบอท, การ์ด 3 ใบ, ช่องพิมพ์ และ Tier ลอยขึ้นมาบนจอ
- **Unit test** เฉพาะ logic ด้วย JUnit 5
- GitHub repo + Actions + Release `SpellDuel.jar`

### Nice-to-have (ทำเมื่อ must-have เสร็จก่อนเย็น 30 ต.ค.)
- Sprite animation ตอนร่าย/โดนตี, particle, จอสั่น
- Sound effect (`javax.sound.sampled`)
- Combo: PERFECT ติดกันได้ดาเมจเพิ่ม

### Out of scope
- ผู้เล่น 2 คน หรือเล่นผ่านเน็ต
- ระบบเงิน, อัปเกรด, cooldown, mana (การ์ดที่สุ่มใหม่ทุกครั้งพอแล้ว)
- หลายฉาก
- รองรับแป้นภาษาไทย (จะแสดงคำเตือนแทน)

## 3. Design overview
รายละเอียดอยู่ใน `DESIGN.md`
- Package `spellduel.ui → model → typing`
- `GameWorld` เป็นห้องครัวที่เก็บกฎทั้งหมดของเกม ส่วน Swing มีหน้าที่แค่รับปุ่มที่กดและวาดจอ
- Game loop ใช้ thread เดียว

| ส่วน | Class หลัก |
|---|---|
| การพิมพ์ (ของเดิม ปรับแค่เกณฑ์ Tier) | `Levenshtein`, `Tier`, `AccuracyGrader`, `InputBuffer` |
| การ์ด | `enum SpellRole`, `Incantation`, `SpellDeck` (`String[][]` ชุดคาถา) |
| นักเวท | `abstract Duelist` → `PlayerWizard`, `BotWizard` · `Cast` (cast bar) · `Shield` |
| สมองบอท | `interface BotBrain` → `ApprenticeBrain`, `AurorBrain`, `DarkWizardBrain` · `BotProfile` (`int[][]` stat) |
| ของที่บินอยู่บนจอ | `Projectile`, `Particle`, `FloatingText` |
| ระบบ | `GameWorld`, `GameStats`, `Hud`, `Assets` · ui: `GameWindow`, `MenuPanel`, `GamePanel`, `ResultPanel`, `GameLoop` |

## 4. Milestones (วันนี้ 27 ต.ค. · หยุดเพิ่มฟีเจอร์เย็น 30 ต.ค.)
ทุกวันจบด้วย **30 นาทีทำความเข้าใจ**: อ่าน class ที่ LLM เขียนในวันนั้น แล้วจดลง `notes/understanding.md` ว่าแต่ละ class ทำอะไรและทำไมออกแบบแบบนี้ (ด้วยคำพูดของตัวเอง) โน้ตนี้จะกลายเป็นวัตถุดิบของรายงานบทที่ 2

| # | วัน | ผลลัพธ์ที่รันหรือทดสอบได้ |
|---|---|---|
| D1 | 27 ต.ค. | tag `td-prototype` (master) และ `army-prototype` (proto-army) แล้วแตก branch `duel` จาก master · rename package เป็น `spellduel` · ลบ model/ui ของ tower defense · เก็บ `typing` + test ไว้ → `./test.sh` ผ่าน |
| D2 | 28 ต.ค. | `SpellDeck`, `Duelist`, `Cast`, `Projectile`, `GameWorld`, `ApprenticeBrain` + test · `GamePanel` วาดเป็นรูปทรง เล่นชนะหรือแพ้ Apprentice ได้ 1 รอบ |
| D3 | 29 ต.ค. | `AurorBrain`, `DarkWizardBrain`, ladder 3 ตัว, Menu/Result/Pause, การ์ดและ cast bar บน HUD |
| D4 | 30 ต.ค. | sprite มุมมองด้านข้าง + effect, balance จาก playtest (ให้เพื่อนเล่น 2 คน), อัปเดต README, Release `v1.0` jar → **หยุดเพิ่มฟีเจอร์** |
| D5–D7 | 31 ต.ค.–2 พ.ย. | รายงานบทที่ 1–3 (class diagram, GUI, event, algorithm), สไลด์ 10 นาที, demo script, ซ้อมพรีเซนต์ |
| D8 | 3 พ.ย. | **ส่งรอบแรก**: PDF + code (ถามอาจารย์ว่าต้องส่งก่อนกี่โมง) |
| D9 | 4 พ.ย. | พรีเซนต์ |
| D10 | 5–7 พ.ย. | แก้ตามคำติชม → **ส่งฉบับสมบูรณ์ 7 พ.ย. 16:00 น.** (.jar + source + ลิงก์ GitHub + รายงาน PDF ใน Classroom) |

## 5. Verification
- **D1**: `./test.sh` ผ่าน (`LevenshteinTest`, `AccuracyGraderTest`, `InputBufferTest`, `ArchitectureTest`) และ `grep -r spelldefender src` ต้องไม่เจออะไร
- **D2**: JUnit ผ่าน
  - ตารางเป่ายิ้งฉุบครบทุกคู่ (รวมออกใบเดียวกัน)
  - Shield ทุก tier สะท้อน Attack กลับใส่คนตี และแรงตาม tier ของโล่
  - Interrupt ทำลายโล่ได้ และแตกเมื่อเจอ Attack
  - Fizzle แล้วการ์ดยังอยู่ และพิมพ์ใหม่ได้
  - ใช้การ์ดแล้วได้คำใหม่ที่ไม่ซ้ำคำเดิม
  - วงจรรอบ: ล็อกครบแล้วเปิดไพ่, บอทล็อกแล้วเหลือเวลา 1 วินาที, หมดเวลาแล้วส่งข้อความที่ค้าง, โล่หายตอนขึ้นรอบใหม่
  - ล็อกก่อนบอทเปิดไพ่ได้แรง ×1.5 ล็อกหลังเปิดไม่ได้
  - HP หมดแล้ว status เป็น WON หรือ LOST
  - เล่นด้วยมือ: ชนะ Apprentice ได้
- **D3**: JUnit ผ่าน (ใช้ seed คงที่)
  - Apprentice ออก Attack เป็นส่วนใหญ่
  - Auror ออกใบที่ชนะการ์ดรอบก่อนของผู้เล่นเป็นส่วนใหญ่
  - บอททุกตัวเปิดหน้าที่ตอนครึ่งแถบ
  - Dark Wizard หลอกหลังเปิดไพ่เท่านั้น และครั้งเดียวต่อ cast
  - เล่นด้วยมือ: ชนะแล้วไปบอทตัวถัดไป แพ้แล้ว Retry ที่ตัวเดิม และ state รีเซ็ตครบ
- **D4**: เปิด jar จาก Release บนเครื่องอื่นได้ asset โหลดครบ และเล่น 10 นาทีไม่ crash
- **End-to-end**: ดาวน์โหลด `SpellDuel.jar` จาก Release บนเครื่องที่มีแค่ Java 17 แล้ว `java -jar` → Menu → ชนะบอททั้ง 3 ตัว ระหว่างนั้นต้องโชว์ได้ว่า
  1. พิมพ์ผิดแล้วคาถาอ่อนลง หรือ Fizzle
  2. ครบวงจรเป่ายิ้งฉุบ: Shield สะท้อน Attack, Interrupt ทำลายโล่, Attack ชนะ Interrupt
  3. ล็อกก่อนบอทเปิดไพ่แล้วขึ้น "กล้าเดา ×1.5"
  4. การ์ดเปลี่ยนคำหลังใช้
  5. Dark Wizard หลอกเปลี่ยนคาถาได้
  6. Pause/Resume ทำงาน

  และ `./test.sh` ผ่านทั้งหมด

## 6. Constraints and rules
- **ส่งรอบแรก 3 พ.ย.** (PDF + code), **พรีเซนต์ 4 พ.ย.** (10 นาที), **ส่งฉบับสมบูรณ์ 7 พ.ย. 2569 16:00 น.**
  - ฉบับสมบูรณ์ประกอบด้วย .jar (ถ้าทำไม่ได้ ใช้ .rar), source code, ลิงก์ GitHub และรายงาน PDF
  - เป้า 10/10
- ทำคนเดียว ให้ LLM เขียนโค้ดได้ แต่**ต้องอธิบายได้ทุก class** ตอนพรีเซนต์ จึงต้องทำ "30 นาทีทำความเข้าใจ" ทุกวัน ห้ามข้าม
- ต้องใช้ทุกหัวข้อ: Class/Object, Method, Array, 2D array/ArrayList, Inheritance, Polymorphism, Interface/Abstract, GUI/Event, Graphics, Thread
- Java 17 + Swing ใช้ library นอกได้ถ้ามีเหตุผล (ตอนนี้ใช้ JUnit 5)
- asset ต้องมี license ที่ใช้ฟรีได้ และต้องใส่ credit ไว้ใน README กับรายงาน
- รายงานใช้โครงเดียวกับตัวอย่างใน `docs/example/`

## 7. Open questions
- asset pack ของพ่อมดมุมมองด้านข้าง 3–4 ตัว (หาบน itch.io คำค้น "pixel wizard side view free") ถ้าไม่เจอ ใช้รูปทรง + สีไปก่อน
- ค่า balance เริ่มต้นใน DESIGN §6 (HP, ความเร็วพิมพ์ของบอท, ดาเมจ) จะปรับตอน playtest ใน D4
- จะ rename repo บน GitHub เป็น `spell-duel` ไหม (GitHub redirect ลิงก์เก่าให้เอง)
- รอบแรก 3 พ.ย. ต้องส่งก่อนกี่โมง
