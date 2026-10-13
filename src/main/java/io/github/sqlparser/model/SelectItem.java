package io.github.sqlparser.model;

import io.github.sqlparser.model.expr.Expression;

/**
 * A single item in the SELECT list: an expression with an optional alias,
 * e.g. {@code author.name} or {@code sum(book.cost) AS total_cost}.
 */
public record SelectItem(Expression expression, String alias) {

    public SelectItem {
        if (expression == null) {
            throw new IllegalArgumentException("expression must not be null");
        }
    }

    public SelectItem(Expression expression) {
        this(expression, null);
    }

    public boolean hasAlias() {
        return alias != null;
    }

    @Override
    public String toString() {
        return alias == null ? expression.toString() : expression + " AS " + alias;
    }
}
