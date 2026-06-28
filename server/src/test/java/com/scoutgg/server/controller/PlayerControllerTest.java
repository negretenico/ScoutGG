package com.scoutgg.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void getFeaturedPlayers_returns200() throws Exception {
        mvc.perform(get("/players/featured"))
                .andExpect(status().isOk());
    }

    @Test
    void getPlayer_returns200() throws Exception {
        mvc.perform(get("/players/some-player-id"))
                .andExpect(status().isOk());
    }
}
