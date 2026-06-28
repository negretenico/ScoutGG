package com.scoutgg.riotapi.match;

import com.scoutgg.riotapi.esports.LivestatsClient;
import com.scoutgg.riotapi.esports.LoLEsportsClient;
import com.scoutgg.riotapi.esports.model.EventDetailsResponse;
import com.scoutgg.riotapi.esports.model.EventDetailsResponse.*;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.*;
import com.scoutgg.riotapi.match.events.MatchEventsConfig;
import com.scoutgg.riotapi.match.events.MatchUpdatedMessage;
import com.scoutgg.riotapi.match.model.RiotData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchDataServiceTest {

    @Mock LoLEsportsClient esportsClient;
    @Mock LivestatsClient livestatsClient;
    @Mock MatchDataRepository repository;
    @Mock RabbitTemplate rabbitTemplate;

    MatchDataService service;

    @BeforeEach
    void setUp() {
        service = new MatchDataService(esportsClient, livestatsClient, repository, rabbitTemplate, new GameDataMapper());
    }

    @Test
    void pullMatch_skipsAlreadyProcessedGames() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(true);

        service.pullMatch("event-1");

        verify(livestatsClient, never()).getWindow(any(), any());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_skipsNonCompletedGames() {
        var game = new Game("game-1", 1, "in_progress", null, List.of());
        stubEventDetails("event-1", game);

        service.pullMatch("event-1");

        verify(livestatsClient, never()).getWindow(any(), any());
    }

    @Test
    void pullMatch_skipsRemadeGames() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(false);

        var frame = new WindowFrame("2026-05-24T20:12:10Z",
                new TeamFrame(List.of(new ParticipantStats(1, 0, 0, 0, 0))),
                new TeamFrame(List.of()));
        when(livestatsClient.getWindow(eq("game-1"), any())).thenReturn(
                new LivestatsWindowResponse("game-1", null, List.of(frame)));

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_persistsPlayersAndPublishesMessages() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(false);

        var metadata = new GameMetadata(
                new TeamMetadata("team-blue", List.of(new PlayerMetadata(1, "Ruler"))),
                new TeamMetadata("team-red", List.of(new PlayerMetadata(6, "Chovy"))));
        var blueStats = new ParticipantStats(1, 10, 2, 5, 300);
        var redStats = new ParticipantStats(6, 5, 3, 8, 280);
        var frame = new WindowFrame("2026-05-24T20:12:10Z",
                new TeamFrame(List.of(blueStats)),
                new TeamFrame(List.of(redStats)));
        when(livestatsClient.getWindow(eq("game-1"), any())).thenReturn(
                new LivestatsWindowResponse("game-1", metadata, List.of(frame, frame, frame)));

        service.pullMatch("event-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RiotData>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(savedCaptor.capture());
        List<RiotData> saved = savedCaptor.getValue();
        assertThat(saved).hasSize(2);

        RiotData ruler = saved.stream().filter(r -> "Ruler".equals(r.getPlayerId())).findFirst().orElseThrow();
        assertThat(ruler.getKills()).isEqualTo(10);
        assertThat(ruler.getDeaths()).isEqualTo(2);
        assertThat(ruler.getAssists()).isEqualTo(5);
        assertThat(ruler.getCs()).isEqualTo(300);

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(MatchEventsConfig.EXCHANGE),
                eq(MatchEventsConfig.ROUTING_KEY),
                any(MatchUpdatedMessage.class));
    }

    @Test
    void pullMatch_handlesNullEventDetails() {
        when(esportsClient.getEventDetails("event-1")).thenReturn(null);

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_handlesNullEventData() {
        when(esportsClient.getEventDetails("event-1")).thenReturn(new EventDetailsResponse(null));

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_skipsGameWhenInitialWindowIsNull() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(false);
        when(livestatsClient.getWindow(eq("game-1"), isNull())).thenReturn(null);

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_skipsGameWhenInitialWindowHasNoFrames() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(false);
        when(livestatsClient.getWindow(eq("game-1"), isNull()))
                .thenReturn(new LivestatsWindowResponse("game-1", null, List.of()));

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_skipsGameWhenFinalWindowIsNull() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(false);

        var frame = new WindowFrame("2026-05-24T20:12:10Z",
                new TeamFrame(List.of()), new TeamFrame(List.of()));
        var initialWindow = new LivestatsWindowResponse("game-1", null, List.of(frame, frame, frame));
        when(livestatsClient.getWindow(eq("game-1"), isNull())).thenReturn(initialWindow);
        when(livestatsClient.getWindow(eq("game-1"), notNull())).thenReturn(null);

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void pullMatch_skipsGameWhenFinalWindowHasNoFrames() {
        var game = new Game("game-1", 1, "completed", null, List.of());
        stubEventDetails("event-1", game);
        when(repository.existsByMatchId("game-1")).thenReturn(false);

        var frame = new WindowFrame("2026-05-24T20:12:10Z",
                new TeamFrame(List.of()), new TeamFrame(List.of()));
        var initialWindow = new LivestatsWindowResponse("game-1", null, List.of(frame, frame, frame));
        when(livestatsClient.getWindow(eq("game-1"), isNull())).thenReturn(initialWindow);
        when(livestatsClient.getWindow(eq("game-1"), notNull()))
                .thenReturn(new LivestatsWindowResponse("game-1", null, List.of()));

        service.pullMatch("event-1");

        verify(repository, never()).saveAll(any());
    }

    private void stubEventDetails(String eventId, Game game) {
        var eventDetails = new EventDetailsResponse(
                new EventData(new EventDetails(eventId, new EventMatch(List.of(game)))));
        when(esportsClient.getEventDetails(eventId)).thenReturn(eventDetails);
    }
}
