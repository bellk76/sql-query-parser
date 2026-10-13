package io.github.sqlparser.model.expr;

/**
 * A {@code [NOT] BETWEEN low AND high} predicate.
 */
public record BetweenExpression(Expression operand, Expression low, Expression high, boolean negated)
        implements Expression {

    public BetweenExpression {
        if (operand == null || low == null || high == null) {
            throw new IllegalArgumentException("operand, low and high must not be null");
        }
    }

    @Override
    public String toString() {
        return operand + (negated ? " NOT BETWEEN " : " BETWEEN ") + low + " AND " + high;
    }
}
