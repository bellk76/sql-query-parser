package io.github.sqlparser.model;

import io.github.sqlparser.model.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * An explicit join, e.g. {@code LEFT JOIN book ON author.id = book.author_id}.
 * The join condition is either an {@code ON} expression or a list of
 * {@code USING} column names (at most one of them is set).
 */
public final class Join {

    private final JoinType type;
    private final Source source;
    private final Expression condition;
    private final List<String> usingColumns;

    public Join(JoinType type, Source source, Expression condition) {
        this(type, source, condition, List.of());
    }

    public Join(JoinType type, Source source, Expression condition, List<String> usingColumns) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.condition = condition;
        this.usingColumns = List.copyOf(usingColumns);
    }

    public JoinType type() {
        return type;
    }

    public Source source() {
        return source;
    }

    public Expression condition() {
        return condition;
    }

    public List<String> usingColumns() {
        return usingColumns;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type.sql()).append(' ').append(source);
        if (!usingColumns.isEmpty()) {
            sb.append(" USING (").append(String.join(", ", usingColumns)).append(')');
        } else if (condition != null) {
            sb.append(" ON ").append(condition);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Join other)) {
            return false;
        }
        return type == other.type
                && source.equals(other.source)
                && Objects.equals(condition, other.condition)
                && usingColumns.equals(other.usingColumns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, source, condition, usingColumns);
    }
}
