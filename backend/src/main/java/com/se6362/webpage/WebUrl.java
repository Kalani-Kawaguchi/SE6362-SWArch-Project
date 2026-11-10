package com.se6362.webpage;

import java.net.URI;
import java.net.URL;

public class WebUrl
{
    private String url;

    public WebUrl(String url)
    {

        if (!isValidWebPageUrl(url))
        {
            throw new IllegalArgumentException("url format incorrect: " + url);
        }
        this.url = url;
    }

    public static boolean isValidWebPageUrl(String urlString)
    {
        if (urlString.isBlank())
        {
            return true;
        }
        try
        {
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            String protocol = url.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public String toString()
    {
        return url;
    }
}
