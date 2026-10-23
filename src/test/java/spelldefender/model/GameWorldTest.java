package spelldefender.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import spelldefender.model.GameWorld.CastResult.Outcome;
import spelldefender.typing.Tier;

class GameWorldTest {

    private static GameWorld emptyWorld() {
        return new GameWorld(SpellBook.standard(), WaveManager.idle(), TileMap.standard());
    }

    private static GameWorld.CastResult type(GameWorld w, String text) {
        for (char c : text.toCharArray()) {
            w.typeChar(c);
        }
        return w.castTyped();
    }

    /** เดินเวลาเป็นช่วงเล็ก ๆ เหมือนเกมจริง */
    private static void run(GameWorld w, double seconds) {
        for (double t = 0; t < seconds; t += 0.02) {
            w.update(0.02);
        }
    }

    @Test
    void lockOnWithNoEnemiesFizzlesAndKeepsCooldown() {
        GameWorld w = emptyWorld();
        GameWorld.CastResult r = type(w, "stupefy");
        assertEquals(Outcome.FIZZLE, r.outcome());
        assertEquals(Outcome.FIZZLE, type(w, "stupefy").outcome()); // ไม่ติด cooldown
    }

    @Test
    void cannotRecastBeforeCooldownEnds() {
        GameWorld w = emptyWorld();
        w.addEnemy(new Troll(800, 304));
        assertEquals(Outcome.CAST, type(w, "stupefy").outcome());
        assertEquals(Outcome.ON_COOLDOWN, type(w, "stupefy").outcome());
        run(w, 2.1); // cooldown ของ stupefy = 2 วินาที
        assertEquals(Outcome.CAST, type(w, "stupefy").outcome());
    }

    @Test
    void fastExactTypingIsPerfect() {
        GameWorld w = emptyWorld();
        w.addEnemy(new Goblin(800, 304));
        assertEquals(Tier.PERFECT, type(w, "stupefy").tier());
    }

    @Test
    void dementorTakesMoreDamageFromPatronumThanStupefy() {
        GameWorld a = emptyWorld();
        Dementor hitByStupefy = new Dementor(500, 304);
        a.addEnemy(hitByStupefy);
        type(a, "stupefy");
        run(a, 1.5);

        GameWorld b = emptyWorld();
        Dementor hitByPatronum = new Dementor(500, 304);
        b.addEnemy(hitByPatronum);
        type(b, "expecto patronum");
        run(b, 1.5);

        double stupefyDamage = hitByStupefy.maxHp() - hitByStupefy.hp();
        double patronumDamage = hitByPatronum.maxHp() - hitByPatronum.hp();
        assertTrue(stupefyDamage > 0, "stupefy should still scratch it");
        assertTrue(patronumDamage > stupefyDamage * 5,
                "patronum " + patronumDamage + " vs stupefy " + stupefyDamage);
    }

    @Test
    void lockOnRetargetsWhenLockedEnemyDies() {
        GameWorld w = emptyWorld();
        Goblin near = new Goblin(300, 304);
        Goblin far = new Goblin(700, 304);
        w.addEnemy(near);
        w.addEnemy(far);
        assertSame(near, w.currentLockTarget()); // ค่าเริ่มต้น = ใกล้ฐานที่สุด

        w.cycleTarget();
        assertSame(far, w.currentLockTarget()); // Tab ไปตัวถัดไป

        far.kill();
        assertSame(near, w.currentLockTarget()); // ตัวที่ล็อกตาย → กลับไปตัวใกล้สุด
    }

    @Test
    void protegoShieldAbsorbsDamage() {
        GameWorld w = emptyWorld();
        assertEquals(Outcome.CAST, type(w, "protego").outcome());
        w.base().takeDamage(10);
        assertEquals(Base.MAX_HP, w.base().hp());
    }

    @Test
    void enemyReachingBaseHurtsIt() {
        GameWorld w = emptyWorld();
        w.addEnemy(new Goblin(w.base().frontX() + 20, 304));
        run(w, 1.0);
        assertEquals(Base.MAX_HP - 5, w.base().hp());
        assertTrue(w.enemies().isEmpty());
    }

    @Test
    void incendioHitsSeveralEnemies() {
        GameWorld w = emptyWorld();
        double y = w.wizard().y(); // ไม้ชี้ไปทางขวาตรง ๆ
        Troll a = new Troll(500, y);
        Troll b = new Troll(530, y + 20);
        w.addEnemy(a);
        w.addEnemy(b);
        assertEquals(Outcome.CAST, type(w, "incendio").outcome());
        run(w, 1.5);
        assertTrue(a.hp() < a.maxHp() && b.hp() < b.maxHp());
    }

    @Test
    void losingAllHpEndsTheGame() {
        GameWorld w = emptyWorld();
        w.base().takeDamage(Base.MAX_HP);
        w.update(0.016);
        assertEquals(GameWorld.Status.LOST, w.status());
        assertEquals(false, w.stats().won());
    }

    @Test
    void consecutivePerfectsBuildComboAndBoostDamage() {
        GameWorld w = emptyWorld();
        w.addEnemy(new Troll(800, 304));
        type(w, "stupefy");
        assertEquals(1, w.combo());
        assertEquals(1.0, w.comboMultiplier(), 1e-9);
        run(w, 1.6);
        type(w, "stupefy");
        assertEquals(2, w.combo());
        assertEquals(1.1, w.comboMultiplier(), 1e-9);
    }

    @Test
    void nonPerfectCastResetsCombo() {
        GameWorld w = emptyWorld();
        w.addEnemy(new Troll(800, 304));
        type(w, "stupefy");
        type(w, "incendoi"); // พิมพ์ผิด → ไม่ใช่ PERFECT
        assertEquals(0, w.combo());
        assertEquals(1, w.stats().bestCombo());
    }

    @Test
    void mouseAimsWandAndClickLocksTarget() {
        GameWorld w = emptyWorld();
        Wizard wiz = w.wizard();
        w.aimWandAt(wiz.x() + 100, wiz.y() + 100); // ล่างขวา 45°
        assertEquals(Math.PI / 4, wiz.wandAngle(), 1e-9);

        Goblin near = new Goblin(300, 304);
        Goblin far = new Goblin(700, 144);
        w.addEnemy(near);
        w.addEnemy(far);
        w.lockTargetAt(705, 150);
        assertSame(far, w.currentLockTarget());
    }
}
