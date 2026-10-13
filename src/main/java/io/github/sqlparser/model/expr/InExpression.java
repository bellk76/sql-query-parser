package io.github.sqlparser.model.expr;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An {@code [NOT] IN (value, ...)} predicate.
 */
public record InExpression(Expression operand, List<Expression> values, boolean negated) implements Expression {

    public InExpression {
        if (operand == null) {
            throw new IllegalArgumentException("operand must not be null");
        }
        values = List.copyOf(values);
    }

    @Override
    public String toString() {
        String joined = values.stream().map(Object::toString).collect(Collectors.joining(", "));
        return operand + (negated ? " NOT IN (" : " IN (") + joined + ")";
    }
}
