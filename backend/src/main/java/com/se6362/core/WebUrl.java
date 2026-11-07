package com.se6362.core;

public class WebUrl
{
    private String url;

    public WebUrl(String url)
    {
        this.url = url;
        if (!isCorrectSyntax())
        {
            throw new IllegalArgumentException("url format incorrect");
        }
    }

    private boolean isCorrectSyntax()
    {

        String identifier = "[a-zA-Z0-9]+";

        String protocol = "https?";

        String domain = "(www\\.)?" + identifier + "\\." +
                "(com|edu|org|net|gov|mil|us)";

        String pathSegment = identifier + "[a-zA-Z0-9._-]*";

        String path = "(/" + pathSegment + "(/" + pathSegment + ")*/?)?";

        String regex = protocol + "://" + domain + path;

        return (url != null && url.matches(regex)) || url.equals("");

    }

    public String toString()
    {
        return url;
    }
}
