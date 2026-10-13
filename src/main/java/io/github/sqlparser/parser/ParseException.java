package io.github.sqlparser.parser;

/**
 * Raised by the {@link io.github.sqlparser.lexer.Lexer Lexer} and
 * {@link SqlParser} when the input cannot be tokenised or parsed. Carries the
 * position (1-based line/column and 0-based offset) of the offending character
 * or token to make error messages actionable.
 */
public class ParseException extends RuntimeException {

    private final int line;
    private final int column;
    private final int offset;

    public ParseException(String message, int line, int column, int offset) {
        super(message + " (line " + line + ", column " + column + ")");
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int offset() {
        return offset;
    }
}
