package com.se6362.search;

public interface QueryVisitor
{
    String visitKeyword(KeywordNode node);

    String visitBinary(BinaryNode node);

    String visitNot(NotNode node);
}