package com.scoutgg.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teams")
public class TeamController {

    @GetMapping
    public ResponseEntity<?> getAllTeams() {
        // TODO: query riot_data + narratives for all LCS teams
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTeam(@PathVariable String id) {
        // TODO: query riot_data + narratives + unsung hero by teamId
        return ResponseEntity.ok().build();
    }
}
