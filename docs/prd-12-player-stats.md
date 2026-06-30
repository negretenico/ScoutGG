# PRD: Store and Update Per-Player Stats (Issue #12)

## Problem

The `riot-api` module stores raw game stats (kills, deaths, assists, CS) but:
- Captures no game context (champion, role)
- Has no aggregate player stats (KDA average, games played)
- Uses a fragile fixed +35min probe that silently drops data when games are shorter
- Has no safe write coordination when concurrent games land

## Goal

Complete the data foundation for player profiles: raw game records, per-game metadata, and pre-computed aggregate stats — written reliably by riot-api, ready for the server module to serve.

## Out of Scope

- W/L record (no endpoint reliably provides this; deferred to broadcast pipeline inference)
- Items, wards, objectives (post-MVP)
- Server module exposing these stats (separate issue)

---

## Data Model

### Table: `riot_data` (enrich existing)

Per-game per-player performance stats. Append-only.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `event_id` | VARCHAR | LCS event reference |
| `match_id` | VARCHAR | Game ID (dedup key) |
| `player_id` | VARCHAR | Summoner name |
| `team_id` | VARCHAR | "100" blue / "200" red |
| `kills` | INT | |
| `deaths` | INT | |
| `assists` | INT | |
| `cs` | INT | Creep score |
| `gold` | INT | **NEW** — total gold from final frame |
| `pulled_at` | TIMESTAMPTZ | |

### Table: `player_game_metadata` (new)

Per-game context for a player. Separates *who they were* in a game from *what they did*.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `match_id` | VARCHAR | FK to `riot_data.match_id` |
| `player_id` | VARCHAR | Summoner name |
| `champion_id` | VARCHAR | Champion played |
| `role` | VARCHAR | Assigned role |

### Table: `player_stats` (new)

Pre-computed aggregate stats per player. Upserted after each game, never deleted.

| Column | Type | Notes |
|---|---|---|
| `player_id` | VARCHAR PK | Summoner name |
| `summoner_name` | VARCHAR | Display name |
| `total_kills` | INT | |
| `total_deaths` | INT | |
| `total_assists` | INT | |
| `games_played` | INT | |
| `last_updated_at` | TIMESTAMPTZ | |

**KDA** is computed at read time: `(total_kills + total_assists) / max(total_deaths, 1)`
**Recent form (last 5)** is derived at query time from the last 5 `riot_data` rows for a `player_id`

---

## Probe Strategy Fix

**Current problem:** Fixed +35min probe. If the game ends early, all stats come back zero. No retry.

**New approach:** Exponential backoff polling until `gameState = finished`

- Poll the Livestats `/window/{gameId}` endpoint
- Read `gameState` from each frame
- Backoff: 30s → 60s → 120s → 240s → 300s (cap)
- Timeout: 90 minutes total — log warning and skip if exceeded (same skip behavior as today)
- On `gameState = finished`: take the final frame as the source of truth

**Required model change:** Add `gameState` to `WindowFrame` in `LivestatsWindowResponse`.
**Required model change:** Add `totalGold` to `ParticipantStats`.
**Required model change:** Add `championId` and `role` to `PlayerMetadata`.

---

## Write Flow (Saga Pattern)

```
AdminController.pullMatch(eventId)
  └── MatchDataService.pullMatch(eventId)
        1. Fetch event details → filter completed, non-duplicate games
        2. For each game:
           a. [Transaction 1] Poll with backoff until gameState = finished
           b. [Transaction 2] Persist riot_data rows (game stats)
           c. [Transaction 3] Persist player_game_metadata rows (champion, role)
           d. Publish Spring ApplicationEvent: PlayerStatsUpdateEvent(matchId, playerIds)

PlayerStatsAggregator (Spring event listener)
        e. [Transaction 4, serialized per player_id]
              SELECT * FROM riot_data WHERE player_id = ?  → recalculate totals
              UPSERT player_stats (pessimistic lock on player_id row)
        f. Publish RabbitMQ MatchUpdatedMessage (existing — notifies downstream)
```

**Key invariant:** `player_stats` is always a full recalculation from `riot_data` — never an incremental delta. Prevents drift if a game is reprocessed.

**Concurrency safety:** `PlayerStatsAggregator` issues a single atomic SQL UPSERT that recalculates from `riot_data` in one statement — no application-level locking, no thread blocking:
```sql
INSERT INTO player_stats (player_id, summoner_name, total_kills, total_deaths, total_assists, games_played, last_updated_at)
SELECT player_id, player_id, SUM(kills), SUM(deaths), SUM(assists), COUNT(*), NOW()
FROM riot_data
WHERE player_id = :playerId
GROUP BY player_id
ON CONFLICT (player_id) DO UPDATE SET
  total_kills = EXCLUDED.total_kills,
  total_deaths = EXCLUDED.total_deaths,
  total_assists = EXCLUDED.total_assists,
  games_played = EXCLUDED.games_played,
  last_updated_at = EXCLUDED.last_updated_at;
```

---

## Acceptance Criteria

- [ ] `riot_data` includes `gold` per game
- [ ] `player_game_metadata` stores `champion_id` and `role` per game
- [ ] `player_stats` is upserted after every completed game with accurate KDA totals and `games_played`
- [ ] Probe uses exponential backoff until `gameState = finished`, not fixed +35min
- [ ] Probe times out after 90 minutes with a warning log (no crash)
- [ ] Concurrent game events for the same player do not produce incorrect aggregate stats
- [ ] All new behavior is covered by unit tests (TDD — tests first)
- [ ] W/L is explicitly not stored (tracked in follow-up issue)

---

## What Does NOT Change

- `AdminController` API contract (`POST /admin/pull-match/{eventId}`)
- Remake detection (< 3 frames = skip)
- Dedup by `match_id` (`existsByMatchId`)
- RabbitMQ `MatchUpdatedMessage` shape
