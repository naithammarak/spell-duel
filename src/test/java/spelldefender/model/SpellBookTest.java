package spelldefender.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import spelldefender.typing.AccuracyGrader;
import spelldefender.typing.Tier;

class SpellBookTest {

    private final SpellBook book = SpellBook.standard();

    @Test
    void typoStillMatchesClosestSpell() {
        SpellBook.Match m = book.match("stupify");
        assertEquals("stupefy", m.spell().name());
        assertEquals(1, m.distance());
    }

    @Test
    void exactNameMatchesWithDistanceZero() {
        SpellBook.Match m = book.match("expecto patronum");
        assertEquals("expecto patronum", m.spell().name());
        assertEquals(0, m.distance());
    }

    @Test
    void tieGoesToShorterSpell() {
        // "stupendio" ห่างจาก stupefy และ incendio เท่ากัน (4) → stupefy สั้นกว่า
        // ใส่ incendio ไว้ก่อน เพื่อให้แน่ใจว่าเลือกจากความยาว ไม่ใช่ลำดับ
        SpellBook tieBook = new SpellBook(new Incendio(), new Stupefy());
        SpellBook.Match m = tieBook.match("stupendio");
        assertEquals(4, m.distance());
        assertEquals("stupefy", m.spell().name());
    }

    @Test
    void garbageGradesAsFizzle() {
        SpellBook.Match m = book.match("xyz");
        Tier tier = new AccuracyGrader().grade(m.distance(), m.spell().length(), 0);
        assertEquals(Tier.FIZZLE, tier);
    }

    @Test
    void emptyInputDoesNotCrash() {
        assertNotNull(book.match("").spell());
    }

    @Test
    void customSpellCanBeAdded() {
        Spell reducto = new Spell("reducto", 30, 5, Color.RED, new LockOnTargeting()) {
            @Override
            void impact(GameWorld world, Enemy hit, Tier tier) {
                hit.takeDamage(damage(world, tier), damageType());
            }
        };
        SpellBook custom = new SpellBook(new Stupefy(), reducto);
        assertEquals("reducto", custom.match("redcto").spell().name());
    }
}
