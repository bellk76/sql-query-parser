package io.github.sqlparser.model;

/**
 * SQL join kinds supported by the parser. {@code OUTER} is accepted as an
 * optional keyword but does not change the join kind.
 */
public enum JoinType {
    INNER("INNER JOIN"),
    LEFT("LEFT JOIN"),
    RIGHT("RIGHT JOIN"),
    FULL("FULL JOIN"),
    CROSS("CROSS JOIN");

    private final String sql;

    JoinType(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }

    @Override
    public String toString() {
        return sql;
    }
}
