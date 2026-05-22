'use client'
import { useRouter } from 'next/navigation'
import { usePlayer } from '../../lib/hooks.js'
import styles from './PlayerProfile.module.css'

export default function PlayerProfile({ id }) {
  const router = useRouter()
  const { data: player } = usePlayer(id)
  const { wins, losses, kda, recentForm } = player.stats

  return (
    <>
      <button type="button" className={styles.back} onClick={() => router.back()}>← Back</button>

      <div className={styles.header}>
        <div className={styles.name}>{player.name}</div>
        <div className={styles.meta}>{player.role} · {player.team}</div>
      </div>

      <p className={styles.narrative}>{player.narrative}</p>

      <div className={styles.statsGrid}>
        <div className={styles.statBlock}>
          <div className={styles.statLabel}>KDA</div>
          <div className={styles.statValue}>{kda}</div>
        </div>
        <div className={styles.statBlock}>
          <div className={styles.statLabel}>Record</div>
          <div className={styles.statValue}>{wins}–{losses}</div>
        </div>
        <div className={styles.statBlock}>
          <div className={styles.statLabel}>Win rate</div>
          <div className={styles.statValue}>{Math.round((wins / (wins + losses)) * 100)}%</div>
        </div>
        {recentForm && (
          <div className={styles.statBlock}>
            <div className={styles.statLabel}>Recent form</div>
            <div className={styles.formDots}>
              {recentForm.map((r, i) => (
                <div key={i} className={`${styles.dot} ${r === 'W' ? styles.dotW : styles.dotL}`} />
              ))}
            </div>
          </div>
        )}
      </div>
    </>
  )
}
