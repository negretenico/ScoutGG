package com.scoutgg.riotapi.esports;

import com.scoutgg.riotapi.esports.model.EventDetailsResponse;
import com.scoutgg.riotapi.esports.model.ScheduleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LoLEsportsClient {

    private static final Logger log = LoggerFactory.getLogger(LoLEsportsClient.class);
    private static final String LCS_LEAGUE_ID = "98767991299243165";

    private final WebClient webClient;

    public LoLEsportsClient(@Qualifier("lolEsportsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public ScheduleResponse getSchedule(String pageToken) {
        return webClient.get()
                .uri(b -> {
                    var builder = b.path("/getSchedule")
                            .queryParam("hl", "en-US")
                            .queryParam("leagueId", LCS_LEAGUE_ID);
                    if (pageToken != null) builder.queryParam("pageToken", pageToken);
                    return builder.build();
                })
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class).map(body -> {
                            log.error("getSchedule returned HTTP {}: {}", response.statusCode(), body);
                            return new RuntimeException("getSchedule HTTP " + response.statusCode());
                        }))
                .bodyToMono(ScheduleResponse.class)
                .block();
    }

    public EventDetailsResponse getEventDetails(String eventId) {
        return webClient.get()
                .uri("/getEventDetails?hl=en-US&id={id}", eventId)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class).map(body -> {
                            log.error("getEventDetails({}) returned HTTP {}: {}", eventId, response.statusCode(), body);
                            return new RuntimeException("getEventDetails HTTP " + response.statusCode());
                        }))
                .bodyToMono(EventDetailsResponse.class)
                .block();
    }
}
