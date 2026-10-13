package io.github.sqlparser.model.expr;

/**
 * An explicitly parenthesised expression, e.g. {@code (a + b) * c}. Preserving
 * user parentheses keeps {@code toString()} faithful to the original query.
 */
public record ParenthesizedExpression(Expression inner) implements Expression {

    public ParenthesizedExpression {
        if (inner == null) {
            throw new IllegalArgumentException("inner expression must not be null");
        }
    }

    @Override
    public String toString() {
        return "(" + inner + ")";
    }
}
