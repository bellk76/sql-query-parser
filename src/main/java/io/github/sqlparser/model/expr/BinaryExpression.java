package io.github.sqlparser.model.expr;

/**
 * A binary operation: logical (AND, OR), comparison (=, &lt;, &gt;, ...) or
 * arithmetic. The tree shape encodes operator precedence, so {@code AND} nodes
 * are nested deeper than {@code OR} nodes.
 */
public record BinaryExpression(Expression left, BinaryOperator operator, Expression right) implements Expression {

    public BinaryExpression {
        if (left == null || operator == null || right == null) {
            throw new IllegalArgumentException("left, operator and right must not be null");
        }
    }

    @Override
    public String toString() {
        return left + " " + operator + " " + right;
    }
}
