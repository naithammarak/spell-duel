package spellduel.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * โหลดรูป sprite จาก /sprites/&lt;name&gt;.png ใน resources และเก็บ cache ไว้
 * ถ้าไม่มีไฟล์จะคืน null ให้ entity วาดรูปทรงแทน (เกมจึงเล่นได้แม้ยังไม่มีรูป)
 */
public final class Assets {

    private final Map<String, BufferedImage> cache = new HashMap<>();
    private final boolean enabled;

    private Assets(boolean enabled) {
        this.enabled = enabled;
    }

    /** โหลดรูปจริงจาก resources */
    public static Assets load() {
        return new Assets(true);
    }

    /** ไม่โหลดรูปเลย ทุกอย่างวาดเป็นรูปทรง (ใช้ในเทสต์) */
    public static Assets none() {
        return new Assets(false);
    }

    /** คืนรูป หรือ null ถ้าไม่มี */
    BufferedImage get(String name) {
        if (!enabled) {
            return null;
        }
        if (!cache.containsKey(name)) {
            cache.put(name, read("/sprites/" + name + ".png"));
        }
        return cache.get(name);
    }

    private BufferedImage read(String path) {
        try (InputStream in = Assets.class.getResourceAsStream(path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }
}
