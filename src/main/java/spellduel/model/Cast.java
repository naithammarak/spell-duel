package spellduel.model;

/**
 * คาถาที่บอทกำลังร่าย = cast bar บนจอ
 * เต็มเมื่อ elapsed ≥ duration แล้วบอทจะปล่อยคาถาออกไป
 */
class Cast {

    /** แถบถึงตรงนี้แล้วผู้เล่นจะเห็นว่าบอทกำลังร่ายอะไร */
    static final double REVEAL_AT = 0.2;

    private SpellRole role;
    private String word;
    private final double duration;
    private double elapsed;
    private boolean feinted;

    Cast(SpellRole role, String word, double duration) {
        this.role = role;
        this.word = word;
        this.duration = duration;
    }

    SpellRole role() {
        return role;
    }

    String word() {
        return word;
    }

    double duration() {
        return duration;
    }

    void advance(double dt) {
        elapsed += dt;
    }

    /** 0..1 ร่ายไปได้กี่ส่วนแล้ว */
    double progress() {
        return Math.min(1, elapsed / duration);
    }

    /** บอททุกตัวเปิดหน้าที่เมื่อร่ายถึง 20% ของแถบ */
    boolean isRevealed() {
        return progress() >= REVEAL_AT;
    }

    boolean isDone() {
        return elapsed >= duration;
    }

    /** หลอก: เปลี่ยนหน้าที่กลางทาง แต่เวลาที่เหลือเท่าเดิม (Dark Wizard) */
    void switchTo(SpellRole newRole, String newWord) {
        role = newRole;
        word = newWord;
        feinted = true;
    }

    boolean isFeinted() {
        return feinted;
    }
}
