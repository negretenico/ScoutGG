package com.scoutgg.riotapi.match;

import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse;
import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameDataMapperTest {

    private final GameDataMapper mapper = new GameDataMapper();

    @Test
    void map_withFullMetadata_resolvesPlayerNames() {
        var window = windowWithMetadata(
                new TeamMetadata("team-blue", List.of(new PlayerMetadata(1, "Ruler"))),
                new TeamMetadata("team-red",  List.of(new PlayerMetadata(6, "Chovy"))),
                new ParticipantStats(1, 5, 1, 3, 200),
                new ParticipantStats(6, 2, 2, 4, 180)
        );

        var result = mapper.map("event-1", "game-1", window);

        assertThat(result.entities()).hasSize(2);
        assertThat(result.entities()).anyMatch(e -> "Ruler".equals(e.getPlayerId()));
        assertThat(result.entities()).anyMatch(e -> "Chovy".equals(e.getPlayerId()));
    }

    @Test
    void map_withNullMetadata_fallsBackToUnknownNames() {
        var frame = new WindowFrame("ts",
                new TeamFrame(List.of(new ParticipantStats(1, 3, 0, 2, 150))),
                new TeamFrame(List.of()));
        var window = new LivestatsWindowResponse("game-1", null, List.of(frame));

        var result = mapper.map("event-1", "game-1", window);

        assertThat(result.entities()).hasSize(1);
        assertThat(result.entities().get(0).getPlayerId()).startsWith("Unknown#");
    }

    @Test
    void map_allZeroStats_stillReturnsEntities() {
        var window = windowWithMetadata(
                new TeamMetadata("blue", List.of(new PlayerMetadata(1, "Player1"))),
                new TeamMetadata("red",  List.of()),
                new ParticipantStats(1, 0, 0, 0, 0)
        );

        var result = mapper.map("event-1", "game-1", window);

        assertThat(result.entities()).hasSize(1);
        assertThat(result.entities().get(0).getKills()).isZero();
    }

    @Test
    void map_setsCorrectTeamIds() {
        var window = windowWithMetadata(
                new TeamMetadata("blue", List.of(new PlayerMetadata(1, "BluePlayer"))),
                new TeamMetadata("red",  List.of(new PlayerMetadata(6, "RedPlayer"))),
                new ParticipantStats(1, 1, 0, 0, 100),
                new ParticipantStats(6, 0, 1, 0, 90)
        );

        var result = mapper.map("event-1", "game-1", window);

        assertThat(result.entities()).anyMatch(e -> "BluePlayer".equals(e.getPlayerId()) && "100".equals(e.getTeamId()));
        assertThat(result.entities()).anyMatch(e -> "RedPlayer".equals(e.getPlayerId()) && "200".equals(e.getTeamId()));
    }

    @Test
    void map_withNullTeamInMetadata_treatsItAsEmpty() {
        // metadata non-null but one team is null — safeParticipants must handle this
        var frame = new WindowFrame("ts",
                new TeamFrame(List.of(new ParticipantStats(1, 2, 1, 3, 100))),
                new TeamFrame(List.of()));
        var metadata = new GameMetadata(null, new TeamMetadata("red", null));
        var window = new LivestatsWindowResponse("game-1", metadata, List.of(frame));

        var result = mapper.map("event-1", "game-1", window);

        // blue participant falls back to Unknown since no metadata resolved names
        assertThat(result.entities()).hasSize(1);
        assertThat(result.entities().get(0).getPlayerId()).startsWith("Unknown#");
    }

    // Helper — builds a window with one frame containing the given participants
    private LivestatsWindowResponse windowWithMetadata(
            TeamMetadata blueMeta, TeamMetadata redMeta,
            ParticipantStats... stats) {
        int split = blueMeta.participantMetadata().size();
        var blueStats = List.of(stats).subList(0, split);
        var redStats  = List.of(stats).subList(split, stats.length);

        var frame = new WindowFrame("ts",
                new TeamFrame(blueStats),
                new TeamFrame(redStats));
        var metadata = new GameMetadata(blueMeta, redMeta);
        return new LivestatsWindowResponse("game-1", metadata, List.of(frame));
    }
}
