package com.scoutgg.riotapi.match;

import com.common.functionico.evaluation.Result;
import com.scoutgg.riotapi.esports.LivestatsClient;
import com.scoutgg.riotapi.esports.LoLEsportsClient;
import com.scoutgg.riotapi.esports.model.EventDetailsResponse;
import com.scoutgg.riotapi.esports.model.EventDetailsResponse.Game;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.GameMetadata;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.ParticipantStats;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.PlayerMetadata;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.TeamMetadata;
import com.scoutgg.riotapi.match.events.MatchEventsConfig;
import com.scoutgg.riotapi.match.events.MatchUpdatedMessage;
import com.scoutgg.riotapi.match.model.RiotData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MatchDataService {

    private static final Logger log = LoggerFactory.getLogger(MatchDataService.class);

    private final LoLEsportsClient esportsClient;
    private final LivestatsClient livestatsClient;
    private final MatchDataRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public MatchDataService(LoLEsportsClient esportsClient, LivestatsClient livestatsClient,
                            MatchDataRepository repository, RabbitTemplate rabbitTemplate) {
        this.esportsClient = esportsClient;
        this.livestatsClient = livestatsClient;
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public boolean pullMatch(String eventId) {
        log.info("Pulling match data for event {}", eventId);

        EventDetailsResponse eventDetails = esportsClient.getEventDetails(eventId);
        if (Objects.isNull(eventDetails) || Objects.isNull(eventDetails.data())) {
            log.warn("No event details returned for event {}", eventId);
            return false;
        }

        var savedCount = new AtomicInteger(0);

        eventDetails.data().event().match().games().stream()
                .filter(game -> "completed".equals(game.state()))
                .filter(game -> !repository.existsByMatchId(game.id()))
                .map(game -> processGame(eventId, game))
                .forEach(result -> result
                        .onSuccess(data -> { applyGameData(data); savedCount.incrementAndGet(); })
                        .onFailure(e -> log.warn("Skipped game in event {}: {}", eventId, e.getMessage())));

        return savedCount.get() > 0;
    }

    private Result<GameData> processGame(String eventId, Game game) {
        // First call: no startingTime → game-start frames, used only to detect remakes
        // and extract the first frame's timestamp for the probe call
        return Result.of(() -> livestatsClient.getWindow(game.id(), null))
                .flatMap(initialWindow -> {
                    if (Objects.isNull(initialWindow) || Objects.isNull(initialWindow.frames()) || initialWindow.frames().isEmpty()) {
                        return Result.failure(new RuntimeException("No livestats data for game " + game.id()));
                    }
                    if (initialWindow.frames().size() < 3) {
                        return Result.failure(new RuntimeException(
                                "Game " + game.id() + " has only " + initialWindow.frames().size() + " frames — likely a remake"));
                    }
                    // Second call: probe at game start + 35 min to land in late-game frames
                    String probeTime = probeTime(initialWindow.frames().getFirst().rfc460Timestamp());
                    return Result.of(() -> livestatsClient.getWindow(game.id(), probeTime));
                })
                .flatMap(window -> {
                    if (Objects.isNull(window) || Objects.isNull(window.frames()) || window.frames().isEmpty()) {
                        return Result.failure(new RuntimeException("No final frame data for game " + game.id()));
                    }
                    return Result.success(toGameData(eventId, game, window));
                });
    }

    // Adds 35 minutes to the first frame's timestamp and rounds down to the nearest
    // 10-second boundary, which the livestats API requires for startingTime.
    private String probeTime(String firstFrameTimestamp) {
        if (firstFrameTimestamp == null) return null;
        Instant probe = Instant.parse(firstFrameTimestamp).plus(35, ChronoUnit.MINUTES);
        long rounded = (probe.getEpochSecond() / 10) * 10;
        return Instant.ofEpochSecond(rounded).toString();
    }

    private GameData toGameData(String eventId, Game game, LivestatsWindowResponse window) {
        Map<Integer, String> playerNames = buildPlayerNameMap(window.gameMetadata());
        if (playerNames.isEmpty()) {
            log.warn("No player metadata for game {} in event {} — names will be Unknown", game.id(), eventId);
        }
        var lastFrame = window.frames().getLast();
        Instant pulledAt = Instant.now();

        List<RiotData> entities = Stream.concat(
                lastFrame.blueTeam().participants().stream()
                        .map(p -> toRiotData(eventId, game.id(), p, playerNames, "100", pulledAt)),
                lastFrame.redTeam().participants().stream()
                        .map(p -> toRiotData(eventId, game.id(), p, playerNames, "200", pulledAt))
        ).toList();

        boolean allZero = entities.stream()
                .allMatch(e -> e.getKills() == 0 && e.getDeaths() == 0 && e.getCs() == 0);
        if (allZero) {
            log.warn("All stats are zero for game {} — livestats window may not have data for this game ID", game.id());
        }

        return new GameData(game.id(), entities);
    }

    private void applyGameData(GameData data) {
        repository.saveAll(data.entities());
        log.info("Saved {} player records for game {}", data.entities().size(), data.gameId());

        data.entities().stream()
                .map(entity -> new MatchUpdatedMessage(data.gameId(), entity.getPlayerId()))
                .forEach(msg -> rabbitTemplate.convertAndSend(MatchEventsConfig.EXCHANGE, MatchEventsConfig.ROUTING_KEY, msg));
        log.info("Published {} events for game {}", data.entities().size(), data.gameId());
    }

    private Map<Integer, String> buildPlayerNameMap(GameMetadata metadata) {
        if (Objects.isNull(metadata)) return Map.of();
        return Stream.concat(
                safeParticipants(metadata.blueTeamMetadata()),
                safeParticipants(metadata.redTeamMetadata())
        ).collect(Collectors.toMap(PlayerMetadata::participantId, PlayerMetadata::summonerName));
    }

    private Stream<PlayerMetadata> safeParticipants(TeamMetadata team) {
        if (Objects.isNull(team) || Objects.isNull(team.participantMetadata())) return Stream.empty();
        return team.participantMetadata().stream();
    }

    private RiotData toRiotData(String eventId, String gameId, ParticipantStats p,
                                Map<Integer, String> playerNames, String teamId, Instant pulledAt) {
        String name = playerNames.getOrDefault(p.participantId(), "Unknown#" + p.participantId());
        return new RiotData(eventId, gameId, name, teamId,
                p.kills(), p.deaths(), p.assists(), p.creepScore(), pulledAt);
    }

    private record GameData(String gameId, List<RiotData> entities) {}
}
