package com.scoutgg.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void getAllTeams_returns200() throws Exception {
        mvc.perform(get("/teams"))
                .andExpect(status().isOk());
    }

    @Test
    void getTeam_returns200() throws Exception {
        mvc.perform(get("/teams/team-liquid"))
                .andExpect(status().isOk());
    }
}
