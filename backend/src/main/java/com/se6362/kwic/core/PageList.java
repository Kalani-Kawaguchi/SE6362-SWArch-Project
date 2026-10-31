package com.se6362.kwic.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.StringJoiner;

public class PageList implements Iterable<WebPage>
{
    private ArrayList<WebPage> pages = new ArrayList<>();

    @Override
    public Iterator<WebPage> iterator()
    {
        // Delegate directly to ArrayList's built-in iterator
        return pages.iterator();
    }

    public void appendPage(WebPage page)
    {
        pages.add(page);
    }

    public WebPage getPage(int index)
    {
        return pages.get(index);
    }

    public int getPageCount()
    {
        return pages.size();
    }

    public void clear()
    {
        pages.clear();
    }

    public void sortPages()
    {
        Collections.sort(pages);
    }

    public void deletePage(int index)
    {
        pages.remove(index);
    }

    public String toString()
    {
        StringJoiner stringJoiner = new StringJoiner("\n");
        for (WebPage webPage : pages)
        {
            stringJoiner.add(webPage.getDescriptor().toString());
        }
        return stringJoiner.toString();
    }
}
