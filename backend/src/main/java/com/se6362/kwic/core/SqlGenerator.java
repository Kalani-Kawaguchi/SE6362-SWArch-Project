package com.se6362.kwic.core;

import java.util.ArrayList;

public class SqlGenerator
{
    private final ArrayList<String> keywords = new ArrayList<>();

    public ArrayList<String> getKeywords()
    {
        return keywords;
    }

    public String getSearchSQL(QueryTree tree)
    {
        keywords.clear();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT url, title, description ");
        sqlBuilder.append("FROM WEB_DATA ");
        sqlBuilder.append("WHERE id IN (").append(generateNodeSQL(tree.getRoot())).append(");");
        return sqlBuilder.toString();
    }

    private String generateNodeSQL(QueryTree.QueryNode node)
    {
        if (node instanceof QueryTree.KeywordNode keywordNode)
        {
            return generateKeywordNodeSQL(keywordNode);
        }
        else if (node instanceof QueryTree.BinaryNode binaryNode)
        {
            return generateBinaryNodeSQL(binaryNode);
        }
        else if (node instanceof QueryTree.NotNode notNode)
        {
            return generateNotNodeSQL(notNode);
        }
        else
        {
            throw new IllegalArgumentException("Unknown query node type.");
        }
    }

    private String generateKeywordNodeSQL(QueryTree.KeywordNode node)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT page_id ");
        sqlBuilder.append("FROM KWIC_INDEX ");
        sqlBuilder.append("WHERE keyword = ? ");
        keywords.add(node.keyword());
        return sqlBuilder.toString();
    }

    private String generateBinaryNodeSQL(QueryTree.BinaryNode node)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT page_id ");
        sqlBuilder.append("FROM KWIC_INDEX ");
        sqlBuilder.append("WHERE page_id IN (").append(generateNodeSQL(node.left())).append(") ");
        sqlBuilder.append(node.operator()).append(" page_id IN (").append(generateNodeSQL(node.right())).append(") ");
        return sqlBuilder.toString();
    }

    private String generateNotNodeSQL(QueryTree.NotNode node)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT page_id ");
        sqlBuilder.append("FROM KWIC_INDEX ");
        sqlBuilder.append("WHERE page_id NOT IN (").append(generateNodeSQL(node.operand())).append(") ");

        return sqlBuilder.toString();
    }
}
