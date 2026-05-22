package com.scoutgg.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/players")
public class PlayerController {

    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedPlayers() {
        // TODO: query narratives + riot_data for top-signal players
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlayer(@PathVariable String id) {
        // TODO: query riot_data + narratives by playerId
        return ResponseEntity.ok().build();
    }
}
