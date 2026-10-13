package io.github.sqlparser.lexer;

/**
 * Lexical token categories produced by the {@link Lexer}.
 */
public enum TokenType {
    /** A reserved SQL keyword such as {@code SELECT} or {@code WHERE}. */
    KEYWORD,
    /** An unquoted or quoted identifier (table/column/alias name). */
    IDENTIFIER,
    /** A numeric literal, integer or decimal. */
    NUMBER,
    /** A single-quoted string literal. */
    STRING,
    /** The {@code *} character (wildcard or multiplication). */
    ASTERISK,
    /** The {@code ,} character. */
    COMMA,
    /** The {@code .} character. */
    DOT,
    /** The {@code (} character. */
    LPAREN,
    /** The {@code )} character. */
    RPAREN,
    /** The statement terminator {@code ;}. */
    SEMICOLON,
    /** A comparison or arithmetic operator. */
    OPERATOR,
    /** End of input. */
    EOF
}
