package com.scoutgg.riotapi.esports;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class EsportsApiConfig {

    // Public key for the LoL Esports API — not a personal dev key
    private static final String LOL_ESPORTS_API_KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";

    @Value("${riot.api.base-url}")
    private String baseUrl;

    @Bean
    public WebClient lolEsportsWebClient() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", LOL_ESPORTS_API_KEY)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient livestatsWebClient() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder()
                .baseUrl("https://feed.lolesports.com/livestats/v1")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
