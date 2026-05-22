'use client'
import { useTeams, useFeaturedPlayers } from '../../lib/hooks.js'
import { Card } from '../Card/index.jsx'
import styles from './Home.module.css'

function TeamCard({ team }) {
  return (
    <Card href={`/teams/${team.id}`}>
      <div className={styles.teamCardInner}>
        <div className={styles.teamAbbr}>{team.abbreviation}</div>
        <div>
          <div className={styles.teamName}>{team.name}</div>
          <div className={styles.teamRecord}>{team.record.wins}W – {team.record.losses}L</div>
          <div className={styles.teamHook}>{team.narrativeHook}</div>
        </div>
      </div>
    </Card>
  )
}

function PlayerCard({ player }) {
  return (
    <Card href={`/players/${player.id}`}>
      <div className={styles.playerHeader}>
        <span className={styles.playerName}>{player.name}</span>
        <span className={styles.playerRole}>{player.role}</span>
      </div>
      <div className={styles.playerTeam}>{player.team}</div>
      <div className={styles.playerNarrative}>{player.narrative}</div>
    </Card>
  )
}

export default function Home() {
  const { data: teams } = useTeams()
  const { data: featured } = useFeaturedPlayers()

  const sorted = [...teams].sort(
    (a, b) =>
      b.record.wins / (b.record.wins + b.record.losses) -
      a.record.wins / (a.record.wins + a.record.losses)
  )

  return (
    <>
      <section className={styles.section}>
        <div className={styles.sectionLabel}>Worth watching this week</div>
        <div className={styles.featuredGrid}>
          {featured.map((p) => <PlayerCard key={p.id} player={p} />)}
        </div>
      </section>

      <section className={styles.section}>
        <div className={styles.sectionLabel}>LCS standings</div>
        <div className={styles.teamsGrid}>
          {sorted.map((t) => <TeamCard key={t.id} team={t} />)}
        </div>
      </section>
    </>
  )
}
