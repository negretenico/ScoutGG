package com.scoutgg.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void search_returns200() throws Exception {
        mvc.perform(get("/search").param("q", "Faker"))
                .andExpect(status().isOk());
    }

    @Test
    void search_missingQuery_returns400() throws Exception {
        mvc.perform(get("/search"))
                .andExpect(status().isBadRequest());
    }
}
