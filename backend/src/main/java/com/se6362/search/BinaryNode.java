package com.se6362.search;

public record BinaryNode(String operator, QueryNode left, QueryNode right) implements QueryNode
{
    @Override
    public String accept(QueryVisitor visitor)
    {
        return visitor.visitBinary(this);
    }
}