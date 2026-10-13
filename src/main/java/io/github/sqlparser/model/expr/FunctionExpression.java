package io.github.sqlparser.model.expr;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A function call such as {@code count(book.id)} or {@code sum(book.cost)}.
 * Aggregate functions (COUNT, SUM, MIN, MAX, AVG) are just ordinary
 * {@code FunctionExpression}s.
 */
public record FunctionExpression(String name, List<Expression> arguments, boolean distinct) implements Expression {

    public FunctionExpression {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("function name must not be empty");
        }
        arguments = List.copyOf(arguments);
    }

    public FunctionExpression(String name, Expression... arguments) {
        this(name, List.of(arguments), false);
    }

    @Override
    public String toString() {
        String args = arguments.stream().map(Object::toString).collect(Collectors.joining(", "));
        return name + "(" + (distinct ? "DISTINCT " : "") + args + ")";
    }
}
