package com.se6362.search;

public interface QueryNode
{
    String accept(QueryVisitor visitor);
}