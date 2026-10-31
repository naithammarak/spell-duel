package spellduel.model;

import spellduel.typing.Tier;

/** สรุปผลการดวล 1 ครั้งสำหรับหน้า Result (แก้ไขไม่ได้ ส่งข้าม thread ได้ปลอดภัย) */
public record GameStats(boolean won, int botLevel, String botName, int hpLeft, double seconds,
                        double averageAccuracy, int[] tierCounts) {

    public GameStats {
        tierCounts = tierCounts.clone();
    }

    /** ยังมีบอทตัวถัดไปให้ไต่อีกไหม */
    public boolean hasNextBot() {
        return botLevel < GameWorld.BOT_COUNT - 1;
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
