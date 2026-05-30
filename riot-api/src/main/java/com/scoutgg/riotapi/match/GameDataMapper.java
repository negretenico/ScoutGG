package com.scoutgg.riotapi.match;

import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.GameMetadata;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.ParticipantStats;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.PlayerMetadata;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.TeamMetadata;
import com.scoutgg.riotapi.match.model.GameData;
import com.scoutgg.riotapi.match.model.RiotData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GameDataMapper {

    private static final Logger log = LoggerFactory.getLogger(GameDataMapper.class);

    public GameData map(String eventId, String gameId, LivestatsWindowResponse window) {
        Map<Integer, String> playerNames = buildPlayerNameMap(window.gameMetadata());
        if (playerNames.isEmpty()) {
            log.warn("No player metadata for game {} in event {} — names will be Unknown", gameId, eventId);
        }

        var lastFrame = window.frames().getLast();
        Instant pulledAt = Instant.now();

        List<RiotData> entities = Stream.concat(
                lastFrame.blueTeam().participants().stream()
                        .map(p -> toRiotData(eventId, gameId, p, playerNames, "100", pulledAt)),
                lastFrame.redTeam().participants().stream()
                        .map(p -> toRiotData(eventId, gameId, p, playerNames, "200", pulledAt))
        ).toList();

        boolean allZero = entities.stream()
                .allMatch(e -> e.getKills() == 0 && e.getDeaths() == 0 && e.getCs() == 0);
        if (allZero) {
            log.warn("All stats are zero for game {} — probe time may have missed the final frame", gameId);
        }

        return new GameData(gameId, entities);
    }

    private Map<Integer, String> buildPlayerNameMap(GameMetadata metadata) {
        if (metadata == null) return Map.of();
        return Stream.concat(
                safeParticipants(metadata.blueTeamMetadata()),
                safeParticipants(metadata.redTeamMetadata())
        ).collect(Collectors.toMap(PlayerMetadata::participantId, PlayerMetadata::summonerName));
    }

    private Stream<PlayerMetadata> safeParticipants(TeamMetadata team) {
        if (team == null || team.participantMetadata() == null) return Stream.empty();
        return team.participantMetadata().stream();
    }

    private RiotData toRiotData(String eventId, String gameId, ParticipantStats p,
                                Map<Integer, String> playerNames, String teamId, Instant pulledAt) {
        String name = playerNames.getOrDefault(p.participantId(), "Unknown#" + p.participantId());
        return new RiotData(eventId, gameId, name, teamId,
                p.kills(), p.deaths(), p.assists(), p.creepScore(), pulledAt);
    }
}
