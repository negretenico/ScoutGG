# Scout.gg

## Why
Casual LCS viewers don't know why they should care about most players and teams. Stats sites exist — stories don't. Scout.gg fixes that with AI-generated narratives that tell the human story behind the standings.

## What
An ESPN-style LCS companion. No accounts. No friction. Land, browse, leave invested.

**Design Principle (non-negotiable):** One story at a time. Clarity over completeness. If it competes for attention, it doesn't ship.

## How It Works
```
Riot API ──────────────────────────────┐
                                       ├──► Claude API ──► Player / Team Narrative
VOD Pipeline (Whisper → extraction) ───┘
```

- **Riot API** — official facts: roster, stats, record, live match state
- **VOD Pipeline** — narrative signals: broadcast mentions, sentiment, storylines
- **Claude API** — synthesizes both into 2–3 sentence human narratives
- **Frontend** — React, desktop-first, no auth in MVP

## MVP Scope
In: player profiles, team narratives, real-time pipeline, VOD pipeline, Riot API integration, search/discovery, homepage.
Out: accounts, following, notifications, mobile, non-LCS leagues, social sharing.

## Key Distinctions
- **Player Profile** = individual story (who this person is)
- **Team Narrative** = collective story (why this team is worth watching)
- **Unsung hero callout** = dedicated section on team page, never buried in narrative text

## Commands
```bash
# Install
npm install

# Dev
npm run dev

# Test
npm test

# Lint
npm run lint
```

## Agents
Use the agents in `.claude/agents/` for focused work:
- `/product-guardian` — scope, vision, user truth
- `/api-specialist` — Riot API, Claude API, Whisper, YouTube/Twitch
- `/ux-guardian` — frontend, components, design principle enforcement

## External APIs
- Riot Games API — match data, player stats, team records
- Anthropic Claude API (`claude-sonnet-4-20250514`) — narrative generation
- YouTube Data API v3 + `yt-dlp` — live stream ingestion
- OpenAI Whisper API — audio transcription
