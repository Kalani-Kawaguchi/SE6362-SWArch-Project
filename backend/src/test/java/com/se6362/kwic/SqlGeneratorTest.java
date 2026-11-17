package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.search.QueryTree;
import com.se6362.search.SearchSqlGenerator;

@ExtendWith(MockitoExtension.class)
public class SqlGeneratorTest
{
        private SearchSqlGenerator sqlGenerator;

        @BeforeEach
        void setUp()
        {
                sqlGenerator = new SearchSqlGenerator(false);
        }

        // A single keyword should generate one EXISTS clause and one keyword parameter.
        @Test
        void testSingleKeyword()
        {
                QueryTree tree = new QueryTree("apple");

                String sql = sqlGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?);",
                                sql);

                assertEquals(List.of("apple"), sqlGenerator.getKeywords());
        }

        // AND should generate two EXISTS predicates joined by AND inside parentheses.
        @Test
        void testAndExpression()
        {
                QueryTree tree = new QueryTree("apple AND banana");

                String sql = sqlGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?) "
                                                +
                                                "AND EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?));",
                                sql);

                assertEquals(List.of("apple", "banana"), sqlGenerator.getKeywords());
        }

        // OR should generate two EXISTS predicates joined by OR inside parentheses.
        @Test
        void testOrExpression()
        {
                QueryTree tree = new QueryTree("apple OR banana");

                String sql = sqlGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?) "
                                                +
                                                "OR EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?));",
                                sql);

                assertEquals(List.of("apple", "banana"), sqlGenerator.getKeywords());
        }

        // NOT should wrap the inner predicate in NOT (...).
        @Test
        void testNotExpression()
        {
                QueryTree tree = new QueryTree("NOT apple");

                String sql = sqlGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE NOT (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?));",
                                sql);

                assertEquals(List.of("apple"), sqlGenerator.getKeywords());
        }

        // NOT should work cleanly as an operand of a binary expression.
        @Test
        void testNotWithAnd()
        {
                QueryTree tree = new QueryTree("apple AND NOT banana");

                String sql = sqlGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?) "
                                                +
                                                "AND NOT (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?)));",
                                sql);

                assertEquals(List.of("apple", "banana"), sqlGenerator.getKeywords());
        }

        // Nested expressions should preserve tree grouping and parameter order.
        @Test
        void testNestedExpression()
        {
                QueryTree tree = new QueryTree("apple AND (banana OR cherry)");

                String sql = sqlGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?) "
                                                +
                                                "AND (EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?) "
                                                +
                                                "OR EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?)));",
                                sql);

                assertEquals(List.of("apple", "banana", "cherry"), sqlGenerator.getKeywords());
        }

        // A new generation should clear keywords collected for the previous query.
        @Test
        void testKeywordsAreClearedBetweenQueries()
        {
                QueryTree firstTree = new QueryTree("apple AND banana");
                QueryTree secondTree = new QueryTree("cherry");

                sqlGenerator.getSearchSQL(firstTree);
                String secondSql = sqlGenerator.getSearchSQL(secondTree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = ?);",
                                secondSql);

                assertEquals(List.of("cherry"), sqlGenerator.getKeywords());
        }

        // Repeated keywords should have a parameter for each SQL placeholder.
        @Test
        void testRepeatedKeyword()
        {
                QueryTree tree = new QueryTree("apple OR apple");

                sqlGenerator.getSearchSQL(tree);

                assertEquals(List.of("apple", "apple"), sqlGenerator.getKeywords());
        }

        // Case-sensitive mode should apply BINARY to the parameter placeholder and
        // preserve casing.
        @Test
        void testCaseSensitiveKeyword()
        {
                SearchSqlGenerator caseSensitiveGenerator = new SearchSqlGenerator(true);
                QueryTree tree = new QueryTree("Apple");

                String sql = caseSensitiveGenerator.getSearchSQL(tree);

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = BINARY ?);",
                                sql);

                assertEquals(List.of("Apple"), caseSensitiveGenerator.getKeywords());
        }
}
