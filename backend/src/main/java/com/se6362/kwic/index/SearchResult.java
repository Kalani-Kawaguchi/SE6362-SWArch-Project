package com.se6362.kwic.index;

import java.util.List;

public record SearchResult(
        String query,
        int resultCount,
        List<Group> groups)
{
    public record Group(String url, List<String> contexts)
    {
    }
}
