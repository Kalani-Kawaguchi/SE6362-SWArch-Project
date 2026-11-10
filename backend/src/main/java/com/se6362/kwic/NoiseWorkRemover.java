package com.se6362.kwic;

import com.se6362.webpage.*;
import java.util.Set;

public class NoiseWorkRemover implements KwicProcessingStep
{
    private PageList inputPages;

    private static final Set<String> NOISE_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am",
            "an", "and", "any", "are", "aren't", "as", "at",
            "be", "because", "been", "before", "being", "below", "between",
            "both", "but", "by",
            "can", "can't", "cannot", "could", "couldn't",
            "did", "didn't", "do", "does", "doesn't", "doing", "don't",
            "down", "during",
            "each",
            "few", "for", "from", "further",
            "had", "hadn't", "has", "hasn't", "have", "haven't", "having",
            "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers",
            "herself", "him", "himself", "his", "how", "how's",
            "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
            "isn't", "it", "it's", "its", "itself",
            "just",
            "me", "more", "most", "mustn't", "my", "myself",
            "no", "nor",
            "of", "off", "on", "once", "only", "or", "other", "ought",
            "our", "ours", "ourselves", "out", "over", "own",
            "same", "shan't", "she", "she'd", "she'll", "she's", "should",
            "shouldn't", "so", "some", "such",
            "than", "that", "that's", "the", "their", "theirs", "them",
            "themselves", "then", "there", "there's", "these", "they",
            "they'd", "they'll", "they're", "they've", "this", "those",
            "through", "to", "too",
            "under", "until", "up",
            "very",
            "was", "wasn't", "we", "we'd", "we'll", "we're", "we've",
            "were", "weren't", "what", "what's", "when", "when's",
            "where", "where's", "which", "while", "who", "who's", "whom",
            "why", "why's", "with", "won't", "would", "wouldn't",
            "you", "you'd", "you'll", "you're", "you've", "your", "yours",
            "yourself", "yourselves");

    public NoiseWorkRemover(PageList pages)
    {
        this.inputPages = pages;
    }

    @Override
    public PageList getOutput()
    {
        PageList outputPages = new PageList();
        outputPages.append(inputPages);
        for (int i = outputPages.getPageCount() - 1; i >= 0; i--)
        {
            if (isNoiseWord(outputPages.getPage(i).getDescriptor().getWord(0)))
            {
                outputPages.deletePage(i);
            }
        }
        return outputPages;
    }

    public boolean isNoiseWord(String word)
    {
        return NOISE_WORDS.contains(word.toLowerCase());
    }
}
