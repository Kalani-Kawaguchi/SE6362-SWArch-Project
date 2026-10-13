package com.se6362.kwic.index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "kwic_line", indexes = {
        @Index(name = "idx_first_word", columnList = "first_word"),
        @Index(name = "idx_uri", columnList = "uri")
})
public class KwicLine
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uri", length = 2048)
    private String uri;

    @Column(name = "first_word", length = 255)
    private String firstWord;

    @Column(name = "line_text", length = 1024)
    private String lineText;

    protected KwicLine()
    {
    }

    public KwicLine(String uri, String firstWord, String lineText)
    {
        this.uri = uri;
        this.firstWord = firstWord;
        this.lineText = lineText;
    }

    public Long getId()
    {
        return id;
    }

    public String getUri()
    {
        return uri;
    }

    public String getFirstWord()
    {
        return firstWord;
    }

    public String getLineText()
    {
        return lineText;
    }
}
