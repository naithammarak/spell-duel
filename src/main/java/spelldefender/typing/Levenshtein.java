package spelldefender.typing;

/**
 * Levenshtein distance: นับจำนวนครั้งน้อยที่สุดที่ต้อง "เพิ่ม / ลบ / เปลี่ยน" ตัวอักษร
 * เพื่อให้ข้อความ a กลายเป็นข้อความ b เช่น distance("stupify", "stupefy") = 1
 *
 * ใช้ Dynamic Programming บนตาราง 2 มิติ (int[][] dp):
 * dp[i][j] = ระยะห่างระหว่าง i ตัวแรกของ a กับ j ตัวแรกของ b
 */
public final class Levenshtein {

    private Levenshtein() {
    }

    public static int distance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        // แถวแรก / คอลัมน์แรก: เทียบกับข้อความว่าง ต้องเพิ่มหรือลบทุกตัว
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int delete = dp[i - 1][j] + 1;       // ลบตัวอักษรจาก a
                int insert = dp[i][j - 1] + 1;       // เพิ่มตัวอักษรเข้า a
                int replace = dp[i - 1][j - 1] + cost; // เปลี่ยน (หรือตรงกันพอดี)
                dp[i][j] = Math.min(Math.min(delete, insert), replace);
            }
        }
        return dp[a.length()][b.length()];
    }
}
