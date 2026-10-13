package io.github.sqlparser.model.expr;

/**
 * A column reference, optionally qualified by a table name or alias, e.g.
 * {@code name} or {@code author.name}.
 */
public record ColumnExpression(String table, String column) implements Expression {

    public ColumnExpression {
        if (column == null || column.isEmpty()) {
            throw new IllegalArgumentException("column name must not be empty");
        }
    }

    public ColumnExpression(String column) {
        this(null, column);
    }

    public boolean qualified() {
        return table != null;
    }

    public String qualifiedName() {
        return table == null ? column : table + "." + column;
    }

    @Override
    public String toString() {
        return qualifiedName();
    }
}
