package io.github.sqlparser.model.expr;

/**
 * The wildcard select item {@code *} or a qualified wildcard {@code table.*}.
 */
public record AsteriskExpression(String qualifier) implements Expression {

    public AsteriskExpression() {
        this((String) null);
    }

    @Override
    public String toString() {
        return qualifier == null ? "*" : qualifier + ".*";
    }
}
