package com.scoutgg.riotapi.match;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@TestPropertySource(properties = "admin.api-key=test-key")
class AdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    MatchDataService matchDataService;

    @Test
    void pullMatch_validKey_returns200AndDelegates() throws Exception {
        mvc.perform(post("/admin/pull-match/event-1")
                        .header("X-Admin-Key", "test-key"))
                .andExpect(status().isOk());

        verify(matchDataService).pullMatch("event-1");
    }

    @Test
    void pullMatch_wrongKey_returns401() throws Exception {
        mvc.perform(post("/admin/pull-match/event-1")
                        .header("X-Admin-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());

        verify(matchDataService, never()).pullMatch(any());
    }

    @Test
    void pullMatch_missingKey_returns400() throws Exception {
        mvc.perform(post("/admin/pull-match/event-1"))
                .andExpect(status().isBadRequest());

        verify(matchDataService, never()).pullMatch(any());
    }
}
