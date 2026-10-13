package io.github.sqlparser.model.expr;

/**
 * An {@code IS [NOT] NULL} predicate.
 */
public record IsNullExpression(Expression operand, boolean negated) implements Expression {

    public IsNullExpression {
        if (operand == null) {
            throw new IllegalArgumentException("operand must not be null");
        }
    }

    @Override
    public String toString() {
        return operand + " IS" + (negated ? " NOT" : "") + " NULL";
    }
}
