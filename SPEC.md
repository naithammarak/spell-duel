# SPEC: Spell Duel

> เปลี่ยนแนวเมื่อ 27 ต.ค. 2026 จาก tower defense (เก็บไว้ที่ tag `td-prototype`) และโหมดกองทัพ (tag `army-prototype`, ดู `SPEC-ARMY.md`) มาเป็นเกมดวลเวท 1v1
> เหตุผล: prototype เดิมพิมพ์คาถาเดิมซ้ำจนเบื่อ ศัตรูเยอะจนมองจอไม่ทัน ไม่มีการตัดสินใจ และระบบ (เงิน/ซื้อพ่อมด/grid) ซับซ้อนเกินไป

## 1. Goal
เกม 2D pixel มุมมองด้านข้าง ผู้เล่น (ซ้าย) ดวลเวทกับบอท (ขวา) แบบ real-time โดยร่ายคาถาด้วยการ**พิมพ์**
หัวใจของเกมคือ**อ่านทางบอท แล้วเลือกคาถาให้ถูกจังหวะ** (เหมือนเป่ายิ้งฉุบ) และ**ร่ายให้เสร็จก่อน** ยิ่งพิมพ์ถูกและเร็ว คาถายิ่งแรง
เป็น mini project วิชา OOP (ทำคนเดียว) ใช้ Java 17 + Swing ต้องใช้ทุกหัวข้อที่เรียน และอธิบายได้ทุก class

## 2. Features
### Must-have
- **การ์ดคาถา 3 ใบ** บนจอ ใบละ 1 หน้าที่ และแต่ละใบแสดงคาถา (ยาว) ที่ต้องพิมพ์ในตอนนั้น
  - ใช้การ์ดใบไหนไปแล้ว ใบนั้นจะสุ่มคาถาใหม่จากชุดของหน้าที่เดียวกัน (ไม่ซ้ำกับคำเดิม) ผู้เล่นจะได้ไม่ต้องพิมพ์คำเดิมซ้ำ
  - พิมพ์ A–Z/Space, Backspace ลบ, Enter ร่าย, Esc หยุดเกม

  | หน้าที่ | ชุดคาถา (5 บทต่อหน้าที่) | ผล |
  |---|---|---|
  | ⚔ **Attack** | bombarda maxima, petrificus totalus, incendio maxima, confringo maxima, sectumsempra | ดาเมจสูง ถ้าโดนก่อน cast bar ของอีกฝ่ายเต็ม คาถาของอีกฝ่ายจะถูก**ตัด** |
  | 🛡 **Shield** | protego maxima, protego totalum, protego horribilis, salvio hexia, cave inimicum | กันการโจมตีครั้งถัดไป (นาน 3 วินาที) และ**สะท้อน**กลับใส่คนตีเสมอ ยิ่งร่ายแม่น ลูกสะท้อนยิ่งแรง |
  | ✋ **Interrupt** | expelliarmus, impedimenta, depulso maxima, reducto maxima, diffindo maxima | คาถายาวปานกลาง ดาเมจน้อย **ทำลายโล่** และ**ตัด**ได้เฉพาะตอนอีกฝ่ายกำลังร่าย Shield (ตัด Attack ไม่ได้) |

  **ต้องแม่นถึงจะตัดได้:** การตัด cast ต้องได้ GREAT ขึ้นไป ถ้าได้ GOOD/WEAK ทำได้แค่ดาเมจ (กันการพิมพ์ครึ่งคำรัว ๆ)
  **วงจรเป่ายิ้งฉุบ:** Attack ชนะ Interrupt / Interrupt ชนะ Shield / Shield ชนะ Attack
- **ระบบความแม่นยำ** (ใช้ package `typing` เดิม)
  1. หาการ์ดที่ใกล้กับคำที่พิมพ์ที่สุดด้วย Levenshtein
  2. แบ่งเป็น Tier: PERFECT ×1.25 · Great ×1.0 (แม่น ≥ 85%) · Good ×0.6 (≥ 75%) · Weak ×0.3 (≥ 50%) · Fizzle ×0 (ต่ำกว่า 50% เช่นพิมพ์ไม่ถึงครึ่งคำ)
  3. PERFECT ต้องพิมพ์ถูกหมด และเสร็จภายใน `parTime = length × k + 1s`
  4. Fizzle ไม่เกิดผลอะไร และการ์ดไม่ถูกใช้
- **ฝั่งผู้เล่น "กำลังร่าย"** ตลอดเวลาที่ช่องพิมพ์ไม่ว่าง ถ้าโดน Attack หรือ Interrupt ระหว่างนั้น ข้อความที่พิมพ์ค้างไว้จะหาย (โดนตัด)
- **ฝั่งบอทมี cast bar** บอกหน้าที่ของคาถาที่กำลังร่าย และเวลาที่เหลือ เวลาร่ายคิดจาก `ความยาวคาถา / ความเร็วพิมพ์ของบอท` ส่วน Tier ของบอทสุ่มจากค่าความแม่นยำของบอทตัวนั้น
- **บอท 3 ตัว ไต่ทีละตัว** นิสัยและความเร็วต่างกัน
  1. **Apprentice**: พิมพ์ช้า โจมตีอย่างเดียว มีช่วงพักระหว่างร่าย ใช้เป็นตัวสอนเล่น
  2. **Auror**: ถ้าเห็นผู้เล่นเริ่มพิมพ์คาถา Attack จะตั้งโล่ และถ้าผู้เล่นมีโล่อยู่ จะใช้ Interrupt
  3. **Dark Wizard** (boss): เร็ว และบางครั้ง**หลอก**ด้วยการเปลี่ยนคาถากลางทาง (cast bar เปลี่ยนหน้าที่ตอนร่ายไปได้ครึ่งทาง)
- **แพ้** (HP ผู้เล่นหมด) → Retry ที่บอทตัวเดิม · **ชนะ** → ไปบอทตัวถัดไป · ชนะ Dark Wizard → จบเกม
- **หน้าจอ**: Menu (Start / How to play / Exit) → Duel → Result (Next / Retry / Menu) และมีหน้า Pause
  - หน้าจอ Duel มี HP bar ของทั้งสองฝั่ง, cast bar ของบอท, การ์ด 3 ใบ, ช่องพิมพ์ และ Tier ลอยขึ้นมาบนจอ
- **Unit test** เฉพาะ logic ด้วย JUnit 5
- GitHub repo + Actions + Release `SpellDuel.jar`

### Nice-to-have (ทำเมื่อ must-have เสร็จก่อนเย็น 30 ต.ค.)
- Sprite animation ตอนร่าย/โดนตี, particle, จอสั่น
- Sound effect (`javax.sound.sampled`)
- Combo: PERFECT ติดกันได้ดาเมจเพิ่ม
- Beam Clash: ถ้า Attack ของทั้งสองฝั่งชนกันกลางจอ ให้แข่งพิมพ์คำสุ่มเพื่อดันลำแสง

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
  - Attack โดนแล้ว HP ลด และ cast ของบอทถูกตัด
  - Shield ทุก tier สะท้อน Attack กลับใส่คนตี และแรงตาม tier ของโล่
  - Interrupt ทำลายโล่ได้
  - Fizzle แล้วการ์ดยังอยู่
  - ใช้การ์ดแล้วได้คำใหม่ที่ไม่ซ้ำคำเดิม
  - โดนตีแล้วช่องพิมพ์ของผู้เล่นถูกล้าง
  - HP หมดแล้ว status เป็น WON หรือ LOST
  - เล่นด้วยมือ: ชนะ Apprentice ได้
- **D3**: JUnit ผ่าน (ใช้ seed คงที่)
  - Apprentice ร่ายแต่ Attack
  - Auror ตั้งโล่เมื่อผู้เล่นพิมพ์ต้นคำของการ์ด Attack ไป 3 ตัว
  - Dark Wizard เปลี่ยนหน้าที่กลางทาง
  - เล่นด้วยมือ: ชนะแล้วไปบอทตัวถัดไป แพ้แล้ว Retry ที่ตัวเดิม และ state รีเซ็ตครบ
- **D4**: เปิด jar จาก Release บนเครื่องอื่นได้ asset โหลดครบ และเล่น 10 นาทีไม่ crash
- **End-to-end**: ดาวน์โหลด `SpellDuel.jar` จาก Release บนเครื่องที่มีแค่ Java 17 แล้ว `java -jar` → Menu → ชนะบอททั้ง 3 ตัว ระหว่างนั้นต้องโชว์ได้ว่า
  1. พิมพ์ผิดแล้วคาถาอ่อนลง หรือ Fizzle
  2. Shield กัน Attack ได้
  3. Interrupt ตัด cast bar Shield ของบอทได้ แต่ตัด Attack ไม่ได้ และพิมพ์ครึ่งคำ (WEAK) ตัดอะไรไม่ได้
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
