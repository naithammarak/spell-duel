package spellduel.model;

import java.awt.Color;
import spellduel.typing.InputBuffer;

/** ผู้เล่น (ซ้ายจอ) "กำลังร่าย" ตลอดเวลาที่ช่องพิมพ์ไม่ว่าง */
class PlayerWizard extends Duelist {

    static final int MAX_HP = 100;

    private final InputBuffer input;
    private final SpellDeck deck;

    PlayerWizard(double x, double y, InputBuffer input, SpellDeck deck) {
        super("คุณ", MAX_HP, x, y, +1, new Color(70, 90, 200));
        this.input = input;
        this.deck = deck;
    }

    /** โดนตัด: ข้อความที่พิมพ์ค้างไว้หาย */
    @Override
    void interruptCast() {
        input.clear();
    }

    @Override
    boolean isCasting() {
        return !input.isEmpty();
    }

    /** เดาจากสิ่งที่พิมพ์ (นิยามเดียวกับที่บอทใช้อ่านทาง) */
    @Override
    SpellRole castingRole() {
        return deck.guessRole(input.text());
    }
}
