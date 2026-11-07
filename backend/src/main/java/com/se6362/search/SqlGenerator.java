package com.se6362.search;

import java.util.ArrayList;

public class SqlGenerator implements QueryVisitor
{
    private final ArrayList<String> keywords = new ArrayList<>();

    public ArrayList<String> getKeywords()
    {
        return keywords;
    }

    public String getSearchSQL(QueryTree tree)
    {
        keywords.clear();

        return "SELECT url, title, description " +
                "FROM WEB_DATA " +
                "WHERE id IN (" + tree.getRoot().accept(this) + ");";
    }

    public String getSearchSQL(QueryTree tree, SearchSortStrategy sortStrategy)
    {
        keywords.clear();

        return "SELECT url, title, description " +
                "FROM WEB_DATA " +
                "WHERE id IN (" + tree.getRoot().accept(this) + ")" +
                sortStrategy.generateOrderBy();
    }

    @Override
    public String visitKeyword(KeywordNode node)
    {
        keywords.add(node.keyword());

        return "SELECT page_id " +
                "FROM KWIC_INDEX " +
                "WHERE keyword = ?";
    }

    @Override
    public String visitBinary(BinaryNode node)
    {
        return "SELECT page_id " +
                "FROM KWIC_INDEX " +
                "WHERE page_id IN (" + node.left().accept(this) + ") " +
                node.operator() +
                " page_id IN (" + node.right().accept(this) + ")";
    }

    @Override
    public String visitNot(NotNode node)
    {
        return "SELECT page_id " +
                "FROM KWIC_INDEX " +
                "WHERE page_id NOT IN (" + node.operand().accept(this) + ")";
    }
}
