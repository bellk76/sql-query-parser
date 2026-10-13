package io.github.sqlparser.lexer;

import io.github.sqlparser.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written tokenizer for SQL {@code SELECT} queries.
 *
 * <p>The lexer is case-insensitive for keywords (see {@link Keyword}) and
 * skips whitespace and comments ({@code -- line} and {@code /* block *}{@code /}).
 * Single quotes denote string literals; double quotes and backticks denote
 * quoted identifiers. Tokens carry their source position so the parser can
 * report precise errors.
 */
public final class Lexer {

    private final String input;
    private int pos;
    private int line = 1;
    private int column = 1;

    public Lexer(String input) {
        this.input = input == null ? "" : input;
    }

    /** Tokenizes the whole input, terminating with an {@link TokenType#EOF} token. */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.type() != TokenType.EOF);
        return tokens;
    }

    /** Reads and returns the next token, or an EOF token at end of input. */
    public Token nextToken() {
        skipWhitespaceAndComments();
        if (pos >= input.length()) {
            return new Token(TokenType.EOF, "", null, line, column, pos);
        }

        char c = input.charAt(pos);

        if (isIdentifierStart(c)) {
            return readIdentifierOrKeyword();
        }
        if (c == '`' || c == '"') {
            return readQuotedIdentifier(c);
        }
        if (c == '\'') {
            return readString();
        }
        if (Character.isDigit(c) || (c == '.' && isDigitAt(pos + 1))) {
            return readNumber();
        }

        return switch (c) {
            case '*' -> readSingle(TokenType.ASTERISK);
            case ',' -> readSingle(TokenType.COMMA);
            case '.' -> readSingle(TokenType.DOT);
            case '(' -> readSingle(TokenType.LPAREN);
            case ')' -> readSingle(TokenType.RPAREN);
            case ';' -> readSingle(TokenType.SEMICOLON);
            case '=', '+', '-', '/', '%' -> readSingle(TokenType.OPERATOR);
            case '<', '>' -> readComparisonOperator();
            case '!' -> readNotEqualOperator();
            default -> throw error("Unexpected character '" + c + "'");
        };
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isWhitespace(c)) {
                advance();
            } else if (c == '-' && charAt(pos + 1) == '-') {
                skipLineComment();
            } else if (c == '/' && charAt(pos + 1) == '*') {
                skipBlockComment();
            } else {
                break;
            }
        }
    }

    private void skipLineComment() {
        while (pos < input.length() && input.charAt(pos) != '\n') {
            advance();
        }
    }

    private void skipBlockComment() {
        advance();
        advance();
        while (pos < input.length()
                && !(input.charAt(pos) == '*' && charAt(pos + 1) == '/')) {
            advance();
        }
        if (pos >= input.length()) {
            throw error("Unterminated block comment");
        }
        advance();
        advance();
    }

    private Token readIdentifierOrKeyword() {
        int startLine = line;
        int startColumn = column;
        int startOffset = pos;
        int start = pos;
        while (pos < input.length() && isIdentifierPart(input.charAt(pos))) {
            advance();
        }
        String text = input.substring(start, pos);
        Keyword keyword = Keyword.from(text);
        if (keyword != null && keyword.reserved()) {
            return new Token(TokenType.KEYWORD, text, keyword, startLine, startColumn, startOffset);
        }
        return new Token(TokenType.IDENTIFIER, text, null, startLine, startColumn, startOffset);
    }

    private Token readQuotedIdentifier(char quote) {
        int startLine = line;
        int startColumn = column;
        int startOffset = pos;
        advance();
        StringBuilder value = new StringBuilder();
        while (true) {
            if (pos >= input.length()) {
                throw error("Unterminated quoted identifier");
            }
            char c = input.charAt(pos);
            if (c == quote) {
                if (charAt(pos + 1) == quote) {
                    value.append(quote);
                    advance();
                    advance();
                } else {
                    advance();
                    break;
                }
            } else {
                value.append(c);
                advance();
            }
        }
        return new Token(TokenType.IDENTIFIER, value.toString(), null, startLine, startColumn, startOffset);
    }

    private Token readString() {
        int startLine = line;
        int startColumn = column;
        int startOffset = pos;
        advance();
        StringBuilder value = new StringBuilder();
        while (true) {
            if (pos >= input.length()) {
                throw error("Unterminated string literal");
            }
            char c = input.charAt(pos);
            if (c == '\'') {
                if (charAt(pos + 1) == '\'') {
                    value.append('\'');
                    advance();
                    advance();
                } else {
                    advance();
                    break;
                }
            } else {
                value.append(c);
                advance();
            }
        }
        return new Token(TokenType.STRING, value.toString(), null, startLine, startColumn, startOffset);
    }

    private Token readNumber() {
        int startLine = line;
        int startColumn = column;
        int startOffset = pos;
        int start = pos;
        while (isDigitAt(pos)) {
            advance();
        }
        if (charAt(pos) == '.' && isDigitAt(pos + 1)) {
            advance();
            while (isDigitAt(pos)) {
                advance();
            }
        }
        return new Token(TokenType.NUMBER, input.substring(start, pos), null, startLine, startColumn, startOffset);
    }

    private Token readComparisonOperator() {
        int startLine = line;
        int startColumn = column;
        int startOffset = pos;
        char c = input.charAt(pos);
        advance();
        StringBuilder text = new StringBuilder().append(c);
        if (charAt(pos) == '=' || (c == '<' && charAt(pos) == '>')) {
            text.append(input.charAt(pos));
            advance();
        }
        return new Token(TokenType.OPERATOR, text.toString(), null, startLine, startColumn, startOffset);
    }

    private Token readNotEqualOperator() {
        int startLine = line;
        int startColumn = column;
        int startOffset = pos;
        advance();
        if (charAt(pos) != '=') {
            throw error("Expected '=' after '!'");
        }
        advance();
        return new Token(TokenType.OPERATOR, "!=", null, startLine, startColumn, startOffset);
    }

    private Token readSingle(TokenType type) {
        char c = input.charAt(pos);
        Token token = new Token(type, String.valueOf(c), null, line, column, pos);
        advance();
        return token;
    }

    private void advance() {
        char c = input.charAt(pos);
        pos++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    private char charAt(int index) {
        return index < input.length() ? input.charAt(index) : '\0';
    }

    private boolean isDigitAt(int index) {
        return index < input.length() && Character.isDigit(input.charAt(index));
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private ParseException error(String message) {
        return new ParseException(message, line, column, pos);
    }
}
