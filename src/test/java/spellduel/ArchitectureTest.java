package spellduel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** กฎ: logic ของเกม (model, typing) ห้ามใช้ Swing เพื่อให้เทสต์ได้โดยไม่ต้องเปิดหน้าต่าง */
class ArchitectureTest {

    @Test
    void modelAndTypingDoNotImportSwing() throws IOException {
        for (String pkg : List.of("model", "typing")) {
            Path dir = Path.of("src/main/java/spellduel", pkg);
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String src = Files.readString(f);
                    assertTrue(!src.contains("javax.swing"), f + " must not use javax.swing");
                }
            }
        }
    }

    /** ui คุยกับ model ได้แค่ผ่าน GameWorld: class อื่นใน model ต้องเป็น package-private */
    @Test
    void onlyGameWorldStatsAndAssetsArePublicInModel() throws IOException {
        Set<String> allowed = Set.of("GameWorld.java", "GameStats.java", "Assets.java");
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/spellduel/model"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                boolean isPublic = Files.readAllLines(f).stream().anyMatch(line -> line.startsWith("public "));
                if (!allowed.contains(f.getFileName().toString())) {
                    assertFalse(isPublic, f + " must be package-private");
                }
            }
        }
    }
}
