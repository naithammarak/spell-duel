package spelldefender;

import javax.swing.SwingUtilities;
import spelldefender.ui.GameWindow;

/** จุดเริ่มโปรแกรม: สร้างหน้าต่างบน Swing thread (EDT) */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
