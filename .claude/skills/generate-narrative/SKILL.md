---
name: generate-narrative
description: Generate an AI narrative for a Scout.gg player profile or team page using the Claude API. Use when you need to create or regenerate a narrative from Riot API stats and VOD pipeline signals.
---

# Generate Narrative

Generates a Scout.gg narrative (player or team) by calling the Claude API with the correct prompt structure and data shape.

## Input Required
Before calling the API, confirm you have:
- Entity type: `player` or `team`
- Name, role (player only), team name
- Current stats: KDA, W/L record, recent form (last 5)
- Pipeline signals: array of broadcast mentions with sentiment and context text

## Player Narrative
```javascript
async function generatePlayerNarrative({ name, role, team, stats, signals }) {
  const signalText = signals
    .map(s => `[${s.sentiment}] "${s.context}"`)
    .join("\n");

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      model: "claude-sonnet-4-20250514",
      max_tokens: 400,
      system: `You write player narratives for Scout.gg, an LCS companion for casual viewers.
Tone: accessible, story-driven, not a stats recap.
Length: exactly 2–3 sentences.
Rules: no jargon, lead with narrative not numbers, accessible to someone who just started watching LCS.`,
      messages: [{
        role: "user",
        content: `Player: ${name}
Role: ${role} for ${team}
Stats this split: KDA ${stats.kda}, Record ${stats.record}, Recent form: ${stats.recentForm.join(" ")}
Broadcast signals:\n${signalText}

Write the narrative.`
      }]
    })
  });

  const data = await response.json();
  return data.content[0].text;
}
```

## Team Narrative
```javascript
async function generateTeamNarrative({ team, record, roster, signals }) {
  const rosterText = roster
    .map(p => `${p.name} (${p.role}) — KDA ${p.kda}`)
    .join("\n");

  const signalText = signals
    .map(s => `[${s.sentiment}] "${s.context}"`)
    .join("\n");

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      model: "claude-sonnet-4-20250514",
      max_tokens: 500,
      system: `You write team narratives for Scout.gg, an LCS companion for casual viewers.
Tone: accessible, story-driven. This is about the collective — the team's story, not individual stats.
Length: exactly 2–3 sentences.
Rules: explain why the record means what it means, surface the team's arc this split, give a casual viewer a reason to watch them.`,
      messages: [{
        role: "user",
        content: `Team: ${team}
Current record: ${record}
Roster:\n${rosterText}
Broadcast signals:\n${signalText}

Write the team narrative.`
      }]
    })
  });

  const data = await response.json();
  return data.content[0].text;
}
```

## After Generation
Always persist the result:
```javascript
await db.narratives.upsert({
  entityId,         // player or team ID
  entityType,       // "player" | "team"
  text,             // generated narrative
  generatedAt: new Date().toISOString(),
  signalCount: signals.length
});
```

## Rules
- Never generate on page load — always serve from cache, trigger regeneration from pipeline events
- Log a warning if signals array is empty — narrative will be weaker without broadcast context
- Exponential backoff on 529 (API overloaded): wait 2s, 4s, 8s before failing
