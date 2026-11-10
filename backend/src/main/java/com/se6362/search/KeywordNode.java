package com.se6362.search;

public record KeywordNode(String keyword) implements QueryNode
{
    @Override
    public String accept(QueryVisitor visitor)
    {
        return visitor.visitKeyword(this);
    }
}