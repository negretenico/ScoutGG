package com.scoutgg.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    @GetMapping
    public ResponseEntity<?> search(@RequestParam String q) {
        // TODO: query riot_data for players + teams matching q
        return ResponseEntity.ok().build();
    }
}
