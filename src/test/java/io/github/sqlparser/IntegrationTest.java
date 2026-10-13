package io.github.sqlparser;

import io.github.sqlparser.model.Join;
import io.github.sqlparser.model.JoinType;
import io.github.sqlparser.model.Query;
import io.github.sqlparser.model.SelectItem;
import io.github.sqlparser.model.SubquerySource;
import io.github.sqlparser.model.TableSource;
import io.github.sqlparser.model.expr.AsteriskExpression;
import io.github.sqlparser.model.expr.BetweenExpression;
import io.github.sqlparser.model.expr.BinaryExpression;
import io.github.sqlparser.model.expr.BinaryOperator;
import io.github.sqlparser.model.expr.ColumnExpression;
import io.github.sqlparser.model.expr.Expression;
import io.github.sqlparser.model.expr.FunctionExpression;
import io.github.sqlparser.model.expr.IsNullExpression;
import io.github.sqlparser.model.expr.LikeExpression;
import io.github.sqlparser.model.expr.LiteralExpression;
import io.github.sqlparser.model.expr.ParenthesizedExpression;
import io.github.sqlparser.model.expr.UnaryExpression;
import io.github.sqlparser.model.expr.UnaryOperator;
import io.github.sqlparser.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * End-to-end tests that parse full queries and compare the resulting AST with a
 * programmatically built {@link Query}. Relies on {@code equals} implemented for
 * every AST node.
 */
class IntegrationTest {

    private static ColumnExpression col(String name) {
        return new ColumnExpression(null, name);
    }

    @Test
    void parsesAssignmentAggregateQuery() {
        String sql = "SELECT author.name, count(book.id), sum(book.cost) "
                + "FROM author "
                + "LEFT JOIN book ON (author.id = book.author_id) "
                + "GROUP BY author.name "
                + "HAVING COUNT(*) > 1 AND SUM(book.cost) > 500 "
                + "LIMIT 10;";

        Query expected = Query.builder()
                .addColumn(new SelectItem(new ColumnExpression("author", "name")))
                .addColumn(new SelectItem(new FunctionExpression("count",
                        List.of(new ColumnExpression("book", "id")), false)))
                .addColumn(new SelectItem(new FunctionExpression("sum",
                        List.of(new ColumnExpression("book", "cost")), false)))
                .addFromSource(new TableSource("author"))
                .addJoin(new Join(JoinType.LEFT, new TableSource("book"),
                        new ParenthesizedExpression(new BinaryExpression(
                                new ColumnExpression("author", "id"),
                                BinaryOperator.EQ,
                                new ColumnExpression("book", "author_id")))))
                .addGroupByColumn(new ColumnExpression("author", "name"))
                .addHavingClause(new BinaryExpression(
                        new BinaryExpression(
                                new FunctionExpression("COUNT", List.of(new AsteriskExpression()), false),
                                BinaryOperator.GT,
                                LiteralExpression.number("1")),
                        BinaryOperator.AND,
                        new BinaryExpression(
                                new FunctionExpression("SUM",
                                        List.of(new ColumnExpression("book", "cost")), false),
                                BinaryOperator.GT,
                                LiteralExpression.number("500"))))
                .limit(10)
                .build();

        assertEquals(expected, SqlParser.parse(sql));
    }

    @Test
    void parsesSubqueryInFrom() {
        String sql = "SELECT * FROM (SELECT id, name FROM A) a_alias";

        Query inner = Query.builder()
                .addColumn(new SelectItem(new ColumnExpression(null, "id")))
                .addColumn(new SelectItem(new ColumnExpression(null, "name")))
                .addFromSource(new TableSource("A"))
                .build();

        Query expected = Query.builder()
                .addColumn(new SelectItem(new AsteriskExpression()))
                .addFromSource(new SubquerySource(inner, "a_alias"))
                .build();

        assertEquals(expected, SqlParser.parse(sql));
    }

    @Test
    void parsesImplicitJoinWithComplexWhere() {
        String sql = "SELECT u.name, o.total FROM users u, orders o "
                + "WHERE u.id = o.user_id AND o.total > 100 AND o.status <> 'cancelled'";

        Query expected = Query.builder()
                .addColumn(new SelectItem(new ColumnExpression("u", "name")))
                .addColumn(new SelectItem(new ColumnExpression("o", "total")))
                .addFromSource(new TableSource("users", "u"))
                .addFromSource(new TableSource("orders", "o"))
                .addWhereClause(new BinaryExpression(
                        new BinaryExpression(
                                new BinaryExpression(
                                        new ColumnExpression("u", "id"),
                                        BinaryOperator.EQ,
                                        new ColumnExpression("o", "user_id")),
                                BinaryOperator.AND,
                                new BinaryExpression(
                                        new ColumnExpression("o", "total"),
                                        BinaryOperator.GT,
                                        LiteralExpression.number("100"))),
                        BinaryOperator.AND,
                        new BinaryExpression(
                                new ColumnExpression("o", "status"),
                                BinaryOperator.NEQ,
                                LiteralExpression.string("cancelled"))))
                .build();

        assertEquals(expected, SqlParser.parse(sql));
    }

    @Test
    void parsesArithmeticAndUnaryMinus() {
        Query query = SqlParser.parse("SELECT -a + b * c FROM t");
        Expression expression = query.columns().get(0).expression();
        Expression expected = new BinaryExpression(
                new UnaryExpression(UnaryOperator.NEGATIVE, col("a")),
                BinaryOperator.PLUS,
                new BinaryExpression(col("b"), BinaryOperator.MULTIPLY, col("c")));
        assertEquals(expected, expression);
    }

    @Test
    void parsesNegativePredicatesAndIsNull() {
        String sql = "SELECT * FROM t WHERE a NOT BETWEEN 1 AND 5 AND b NOT LIKE 'x%' AND c IS NULL";
        Query query = SqlParser.parse(sql);

        Expression notBetween = new BetweenExpression(col("a"), LiteralExpression.number("1"),
                LiteralExpression.number("5"), true);
        Expression notLike = new LikeExpression(col("b"), LiteralExpression.string("x%"), true);
        Expression isNull = new IsNullExpression(col("c"), false);
        Expression expected = new BinaryExpression(
                new BinaryExpression(notBetween, BinaryOperator.AND, notLike),
                BinaryOperator.AND,
                isNull);

        assertEquals(expected, query.whereClauses().get(0));
    }

    @Test
    void parsesBooleanLiterals() {
        Query query = SqlParser.parse("SELECT * FROM t WHERE a = TRUE AND b = FALSE AND c IS NULL");
        Expression expected = new BinaryExpression(
                new BinaryExpression(
                        new BinaryExpression(col("a"), BinaryOperator.EQ, LiteralExpression.keyword("TRUE")),
                        BinaryOperator.AND,
                        new BinaryExpression(col("b"), BinaryOperator.EQ, LiteralExpression.keyword("FALSE"))),
                BinaryOperator.AND,
                new IsNullExpression(col("c"), false));
        assertEquals(expected, query.whereClauses().get(0));
    }

    @Test
    void parsesCountDistinct() {
        Query query = SqlParser.parse("SELECT count(DISTINCT x) FROM t");
        SelectItem expected = new SelectItem(
                new FunctionExpression("count", List.of(col("x")), true));
        assertEquals(expected, query.columns().get(0));
    }

    @Test
    void parsesNestedSubquery() {
        String sql = "SELECT * FROM (SELECT * FROM (SELECT id FROM x) y) z";

        Query innermost = Query.builder()
                .addColumn(new SelectItem(col("id")))
                .addFromSource(new TableSource("x"))
                .build();

        Query middle = Query.builder()
                .addColumn(new SelectItem(new AsteriskExpression()))
                .addFromSource(new SubquerySource(innermost, "y"))
                .build();

        Query expected = Query.builder()
                .addColumn(new SelectItem(new AsteriskExpression()))
                .addFromSource(new SubquerySource(middle, "z"))
                .build();

        assertEquals(expected, SqlParser.parse(sql));
    }

    @Test
    void parsesOffsetWithoutLimit() {
        Query query = SqlParser.parse("SELECT * FROM t OFFSET 3");
        assertNull(query.limit());
        assertEquals(3, query.offset());
    }

    @Test
    void parsesRightAndFullJoinChain() {
        String sql = "SELECT * FROM a RIGHT JOIN b ON a.id = b.id FULL JOIN c ON b.id = c.id";
        Query query = SqlParser.parse(sql);

        Join right = new Join(JoinType.RIGHT, new TableSource("b"),
                new BinaryExpression(new ColumnExpression("a", "id"), BinaryOperator.EQ,
                        new ColumnExpression("b", "id")));
        Join full = new Join(JoinType.FULL, new TableSource("c"),
                new BinaryExpression(new ColumnExpression("b", "id"), BinaryOperator.EQ,
                        new ColumnExpression("c", "id")));

        assertEquals(List.of(right, full), query.joins());
    }
}
