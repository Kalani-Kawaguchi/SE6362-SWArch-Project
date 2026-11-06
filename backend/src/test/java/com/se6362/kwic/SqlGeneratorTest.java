package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.kwic.core.QueryTree;
import com.se6362.kwic.core.SqlGenerator;

@ExtendWith(MockitoExtension.class)
public class SqlGeneratorTest
{
    private SqlGenerator sqlGenerator;

    @BeforeEach
    void setUp()
    {
        sqlGenerator = new SqlGenerator();
    }

    // A single keyword should generate one placeholder and one keyword parameter.
    @Test
    void testSingleKeyword()
    {
        QueryTree tree = new QueryTree("apple");

        String sql = sqlGenerator.getSearchSQL(tree);

        assertEquals(
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? );",
                sql);

        assertEquals(List.of("apple"), sqlGenerator.getKeywords());
    }

    // AND should generate two subqueries joined by AND.
    @Test
    void testAndExpression()
    {
        QueryTree tree = new QueryTree("apple AND banana");

        String sql = sqlGenerator.getSearchSQL(tree);

        assertEquals(
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) " +
                        "AND page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) );",
                sql);

        assertEquals(List.of("apple", "banana"), sqlGenerator.getKeywords());
    }

    // OR should generate two subqueries joined by OR.
    @Test
    void testOrExpression()
    {
        QueryTree tree = new QueryTree("apple OR banana");

        String sql = sqlGenerator.getSearchSQL(tree);

        assertEquals(
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) " +
                        "OR page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) );",
                sql);

        assertEquals(List.of("apple", "banana"), sqlGenerator.getKeywords());
    }

    // NOT should generate a NOT IN subquery.
    @Test
    void testNotExpression()
    {
        QueryTree tree = new QueryTree("NOT apple");

        String sql = sqlGenerator.getSearchSQL(tree);

        assertEquals(
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id NOT IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) );",
                sql);

        assertEquals(List.of("apple"), sqlGenerator.getKeywords());
    }

    // NOT should work as an operand of a binary expression.
    @Test
    void testNotWithAnd()
    {
        QueryTree tree = new QueryTree("apple AND NOT banana");

        String sql = sqlGenerator.getSearchSQL(tree);

        assertEquals(
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) " +
                        "AND page_id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id NOT IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) ) );",
                sql);

        assertEquals(List.of("apple", "banana"), sqlGenerator.getKeywords());
    }

    // Nested expressions should preserve tree structure and parameter order.
    @Test
    void testNestedExpression()
    {
        QueryTree tree = new QueryTree("apple AND (banana OR cherry)");

        String sql = sqlGenerator.getSearchSQL(tree);

        assertEquals(
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) " +
                        "AND page_id IN (SELECT page_id FROM KWIC_INDEX " +
                        "WHERE page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) " +
                        "OR page_id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? ) ) );",
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
                "SELECT url, title, description " +
                        "FROM WEB_DATA " +
                        "WHERE id IN (SELECT page_id FROM KWIC_INDEX WHERE keyword = ? );",
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
}
