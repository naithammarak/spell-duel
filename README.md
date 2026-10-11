# Spell Defender

เกมพิมพ์ร่ายคาถาป้องกันปราสาท — พิมพ์ชื่อคาถาให้ถูกและเร็ว ยิ่งแม่นยิ่งแรง
*A typing-based wizard defense game made with Java Swing for an Object Oriented Programming course.*

<!-- TODO T-19: ใส่ screenshot: ![gameplay](docs/report/screenshots/gameplay.png) -->

## เล่นเลย

1. ติดตั้ง **Java 17 ขึ้นไป**
2. ดาวน์โหลด `SpellDefender.jar` จากหน้า [Releases](../../releases)
3. ดับเบิลคลิก หรือรัน `java -jar SpellDefender.jar`

##  วิธีเล่น

| ปุ่ม | ทำอะไร |
|---|---|
| พิมพ์ A–Z / Space | พิมพ์คาถา (ใช้แป้นภาษาอังกฤษ) |
| Enter | ร่ายคาถาใส่ศัตรูที่ใกล้ที่สุด |
| Backspace | ลบ |
| Esc | หยุดเกม |

พิมพ์ผิดยิ่งเยอะ คาถายิ่งอ่อน: **PERFECT! ×1.25 → Great ×1.0 → Good ×0.6 → Weak ×0.3 → Fizzle!**

| คาถา | ผล |
|---|---|
| `stupefy` | ดาเมจเล็กน้อย + สตัน |
| `protego` | โล่ดูดซับดาเมจ |
| `expecto patronum` | แรงมากกับ Dementor |

##  พัฒนาต่อ

ต้องมี JDK 17+ (ไม่ต้องติดตั้ง Maven/Gradle)

```bash
./test.sh     # รันเทสต์
./run.sh      # build + เล่น
./build.sh    # สร้าง dist/SpellDefender.jar
```
Windows ใช้ `test.bat` / `run.bat` / `build.bat` · VS Code: เปิดโฟลเดอร์แล้วกด **F5**

##  เอกสาร

| เอกสาร | เนื้อหา |
|---|---|
| [docs/GDD.md](docs/GDD.md) | ออกแบบเกม: คาถา ศัตรู wave ระบบความแม่นยำ |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | tech stack, โครงสร้างไฟล์, class diagram, thread |
| [docs/DECISIONS.md](docs/DECISIONS.md) | trade-off ของแต่ละฟีเจอร์และเทคนิค |
| [docs/ROADMAP.md](docs/ROADMAP.md) | timeline ถึง 4 พ.ย. + backlog |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | git workflow, commit, release |
| [docs/GITHUB_SETUP.md](docs/GITHUB_SETUP.md) | ตั้ง GitHub ทีละขั้น |
| [docs/PROPOSAL.md](docs/PROPOSAL.md) | ข้อเสนอโครงงาน |
| [docs/report/](docs/report/) | โครงรายงาน, dev log, screenshots |

##  Tech

Java 17 · Swing/AWT · Java2D · Threads · GitHub Actions — ไม่มี library ภายนอก


