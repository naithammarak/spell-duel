package spelldefender.model;

/** รายการสายนักเวทที่ซื้อได้ (ที่เดียวในเกม): ชื่อ ราคา และวิธีสร้าง */
enum AllyType {
    GUARDIAN("ผู้พิทักษ์", 30),
    FIRE("นักเวทไฟ", 50),
    ICE("นักเวทน้ำแข็ง", 40),
    LIGHTNING("นักเวทสายฟ้า", 70);

    private final String label;
    private final int cost;

    AllyType(String label, int cost) {
        this.label = label;
        this.cost = cost;
    }

    String label() {
        return label;
    }

    int cost() {
        return cost;
    }

    Ally create(double x, double y) {
        return switch (this) {
            case GUARDIAN -> new Guardian(x, y);
            case FIRE -> new FireMage(x, y);
            case ICE -> new IceMage(x, y);
            case LIGHTNING -> new LightningMage(x, y);
        };
    }
}
