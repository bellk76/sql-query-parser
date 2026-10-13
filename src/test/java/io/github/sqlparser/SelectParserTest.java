package io.github.sqlparser;

import io.github.sqlparser.model.Join;
import io.github.sqlparser.model.JoinType;
import io.github.sqlparser.model.Query;
import io.github.sqlparser.model.SelectItem;
import io.github.sqlparser.model.Sort;
import io.github.sqlparser.model.SortDirection;
import io.github.sqlparser.model.TableSource;
import io.github.sqlparser.model.expr.AsteriskExpression;
import io.github.sqlparser.model.expr.BetweenExpression;
import io.github.sqlparser.model.expr.BinaryExpression;
import io.github.sqlparser.model.expr.BinaryOperator;
import io.github.sqlparser.model.expr.ColumnExpression;
import io.github.sqlparser.model.expr.Expression;
import io.github.sqlparser.model.expr.InExpression;
import io.github.sqlparser.model.expr.IsNullExpression;
import io.github.sqlparser.model.expr.LikeExpression;
import io.github.sqlparser.model.expr.LiteralExpression;
import io.github.sqlparser.model.expr.ParenthesizedExpression;
import io.github.sqlparser.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectParserTest {

    private static ColumnExpression col(String name) {
        return new ColumnExpression(null, name);
    }

    private static Join firstJoin(String sql) {
        return SqlParser.parse(sql).joins().get(0);
    }

    @Test
    void parsesSelectAll() {
        Query query = SqlParser.parse("SELECT * FROM book");
        assertFalse(query.distinct());
        assertEquals(1, query.columns().size());
        assertEquals(new AsteriskExpression(), query.columns().get(0).expression());
        assertNull(query.columns().get(0).alias());
        assertEquals(new TableSource("book"), query.fromSources().get(0));
    }

    @Test
    void parsesExplicitColumnsWithAliases() {
        Query query = SqlParser.parse("SELECT author.name AS name, book.id FROM book");
        assertEquals(new SelectItem(new ColumnExpression("author", "name"), "name"), query.columns().get(0));
        assertEquals(new SelectItem(new ColumnExpression("book", "id"), null), query.columns().get(1));
    }

    @Test
    void parsesQualifiedAsterisk() {
        Query query = SqlParser.parse("SELECT t.* FROM t");
        assertEquals(new AsteriskExpression("t"), query.columns().get(0).expression());
    }

    @Test
    void parsesDistinct() {
        assertTrue(SqlParser.parse("SELECT DISTINCT id FROM t").distinct());
    }

    @Test
    void parsesImplicitJoin() {
        Query query = SqlParser.parse("SELECT * FROM A, B, C");
        assertEquals(List.of(new TableSource("A"), new TableSource("B"), new TableSource("C")),
                query.fromSources());
        assertTrue(query.joins().isEmpty());
    }

    @Test
    void parsesAllJoinTypes() {
        assertEquals(JoinType.INNER, firstJoin("SELECT * FROM a JOIN b ON a.id = b.id").type());
        assertEquals(JoinType.INNER, firstJoin("SELECT * FROM a INNER JOIN b ON a.id = b.id").type());
        assertEquals(JoinType.LEFT, firstJoin("SELECT * FROM a LEFT JOIN b ON a.id = b.id").type());
        assertEquals(JoinType.LEFT, firstJoin("SELECT * FROM a LEFT OUTER JOIN b ON a.id = b.id").type());
        assertEquals(JoinType.RIGHT, firstJoin("SELECT * FROM a RIGHT JOIN b ON a.id = b.id").type());
        assertEquals(JoinType.FULL, firstJoin("SELECT * FROM a FULL OUTER JOIN b ON a.id = b.id").type());
        assertEquals(JoinType.CROSS, firstJoin("SELECT * FROM a CROSS JOIN b").type());
    }

    @Test
    void parsesJoinWithUsing() {
        Query query = SqlParser.parse("SELECT * FROM a JOIN b USING (id, tenant)");
        assertEquals(List.of("id", "tenant"), query.joins().get(0).usingColumns());
    }

    @Test
    void andBindsTighterThanOr() {
        Query query = SqlParser.parse("SELECT * FROM t WHERE a = 1 AND b = 2 OR c = 3");
        Expression and = new BinaryExpression(
                new BinaryExpression(col("a"), BinaryOperator.EQ, LiteralExpression.number("1")),
                BinaryOperator.AND,
                new BinaryExpression(col("b"), BinaryOperator.EQ, LiteralExpression.number("2")));
        Expression expected = new BinaryExpression(and, BinaryOperator.OR,
                new BinaryExpression(col("c"), BinaryOperator.EQ, LiteralExpression.number("3")));
        assertEquals(expected, query.whereClauses().get(0));
    }

    @Test
    void parenthesesOverridePrecedence() {
        Query query = SqlParser.parse("SELECT * FROM t WHERE (a = 1 OR b = 2) AND c = 3");
        BinaryExpression where = assertInstanceOf(BinaryExpression.class, query.whereClauses().get(0));
        assertEquals(BinaryOperator.AND, where.operator());
        assertInstanceOf(ParenthesizedExpression.class, where.left());
    }

    @Test
    void parsesPredicates() {
        Query query = SqlParser.parse("SELECT * FROM t WHERE "
                + "a IN (1, 2, 3) AND b BETWEEN 1 AND 10 AND c LIKE 'x%' AND d IS NOT NULL");
        List<Expression> parts = new ArrayList<>();
        Expression current = query.whereClauses().get(0);
        while (current instanceof BinaryExpression binary && binary.operator() == BinaryOperator.AND) {
            parts.add(binary.right());
            current = binary.left();
        }
        parts.add(current);

        assertTrue(parts.stream().anyMatch(p -> p instanceof InExpression));
        assertTrue(parts.stream().anyMatch(p -> p instanceof BetweenExpression));
        assertTrue(parts.stream().anyMatch(p -> p instanceof LikeExpression));
        assertTrue(parts.stream().anyMatch(p -> p instanceof IsNullExpression));
    }

    @Test
    void parsesNotInWithNegation() {
        Query query = SqlParser.parse("SELECT * FROM t WHERE a NOT IN (1, 2)");
        InExpression in = assertInstanceOf(InExpression.class, query.whereClauses().get(0));
        assertTrue(in.negated());
        assertEquals(2, in.values().size());
    }

    @Test
    void parsesGroupByAndHaving() {
        Query query = SqlParser.parse("SELECT a, count(*) FROM t GROUP BY a, b HAVING count(*) > 1");
        assertEquals(List.of(col("a"), col("b")), query.groupByColumns());
        assertEquals(1, query.havingClauses().size());
    }

    @Test
    void parsesOrderByWithDirections() {
        Query query = SqlParser.parse("SELECT * FROM t ORDER BY a ASC, b DESC, c");
        assertEquals(new Sort(col("a"), SortDirection.ASC), query.sortColumns().get(0));
        assertEquals(new Sort(col("b"), SortDirection.DESC), query.sortColumns().get(1));
        assertEquals(new Sort(col("c"), SortDirection.ASC), query.sortColumns().get(2));
    }

    @Test
    void parsesLimitAndOffsetInAnyOrder() {
        Query both = SqlParser.parse("SELECT * FROM t LIMIT 10 OFFSET 5");
        assertEquals(10, both.limit());
        assertEquals(5, both.offset());

        Query swapped = SqlParser.parse("SELECT * FROM t OFFSET 5 LIMIT 10");
        assertEquals(10, swapped.limit());
        assertEquals(5, swapped.offset());
    }

    @Test
    void limitAndOffsetAreNullWhenAbsent() {
        Query query = SqlParser.parse("SELECT * FROM t");
        assertNull(query.limit());
        assertNull(query.offset());
    }

    @Test
    void toleratesTrailingSemicolon() {
        Query query = SqlParser.parse("SELECT * FROM book;");
        assertEquals(new TableSource("book"), query.fromSources().get(0));
    }

    @Test
    void allowsNonReservedKeywordsAsIdentifiers() {
        Query columns = SqlParser.parse("SELECT desc, left, right FROM t");
        assertEquals(new SelectItem(new ColumnExpression(null, "desc")), columns.columns().get(0));
        assertEquals(new SelectItem(new ColumnExpression(null, "left")), columns.columns().get(1));
        assertEquals(new SelectItem(new ColumnExpression(null, "right")), columns.columns().get(2));

        Query table = SqlParser.parse("SELECT * FROM inner");
        assertEquals(new TableSource("inner"), table.fromSources().get(0));

        Query alias = SqlParser.parse("SELECT a AS left FROM t");
        assertEquals("left", alias.columns().get(0).alias());
    }
}
