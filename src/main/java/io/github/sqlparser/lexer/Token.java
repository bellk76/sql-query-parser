package io.github.sqlparser.lexer;

/**
 * A single lexical token.
 *
 * @param type    the token category
 * @param text    the lexeme; for strings and quoted identifiers the surrounding
 *                quotes are stripped
 * @param keyword the resolved keyword when {@code type == KEYWORD}, otherwise {@code null}
 * @param line    1-based line number of the token start
 * @param column  1-based column number of the token start
 * @param offset  0-based character offset of the token start
 */
public record Token(TokenType type, String text, Keyword keyword, int line, int column, int offset) {

    public boolean isKeyword(Keyword kw) {
        if (type == TokenType.KEYWORD) {
            return keyword == kw;
        }
        // Non-reserved keywords are lexed as identifiers; match them by text.
        return kw != null && !kw.reserved() && text.equalsIgnoreCase(kw.name());
    }

    @Override
    public String toString() {
        String kw = keyword == null ? "" : " (" + keyword + ")";
        return String.format("%-10s '%s'%s @%d:%d", type, text, kw, line, column);
    }
}
