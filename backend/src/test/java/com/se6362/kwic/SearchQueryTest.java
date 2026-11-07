package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.search.BinaryNode;
import com.se6362.search.KeywordNode;
import com.se6362.search.NotNode;
import com.se6362.search.QueryTree;

@ExtendWith(MockitoExtension.class)
public class SearchQueryTest
{
        // A single keyword should produce a KeywordNode.
        @Test
        void testSingleKeyword()
        {
                QueryTree query = new QueryTree("apple");

                assertEquals(new KeywordNode("apple"), query.getRoot());
        }

        // Adjacent keywords should be combined using implicit OR.
        @Test
        void testImplicitOr()
        {
                QueryTree query = new QueryTree("apple banana");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getRoot());
        }

        // An explicit OR should produce the same tree as implicit OR.
        @Test
        void testExplicitOr()
        {
                QueryTree query = new QueryTree("apple OR banana");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getRoot());
        }

        // AND should combine two keywords.
        @Test
        void testAnd()
        {
                QueryTree query = new QueryTree("apple AND banana");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getRoot());
        }

        // AND should have higher precedence than implicit OR.
        @Test
        void testAndPrecedenceWithImplicitOr()
        {
                QueryTree query = new QueryTree("apple banana AND cherry");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new BinaryNode("AND",
                                                                new KeywordNode("banana"),
                                                                new KeywordNode("cherry"))),
                                query.getRoot());
        }

        // AND should have higher precedence than explicit OR.
        @Test
        void testAndPrecedenceWithExplicitOr()
        {
                QueryTree query = new QueryTree("apple OR banana AND cherry");

                assertEquals(
                                new BinaryNode("OR",
                                                new KeywordNode("apple"),
                                                new BinaryNode("AND",
                                                                new KeywordNode("banana"),
                                                                new KeywordNode("cherry"))),
                                query.getRoot());
        }

        // Implicit OR should be evaluated after an AND expression.
        @Test
        void testImplicitOrAfterAnd()
        {
                QueryTree query = new QueryTree("apple AND banana cherry");

                assertEquals(
                                new BinaryNode("OR",
                                                new BinaryNode("AND",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana")),
                                                new KeywordNode("cherry")),
                                query.getRoot());
        }

        // Parentheses should override the default operator precedence.
        @Test
        void testParenthesesOverridePrecedence()
        {
                QueryTree query = new QueryTree("(apple OR banana) AND cherry");

                assertEquals(
                                new BinaryNode("AND",
                                                new BinaryNode("OR",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana")),
                                                new KeywordNode("cherry")),
                                query.getRoot());
        }

        // Nested parentheses should be parsed correctly.
        @Test
        void testNestedParentheses()
        {
                QueryTree query = new QueryTree("apple AND (banana OR (cherry AND date))");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new BinaryNode("OR",
                                                                new KeywordNode("banana"),
                                                                new BinaryNode("AND",
                                                                                new KeywordNode("cherry"),
                                                                                new KeywordNode("date")))),
                                query.getRoot());
        }

        // NOT should apply to the following keyword.
        @Test
        void testNotKeyword()
        {
                QueryTree query = new QueryTree("NOT apple");

                assertEquals(
                                new NotNode(new KeywordNode("apple")),
                                query.getRoot());
        }

        // NOT should apply to a parenthesized expression.
        @Test
        void testNotParenthesizedExpression()
        {
                QueryTree query = new QueryTree("NOT (apple OR banana)");

                assertEquals(
                                new NotNode(
                                                new BinaryNode("OR",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana"))),
                                query.getRoot());
        }

        // NOT should be supported after an operator.
        @Test
        void testNotAfterAnd()
        {
                QueryTree query = new QueryTree("apple AND NOT banana");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new NotNode(new KeywordNode("banana"))),
                                query.getRoot());
        }

        // Multiple operators should be grouped from left to right at the same
        // precedence.
        @Test
        void testLeftAssociativity()
        {
                QueryTree query = new QueryTree("apple AND banana AND cherry");

                assertEquals(
                                new BinaryNode("AND",
                                                new BinaryNode("AND",
                                                                new KeywordNode("apple"),
                                                                new KeywordNode("banana")),
                                                new KeywordNode("cherry")),
                                query.getRoot());
        }

        // Keywords may contain letters and digits.
        @Test
        void testKeywordWithDigits()
        {
                QueryTree query = new QueryTree("apple123 AND banana2");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple123"),
                                                new KeywordNode("banana2")),
                                query.getRoot());
        }

        // Whitespace between tokens should be ignored.
        @Test
        void testExtraWhitespace()
        {
                QueryTree query = new QueryTree("  apple   AND \n banana  ");

                assertEquals(
                                new BinaryNode("AND",
                                                new KeywordNode("apple"),
                                                new KeywordNode("banana")),
                                query.getRoot());
        }

        // A null query should be rejected.
        @Test
        void testNullQuery()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree(null));
        }

        // An empty or whitespace-only query should be rejected.
        @Test
        void testEmptyQuery()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree(""));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("   \n\t"));
        }

        // An operator without a required operand should be rejected.
        @Test
        void testMissingOperand()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple AND"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("OR apple"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple OR"));
        }

        // A missing closing parenthesis should be rejected.
        @Test
        void testMissingClosingParenthesis()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("(apple OR banana"));
        }

        // An unmatched closing parenthesis should be rejected.
        @Test
        void testUnmatchedClosingParenthesis()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple OR banana)"));
        }

        // Empty parentheses should be rejected.
        @Test
        void testEmptyParentheses()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("()"));
        }

        // Consecutive explicit operators should be rejected.
        @Test
        void testConsecutiveOperators()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple OR OR banana"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple AND OR banana"));
        }

        // Characters outside the allowed keyword character set should be rejected.
        @Test
        void testIllegalCharacter()
        {
                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple-banana"));

                assertThrows(IllegalArgumentException.class,
                                () -> new QueryTree("apple&banana"));
        }
}
