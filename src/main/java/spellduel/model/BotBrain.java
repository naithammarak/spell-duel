package spellduel.model;

/** "โค้ช" ของบอท: ดูสถานการณ์แล้วบอกว่าจะร่ายอะไร เปลี่ยนโค้ช = ได้บอทนิสัยใหม่ */
interface BotBrain {

    /** ต้นรอบ: จะร่ายหน้าที่อะไร */
    SpellRole decide(DuelView view);

    /** ระหว่างร่าย (เรียกทุก tick): คืนหน้าที่ใหม่ถ้าจะเปลี่ยน หรือ null = ไม่เปลี่ยน */
    default SpellRole duringCast(DuelView view, Cast cast) {
        return null;
    }
}
