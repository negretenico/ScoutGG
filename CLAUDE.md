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

## Testing Conventions

### TDD Workflow

- Always write failing tests BEFORE implementation
- Use AAA pattern: Arrange-Act-Assert
- One assertion per test when possible
- Test names describe business intent: "should_return_empty_when_no_players_are_available"

### Test-First Rules

- When I ask for a feature, write tests first
- Tests should FAIL initially (no implementation exists)
- Only after tests are written, implement minimal code to pass

## Commands

All commands go through `make`. The Makefile is the single source of truth for both CI and local dev.

```bash
# Install deps (run once per module)
make setup-frontend      # bun install
make setup-ai            # python venv + pip
make setup-broadcast     # python venv + pip

# Dev servers
make dev-frontend
make dev-riot
make dev-server
make dev-ai
make dev-broadcast

# Build all modules
make build

# Test all modules
make test

# Per-module
make build-riot | build-server | build-ai | build-broadcast | build-frontend
make test-riot  | test-server  | test-ai  | test-broadcast  | test-frontend
```

**Frontend uses Bun** (not npm). `bun.lockb` is committed, `package-lock.json` is gitignored.

## CI/CD Conventions

Reusable workflows live in `negretenico/GithubWorkflows`. CI files in this repo are callers only.

### Rules
- **Never add `environment:` to jobs that use `uses:`** — GitHub Actions doesn't support it; causes instant `startup_failure`.
- **Boolean inputs must be unquoted** — `skip_dist: true`, not `skip_dist: 'true'`.
- **`mvn deploy` is opt-in** — pass `publish: true` to `java_maven_build.yaml` only when you want to publish to GitHub Packages. Default is `false`.
- **Maven cross-repo auth** — add a `<repository id="github">` pointing to `maven.pkg.github.com/negretenico/ScoutGG` in any pom.xml that pulls from GitHub Packages. `setup-java` configures credentials for server id `github`; GitHub resolves packages cross-repo under the same owner.
- **Python services set `skip_dist: true`** — they're deployed as Docker images, not packages.
- **Build Gate** (`build-gate.yml`) is the single required check for branch protection. It passes on `pull_request` and re-runs via `workflow_run` when module CIs complete.
- **`workflow_run` only fires from the default branch** — build-gate won't work on feature branches until merged to main.

### GitHub Pages
- Deployed via `peaceiris/actions-gh-pages@v4` with `keep_files: true`.
- `develop` → `/dev` subfolder, `main` → `/prod` subfolder.
- Both coexist on the `gh-pages` branch.

## Agents

Specialized subagents in `.claude/agents/`. Invoke by name or with `/`:

| Agent | When to use |
|---|---|
| `/product-guardian` | Scope decisions, MVP vs post-MVP, anything that risks feature creep |
| `/api-specialist` | Riot API, Claude API, Whisper, YouTube/Twitch — rate limits, auth, gotchas |
| `/ux-guardian` | Components, layouts, interaction design — enforces "one story at a time" on every pixel |

## Skills

Reusable slash commands in `.claude/skills/`. Invoke with `/`:

| Skill | When to use |
|---|---|
| `/create-github-issues` | Create GitHub issues from the roadmap file |
| `/generate-narrative` | Generate a player or team narrative via the Claude API |
| `/riot-api-query` | Fetch player stats, match history, rosters, standings from Riot/LoL Esports API |
| `/grill-me` | Stress-test a plan or design with relentless questioning |

## External APIs

- Riot Games API — match data, player stats, team records
- Anthropic Claude API (`claude-sonnet-4-20250514`) — narrative generation
- YouTube Data API v3 + `yt-dlp` — live stream ingestion
- OpenAI Whisper API — audio transcription
