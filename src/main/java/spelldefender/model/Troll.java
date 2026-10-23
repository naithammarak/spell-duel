package spelldefender.model;

import java.awt.Color;

/** ช้า HP เยอะ ตัวหนักจึงถูกผลักไปได้แค่ครึ่งเดียว */
class Troll extends Enemy {

    Troll(double x, double y) {
        super(x, y, 22, 80, 25, 15);
    }

    @Override
    void knockBack(double distance) {
        super.knockBack(distance / 2);
    }

    @Override
    protected String spriteName() {
        return "troll";
    }

    @Override
    protected Color bodyColor() {
        return new Color(130, 120, 95);
    }
}
