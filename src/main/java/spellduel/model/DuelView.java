package spellduel.model;

import java.util.Random;

/**
 * สิ่งที่สมองบอทมองเห็นได้ (อ่านอย่างเดียว)
 * playerIntent = หน้าที่ที่ผู้เล่นน่าจะกำลังพิมพ์ (จาก SpellDeck.guessRole) หรือ null
 * lastPlayerRole = การ์ดที่ผู้เล่นใช้รอบก่อน หรือ null ถ้ายังไม่เคยใช้
 */
record DuelView(int playerHp, int botHp, boolean playerShielded, boolean botShielded,
                SpellRole playerIntent, SpellRole lastPlayerRole, Random rnd) {
}
