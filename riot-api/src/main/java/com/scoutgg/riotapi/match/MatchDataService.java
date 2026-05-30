package com.scoutgg.riotapi.match;

import com.common.functionico.evaluation.Result;
import com.scoutgg.riotapi.esports.LivestatsClient;
import com.scoutgg.riotapi.esports.LoLEsportsClient;
import com.scoutgg.riotapi.esports.model.EventDetailsResponse;
import com.scoutgg.riotapi.esports.model.EventDetailsResponse.Game;
import com.scoutgg.riotapi.match.events.MatchEventsConfig;
import com.scoutgg.riotapi.match.events.MatchUpdatedMessage;
import com.scoutgg.riotapi.match.model.GameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MatchDataService {

    private static final Logger log = LoggerFactory.getLogger(MatchDataService.class);

    private final LoLEsportsClient esportsClient;
    private final LivestatsClient livestatsClient;
    private final MatchDataRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final GameDataMapper mapper;

    public MatchDataService(LoLEsportsClient esportsClient, LivestatsClient livestatsClient,
                            MatchDataRepository repository, RabbitTemplate rabbitTemplate,
                            GameDataMapper mapper) {
        this.esportsClient = Objects.requireNonNull(esportsClient);
        this.livestatsClient = Objects.requireNonNull(livestatsClient);
        this.repository = Objects.requireNonNull(repository);
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public boolean pullMatch(String eventId) {
        log.info("Pulling match data for event {}", eventId);

        EventDetailsResponse eventDetails = esportsClient.getEventDetails(eventId);
        if (eventDetails == null || eventDetails.data() == null) {
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
        return Result.of(() -> livestatsClient.getWindow(game.id(), null))
                .flatMap(initialWindow -> {
                    if (initialWindow == null || initialWindow.frames() == null || initialWindow.frames().isEmpty()) {
                        return Result.failure(new RuntimeException("No livestats data for game " + game.id()));
                    }
                    if (initialWindow.frames().size() < 3) {
                        return Result.failure(new RuntimeException(
                                "Game " + game.id() + " has only " + initialWindow.frames().size() + " frames — likely a remake"));
                    }
                    String probeTime = probeTime(initialWindow.frames().getFirst().rfc460Timestamp());
                    return Result.of(() -> livestatsClient.getWindow(game.id(), probeTime));
                })
                .flatMap(window -> {
                    if (window == null || window.frames() == null || window.frames().isEmpty()) {
                        return Result.failure(new RuntimeException("No final frame data for game " + game.id()));
                    }
                    return Result.success(mapper.map(eventId, game.id(), window));
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

    private void applyGameData(GameData data) {
        repository.saveAll(data.entities());
        log.info("Saved {} player records for game {}", data.entities().size(), data.gameId());

        data.entities().stream()
                .map(entity -> new MatchUpdatedMessage(data.gameId(), entity.getPlayerId()))
                .forEach(msg -> rabbitTemplate.convertAndSend(MatchEventsConfig.EXCHANGE, MatchEventsConfig.ROUTING_KEY, msg));
        log.info("Published {} events for game {}", data.entities().size(), data.gameId());
    }
}
