package io.github.sqlparser;

import io.github.sqlparser.parser.ParseException;
import io.github.sqlparser.parser.SqlParser;

import java.util.List;

/**
 * Console demonstration of the SQL query parser.
 *
 * <p>Runs a handful of representative queries through {@link SqlParser} and
 * prints the resulting AST, then shows how {@link ParseException} reports the
 * position of a syntax error.
 */
public final class Main {

    private static final String SEPARATOR = "=".repeat(88);

    private Main() {
    }

    public static void main(String[] args) {
        List<String> validQueries = List.of(
                "SELECT * FROM book",
                "SELECT author.name, count(book.id), sum(book.cost) "
                        + "FROM author "
                        + "LEFT JOIN book ON (author.id = book.author_id) "
                        + "GROUP BY author.name "
                        + "HAVING COUNT(*) > 1 AND SUM(book.cost) > 500 "
                        + "LIMIT 10;",
                "SELECT * FROM (SELECT id, name FROM A) a_alias "
                        + "ORDER BY name DESC LIMIT 5 OFFSET 10",
                "SELECT u.name, o.total FROM users u, orders o "
                        + "WHERE u.id = o.user_id AND o.total > 100 AND o.status <> 'cancelled'"
        );

        int number = 1;
        for (String sql : validQueries) {
            printHeader(number++, "valid", sql);
            System.out.println(SqlParser.parse(sql));
            System.out.println();
        }

        String invalidSql = "SELECT name, FROM book";
        printHeader(number, "invalid", invalidSql);
        try {
            SqlParser.parse(invalidSql);
            System.out.println("(no error raised - unexpected)");
        } catch (ParseException e) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void printHeader(int number, String kind, String sql) {
        System.out.println(SEPARATOR);
        System.out.println("Query " + number + " [" + kind + "]");
        System.out.println("SQL: " + sql);
        System.out.println(SEPARATOR);
    }
}
