package com.se6362.index;

import com.se6362.web.*;
import java.util.Set;

public class SymbolFilter implements KwicProcessingStep
{
    private Set<Character> filteredSymbols;
    PageList inputPages;

    public SymbolFilter(PageList inputPages, Set<Character> filteredSymbols)
    {
        this.filteredSymbols = Set.copyOf(filteredSymbols);
        this.inputPages = inputPages;
    }

    @Override
    public PageList getOutput()
    {
        PageList outputPages = new PageList();
        for (int i = 0; i < inputPages.getPageCount(); i++)
        {
            WebPage current = inputPages.getPage(i);
            outputPages.append(
                    new WebPage(current.getTitle(), current.getUrl(), this.filter(current.getDescriptor().toString())));
        }
        return outputPages;
    }

    public String filter(String text)
    {
        if (text == null)
        {
            return null;
        }

        StringBuilder result = new StringBuilder();

        for (char character : text.toCharArray())
        {
            if (!filteredSymbols.contains(character))
            {
                result.append(character);
            }
        }

        return result.toString();
    }
}
