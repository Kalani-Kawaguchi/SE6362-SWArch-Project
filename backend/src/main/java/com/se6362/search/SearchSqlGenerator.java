package com.se6362.search;

import java.util.ArrayList;

public class SearchSqlGenerator implements QueryVisitor
{
    private final ArrayList<String> keywords = new ArrayList<>();
    private final boolean isCaseSensitive;

    public SearchSqlGenerator(boolean isCaseSensitive)
    {
        this.isCaseSensitive = isCaseSensitive;
    }

    public ArrayList<String> getKeywords()
    {
        return keywords;
    }

    public String getSearchSQL(QueryNode root)
    {
        return getSearchSQL(root, null);
    }

    public String getSearchSQL(QueryNode root, PageRankStrategy sortStrategy)
    {
        keywords.clear();

        String orderBy = (sortStrategy != null) ? " " + sortStrategy.generateOrderBy().trim() : "";

        return "SELECT w.url, w.title, w.description, w.sponsorship_level " +
                "FROM WEB_DATA w " +
                "WHERE " + root.accept(this) +
                orderBy + ";";
    }

    @Override
    public String visitKeyword(KeywordNode node)
    {
        String term = isCaseSensitive ? node.keyword() : node.keyword().toLowerCase();
        keywords.add(term);

        // Apply collation/binary to the parameter (?), NOT the column (k.keyword), to
        // preserve index seeks
        String comparison = isCaseSensitive ? "k.keyword = BINARY ?" : "k.keyword = ?";

        return "EXISTS (" +
                "SELECT 1 FROM KWIC_INDEX k " +
                "WHERE k.page_id = w.id AND " + comparison +
                ")";
    }

    @Override
    public String visitBinary(BinaryNode node)
    {
        return "(" + node.left().accept(this) + " " + node.operator() + " " + node.right().accept(this) + ")";
    }

    @Override
    public String visitNot(NotNode node)
    {
        // Note: Matches pages in WEB_DATA that do not contain the sub-expression
        return "NOT (" + node.operand().accept(this) + ")";
    }
}
