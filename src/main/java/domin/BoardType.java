package domin;
public enum BoardType {
    SMALL("9x9", 9, 9, 10, 300, ""),
    MEDIUM("16x16", 16, 16, 40, 600, "_16x16"),
    LARGE("16x30", 16, 30, 99, 900, "_16x30");
    public final String id;
    public final int rows, cols, mines, timeLimit;
    final String statsSuffix;
    BoardType(String id, int rows, int cols, int mines, int timeLimit, String suffix) {
        this.id = id; this.rows = rows; this.cols = cols; this.mines = mines;
        this.timeLimit = timeLimit; this.statsSuffix = suffix;
    }
    public String displayName() {
        return this == SMALL ? "Cơ bản" : this == MEDIUM ? "Nâng cao" : "Chuyên gia";
    }
    public int safeTotal() { return rows * cols - mines; }
    public static BoardType fromId(String id) {
        if (id == null || id.isEmpty()) return SMALL;
        for (BoardType type : values()) if (type.id.equals(id)) return type;
        throw new IllegalArgumentException("Loại bàn không hợp lệ: " + id);
    }
    @Override public String toString() { return id + " · " + mines + " mìn"; }
}
