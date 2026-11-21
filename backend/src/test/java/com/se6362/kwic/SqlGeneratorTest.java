package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.search.QueryParser;
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
                QueryParser tree = new QueryParser("apple");

                String sql = sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

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
                QueryParser tree = new QueryParser("apple AND banana");

                String sql = sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

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
                QueryParser tree = new QueryParser("apple OR banana");

                String sql = sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

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
                QueryParser tree = new QueryParser("NOT apple");

                String sql = sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

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
                QueryParser tree = new QueryParser("apple AND NOT banana");

                String sql = sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

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
                QueryParser tree = new QueryParser("apple AND (banana OR cherry)");

                String sql = sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

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
                QueryParser firstTree = new QueryParser("apple AND banana");
                QueryParser secondTree = new QueryParser("cherry");

                sqlGenerator.getSearchSQL(firstTree.getParseTreeRoot());
                String secondSql = sqlGenerator.getSearchSQL(secondTree.getParseTreeRoot());

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
                QueryParser tree = new QueryParser("apple OR apple");

                sqlGenerator.getSearchSQL(tree.getParseTreeRoot());

                assertEquals(List.of("apple", "apple"), sqlGenerator.getKeywords());
        }

        // Case-sensitive mode should apply BINARY to the parameter placeholder and
        // preserve casing.
        @Test
        void testCaseSensitiveKeyword()
        {
                SearchSqlGenerator caseSensitiveGenerator = new SearchSqlGenerator(true);
                QueryParser tree = new QueryParser("Apple");

                String sql = caseSensitiveGenerator.getSearchSQL(tree.getParseTreeRoot());

                assertEquals(
                                "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                                                "FROM WEB_DATA w " +
                                                "WHERE EXISTS (SELECT 1 FROM KWIC_INDEX k WHERE k.page_id = w.id AND k.keyword = BINARY ?);",
                                sql);

                assertEquals(List.of("Apple"), caseSensitiveGenerator.getKeywords());
        }
}
