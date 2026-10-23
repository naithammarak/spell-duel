package spelldefender;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** กฎ: logic ของเกม (model, typing) ห้ามใช้ Swing เพื่อให้เทสต์ได้โดยไม่ต้องเปิดหน้าต่าง */
class ArchitectureTest {

    @Test
    void modelAndTypingDoNotImportSwing() throws IOException {
        for (String pkg : List.of("model", "typing")) {
            Path dir = Path.of("src/main/java/spelldefender", pkg);
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String src = Files.readString(f);
                    assertTrue(!src.contains("javax.swing"), f + " must not use javax.swing");
                }
            }
        }
    }
}
