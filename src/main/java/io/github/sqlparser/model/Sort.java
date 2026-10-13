package io.github.sqlparser.model;

import io.github.sqlparser.model.expr.Expression;

/**
 * A single ORDER BY item, e.g. {@code author.name DESC}. Direction defaults to
 * {@link SortDirection#ASC} when omitted in the query.
 */
public record Sort(Expression expression, SortDirection direction) {

    public Sort {
        if (expression == null || direction == null) {
            throw new IllegalArgumentException("expression and direction must not be null");
        }
    }

    public Sort(Expression expression) {
        this(expression, SortDirection.ASC);
    }

    @Override
    public String toString() {
        return expression + " " + direction;
    }
}
