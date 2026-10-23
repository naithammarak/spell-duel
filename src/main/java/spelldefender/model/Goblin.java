package spelldefender.model;

import java.awt.Color;

/** เร็ว แต่ HP น้อย */
class Goblin extends Enemy {

    Goblin(double x, double y) {
        super(x, y, 12, 16, 42, 2);
    }

    @Override
    int bounty() {
        return 2;
    }

    @Override
    protected String spriteName() {
        return "goblin";
    }

    @Override
    protected Color bodyColor() {
        return new Color(90, 170, 60);
    }
}
