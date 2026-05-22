'use client'
import styles from './ErrorState.module.css'

export default function ErrorState({ error, reset }) {
  return (
    <div className={styles.root}>
      <p className={styles.message}>{error?.message ?? 'Something went wrong.'}</p>
      {reset && (
        <button type="button" className={styles.retry} onClick={reset}>
          Try again
        </button>
      )}
    </div>
  )
}
