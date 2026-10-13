package io.github.sqlparser.model.expr;

import java.util.Locale;

/**
 * A literal value: a number, a quoted string, or a keyword literal
 * ({@code NULL}, {@code TRUE}, {@code FALSE}).
 */
public record LiteralExpression(String value, Kind kind) implements Expression {

    public enum Kind {
        NUMBER,
        STRING,
        KEYWORD
    }

    public static LiteralExpression number(String value) {
        return new LiteralExpression(value, Kind.NUMBER);
    }

    public static LiteralExpression string(String value) {
        return new LiteralExpression(value, Kind.STRING);
    }

    public static LiteralExpression keyword(String value) {
        return new LiteralExpression(value.toUpperCase(Locale.ROOT), Kind.KEYWORD);
    }

    @Override
    public String toString() {
        return kind == Kind.STRING ? "'" + value + "'" : value;
    }
}
