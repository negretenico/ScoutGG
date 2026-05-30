package com.scoutgg.riotapi.esports.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventDetailsResponse(EventData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventData(EventDetails event) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventDetails(String id, EventMatch match) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventMatch(List<Game> games) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Game(String id, int number, String state, String startTime, List<GameTeam> teams) {}

    // side only — result.outcome does not exist in this API
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameTeam(String id, String side) {}
}
