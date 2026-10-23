package spelldefender.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * แผนที่แบบตาราง int[][] tiles (แถว × คอลัมน์) ช่องละ 32 พิกเซล
 * ตำแหน่งฐานและจุดเกิดศัตรู "อ่านจากแผนที่" ไม่ได้เขียนตัวเลขซ้ำที่อื่น
 */
class TileMap implements Drawable {

    static final int TILE = 32;

    static final int GRASS = 0;
    static final int PATH = 1;
    static final int BASE = 2;
    static final int SPAWN = 3;
    static final int TREE = 4;

    /** แผนที่มาตรฐาน: . หญ้า, # ทางเดิน, B ฐาน, S จุดเกิดศัตรู, T ต้นไม้ */
    private static final String[] STANDARD_LAYOUT = {
            "..............................",
            "...........T.T.T..........T...",
            "BBB...........TT..............",
            "BBB...........................",
            "BBB##########################S",
            "BBB...........................",
            "BBB..........T.............T..",
            "BBB........T...T..............",
            "BBB...........................",
            "BBB##########################S",
            "BBB...........................",
            "BBB............T........T.....",
            "BBB......TT...TT...T..........",
            "BBB...........................",
            "BBB##########################S",
            "BBB...........................",
            "BBB..........T....T...........",
            "................TT............",
            "..............................",
            "..............................",
    };

    private final int[][] tiles;
    private BufferedImage cache; // วาดแผนที่ครั้งเดียวแล้วเก็บไว้ (แผนที่ไม่เปลี่ยน)

    TileMap(int[][] tiles) {
        this.tiles = tiles;
    }

    static TileMap standard() {
        return fromLayout(STANDARD_LAYOUT);
    }

    /** แปลงตัวอักษรใน layout เป็นตาราง int[][] */
    static TileMap fromLayout(String[] layout) {
        int[][] t = new int[layout.length][layout[0].length()];
        for (int row = 0; row < layout.length; row++) {
            for (int col = 0; col < layout[row].length(); col++) {
                t[row][col] = switch (layout[row].charAt(col)) {
                    case '#' -> PATH;
                    case 'B' -> BASE;
                    case 'S' -> SPAWN;
                    case 'T' -> TREE;
                    default -> GRASS;
                };
            }
        }
        return new TileMap(t);
    }

    int rows() {
        return tiles.length;
    }

    int cols() {
        return tiles[0].length;
    }

    /** จุดกึ่งกลางของทุกช่อง SPAWN เป็น {x, y} */
    List<double[]> spawnPoints() {
        List<double[]> points = new ArrayList<>();
        for (int row = 0; row < rows(); row++) {
            for (int col = 0; col < cols(); col++) {
                if (tiles[row][col] == SPAWN) {
                    points.add(new double[] {col * TILE + TILE / 2.0, row * TILE + TILE / 2.0});
                }
            }
        }
        return points;
    }

    /** สร้าง Base จากกรอบของช่อง BASE ทั้งหมด */
    Base createBase() {
        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int maxCol = -1;
        for (int row = 0; row < rows(); row++) {
            for (int col = 0; col < cols(); col++) {
                if (tiles[row][col] == BASE) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }
        if (maxCol < 0) {
            throw new IllegalStateException("map has no BASE tiles");
        }
        double frontX = (maxCol + 1) * TILE;
        double height = (maxRow - minRow + 1) * TILE;
        double centerY = minRow * TILE + height / 2;
        return new Base(frontX / 2, centerY, frontX, height);
    }

    @Override
    public void draw(Graphics2D g, Assets assets) {
        if (cache == null) {
            cache = new BufferedImage(cols() * TILE, rows() * TILE, BufferedImage.TYPE_INT_RGB);
            Graphics2D cg = cache.createGraphics();
            for (int row = 0; row < rows(); row++) {
                for (int col = 0; col < cols(); col++) {
                    drawTile(cg, assets, tiles[row][col], col * TILE, row * TILE, row, col);
                }
            }
            cg.dispose();
        }
        g.drawImage(cache, 0, 0, null);
    }

    private void drawTile(Graphics2D g, Assets assets, int type, int px, int py, int row, int col) {
        String[] names = {"tile_grass", "tile_path", "tile_grass", "tile_path", "tile_tree"};
        BufferedImage img = assets.get(names[type]);
        if (img != null) {
            g.drawImage(img, px, py, TILE, TILE, null);
            return;
        }
        // ไม่มีรูป: หญ้าลายตารางหมากรุกอ่อน ๆ
        boolean odd = (row + col) % 2 == 0;
        g.setColor(odd ? new Color(70, 130, 60) : new Color(64, 122, 55));
        g.fillRect(px, py, TILE, TILE);
        if (type == PATH || type == SPAWN) {
            g.setColor(odd ? new Color(176, 146, 96) : new Color(168, 138, 90));
            g.fillRect(px, py + 2, TILE, TILE - 4);
        }
        if (type == SPAWN) {
            g.setColor(new Color(60, 20, 80));
            g.fillOval(px + 2, py + 2, TILE - 4, TILE - 4);
        }
        if (type == TREE) {
            g.setColor(new Color(90, 60, 30));
            g.fillRect(px + 13, py + 18, 6, 12);
            g.setColor(new Color(35, 90, 40));
            g.fillOval(px + 4, py + 2, 24, 22);
        }
    }
}
