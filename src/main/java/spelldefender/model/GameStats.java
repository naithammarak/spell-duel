package spelldefender.model;

import spelldefender.typing.Tier;

/** สรุปผลตอนจบเกม สำหรับหน้า Result (แก้ไขไม่ได้ ส่งข้าม thread ได้ปลอดภัย) */
public record GameStats(boolean won, int waveReached, int totalWaves, int enemiesDefeated,
                        double averageAccuracy, int[] tierCounts, int bestCombo) {

    public GameStats {
        tierCounts = tierCounts.clone();
    }

    /** จำนวนครั้งที่ร่ายได้ระดับนี้ */
    public int count(Tier tier) {
        return tierCounts[tier.ordinal()];
    }

    public int totalCasts() {
        int sum = 0;
        for (int c : tierCounts) {
            sum += c;
        }
        return sum;
    }

    @Override
    public int[] tierCounts() {
        return tierCounts.clone();
    }
}
