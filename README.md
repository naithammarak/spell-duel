# Spell Duel

เกมดวลเวท 1v1 ด้วยการพิมพ์ ในสนามประลองที่มีผู้ชมเต็มอัฒจันทร์ อ่านทางบอท เลือกการ์ดให้ชนะ แล้วพิมพ์ให้เร็วและแม่น
*A 1v1 typing wizard duel made with Java Swing for an Object Oriented Programming course.*

## เล่นเลย

1. ติดตั้ง **Java 17 ขึ้นไป**
2. ดาวน์โหลด `SpellDuel.jar` จากหน้า [Releases](../../releases)
3. ดับเบิลคลิก หรือรัน `java -jar SpellDuel.jar`

## วิธีเล่น

| ปุ่ม | ทำอะไร |
|---|---|
| พิมพ์ A–Z / Space | พิมพ์คาถาจากการ์ด (ใช้แป้นภาษาอังกฤษ) |
| Enter | ร่ายคาถา ทำงานทันที ใช้ได้รอบละ 1 ครั้ง |
| Backspace | ลบ |
| Esc | หยุดเกม |

| การ์ด | ผล | แพ้ทาง |
|---|---|---|
| Attack (สายฟ้าเขียว) | ดาเมจเต็ม ถ้าระดับ Great ขึ้นไปจะขัดคาถาอีกฝ่าย | Shield |
| Shield | สะท้อน Attack กลับใส่คนตี | Interrupt |
| Interrupt (สายฟ้าแดง) | ทำลายโล่และดาเมจเต็ม ถ้าไม่เจอโล่จะเบา ×0.25 | Attack |

พิมพ์ผิดยิ่งเยอะ คาถายิ่งอ่อน: **PERFECT! ×1.25 → Great ×1.0 → Good ×0.6 → Weak ×0.3 → Fizzle!**

แถบร่ายของบอทเปิดว่าใช้สกิลอะไรเมื่อถึง 20% ชนะแล้ว HP ติดตัวไปด่านถัดไปและฟื้น 75 แพ้แล้วเริ่มด่าน 1 ใหม่ แข่งกับสถิติด่านสูงสุดของตัวเอง

## พัฒนาต่อ

ต้องมี JDK 17+ (ไม่ต้องติดตั้ง Maven/Gradle)

```bash
./test.sh     # รันเทสต์
./run.sh      # build + เล่น
./build.sh    # สร้าง dist/SpellDuel.jar
```
Windows ใช้ `test.bat` / `run.bat` / `build.bat`

## เอกสาร

| เอกสาร | เนื้อหา |
|---|---|
| [SPEC.md](SPEC.md) | เกมต้องทำอะไร |
| [DESIGN.md](DESIGN.md) | สถาปัตยกรรม, กติกา, class diagram |

## Tech

Java 17 · Swing/AWT · Java2D · Threads · GitHub Actions · JUnit 5 สำหรับเทสต์ · ภาพทั้งหมดวาดด้วย `Graphics2D` ไม่มีไฟล์ asset จากภายนอก
