package com.se6362.kwic;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.se6362.kwic.controller.IndexController;
import com.se6362.kwic.index.IndexResult;
import com.se6362.kwic.index.KwicIndexService;
import com.se6362.kwic.index.SearchResult;
import com.se6362.kwic.index.SearchService;

@WebMvcTest(IndexController.class)
public class IndexControllerTest
{
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KwicIndexService indexService;

    @MockitoBean
    SearchService searchService;

    @Test
    void searchReturnsGroupedJson() throws Exception
    {
        when(searchService.search("soft")).thenReturn(new SearchResult(
                "soft", 2,
                List.of(new SearchResult.Group("http://b",
                        List.of("software architecture is")))));

        mockMvc.perform(get("/api/search").param("q", "soft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("soft"))
                .andExpect(jsonPath("$.groups[0].url").value("http://b"));
    }

    @Test
    void searchWithoutQueryIsBadRequest() throws Exception
    {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void indexReturnsSummary() throws Exception
    {
        when(indexService.index(anyList())).thenReturn(
                new IndexResult(List.of(new IndexResult.Indexed("http://a", 3)),
                        List.of(), 9));

        mockMvc.perform(post("/api/index")
                .contentType("application/json")
                .content("{\"urls\":[\"http://a\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRotations").value(9))
                .andExpect(jsonPath("$.indexed[0].url").value("http://a"));
    }

    @Test
    void indexWithNoUrlsIsBadRequest() throws Exception
    {
        mockMvc.perform(post("/api/index")
                .contentType("application/json")
                .content("{\"urls\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
