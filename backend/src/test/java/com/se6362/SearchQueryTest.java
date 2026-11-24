package com.se6362;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.search.BinaryNode;
import com.se6362.search.KeywordNode;
import com.se6362.search.NotNode;
import com.se6362.search.QueryParser;

@ExtendWith(MockitoExtension.class)
public class SearchQueryTest
{
        // A single keyword should produce a KeywordNode.
        @Test
        void testSingleKeyword()
        {
                QueryParser query = new QueryParser("apple");

                assertEquals(new KeywordNode("apple"), query.getParseTreeRoot());
        }

        // Adjacent keywords should be combined using implicit OR.
        @Test
        void testImplicitOr()
        {
                QueryParser query = new QueryParser("apple banana");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getParseTreeRoot());
        }

        // An explicit OR should produce the same tree as implicit OR.
        @Test
        void testExplicitOr()
        {
                QueryParser query = new QueryParser("apple OR banana");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getParseTreeRoot());
        }

        // AND should combine two keywords.
        @Test
        void testAnd()
        {
                QueryParser query = new QueryParser("apple AND banana");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getParseTreeRoot());
        }

        // AND should have higher precedence than implicit OR.
        @Test
        void testAndPrecedenceWithImplicitOr()
        {
                QueryParser query = new QueryParser("apple banana AND cherry");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new BinaryNode("AND",
                                                                new KeywordNode("banana"),
                                                                new KeywordNode("cherry"))),
                                query.getParseTreeRoot());
        }

        // AND should have higher precedence than explicit OR.
        @Test
        void testAndPrecedenceWithExplicitOr()
        {
                QueryParser query = new QueryParser("apple OR banana AND cherry");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new BinaryNode("AND",
                                                                new KeywordNode("banana"),
                                                                new KeywordNode("cherry"))),
                                query.getParseTreeRoot());
        }

        // Implicit OR should be evaluated after an AND expression.
        @Test
        void testImplicitOrAfterAnd()
        {
                QueryParser query = new QueryParser("apple AND banana cherry");

                assertEquals(
                                new BinaryNode("OR",
                                                new BinaryNode("AND",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana")),
                                                new KeywordNode("cherry")),
                                query.getParseTreeRoot());
        }

        // Parentheses should override the default operator precedence.
        @Test
        void testParenthesesOverridePrecedence()
        {
                QueryParser query = new QueryParser("(apple OR banana) AND cherry");

                assertEquals(
                                new BinaryNode("AND",
                                                new BinaryNode("OR",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana")),
                                                new KeywordNode("cherry")),
                                query.getParseTreeRoot());
        }

        // Nested parentheses should be parsed correctly.
        @Test
        void testNestedParentheses()
        {
                QueryParser query = new QueryParser("apple AND (banana OR (cherry AND date))");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new BinaryNode("OR",
                                                                new KeywordNode("banana"),
                                                                new BinaryNode("AND",
                                                                                new KeywordNode("cherry"),
                                                                                new KeywordNode("date")))),
                                query.getParseTreeRoot());
        }

        // NOT should apply to the following keyword.
        @Test
        void testNotKeyword()
        {
                QueryParser query = new QueryParser("NOT apple");

                assertEquals(
                                new NotNode(new KeywordNode("apple")),
                                query.getParseTreeRoot());
        }

        // NOT should apply to a parenthesized expression.
        @Test
        void testNotParenthesizedExpression()
        {
                QueryParser query = new QueryParser("NOT (apple OR banana)");

                assertEquals(
                                new NotNode(
                                                new BinaryNode("OR",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana"))),
                                query.getParseTreeRoot());
        }

        // NOT should be supported after an operator.
        @Test
        void testNotAfterAnd()
        {
                QueryParser query = new QueryParser("apple AND NOT banana");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new NotNode(new KeywordNode("banana"))),
                                query.getParseTreeRoot());
        }

        // Multiple operators should be grouped from left to right at the same
        // precedence.
        @Test
        void testLeftAssociativity()
        {
                QueryParser query = new QueryParser("apple AND banana AND cherry");

                assertEquals(
                                new BinaryNode("AND",
                                                new BinaryNode("AND",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana")),
                                                new KeywordNode("cherry")),
                                query.getParseTreeRoot());
        }

        // Keywords may contain letters and digits.
        @Test
        void testKeywordWithDigits()
        {
                QueryParser query = new QueryParser("apple123 AND banana2");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple123"),
                                                new KeywordNode("banana2")),
                                query.getParseTreeRoot());
        }

        // Whitespace between tokens should be ignored.
        @Test
        void testExtraWhitespace()
        {
                QueryParser query = new QueryParser("  apple   AND \n banana  ");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getParseTreeRoot());
        }

        // A null query should be rejected.
        @Test
        void testNullQuery()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser(null));
        }

        // An empty or whitespace-only query should be rejected.
        @Test
        void testEmptyQuery()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser(""));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("   \n\t"));
        }

        // An operator without a required operand should be rejected.
        @Test
        void testMissingOperand()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple AND"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("OR apple"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple OR"));
        }

        // A missing closing parenthesis should be rejected.
        @Test
        void testMissingClosingParenthesis()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("(apple OR banana"));
        }

        // An unmatched closing parenthesis should be rejected.
        @Test
        void testUnmatchedClosingParenthesis()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple OR banana)"));
        }

        // Empty parentheses should be rejected.
        @Test
        void testEmptyParentheses()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("()"));
        }

        // Consecutive explicit operators should be rejected.
        @Test
        void testConsecutiveOperators()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple OR OR banana"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple AND OR banana"));
        }

        // Characters outside the allowed keyword character set should be rejected.
        @Test
        void testIllegalCharacter()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple)banana"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryParser("apple(banana"));
        }
}
