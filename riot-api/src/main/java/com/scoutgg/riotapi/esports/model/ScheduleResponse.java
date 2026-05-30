package com.scoutgg.riotapi.esports.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduleResponse(ScheduleData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleData(Schedule schedule) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Schedule(Pages pages, List<ScheduleEvent> events) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pages(String older, String newer) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleEvent(String id, String startTime, String state, String type, ScheduleMatch match) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleMatch(String id) {}
}
