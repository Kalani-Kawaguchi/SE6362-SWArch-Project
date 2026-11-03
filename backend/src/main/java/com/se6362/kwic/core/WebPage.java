package com.se6362.kwic.core;

public class WebPage implements Comparable<WebPage>
{
    private WebUrl url;
    private Descriptor descriptor;

    public WebPage(String url, String descriptor)
    {
        this.url = new WebUrl(url);
        this.descriptor = new Descriptor(descriptor);
    }

    public WebPage(WebUrl url, Descriptor descriptor)
    {
        this.url = url;
        this.descriptor = descriptor;
    }

    public String getUrl()
    {
        return url.toString();
    }

    public Descriptor getDescriptor()
    {
        return descriptor;
    }

    public WebPage[] getShifts()
    {
        WebPage[] shifts = new WebPage[descriptor.getLength()];
        for (int i = 0; i < descriptor.getLength(); i++)
        {
            shifts[i] = new WebPage(url, new ShiftedDescriptor(descriptor, i));
        }
        return shifts;
    }

    @Override
    public int compareTo(WebPage other)
    {
        int descriptorComp = descriptor.toFilteredString().compareToIgnoreCase(other.descriptor.toFilteredString());
        if (descriptorComp != 0)
        {
            return descriptorComp;
        }
        else
        {
            return url.toString().compareToIgnoreCase(other.url.toString());
        }
    }
}
