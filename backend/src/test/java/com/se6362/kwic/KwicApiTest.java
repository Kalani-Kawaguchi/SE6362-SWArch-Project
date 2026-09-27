package com.se6362.kwic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class KwicApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void healthWorksWithoutDatabase() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("KWIC backend is running!"));
        mvc.perform(get("/api/db-health"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void generatesSortedRotationsFromText() throws Exception {
        mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"software architecture project\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputLineCount").value(1))
                .andExpect(jsonPath("$.shiftCount").value(3))
                .andExpect(jsonPath("$.lines[0]").value("architecture project software"))
                .andExpect(jsonPath("$.lines[1]").value("project software architecture"))
                .andExpect(jsonPath("$.lines[2]").value("software architecture project"));
    }

    @Test
    void normalizesWhitespaceAndIgnoresEmptyLines() throws Exception {
        mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  Zebra\\tapple  \\r\\n\\r\\n banana \\n\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputLineCount").value(2))
                .andExpect(jsonPath("$.lines[0]").value("apple Zebra"))
                .andExpect(jsonPath("$.lines[1]").value("banana"))
                .andExpect(jsonPath("$.lines[2]").value("Zebra apple"));
    }

    @Test
    void requestsDoNotShareResults() throws Exception {
        mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"first request\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"second\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0]").value("second"));
    }

    @Test
    void rejectsInvalidRequestsWithReadableErrors() throws Exception {
        for (String body : new String[] {"{}", "{\"text\":null}", "{\"text\":\" \\n\\t\"}", "{", "[]", "null",
                "{\"text\":42}", "{\"text\":true}", "{\"text\":[]}", "{\"text\":{}}"}) {
            mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
    }

    @Test
    void supportsTheMaximumNumberOfRotationsIncludingDuplicates() throws Exception {
        String text = ("word ".repeat(50) + "\\n").repeat(20);
        mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputLineCount").value(20))
                .andExpect(jsonPath("$.shiftCount").value(1000))
                .andExpect(jsonPath("$.lines.length()").value(1000));
    }

    @Test
    void boundsInputAndExpansion() throws Exception {
        for (String text : new String[] {
                "a".repeat(10_001), "word ".repeat(51),
                "line\\n".repeat(101), ("a ".repeat(50) + "\\n").repeat(21)}) {
            mvc.perform(post("/api/kwic").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"text\":\"" + text + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
    }

    @Test
    void allowsLocalFrontendCorsPreflight() throws Exception {
        mvc.perform(options("/api/kwic")
                        .header("Origin", "http://localhost:1234")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:1234"));
    }
}
