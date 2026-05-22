'use client'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useTeam } from '../../lib/hooks.js'
import styles from './TeamPage.module.css'

const ROLE_ORDER = ['Top', 'Jungle', 'Mid', 'Bot', 'Support']

export default function TeamPage({ id }) {
  const router = useRouter()
  const { data: team } = useTeam(id)

  const sorted = [...(team.roster ?? [])].sort(
    (a, b) => ROLE_ORDER.indexOf(a.role) - ROLE_ORDER.indexOf(b.role)
  )

  return (
    <>
      <button type="button" className={styles.back} onClick={() => router.back()}>← Back</button>

      <div className={styles.header}>
        <div className={styles.name}>{team.name}</div>
        <div className={styles.meta}>{team.record.wins}W – {team.record.losses}L · LCS {new Date().getFullYear()}</div>
      </div>

      <p className={styles.narrative}>{team.narrativeHook}</p>

      {team.unsungHero && (
        <div className={styles.unsungHero}>
          <div className={styles.unsungLabel}>Unsung hero</div>
          <div className={styles.unsungText}>
            <strong>{team.unsungHero.name}</strong> — {team.unsungHero.reason}
          </div>
        </div>
      )}

      {sorted.length > 0 && (
        <>
          <div className={styles.rosterLabel}>Roster</div>
          <div className={styles.rosterGrid}>
            {sorted.map((p) => (
              <Link key={p.id} href={`/players/${p.id}`} className={styles.rosterItem}>
                <div className={styles.rosterName}>{p.name}</div>
                <div className={styles.rosterRole}>{p.role}</div>
              </Link>
            ))}
          </div>
        </>
      )}
    </>
  )
}
