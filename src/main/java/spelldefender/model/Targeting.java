package spelldefender.model;

import spelldefender.typing.Tier;

/** วิธีพาคาถาไปถึงเป้า (คาถาแต่ละอันถือ Targeting ของตัวเอง) */
interface Targeting {

    /** @return false ถ้าหาเป้าไม่เจอ (นับเป็น Fizzle) */
    boolean launch(Spell spell, Tier tier, GameWorld world);
}
