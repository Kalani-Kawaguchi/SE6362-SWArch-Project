package com.se6362.search;

import java.util.ArrayList;

public class QueryTree
{
    private String queryString;
    private ArrayList<String> tokens;
    private QueryNode root;
    int tokenIndex;

    public QueryTree(String queryString)
    {
        this.queryString = queryString;
        parseQueryString();
    }

    public QueryNode getRoot()
    {
        return root;
    }

    private void parseQueryString()
    {
        if (queryString == null)
        {
            throw new IllegalArgumentException("Query string cannot be null.");
        }

        tokens = parseTokens();
        tokenIndex = 0;

        if (tokens.isEmpty())
        {
            throw new IllegalArgumentException("Query cannot be empty.");
        }

        root = parseExpression(false);

        if (tokenIndex != tokens.size())
        {
            throw new IllegalArgumentException("Unexpected token: " +
                    tokens.get(tokenIndex));
        }
    }

    private QueryNode parseKeyword()
    {
        if (tokenIndex >= tokens.size())
        {
            throw new IllegalArgumentException("Keyword expected at end of query.");
        }

        if (isKeyword(tokens.get(tokenIndex)))
        {
            QueryNode keywordNode = new KeywordNode(tokens.get(tokenIndex));
            tokenIndex++;
            return keywordNode;
        }
        else
        {
            throw new IllegalArgumentException("Invalid keyword: " + tokens.get(tokenIndex));
        }
    }

    private QueryNode parseExpression(boolean isInParentheses)
    {
        QueryNode root = parseAndExpression();

        while (tokenIndex < tokens.size() &&
                !tokens.get(tokenIndex).equals(")"))
        {
            if (tokens.get(tokenIndex).equals("OR"))
            {
                tokenIndex++;
            }

            QueryNode right = parseAndExpression();

            root = new BinaryNode("OR", root, right);
        }

        if (isInParentheses)
        {
            if (tokenIndex >= tokens.size() ||
                    !tokens.get(tokenIndex).equals(")"))
            {
                throw new IllegalArgumentException("Missing closing parenthesis.");
            }

            tokenIndex++;
        }

        return root;
    }

    private QueryNode parseAndExpression()
    {
        QueryNode root = parseSymbol();

        while (tokenIndex < tokens.size() &&
                tokens.get(tokenIndex).equals("AND"))
        {
            tokenIndex++;

            QueryNode right = parseSymbol();

            root = new BinaryNode("AND", root, right);
        }

        return root;
    }

    private QueryNode parseSymbol()
    {
        if (tokenIndex >= tokens.size())
        {
            throw new IllegalArgumentException("Symbol expected at end of query.");
        }

        if (tokens.get(tokenIndex).equals("NOT"))
        {
            tokenIndex++;
            return new NotNode(parseSymbol());
        }
        else if (tokens.get(tokenIndex).equals("("))
        {
            tokenIndex++;
            return parseExpression(true);
        }
        else
        {
            return parseKeyword();
        }
    }

    private boolean isKeyword(String keyword)
    {
        if (keyword.isEmpty() ||
                keyword.equals("AND") ||
                keyword.equals("OR") ||
                keyword.equals("NOT"))
        {
            return false;
        }

        for (int i = 0; i < keyword.length(); i++)
        {
            if (!isKeywordCharacter(keyword.charAt(i)))
            {
                return false;
            }
        }

        return true;
    }

    private ArrayList<String> parseTokens()
    {
        int index = 0;
        ArrayList<String> tokens = new ArrayList<>();

        while (index < queryString.length())
        {
            if (Character.isWhitespace(queryString.charAt(index)))
            {
                index++;
            }
            else if (queryString.charAt(index) == '(')
            {
                tokens.add("(");
                index++;
            }
            else if (queryString.charAt(index) == ')')
            {
                tokens.add(")");
                index++;
            }
            else if (isKeywordCharacter(queryString.charAt(index)))
            {
                int endIndex = index + 1;

                while (endIndex < queryString.length() &&
                        isKeywordCharacter(queryString.charAt(endIndex)))
                {
                    endIndex++;
                }

                tokens.add(queryString.substring(index, endIndex));
                index = endIndex;
            }
            else
            {
                throw new IllegalArgumentException("Character '" +
                        queryString.charAt(index) + "' is illegal.");
            }
        }

        return tokens;
    }

    private boolean isKeywordCharacter(char c)
    {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9');
    }
}