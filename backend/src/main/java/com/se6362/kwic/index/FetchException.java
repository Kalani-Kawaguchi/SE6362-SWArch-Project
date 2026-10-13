package com.se6362.kwic.index;

public class FetchException extends Exception
{
    public FetchException(String message)
    {
        super(message);
    }

    public FetchException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
