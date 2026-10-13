package io.github.sqlparser.model.expr;

/**
 * Marker interface for any SQL expression node: select items, WHERE/HAVING/ON
 * predicates and function arguments.
 *
 * <p>Sealed so that the fixed set of expression nodes is exhaustive and new
 * implementations cannot be introduced accidentally.
 */
public sealed interface Expression
        permits ColumnExpression, AsteriskExpression, LiteralExpression, BinaryExpression,
                UnaryExpression, FunctionExpression, InExpression, BetweenExpression,
                IsNullExpression, LikeExpression, ParenthesizedExpression {
}
