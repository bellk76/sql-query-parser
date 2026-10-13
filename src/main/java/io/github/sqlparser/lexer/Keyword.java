package io.github.sqlparser.lexer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reserved SQL keywords recognised by the lexer. Matching is
 * case-insensitive: the lexeme is upper-cased before lookup, so
 * {@code select}, {@code Select} and {@code SELECT} all map to
 * {@link #SELECT}.
 *
 * <p>Keywords are split into two groups:
 * <ul>
 *   <li><b>Reserved</b> ({@link #reserved()} == true) - always lexed as
 *       {@link TokenType#KEYWORD} and never usable as a bare identifier.</li>
 *   <li><b>Non-reserved</b> ({@link #reserved()} == false) - lexed as an
 *       ordinary {@link TokenType#IDENTIFIER}, so they can name columns or
 *       tables, but the parser still matches them in their keyword position
 *       (e.g. {@code LEFT JOIN}, {@code ORDER BY ... DESC}).</li>
 * </ul>
 */
public enum Keyword {
    SELECT(true), FROM(true), WHERE(true), JOIN(true),
    INNER(false), LEFT(false), RIGHT(false), FULL(false), OUTER(false), CROSS(false),
    ON(true), USING(false),
    GROUP(true), BY(true), HAVING(true), ORDER(true), ASC(false), DESC(false),
    LIMIT(true), OFFSET(true),
    AND(true), OR(true), NOT(true), AS(true), DISTINCT(false),
    IN(true), BETWEEN(true), LIKE(true), IS(true),
    NULL(true), TRUE(true), FALSE(true);

    private static final Map<String, Keyword> LOOKUP = new HashMap<>();

    static {
        for (Keyword keyword : values()) {
            LOOKUP.put(keyword.name(), keyword);
        }
    }

    private final boolean reserved;

    Keyword(boolean reserved) {
        this.reserved = reserved;
    }

    /** Whether this word is reserved (cannot be a bare identifier). */
    public boolean reserved() {
        return reserved;
    }

    /**
     * Resolves a keyword from raw text (case-insensitively).
     *
     * @return the matching keyword, or {@code null} if the text is not a keyword
     */
    public static Keyword from(String text) {
        return LOOKUP.get(text.toUpperCase(Locale.ROOT));
    }
}
