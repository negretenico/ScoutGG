package com.scoutgg.riotapi.match;

import com.scoutgg.riotapi.match.model.RiotData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MatchDataRepository extends JpaRepository<RiotData, UUID> {
    boolean existsByMatchId(String matchId);
}
