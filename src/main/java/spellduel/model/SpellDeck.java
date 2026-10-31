package spellduel.model;

import java.util.Random;
import spellduel.typing.Levenshtein;

/**
 * การ์ด 3 ใบบนมือผู้เล่น (ใบละหน้าที่) ใช้ใบไหนแล้วใบนั้นสุ่มคำใหม่จากแถวเดิมใน POOLS
 */
class SpellDeck {

    /** 2D array: แถว = หน้าที่ (ตาม SpellRole.ordinal()), คอลัมน์ = คาถาของหน้าที่นั้น */
    static final String[][] POOLS = {
            {"bombarda maxima", "petrificus totalus", "incendio maxima", "confringo maxima", "sectumsempra"},
            {"protego maxima", "protego totalum", "protego horribilis", "salvio hexia", "cave inimicum"},
            {"expelliarmus", "impedimenta", "depulso maxima", "reducto maxima", "diffindo maxima"},
    };

    /** ต้องพิมพ์อย่างน้อยกี่ตัวก่อนจะเดาว่าผู้เล่นกำลังพิมพ์หน้าที่อะไร */
    static final int GUESS_MIN_CHARS = 3;

    /** ผลการหาการ์ดที่ใกล้คำที่พิมพ์ที่สุด */
    record Match(Incantation card, int distance) {
    }

    private final Incantation[] hand = new Incantation[SpellRole.values().length];
    private final Random rnd;

    SpellDeck(Random rnd) {
        this.rnd = rnd;
        for (SpellRole role : SpellRole.values()) {
            hand[role.ordinal()] = new Incantation(randomWord(role, rnd), role);
        }
    }

    static SpellDeck standard(Random rnd) {
        return new SpellDeck(rnd);
    }

    /** สุ่มคำจากแถวของหน้าที่นี้ (บอทก็ใช้) */
    static String randomWord(SpellRole role, Random rnd) {
        String[] row = POOLS[role.ordinal()];
        return row[rnd.nextInt(row.length)];
    }

    /** คืนสำเนา จะได้ไม่มีใครแก้มือจากข้างนอก */
    Incantation[] hand() {
        return hand.clone();
    }

    /** หาการ์ดบนมือที่ใกล้ typed ที่สุด ถ้าเสมอกันเลือกคำที่สั้นกว่า */
    Match match(String typed) {
        Incantation best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Incantation card : hand) {
            int d = Levenshtein.distance(typed, card.text());
            boolean tieButShorter = d == bestDistance && card.text().length() < best.text().length();
            if (d < bestDistance || tieButShorter) {
                best = card;
                bestDistance = d;
            }
        }
        return new Match(best, bestDistance);
    }

    /** ใช้การ์ดใบนี้แล้ว: สุ่มคำใหม่จากแถวเดิม โดยห้ามซ้ำคำเดิม */
    void consume(Incantation card) {
        int slot = card.role().ordinal();
        String old = hand[slot].text();
        String next = old;
        while (next.equals(old)) {
            next = randomWord(card.role(), rnd);
        }
        hand[slot] = new Incantation(next, card.role());
    }

    /**
     * เดาว่าผู้เล่นกำลังพิมพ์หน้าที่อะไร: เทียบ prefix กับ "ต้นคำ" ของการ์ดแต่ละใบ
     * (ตัดคำให้ยาวเท่าที่พิมพ์) · พิมพ์ไม่ถึง 3 ตัวคืน null
     */
    SpellRole guessRole(String prefix) {
        if (prefix.length() < GUESS_MIN_CHARS) {
            return null;
        }
        SpellRole best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Incantation card : hand) {
            String head = card.text().substring(0, Math.min(prefix.length(), card.text().length()));
            int d = Levenshtein.distance(prefix, head);
            if (d < bestDistance) {
                best = card.role();
                bestDistance = d;
            }
        }
        return best;
    }
}
