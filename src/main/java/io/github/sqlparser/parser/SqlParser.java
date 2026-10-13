package io.github.sqlparser.parser;

import io.github.sqlparser.lexer.Keyword;
import io.github.sqlparser.lexer.Lexer;
import io.github.sqlparser.lexer.Token;
import io.github.sqlparser.lexer.TokenType;
import io.github.sqlparser.model.Join;
import io.github.sqlparser.model.JoinType;
import io.github.sqlparser.model.Query;
import io.github.sqlparser.model.SelectItem;
import io.github.sqlparser.model.Sort;
import io.github.sqlparser.model.SortDirection;
import io.github.sqlparser.model.Source;
import io.github.sqlparser.model.SubquerySource;
import io.github.sqlparser.model.TableSource;
import io.github.sqlparser.model.expr.AsteriskExpression;
import io.github.sqlparser.model.expr.BetweenExpression;
import io.github.sqlparser.model.expr.BinaryExpression;
import io.github.sqlparser.model.expr.BinaryOperator;
import io.github.sqlparser.model.expr.ColumnExpression;
import io.github.sqlparser.model.expr.Expression;
import io.github.sqlparser.model.expr.FunctionExpression;
import io.github.sqlparser.model.expr.InExpression;
import io.github.sqlparser.model.expr.IsNullExpression;
import io.github.sqlparser.model.expr.LikeExpression;
import io.github.sqlparser.model.expr.LiteralExpression;
import io.github.sqlparser.model.expr.ParenthesizedExpression;
import io.github.sqlparser.model.expr.UnaryExpression;
import io.github.sqlparser.model.expr.UnaryOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A recursive-descent parser for SQL {@code SELECT} queries.
 *
 * <p>Grammar (simplified):
 * <pre>
 * query        := SELECT [DISTINCT] selectList [FROM fromItems]
 *                 [WHERE expr] [GROUP BY exprList] [HAVING expr]
 *                 [ORDER BY sortList] [limitOffset] [';'] EOF
 * fromItems    := source (',' source | join)*
 * join         := [INNER|LEFT|RIGHT|FULL|CROSS] [OUTER] JOIN source (ON expr | USING '(' names ')')
 * </pre>
 *
 * <p>Expression precedence, from lowest to highest:
 * {@code OR < AND < NOT < comparison/IN/LIKE/BETWEEN/IS NULL < + - < * / %} <
 * unary/parenthesised/primary.
 */
public final class SqlParser {

    private final List<Token> tokens;
    private int index;

    public SqlParser(List<Token> tokens) {
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        if (tokens.isEmpty() || tokens.get(tokens.size() - 1).type() != TokenType.EOF) {
            throw new IllegalArgumentException("tokens must be non-empty and terminate with an EOF token");
        }
    }

    /** Lexes and parses the given SQL text in one step. */
    public static Query parse(String sql) {
        return new SqlParser(new Lexer(sql).tokenize()).parseQuery();
    }

    public Query parseQuery() {
        Query query = parseQueryBody();
        match(TokenType.SEMICOLON);
        expect(TokenType.EOF, "end of query");
        return query;
    }

    /**
     * Parses a query body (from {@code SELECT} up to and including LIMIT/OFFSET)
     * without consuming a trailing {@code ;} or requiring end of input. Used for
     * top-level queries and for subqueries, where the body is terminated by ')'.
     */
    private Query parseQueryBody() {
        expectKeyword(Keyword.SELECT);
        boolean distinct = matchKeyword(Keyword.DISTINCT);
        Query.Builder builder = Query.builder().distinct(distinct);

        parseSelectItems(builder);
        if (matchKeyword(Keyword.FROM)) {
            parseFromAndJoins(builder);
        }
        if (matchKeyword(Keyword.WHERE)) {
            builder.addWhereClause(parseExpression());
        }
        if (matchKeyword(Keyword.GROUP)) {
            expectKeyword(Keyword.BY);
            do {
                builder.addGroupByColumn(parseExpression());
            } while (match(TokenType.COMMA));
        }
        if (matchKeyword(Keyword.HAVING)) {
            builder.addHavingClause(parseExpression());
        }
        if (matchKeyword(Keyword.ORDER)) {
            expectKeyword(Keyword.BY);
            do {
                builder.addSortColumn(parseSortItem());
            } while (match(TokenType.COMMA));
        }
        parseLimitOffset(builder);
        return builder.build();
    }

    // ---------------------------------------------------------------- SELECT

    private void parseSelectItems(Query.Builder builder) {
        builder.addColumn(parseSelectItem());
        while (match(TokenType.COMMA)) {
            builder.addColumn(parseSelectItem());
        }
    }

    private SelectItem parseSelectItem() {
        Expression expression = parseSelectExpression();
        return new SelectItem(expression, parseOptionalAlias());
    }

    private Expression parseSelectExpression() {
        if (check(TokenType.ASTERISK)) {
            advance();
            return new AsteriskExpression();
        }
        if (check(TokenType.IDENTIFIER) && peek(1).type() == TokenType.DOT && peek(2).type() == TokenType.ASTERISK) {
            String qualifier = advance().text();
            advance();
            advance();
            return new AsteriskExpression(qualifier);
        }
        return parseExpression();
    }

    // ------------------------------------------------------------------ FROM

    private void parseFromAndJoins(Query.Builder builder) {
        builder.addFromSource(parseSource());
        while (true) {
            if (match(TokenType.COMMA)) {
                builder.addFromSource(parseSource());
            } else if (isJoinStart()) {
                builder.addJoin(parseJoin());
            } else {
                break;
            }
        }
    }

    private boolean isJoinStart() {
        return checkKeyword(Keyword.JOIN) || checkKeyword(Keyword.INNER) || checkKeyword(Keyword.LEFT)
                || checkKeyword(Keyword.RIGHT) || checkKeyword(Keyword.FULL) || checkKeyword(Keyword.CROSS);
    }

    private Join parseJoin() {
        JoinType type = JoinType.INNER;
        boolean explicitKind = true;
        if (matchKeyword(Keyword.INNER)) {
            type = JoinType.INNER;
        } else if (matchKeyword(Keyword.LEFT)) {
            type = JoinType.LEFT;
        } else if (matchKeyword(Keyword.RIGHT)) {
            type = JoinType.RIGHT;
        } else if (matchKeyword(Keyword.FULL)) {
            type = JoinType.FULL;
        } else if (matchKeyword(Keyword.CROSS)) {
            type = JoinType.CROSS;
        } else {
            explicitKind = false;
        }

        if (explicitKind) {
            if (type == JoinType.LEFT || type == JoinType.RIGHT || type == JoinType.FULL) {
                matchKeyword(Keyword.OUTER);
            } else if (checkKeyword(Keyword.OUTER)) {
                throw error(peek(), "OUTER is only valid after LEFT, RIGHT or FULL");
            }
            expectKeyword(Keyword.JOIN);
        } else {
            expectKeyword(Keyword.JOIN);
        }

        Source source = parseSource();

        Expression condition = null;
        List<String> usingColumns = List.of();
        if (matchKeyword(Keyword.ON)) {
            condition = parseExpression();
        } else if (matchKeyword(Keyword.USING)) {
            usingColumns = parseColumnNameList();
        } else if (type != JoinType.CROSS) {
            throw error(peek(), "expected ON or USING after JOIN");
        }
        return new Join(type, source, condition, usingColumns);
    }

    private List<String> parseColumnNameList() {
        expect(TokenType.LPAREN, "'(' after USING");
        List<String> columns = new ArrayList<>();
        columns.add(expect(TokenType.IDENTIFIER, "column name").text());
        while (match(TokenType.COMMA)) {
            columns.add(expect(TokenType.IDENTIFIER, "column name").text());
        }
        expect(TokenType.RPAREN, "')' after USING columns");
        return columns;
    }

    private Source parseSource() {
        if (check(TokenType.LPAREN) && peek(1).isKeyword(Keyword.SELECT)) {
            advance();
            Query subquery = parseQueryBody();
            expect(TokenType.RPAREN, "')' to close subquery");
            return new SubquerySource(subquery, parseRequiredAlias("subquery"));
        }

        StringBuilder name = new StringBuilder(expect(TokenType.IDENTIFIER, "table name").text());
        while (check(TokenType.DOT) && peek(1).type() == TokenType.IDENTIFIER) {
            advance();
            name.append('.').append(advance().text());
        }
        return new TableSource(name.toString(), parseOptionalAlias());
    }

    // ------------------------------------------------------------- ORDER BY

    private Sort parseSortItem() {
        Expression expression = parseExpression();
        if (matchKeyword(Keyword.DESC)) {
            return new Sort(expression, SortDirection.DESC);
        }
        if (matchKeyword(Keyword.ASC)) {
            return new Sort(expression, SortDirection.ASC);
        }
        return new Sort(expression, SortDirection.ASC);
    }

    // ---------------------------------------------------------- LIMIT/OFFSET

    private void parseLimitOffset(Query.Builder builder) {
        boolean seenLimit = false;
        boolean seenOffset = false;
        while (true) {
            if (matchKeyword(Keyword.LIMIT)) {
                if (seenLimit) {
                    throw error(peek(), "duplicate LIMIT clause");
                }
                seenLimit = true;
                builder.limit(parseIntegerValue("LIMIT"));
            } else if (matchKeyword(Keyword.OFFSET)) {
                if (seenOffset) {
                    throw error(peek(), "duplicate OFFSET clause");
                }
                seenOffset = true;
                builder.offset(parseIntegerValue("OFFSET"));
            } else {
                break;
            }
        }
    }

    private int parseIntegerValue(String clause) {
        Token token = expect(TokenType.NUMBER, clause + " value");
        try {
            return Integer.parseInt(token.text());
        } catch (NumberFormatException e) {
            throw error(token, clause + " value must be an integer");
        }
    }

    // ------------------------------------------------------------ EXPRESSIONS

    private Expression parseExpression() {
        return parseOr();
    }

    private Expression parseOr() {
        Expression left = parseAnd();
        while (matchKeyword(Keyword.OR)) {
            left = new BinaryExpression(left, BinaryOperator.OR, parseAnd());
        }
        return left;
    }

    private Expression parseAnd() {
        Expression left = parseNot();
        while (matchKeyword(Keyword.AND)) {
            left = new BinaryExpression(left, BinaryOperator.AND, parseNot());
        }
        return left;
    }

    private Expression parseNot() {
        if (matchKeyword(Keyword.NOT)) {
            return new UnaryExpression(UnaryOperator.NOT, parseNot());
        }
        return parseComparison();
    }

    private Expression parseComparison() {
        Expression left = parseAdditive();

        if (matchKeyword(Keyword.IS)) {
            boolean negated = matchKeyword(Keyword.NOT);
            expectKeyword(Keyword.NULL);
            return new IsNullExpression(left, negated);
        }

        boolean negated = false;
        if (checkKeyword(Keyword.NOT) && followsNegatablePredicate()) {
            advance();
            negated = true;
        }

        if (matchKeyword(Keyword.IN)) {
            expect(TokenType.LPAREN, "'(' after IN");
            List<Expression> values = new ArrayList<>();
            if (check(TokenType.RPAREN)) {
                throw error(peek(), "IN list must not be empty");
            }
            values.add(parseExpression());
            while (match(TokenType.COMMA)) {
                values.add(parseExpression());
            }
            expect(TokenType.RPAREN, "')' after IN list");
            return new InExpression(left, values, negated);
        }
        if (matchKeyword(Keyword.LIKE)) {
            return new LikeExpression(left, parseAdditive(), negated);
        }
        if (matchKeyword(Keyword.BETWEEN)) {
            Expression low = parseAdditive();
            expectKeyword(Keyword.AND);
            Expression high = parseAdditive();
            return new BetweenExpression(left, low, high, negated);
        }
        if (negated) {
            throw error(peek(), "expected IN, LIKE or BETWEEN after NOT");
        }

        BinaryOperator operator = comparisonOperator();
        if (operator != null) {
            advance();
            return new BinaryExpression(left, operator, parseAdditive());
        }
        return left;
    }

    private boolean followsNegatablePredicate() {
        return peek(1).isKeyword(Keyword.IN) || peek(1).isKeyword(Keyword.LIKE) || peek(1).isKeyword(Keyword.BETWEEN);
    }

    private BinaryOperator comparisonOperator() {
        if (!check(TokenType.OPERATOR)) {
            return null;
        }
        return switch (peek().text()) {
            case "=" -> BinaryOperator.EQ;
            case "<>", "!=" -> BinaryOperator.NEQ;
            case "<" -> BinaryOperator.LT;
            case "<=" -> BinaryOperator.LTE;
            case ">" -> BinaryOperator.GT;
            case ">=" -> BinaryOperator.GTE;
            default -> null;
        };
    }

    private Expression parseAdditive() {
        Expression left = parseMultiplicative();
        while (check(TokenType.OPERATOR) && (peek().text().equals("+") || peek().text().equals("-"))) {
            BinaryOperator operator = advance().text().equals("+") ? BinaryOperator.PLUS : BinaryOperator.MINUS;
            left = new BinaryExpression(left, operator, parseMultiplicative());
        }
        return left;
    }

    private Expression parseMultiplicative() {
        Expression left = parseUnary();
        while (isMultiplicativeOperator()) {
            String symbol = advance().text();
            BinaryOperator operator = switch (symbol) {
                case "/" -> BinaryOperator.DIVIDE;
                case "%" -> BinaryOperator.MODULO;
                default -> BinaryOperator.MULTIPLY;
            };
            left = new BinaryExpression(left, operator, parseUnary());
        }
        return left;
    }

    private boolean isMultiplicativeOperator() {
        if (check(TokenType.ASTERISK)) {
            return true;
        }
        return check(TokenType.OPERATOR) && (peek().text().equals("/") || peek().text().equals("%"));
    }

    private Expression parseUnary() {
        if (check(TokenType.OPERATOR) && peek().text().equals("-")) {
            advance();
            return new UnaryExpression(UnaryOperator.NEGATIVE, parseUnary());
        }
        return parsePrimary();
    }

    private Expression parsePrimary() {
        Token token = peek();
        return switch (token.type()) {
            case NUMBER -> {
                advance();
                yield LiteralExpression.number(token.text());
            }
            case STRING -> {
                advance();
                yield LiteralExpression.string(token.text());
            }
            case ASTERISK -> {
                advance();
                yield new AsteriskExpression();
            }
            case LPAREN -> {
                advance();
                Expression inner = parseExpression();
                expect(TokenType.RPAREN, "')'");
                yield new ParenthesizedExpression(inner);
            }
            case KEYWORD -> {
                if (token.keyword() == Keyword.NULL || token.keyword() == Keyword.TRUE
                        || token.keyword() == Keyword.FALSE) {
                    advance();
                    yield LiteralExpression.keyword(token.text());
                }
                throw error(token, "unexpected keyword '" + token.text() + "' in expression");
            }
            case IDENTIFIER -> parseIdentifierExpression();
            default -> throw error(token, "unexpected token '" + token.text() + "' in expression");
        };
    }

    private Expression parseIdentifierExpression() {
        String name = advance().text();
        if (check(TokenType.LPAREN)) {
            return parseFunctionCall(name);
        }
        if (check(TokenType.DOT) && peek(1).type() == TokenType.IDENTIFIER) {
            advance();
            String column = advance().text();
            return new ColumnExpression(name, column);
        }
        return new ColumnExpression(null, name);
    }

    private Expression parseFunctionCall(String name) {
        expect(TokenType.LPAREN, "'(' after function name");
        boolean distinct = matchKeyword(Keyword.DISTINCT);
        List<Expression> arguments = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            arguments.add(parseExpression());
            while (match(TokenType.COMMA)) {
                arguments.add(parseExpression());
            }
        }
        expect(TokenType.RPAREN, "')' after function arguments");
        return new FunctionExpression(name, arguments, distinct);
    }

    // ---------------------------------------------------------------- ALIASES

    private String parseOptionalAlias() {
        if (matchKeyword(Keyword.AS)) {
            return expect(TokenType.IDENTIFIER, "alias").text();
        }
        if (check(TokenType.IDENTIFIER) && Keyword.from(peek().text()) == null) {
            return advance().text();
        }
        return null;
    }

    private String parseRequiredAlias(String what) {
        if (matchKeyword(Keyword.AS)) {
            return expect(TokenType.IDENTIFIER, what + " alias").text();
        }
        if (check(TokenType.IDENTIFIER) && Keyword.from(peek().text()) == null) {
            return advance().text();
        }
        return expect(TokenType.IDENTIFIER, what + " alias").text();
    }

    // ------------------------------------------------------------ TOKEN ACCESS

    private Token peek() {
        return tokens.get(index);
    }

    private Token peek(int ahead) {
        int target = index + ahead;
        return target < tokens.size() ? tokens.get(target) : tokens.get(tokens.size() - 1);
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean checkKeyword(Keyword keyword) {
        return peek().isKeyword(keyword);
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchKeyword(Keyword keyword) {
        if (checkKeyword(keyword)) {
            advance();
            return true;
        }
        return false;
    }

    private Token advance() {
        Token token = peek();
        if (token.type() != TokenType.EOF) {
            index++;
        }
        return token;
    }

    private Token expect(TokenType type, String what) {
        if (!check(type)) {
            throw error(peek(), "expected " + what + " but found " + describe(peek()));
        }
        return advance();
    }

    private void expectKeyword(Keyword keyword) {
        if (!checkKeyword(keyword)) {
            throw error(peek(), "expected keyword " + keyword + " but found " + describe(peek()));
        }
        advance();
    }

    private String describe(Token token) {
        return token.type() == TokenType.EOF ? "end of input" : "'" + token.text() + "'";
    }

    private ParseException error(Token token, String message) {
        return new ParseException(message, token.line(), token.column(), token.offset());
    }
}
