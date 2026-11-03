package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.kwic.core.SearchQuery;
import com.se6362.kwic.core.SearchQuery.BinaryNode;
import com.se6362.kwic.core.SearchQuery.KeywordNode;
import com.se6362.kwic.core.SearchQuery.NotNode;

@ExtendWith(MockitoExtension.class)
public class SearchQueryTest
{
    // A single keyword should produce a KeywordNode.
    @Test
    void testSingleKeyword()
    {
        SearchQuery query = new SearchQuery("apple");

        assertEquals(new KeywordNode("apple"), query.getParseTree());
    }

    // Adjacent keywords should be combined using implicit OR.
    @Test
    void testImplicitOr()
    {
        SearchQuery query = new SearchQuery("apple banana");

        assertEquals(
                new BinaryNode("OR",
                        new KeywordNode("apple"),
                        new KeywordNode("banana")),
                query.getParseTree());
    }

    // An explicit OR should produce the same tree as implicit OR.
    @Test
    void testExplicitOr()
    {
        SearchQuery query = new SearchQuery("apple OR banana");

        assertEquals(
                new BinaryNode("OR",
                        new KeywordNode("apple"),
                        new KeywordNode("banana")),
                query.getParseTree());
    }

    // AND should combine two keywords.
    @Test
    void testAnd()
    {
        SearchQuery query = new SearchQuery("apple AND banana");

        assertEquals(
                new BinaryNode("AND",
                        new KeywordNode("apple"),
                        new KeywordNode("banana")),
                query.getParseTree());
    }

    // AND should have higher precedence than implicit OR.
    @Test
    void testAndPrecedenceWithImplicitOr()
    {
        SearchQuery query = new SearchQuery("apple banana AND cherry");

        assertEquals(
                new BinaryNode("OR",
                        new KeywordNode("apple"),
                        new BinaryNode("AND",
                                new KeywordNode("banana"),
                                new KeywordNode("cherry"))),
                query.getParseTree());
    }

    // AND should have higher precedence than explicit OR.
    @Test
    void testAndPrecedenceWithExplicitOr()
    {
        SearchQuery query = new SearchQuery("apple OR banana AND cherry");

        assertEquals(
                new BinaryNode("OR",
                        new KeywordNode("apple"),
                        new BinaryNode("AND",
                                new KeywordNode("banana"),
                                new KeywordNode("cherry"))),
                query.getParseTree());
    }

    // Implicit OR should be evaluated after an AND expression.
    @Test
    void testImplicitOrAfterAnd()
    {
        SearchQuery query = new SearchQuery("apple AND banana cherry");

        assertEquals(
                new BinaryNode("OR",
                        new BinaryNode("AND",
                                new KeywordNode("apple"),
                                new KeywordNode("banana")),
                        new KeywordNode("cherry")),
                query.getParseTree());
    }

    // Parentheses should override the default operator precedence.
    @Test
    void testParenthesesOverridePrecedence()
    {
        SearchQuery query = new SearchQuery("(apple OR banana) AND cherry");

        assertEquals(
                new BinaryNode("AND",
                        new BinaryNode("OR",
                                new KeywordNode("apple"),
                                new KeywordNode("banana")),
                        new KeywordNode("cherry")),
                query.getParseTree());
    }

    // Nested parentheses should be parsed correctly.
    @Test
    void testNestedParentheses()
    {
        SearchQuery query = new SearchQuery("apple AND (banana OR (cherry AND date))");

        assertEquals(
                new BinaryNode("AND",
                        new KeywordNode("apple"),
                        new BinaryNode("OR",
                                new KeywordNode("banana"),
                                new BinaryNode("AND",
                                        new KeywordNode("cherry"),
                                        new KeywordNode("date")))),
                query.getParseTree());
    }

    // NOT should apply to the following keyword.
    @Test
    void testNotKeyword()
    {
        SearchQuery query = new SearchQuery("NOT apple");

        assertEquals(
                new NotNode(new KeywordNode("apple")),
                query.getParseTree());
    }

    // NOT should apply to a parenthesized expression.
    @Test
    void testNotParenthesizedExpression()
    {
        SearchQuery query = new SearchQuery("NOT (apple OR banana)");

        assertEquals(
                new NotNode(
                        new BinaryNode("OR",
                                new KeywordNode("apple"),
                                new KeywordNode("banana"))),
                query.getParseTree());
    }

    // NOT should be supported after an operator.
    @Test
    void testNotAfterAnd()
    {
        SearchQuery query = new SearchQuery("apple AND NOT banana");

        assertEquals(
                new BinaryNode("AND",
                        new KeywordNode("apple"),
                        new NotNode(new KeywordNode("banana"))),
                query.getParseTree());
    }

    // Multiple operators should be grouped from left to right at the same
    // precedence.
    @Test
    void testLeftAssociativity()
    {
        SearchQuery query = new SearchQuery("apple AND banana AND cherry");

        assertEquals(
                new BinaryNode("AND",
                        new BinaryNode("AND",
                                new KeywordNode("apple"),
                                new KeywordNode("banana")),
                        new KeywordNode("cherry")),
                query.getParseTree());
    }

    // Keywords may contain letters and digits.
    @Test
    void testKeywordWithDigits()
    {
        SearchQuery query = new SearchQuery("apple123 AND banana2");

        assertEquals(
                new BinaryNode("AND",
                        new KeywordNode("apple123"),
                        new KeywordNode("banana2")),
                query.getParseTree());
    }

    // Whitespace between tokens should be ignored.
    @Test
    void testExtraWhitespace()
    {
        SearchQuery query = new SearchQuery("  apple   AND \n banana  ");

        assertEquals(
                new BinaryNode("AND",
                        new KeywordNode("apple"),
                        new KeywordNode("banana")),
                query.getParseTree());
    }

    // A null query should be rejected.
    @Test
    void testNullQuery()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery(null));
    }

    // An empty or whitespace-only query should be rejected.
    @Test
    void testEmptyQuery()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery(""));

        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("   \n\t"));
    }

    // An operator without a required operand should be rejected.
    @Test
    void testMissingOperand()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple AND"));

        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("OR apple"));

        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple OR"));
    }

    // A missing closing parenthesis should be rejected.
    @Test
    void testMissingClosingParenthesis()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("(apple OR banana"));
    }

    // An unmatched closing parenthesis should be rejected.
    @Test
    void testUnmatchedClosingParenthesis()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple OR banana)"));
    }

    // Empty parentheses should be rejected.
    @Test
    void testEmptyParentheses()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("()"));
    }

    // Consecutive explicit operators should be rejected.
    @Test
    void testConsecutiveOperators()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple OR OR banana"));

        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple AND OR banana"));
    }

    // Characters outside the allowed keyword character set should be rejected.
    @Test
    void testIllegalCharacter()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple-banana"));

        assertThrows(IllegalArgumentException.class,
                () -> new SearchQuery("apple&banana"));
    }
}
