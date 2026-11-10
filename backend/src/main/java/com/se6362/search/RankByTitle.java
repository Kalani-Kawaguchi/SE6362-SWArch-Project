package com.se6362.search;

public class RankByTitle implements PageRankStrategy
{
    @Override
    public String generateOrderBy()
    {
        return " ORDER BY sponsorship_level DESC, title ASC;";
    }
}