package io.github.sqlparser;

import io.github.sqlparser.parser.ParseException;
import io.github.sqlparser.parser.SqlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErrorTest {

    @Test
    void missingTableAfterFrom() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM"));
    }

    @Test
    void trailingCommaInSelectList() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT a, FROM t"));
    }

    @Test
    void unbalancedParenthesis() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM (SELECT id FROM A"));
    }

    @Test
    void missingJoinCondition() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM a LEFT JOIN b"));
    }

    @Test
    void unexpectedCharacter() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM book #"));
    }

    @Test
    void unterminatedStringLiteral() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT 'abc"));
    }

    @Test
    void unterminatedBlockComment() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT /* oops * FROM t"));
    }

    @Test
    void missingSelectKeyword() {
        assertThrows(ParseException.class, () -> SqlParser.parse("FROM book"));
    }

    @Test
    void errorCarriesAccuratePosition() {
        ParseException error = assertThrows(ParseException.class,
                () -> SqlParser.parse("SELECT name, FROM book"));
        assertEquals(1, error.line());
        assertEquals(14, error.column());
    }

    @Test
    void duplicateLimitClause() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM t LIMIT 1 LIMIT 2"));
    }

    @Test
    void duplicateOffsetClause() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM t OFFSET 1 OFFSET 2"));
    }

    @Test
    void outerAfterInnerJoin() {
        assertThrows(ParseException.class,
                () -> SqlParser.parse("SELECT * FROM a INNER OUTER JOIN b ON a.id = b.id"));
    }

    @Test
    void emptyInList() {
        assertThrows(ParseException.class, () -> SqlParser.parse("SELECT * FROM t WHERE a IN ()"));
    }
}
