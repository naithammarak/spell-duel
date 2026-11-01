package spellduel.model;

import spellduel.typing.Tier;

/** สรุปผลการดวล 1 ครั้งสำหรับหน้า Result (แก้ไขไม่ได้ ส่งข้าม thread ได้ปลอดภัย) */
public record GameStats(boolean won, int botLevel, String botName, int hpLeft, double seconds,
                        double averageAccuracy, int[] tierCounts) {

    public GameStats {
        tierCounts = tierCounts.clone();
    }

    /** ชนะแล้วฟื้น HP เท่านี้ก่อนขึ้นด่านใหม่ */
    public static final int HEAL_PER_WIN = 75;

    /** HP ตอนเริ่มด่านถัดไป */
    public int nextHp() {
        return Math.min(GameWorld.PLAYER_MAX_HP, hpLeft + HEAL_PER_WIN);
    }

    /** จำนวนด่านที่ผ่านในการเล่นรอบนี้ */
    public int levelsCleared() {
        return won ? botLevel + 1 : botLevel;
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
