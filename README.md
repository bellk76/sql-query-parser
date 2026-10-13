# SQL Query Parser

A hand-written parser for arbitrary SQL `SELECT` queries in **Java 21**, producing an
object model (AST) of the query. The lexer and the parser are implemented from scratch
(recursive descent) with **no third-party parsing libraries** - the only runtime
dependency is the JDK.

## Features

Supported constructs:

- **Select list** - explicit columns with aliases (`a.name AS n`), expressions,
  aggregate/other function calls (`count(book.id)`), or `*` (including qualified `t.*`)
- **`DISTINCT`**
- **Implicit joins** - comma-separated sources (`FROM A, B, C`)
- **Explicit joins** - `[INNER] JOIN`, `LEFT [OUTER] JOIN`, `RIGHT [OUTER] JOIN`,
  `FULL [OUTER] JOIN` with `ON <expr>` or `USING (col, ...)`, plus `CROSS JOIN`
- **Subqueries in `FROM`** - derived tables (`FROM (SELECT ...) alias`)
- **Filtering** - `WHERE` with `AND` / `OR` / `NOT`, parentheses, comparisons
  (`= <> != < <= > >=`), arithmetic (`+ - * / %`), `IN`, `BETWEEN`, `LIKE`,
  `IS [NOT] NULL`, and their negated forms
- **Grouping** - `GROUP BY` (one or more expressions) and `HAVING`
- **Sorting** - `ORDER BY ... [ASC | DESC]`
- **Pagination** - `LIMIT` and `OFFSET` (in either order); trailing `;` is accepted

Deliberately **out of scope**: `UNION`, CTE (`WITH`), window functions.

## Architecture

The pipeline is a classic three-stage design; each stage has its own package:

```
SQL text --> Lexer --> Parser --> AST (model)
```

| Package | Responsibility | Key types |
| --- | --- | --- |
| `io.github.sqlparser.lexer` | Tokenizer: turns SQL text into a token stream; skips whitespace/comments; case-insensitive keywords; tracks `line`/`column`/`offset` | `Lexer`, `Token`, `TokenType`, `Keyword` |
| `io.github.sqlparser.parser` | Recursive-descent parser; reports errors with position | `SqlParser`, `ParseException` |
| `io.github.sqlparser.model` | Immutable AST for the query structure | `Query`, `SelectItem`, `Source`/`TableSource`/`SubquerySource`, `Join`/`JoinType`, `Sort`/`SortDirection` |
| `io.github.sqlparser.model.expr` | Immutable AST for expressions | `Expression` + `ColumnExpression`, `AsteriskExpression`, `LiteralExpression`, `BinaryExpression`/`UnaryExpression`, `FunctionExpression`, `InExpression`, `BetweenExpression`, `LikeExpression`, `IsNullExpression`, `ParenthesizedExpression` |

Notes:

- All AST nodes are immutable (`record`s where possible) and implement
  `equals`/`hashCode`, so trees can be compared directly in tests.
- Expression precedence is encoded in the tree shape
  (`OR < AND < NOT < comparison/IN/LIKE/BETWEEN < + - < * / %`), with explicit
  `ParenthesizedExpression` nodes preserving user parentheses.
- Keywords are matched case-insensitively (`Keyword.from` upper-cases the lexeme);
  function names such as `count`/`sum` are ordinary identifiers, not keywords.
- Reserved keywords (e.g. `SELECT`, `FROM`, `WHERE`, `AND`, `NULL`) cannot be used as
  bare identifiers; quote them to do so. Non-reserved words such as `desc`, `left`,
  `right`, `inner` are lexed as identifiers and can name columns/tables (e.g.
  `SELECT desc FROM t`), while still matching their keyword meaning in context
  (`LEFT JOIN`, `ORDER BY ... DESC`).
- Single quotes denote string literals; double quotes and backticks denote quoted
  identifiers (ANSI SQL behaviour).
- Strictness: duplicate `LIMIT`/`OFFSET`, `OUTER` after `INNER`/`CROSS`, and an empty
  `IN ()` list are rejected with a `ParseException`.

## Quick start

Requires a JDK 21 (the build uses a Gradle toolchain). The Gradle wrapper downloads
Gradle on first run.

Windows (`cmd` / PowerShell):

```bat
gradlew.bat build
gradlew.bat test
gradlew.bat run
```

Unix / macOS:

```bash
./gradlew build
./gradlew test
./gradlew run
```

`gradlew run` executes the demo class `io.github.sqlparser.Main`, which parses several
representative queries and prints the resulting AST, then shows how a `ParseException`
reports the position of a syntax error.

## Usage

```java
import io.github.sqlparser.model.Query;
import io.github.sqlparser.parser.SqlParser;

Query query = SqlParser.parse(
        "SELECT author.name, count(book.id) " +
        "FROM author LEFT JOIN book ON author.id = book.author_id " +
        "GROUP BY author.name " +
        "HAVING count(*) > 1 " +
        "LIMIT 10");

System.out.println(query.columns());       // [author.name, count(book.id)]
System.out.println(query.joins());         // [LEFT JOIN book ON author.id = book.author_id]
System.out.println(query.limit());         // 10
```

## Testing

```bash
./gradlew test
```

The suite covers the lexer, every supported construct, full-query AST equality, and
error reporting (`ParseException` with an accurate `line`/`column`).

## Project layout

```
src/main/java/io/github/sqlparser/
  Main.java                 demo entry point
  lexer/                    Lexer, Token, TokenType, Keyword
  parser/                   SqlParser, ParseException
  model/                    Query and structural nodes
  model/expr/               expression nodes
src/test/java/io/github/sqlparser/
  LexerTest, SelectParserTest, IntegrationTest, ErrorTest
```
