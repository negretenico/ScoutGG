package com.scoutgg.riotapi.esports.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LivestatsWindowResponse(
        String esportsGameId,
        GameMetadata gameMetadata,
        List<WindowFrame> frames
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameMetadata(
            TeamMetadata blueTeamMetadata,
            TeamMetadata redTeamMetadata
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamMetadata(
            String esportsTeamId,
            List<PlayerMetadata> participantMetadata
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerMetadata(int participantId, String summonerName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WindowFrame(String rfc460Timestamp, TeamFrame blueTeam, TeamFrame redTeam) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamFrame(List<ParticipantStats> participants) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParticipantStats(
            int participantId,
            int kills,
            int deaths,
            int assists,
            int creepScore
    ) {}
}
