package com.se6362.webpage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WebPage implements Comparable<WebPage>
{
    private String title;
    private WebUrl url;
    private Descriptor descriptor;
    private int sponsorshipLevel;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public WebPage(String title, String url, String descriptor)
    {
        this.title = title;
        this.url = new WebUrl(url);
        this.descriptor = new Descriptor(descriptor);
        this.sponsorshipLevel = 0;
    }

    public WebPage(String title, String url, String descriptor, int sponsorshipLevel)
    {
        this.title = title;
        this.url = new WebUrl(url);
        this.descriptor = new Descriptor(descriptor);
        this.sponsorshipLevel = sponsorshipLevel;
    }

    public WebPage(String title, WebUrl url, Descriptor descriptor)
    {
        this.title = title;
        this.url = url;
        this.descriptor = descriptor;
        this.sponsorshipLevel = 0;
    }

    public String getTitle()
    {
        return title;
    }

    public String getUrl()
    {
        return url.toString();
    }

    public Descriptor getDescriptor()
    {
        return descriptor;
    }

    public PageList getShifts()
    {
        PageList shifts = new PageList();
        for (int i = 0; i < descriptor.getLength(); i++)
        {
            shifts.append(new WebPage(title, url, new ShiftedDescriptor(descriptor, i)));
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

    public JsonNode toJsonNode()
    {
        ObjectNode json = MAPPER.createObjectNode();

        json.put("title", title);
        json.put("url", url.toString());
        json.put("description", descriptor.toString());
        json.put("sponsorship_level", this.sponsorshipLevel);

        return json;
    }
}
