package spelldefender.model;

import java.util.Arrays;
import java.util.List;
import spelldefender.typing.Levenshtein;

/**
 * รายการคาถาทั้งหมด (ที่เดียวในเกม) และหาคาถาที่ใกล้กับคำที่พิมพ์ที่สุด
 * เพิ่มคาถาใหม่ = เขียน class ใหม่ + เพิ่มใน standard() ที่เหลือ (HUD, การ match) ตามมาเอง
 */
class SpellBook {

    /** ผลการ match: คาถาที่ใกล้ที่สุด และจำนวนตัวที่ต่างกัน */
    record Match(Spell spell, int distance) {
    }

    private final Spell[] spells;

    SpellBook(Spell... spells) {
        if (spells.length == 0) {
            throw new IllegalArgumentException("spell book is empty");
        }
        this.spells = spells.clone();
    }

    static SpellBook standard() {
        return new SpellBook(
                new Stupefy(),
                new Incendio(),
                new Protego(),
                new Expelliarmus(),
                new ExpectoPatronum());
    }

    List<Spell> spells() {
        return Arrays.asList(spells.clone());
    }

    /**
     * คาถาที่ distance ต่ำสุด ถ้าเสมอกันเลือกอันที่สั้นกว่า (ถ้ายังเสมอ เลือกอันที่อยู่ก่อน)
     * ไม่คืน null: input ว่างก็ได้คาถาที่สั้นที่สุด (distance = ความยาว → Fizzle)
     */
    Match match(String typed) {
        Spell best = spells[0];
        int bestDistance = Levenshtein.distance(typed, best.name());
        for (int i = 1; i < spells.length; i++) {
            Spell s = spells[i];
            int d = Levenshtein.distance(typed, s.name());
            if (d < bestDistance || (d == bestDistance && s.length() < best.length())) {
                best = s;
                bestDistance = d;
            }
        }
        return new Match(best, bestDistance);
    }

    void tickCooldowns(double dt) {
        for (Spell s : spells) {
            s.tick(dt);
        }
    }
}
