---
name: riot-api-query
description: Query the Riot Games API or LoL Esports API for Scout.gg data needs — player stats, match history, live game state, team rosters, LCS standings. Use when you need to fetch or update any official data from Riot.
---

# Riot API Query

Handles all queries to the Riot Games API and the unofficial LoL Esports API for LCS data.

## Setup
```javascript
const RIOT_KEY = process.env.RIOT_API_KEY;
const RIOT_BASE = "https://na1.api.riotgames.com";
const ESPORTS_BASE = "https://esports-api.lolesports.com/persisted/gw";

const riotHeaders = { "X-Riot-Token": RIOT_KEY };
```

## Common Queries

### Get LCS Teams + Rosters (LoL Esports API)
```javascript
async function getLCSTeams() {
  const res = await fetch(
    `${ESPORTS_BASE}/getTeams?hl=en-US`,
    { headers: { "x-api-key": "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z" } }
    // Note: This API key is public/shared — do not treat as secret
  );
  const data = await res.json();
  return data.data.teams;
}
```

### Get LCS Schedule
```javascript
async function getLCSSchedule() {
  const res = await fetch(
    `${ESPORTS_BASE}/getSchedule?hl=en-US&leagueId=98767991299243165`,
    // 98767991299243165 = LCS league ID
  );
  const data = await res.json();
  return data.data.schedule;
}
```

### Resolve Summoner → PUUID
```javascript
async function getSummoner(summonerName) {
  const res = await fetch(
    `${RIOT_BASE}/lol/summoner/v4/summoners/by-name/${encodeURIComponent(summonerName)}`,
    { headers: riotHeaders }
  );
  return res.json(); // { id, accountId, puuid, name, ... }
}
```

### Get Match History
```javascript
async function getMatchHistory(puuid, count = 5) {
  const idsRes = await fetch(
    `https://americas.api.riotgames.com/lol/match/v5/matches/by-puuid/${puuid}/ids?count=${count}`,
    { headers: riotHeaders }
  );
  const matchIds = await idsRes.json();

  const matches = await Promise.all(
    matchIds.map(id =>
      fetch(`https://americas.api.riotgames.com/lol/match/v5/matches/${id}`, { headers: riotHeaders })
        .then(r => r.json())
    )
  );
  return matches;
}
```

### Check Live Match
```javascript
async function getLiveMatch(encryptedSummonerId) {
  const res = await fetch(
    `${RIOT_BASE}/lol/spectator/v5/active-games/by-summoner/${encryptedSummonerId}`,
    { headers: riotHeaders }
  );
  if (res.status === 404) return null; // Player not in game
  return res.json();
}
```

## Rate Limit Handling
```javascript
async function riotFetch(url) {
  const res = await fetch(url, { headers: riotHeaders });

  if (res.status === 429) {
    const retryAfter = parseInt(res.headers.get("Retry-After") || "5");
    await new Promise(r => setTimeout(r, retryAfter * 1000));
    return riotFetch(url); // Retry once
  }

  if (!res.ok) throw new Error(`Riot API error: ${res.status} ${url}`);
  return res.json();
}
```

## Data Mapping: Riot → Scout.gg
After fetching, map to Scout.gg's internal shape:
```javascript
function mapPlayerStats(matchList, summonerName) {
  const playerMatches = matchList.map(match => {
    const participant = match.info.participants
      .find(p => p.summonerName === summonerName);
    return {
      win: participant.win,
      kda: ((participant.kills + participant.assists) / Math.max(1, participant.deaths)).toFixed(2),
      kills: participant.kills,
      deaths: participant.deaths,
      assists: participant.assists,
      gameDate: new Date(match.info.gameStartTimestamp)
    };
  });

  return {
    recentForm: playerMatches.map(m => m.win ? "W" : "L"),
    avgKda: (playerMatches.reduce((sum, m) => sum + parseFloat(m.kda), 0) / playerMatches.length).toFixed(2),
    record: `${playerMatches.filter(m => m.win).length}-${playerMatches.filter(m => !m.win).length}`
  };
}
```

## Notes
- Use `americas.api.riotgames.com` for Match v5 endpoints (not `na1`)
- PUUIDs work across regions; encrypted summoner IDs are NA1-specific
- The LoL Esports API `x-api-key` value above is a public community key — not secret, but still don't log it
