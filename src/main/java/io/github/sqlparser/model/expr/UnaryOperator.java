package io.github.sqlparser.model.expr;

/**
 * Unary operators supported by the parser.
 */
public enum UnaryOperator {
    NOT("NOT"),
    NEGATIVE("-");

    private final String symbol;

    UnaryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
