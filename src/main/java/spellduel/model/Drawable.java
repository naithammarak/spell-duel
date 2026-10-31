package spellduel.model;

import java.awt.Graphics2D;

/** สิ่งที่วาดตัวเองลงจอได้ ถ้า assets ไม่มีรูป ให้วาดเป็นรูปทรงแทน */
interface Drawable {
    void draw(Graphics2D g, Assets assets);
}
