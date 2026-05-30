package com.scoutgg.riotapi.match;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminController {

    private final MatchDataService matchDataService;
    private final String adminApiKey;

    public AdminController(MatchDataService matchDataService,
                           @Value("${admin.api-key}") String adminApiKey) {
        this.matchDataService = matchDataService;
        this.adminApiKey = adminApiKey;
    }

    @PostMapping("/admin/pull-match/{eventId}")
    public ResponseEntity<String> pullMatch(@PathVariable String eventId,
                                            @RequestHeader("X-Admin-Key") String key) {
        if (!adminApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        matchDataService.pullMatch(eventId);
        return ResponseEntity.ok("Pull triggered for event: " + eventId);
    }
}
