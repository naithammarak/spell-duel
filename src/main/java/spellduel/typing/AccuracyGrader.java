package spellduel.typing;

/**
 * ให้เกรดการร่ายคาถา จาก (จำนวนตัวที่ผิด, ความยาวคาถา, เวลาที่ใช้พิมพ์)
 *   accuracy = 1 - distance / length
 *   PERFECT  = ผิด 0 ตัว และพิมพ์เสร็จภายใน parTime = length × k + 1 วินาที
 */
public class AccuracyGrader {

    /** วินาทีต่อตัวอักษร ยิ่งน้อยยิ่งต้องพิมพ์เร็ว (ปรับตอน playtest) */
    public static final double DEFAULT_K = 0.25;

    private final double k;

    public AccuracyGrader() {
        this(DEFAULT_K);
    }

    public AccuracyGrader(double k) {
        this.k = k;
    }

    public double parTime(int spellLength) {
        return spellLength * k + 1.0;
    }

    public static double accuracy(int distance, int spellLength) {
        if (spellLength <= 0) {
            return 0;
        }
        return Math.max(0, 1.0 - (double) distance / spellLength);
    }

    public Tier grade(int distance, int spellLength, double seconds) {
        if (distance == 0 && seconds <= parTime(spellLength)) {
            return Tier.PERFECT;
        }
        return Tier.forAccuracy(accuracy(distance, spellLength));
    }
}
