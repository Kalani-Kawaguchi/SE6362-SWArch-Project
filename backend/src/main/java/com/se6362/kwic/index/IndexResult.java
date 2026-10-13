package com.se6362.kwic.index;

import java.util.List;

public record IndexResult(
        List<Indexed> indexed,
        List<Failed> failed,
        int totalRotations)
{
    public record Indexed(String url, int lineCount)
    {
    }

    public record Failed(String url, String error)
    {
    }
}
