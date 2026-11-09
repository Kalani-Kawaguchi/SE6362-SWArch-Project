package com.se6362.search;

public class RankByAccessFrequency implements PageRankStrategy
{
    @Override
    public String generateOrderBy()
    {
        return " ORDER BY sponsorship_level DESC, access_frequency DESC;";
    }
}