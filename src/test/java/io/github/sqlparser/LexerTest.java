package io.github.sqlparser;

import io.github.sqlparser.lexer.Keyword;
import io.github.sqlparser.lexer.Lexer;
import io.github.sqlparser.lexer.Token;
import io.github.sqlparser.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    private static List<Token> tokenize(String sql) {
        return new Lexer(sql).tokenize();
    }

    @Test
    void recognizesKeywordsCaseInsensitively() {
        List<Token> tokens = tokenize("select Distinct FROM where");
        assertTrue(tokens.get(0).isKeyword(Keyword.SELECT));
        assertTrue(tokens.get(1).isKeyword(Keyword.DISTINCT));
        assertTrue(tokens.get(2).isKeyword(Keyword.FROM));
        assertTrue(tokens.get(3).isKeyword(Keyword.WHERE));
    }

    @Test
    void recognizesIdentifiersAndQualifiedNames() {
        List<Token> tokens = tokenize("author name book.id");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("author", tokens.get(0).text());
        assertEquals("name", tokens.get(1).text());
        assertEquals(TokenType.DOT, tokens.get(3).type());
        assertEquals("id", tokens.get(4).text());
    }

    @Test
    void recognizesNumbers() {
        List<Token> tokens = tokenize("1 1.5 500 3.14");
        assertEquals("1", tokens.get(0).text());
        assertEquals("1.5", tokens.get(1).text());
        assertEquals("500", tokens.get(2).text());
        assertEquals("3.14", tokens.get(3).text());
        assertTrue(tokens.stream().allMatch(t -> t.type() == TokenType.NUMBER || t.type() == TokenType.EOF));
    }

    @Test
    void parsesStringLiteralsAndEscapedQuotes() {
        List<Token> tokens = tokenize("'abc' 'it''s'");
        assertEquals(TokenType.STRING, tokens.get(0).type());
        assertEquals("abc", tokens.get(0).text());
        assertEquals("it's", tokens.get(1).text());
    }

    @Test
    void parsesQuotedIdentifiers() {
        List<Token> tokens = tokenize("\"Weird Col\" `tbl`");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("Weird Col", tokens.get(0).text());
        assertEquals("tbl", tokens.get(1).text());
    }

    @Test
    void recognizesOperators() {
        List<Token> tokens = tokenize("= <> != < <= > >=");
        List<String> operators = tokens.stream()
                .filter(t -> t.type() == TokenType.OPERATOR)
                .map(Token::text)
                .toList();
        assertEquals(List.of("=", "<>", "!=", "<", "<=", ">", ">="), operators);
    }

    @Test
    void skipsLineComments() {
        List<Token> tokens = tokenize("-- a comment\nSELECT");
        assertTrue(tokens.get(0).isKeyword(Keyword.SELECT));
        assertEquals(2, tokens.get(0).line());
    }

    @Test
    void skipsBlockComments() {
        List<Token> tokens = tokenize("SELECT /* multi\nline */ *");
        assertTrue(tokens.get(0).isKeyword(Keyword.SELECT));
        assertEquals(TokenType.ASTERISK, tokens.get(1).type());
    }

    @Test
    void distinguishesMinusOperatorFromLineComment() {
        List<Token> tokens = tokenize("-1");
        assertEquals(TokenType.OPERATOR, tokens.get(0).type());
        assertEquals("-", tokens.get(0).text());
        assertEquals(TokenType.NUMBER, tokens.get(1).type());
    }

    @Test
    void recognizesSemicolonAndParentheses() {
        List<Token> tokens = tokenize("(a);");
        assertEquals(TokenType.LPAREN, tokens.get(0).type());
        assertEquals(TokenType.RPAREN, tokens.get(2).type());
        assertEquals(TokenType.SEMICOLON, tokens.get(3).type());
    }

    @Test
    void tracksLineAndColumn() {
        List<Token> tokens = tokenize("SELECT\n  book");
        Token book = tokens.get(1);
        assertEquals(2, book.line());
        assertEquals(3, book.column());
    }

    @Test
    void terminatesWithEof() {
        List<Token> tokens = tokenize("");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }
}
