package com.scoutgg.riotapi.match.polling;

import com.scoutgg.riotapi.esports.LoLEsportsClient;
import com.scoutgg.riotapi.esports.model.ScheduleResponse;
import com.scoutgg.riotapi.esports.model.ScheduleResponse.ScheduleEvent;
import com.scoutgg.riotapi.match.MatchDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Component
public class MatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchScheduler.class);

    private final LoLEsportsClient client;
    private final MatchDataService matchDataService;
    private final Set<String> processedEvents = new HashSet<>();

    public MatchScheduler(LoLEsportsClient client, MatchDataService matchDataService) {
        this.client = client;
        this.matchDataService = matchDataService;
    }

    @Scheduled(fixedDelayString = "${riot.poll.interval-ms}")
    public void pollForCompletedMatches() {
        log.info("Polling LCS schedule for completed matches");
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);

        ScheduleResponse schedule;
        try {
            schedule = client.getSchedule(null);
        } catch (Exception e) {
            log.error("Failed to fetch LCS schedule: {}", e.getMessage());
            return;
        }

        if (Objects.isNull(schedule) || Objects.isNull(schedule.data())) {
            log.warn("LCS schedule response was empty");
            return;
        }

        List<ScheduleResponse.ScheduleEvent> events = schedule.data().schedule().events();
        if (events == null || events.isEmpty()) {
            log.warn("LCS schedule returned no events");
            return;
        }

        events.stream()
                .filter(e -> "completed".equals(e.state()) && "match".equals(e.type()))
                .filter(e -> Objects.nonNull(e.id()))
                .filter(e -> e.startTime() == null || Instant.parse(e.startTime()).isAfter(cutoff))
                .filter(e -> !processedEvents.contains(e.id()))
                .forEach(e -> {
                    try {
                        boolean hadNewData = matchDataService.pullMatch(e.id());
                        if (!hadNewData) {
                            processedEvents.add(e.id());
                        }
                    } catch (Exception ex) {
                        log.error("Failed to pull match data for event {}: {}", e.id(), ex.getMessage());
                    }
                });
    }
}
