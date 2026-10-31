package spellduel.typing;

/**
 * เก็บตัวอักษรที่ผู้เล่นพิมพ์ และเวลาที่กดตัวแรก (เวลาในเกม หน่วยวินาที)
 * รับเฉพาะ a-z และช่องว่าง ตัวพิมพ์ใหญ่จะถูกแปลงเป็นตัวเล็ก
 */
public class InputBuffer {

    public static final int MAX_LENGTH = 24;

    private final StringBuilder text = new StringBuilder();
    private double startTime;

    /** ตัวอักษรนี้ใช้พิมพ์คาถาได้ไหม */
    public static boolean accepts(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= 'a' && lower <= 'z') || c == ' ';
    }

    /** @return true ถ้ารับตัวอักษรนี้ */
    public boolean add(char c, double now) {
        if (!accepts(c) || text.length() >= MAX_LENGTH) {
            return false;
        }
        if (text.length() == 0) {
            startTime = now; // เริ่มจับเวลาตั้งแต่ตัวแรก
        }
        text.append(Character.toLowerCase(c));
        return true;
    }

    public void backspace() {
        if (text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
    }

    public String text() {
        return text.toString();
    }

    public boolean isEmpty() {
        return text.length() == 0;
    }

    /** วินาทีตั้งแต่กดตัวแรกจนถึง now */
    public double elapsed(double now) {
        return isEmpty() ? 0 : now - startTime;
    }

    public void clear() {
        text.setLength(0);
    }
}
