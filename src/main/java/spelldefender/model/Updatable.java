package spelldefender.model;

/** สิ่งที่เปลี่ยนไปตามเวลา dt = วินาทีที่ผ่านไปตั้งแต่เฟรมก่อน */
interface Updatable {
    void update(double dt);
}
