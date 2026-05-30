package com.scoutgg.riotapi.match.model;

import java.util.List;

public record GameData(String gameId, List<RiotData> entities) {}
