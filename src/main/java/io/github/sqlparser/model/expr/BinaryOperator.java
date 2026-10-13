package io.github.sqlparser.model.expr;

/**
 * Binary operators supported by the parser: logical (AND, OR), comparison
 * (=, &lt;&gt;, &lt;, ...) and arithmetic (+ - * / %).
 */
public enum BinaryOperator {
    OR("OR"),
    AND("AND"),
    EQ("="),
    NEQ("<>"),
    LT("<"),
    LTE("<="),
    GT(">"),
    GTE(">="),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%");

    private final String symbol;

    BinaryOperator(String symbol) {
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
