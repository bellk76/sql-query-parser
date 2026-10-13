package io.github.sqlparser.model.expr;

/**
 * A unary operation, e.g. logical negation {@code NOT active} or arithmetic
 * negation {@code -1}.
 */
public record UnaryExpression(UnaryOperator operator, Expression operand) implements Expression {

    public UnaryExpression {
        if (operator == null || operand == null) {
            throw new IllegalArgumentException("operator and operand must not be null");
        }
    }

    @Override
    public String toString() {
        if (operator == UnaryOperator.NOT) {
            return "NOT " + operand;
        }
        String operandText = operand.toString();
        return operandText.startsWith("-") ? "- " + operandText : "-" + operandText;
    }
}
