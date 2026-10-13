package io.github.sqlparser.model.expr;

/**
 * A {@code [NOT] LIKE pattern} predicate.
 */
public record LikeExpression(Expression operand, Expression pattern, boolean negated) implements Expression {

    public LikeExpression {
        if (operand == null || pattern == null) {
            throw new IllegalArgumentException("operand and pattern must not be null");
        }
    }

    @Override
    public String toString() {
        return operand + (negated ? " NOT LIKE " : " LIKE ") + pattern;
    }
}
