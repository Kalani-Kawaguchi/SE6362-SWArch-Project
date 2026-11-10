package com.se6362.search;

public record NotNode(QueryNode operand) implements QueryNode
{
    @Override
    public String accept(QueryVisitor visitor)
    {
        return visitor.visitNot(this);
    }
}