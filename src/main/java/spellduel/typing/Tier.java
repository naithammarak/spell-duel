package spellduel.typing;

/**
 * ระดับความแม่นยำของคาถา แต่ละระดับมีตัวคูณดาเมจ (multiplier) และค่า accuracy ขั้นต่ำ
 * PERFECT ไม่ได้มาจาก accuracy อย่างเดียว ต้องพิมพ์ถูกหมดและทันเวลาด้วย (ดู AccuracyGrader)
 */
public enum Tier {
    PERFECT("PERFECT!", 1.25, 1.00),
    GREAT("Great", 1.00, 0.85),
    GOOD("Good", 0.60, 0.60),
    WEAK("Weak", 0.30, 0.30),
    FIZZLE("Fizzle!", 0.00, 0.00);

    private final String label;
    private final double multiplier;
    private final double minAccuracy;

    Tier(String label, double multiplier, double minAccuracy) {
        this.label = label;
        this.multiplier = multiplier;
        this.minAccuracy = minAccuracy;
    }

    public String label() {
        return label;
    }

    public double multiplier() {
        return multiplier;
    }

    /** หาระดับจาก accuracy (0..1) โดยไม่สนเวลา จึงได้สูงสุดแค่ GREAT */
    public static Tier forAccuracy(double accuracy) {
        Tier[] candidates = {GREAT, GOOD, WEAK};
        for (Tier t : candidates) {
            if (accuracy >= t.minAccuracy) {
                return t;
            }
        }
        return FIZZLE;
    }
}
