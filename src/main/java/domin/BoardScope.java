package domin;

/** A validated admin filter; ALL combines statistics across the three independent modes. */
public enum BoardScope {
    ALL(null), SMALL(BoardType.SMALL), MEDIUM(BoardType.MEDIUM), LARGE(BoardType.LARGE);
    public final BoardType type;
    BoardScope(BoardType type) { this.type = type; }
    public String id() { return type == null ? "ALL" : type.id; }
    public boolean matches(BoardType board) { return type == null || type == board; }
    public BoardType[] types() { return type == null ? BoardType.values() : new BoardType[]{type}; }
    public static BoardScope fromId(String id) {
        if (id == null || id.isEmpty() || "ALL".equals(id)) return ALL;
        for (BoardScope scope : values()) if (scope.id().equals(id)) return scope;
        throw new IllegalArgumentException("Chế độ quản trị không hợp lệ");
    }
    @Override public String toString() { return type == null ? "Tất cả chế độ" : type.displayName(); }
}
