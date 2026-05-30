package com.scoutgg.riotapi.match.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "riot_data", indexes = {
    @Index(name = "idx_riot_data_match_id", columnList = "match_id"),
    @Index(name = "idx_riot_data_event_id", columnList = "event_id")
})
public class RiotData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "match_id", nullable = false)
    private String matchId; // game ID within the series

    @Column(name = "player_id", nullable = false)
    private String playerId; // summonerName

    @Column(name = "team_id")
    private String teamId;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private int cs;

    @Column(name = "pulled_at", nullable = false)
    private Instant pulledAt;

    public RiotData() {}

    public RiotData(String eventId, String matchId, String playerId, String teamId,
                    int kills, int deaths, int assists, int cs, Instant pulledAt) {
        this.eventId = eventId;
        this.matchId = matchId;
        this.playerId = playerId;
        this.teamId = teamId;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.cs = cs;
        this.pulledAt = pulledAt;
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getMatchId() { return matchId; }
    public String getPlayerId() { return playerId; }
    public String getTeamId() { return teamId; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public int getCs() { return cs; }
    public Instant getPulledAt() { return pulledAt; }
}
