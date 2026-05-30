package com.scoutgg.riotapi.esports;

import com.scoutgg.riotapi.esports.model.LivestatsWindowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LivestatsClient {

    private static final Logger log = LoggerFactory.getLogger(LivestatsClient.class);

    private final WebClient webClient;

    public LivestatsClient(@Qualifier("livestatsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public LivestatsWindowResponse getWindow(String gameId, String startingTime) {
        return webClient.get()
                .uri(b -> {
                    var builder = b.path("/window/{gameId}");
                    if (startingTime != null) builder.queryParam("startingTime", startingTime);
                    return builder.build(gameId);
                })
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class).map(body -> {
                            log.error("getWindow({}) returned HTTP {}: {}", gameId, response.statusCode(), body);
                            return new RuntimeException("getWindow HTTP " + response.statusCode());
                        }))
                .bodyToMono(LivestatsWindowResponse.class)
                .block();
    }
}
