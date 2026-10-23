package spelldefender.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmyTest {

    private static GameWorld emptyWorld() {
        return new GameWorld(SpellBook.standard(), WaveManager.idle(), TileMap.standard());
    }

    private static void run(GameWorld w, double seconds) {
        for (double t = 0; t < seconds; t += 0.02) {
            w.update(0.02);
        }
    }

    @Test
    void buyingCostsGoldAndFillsSlot() {
        GameWorld w = emptyWorld();
        w.selectAlly(AllyType.FIRE.ordinal());
        assertTrue(w.buyAlly(1, 0));
        assertEquals(GameWorld.START_GOLD - AllyType.FIRE.cost(), w.gold());
        assertFalse(w.army().isEmpty(1, 0));
        assertFalse(w.buyAlly(1, 0)); // ช่องเต็มแล้ว
    }

    @Test
    void cannotBuyWithoutEnoughGold() {
        GameWorld w = emptyWorld();
        w.selectAlly(AllyType.LIGHTNING.ordinal());
        while (w.gold() >= AllyType.LIGHTNING.cost()) { // ซื้อจนทองไม่พอ
            w.buyAlly(0, ArmyGrid.COLS - 1 - w.army().allies().size());
        }
        int left = w.gold();
        assertFalse(w.buyAlly(2, 0));
        assertEquals(left, w.gold());
    }

    @Test
    void clickingSlotPlacesSelectedAlly() {
        GameWorld w = emptyWorld();
        ArmyGrid army = w.army();
        w.selectAlly(AllyType.GUARDIAN.ordinal());
        w.click(army.slotX(2), army.slotY(0));
        assertFalse(army.isEmpty(0, 2));
    }

    @Test
    void guardianBlocksEnemyAndCanBeKilled() {
        GameWorld w = emptyWorld();
        ArmyGrid army = w.army();
        w.selectAlly(AllyType.GUARDIAN.ordinal());
        w.buyAlly(1, 3);
        Ally guardian = army.allies().get(0);
        Troll troll = new Troll(guardian.x() + 60, army.slotY(1));
        w.addEnemy(troll);

        run(w, 5);
        assertTrue(troll.x() > guardian.x(), "troll must not walk through the guardian");
        assertTrue(guardian.hp() < 220, "troll should be hitting the guardian");
    }

    @Test
    void killingEnemiesGivesGold() {
        GameWorld w = emptyWorld();
        w.selectAlly(AllyType.FIRE.ordinal());
        w.buyAlly(1, 0);
        int before = w.gold();
        Goblin goblin = new Goblin(w.army().slotX(0) + 200, w.army().slotY(1));
        w.addEnemy(goblin);
        run(w, 4);
        assertTrue(w.enemies().isEmpty());
        assertEquals(before + goblin.bounty(), w.gold());
    }

    @Test
    void iceMageSlowsEnemies() {
        GameWorld w = emptyWorld();
        w.selectAlly(AllyType.ICE.ordinal());
        w.buyAlly(1, 0);
        Troll troll = new Troll(w.army().slotX(0) + 200, w.army().slotY(1));
        w.addEnemy(troll);
        run(w, 0.1);
        assertTrue(troll.currentSpeed() < 24);
    }
}
