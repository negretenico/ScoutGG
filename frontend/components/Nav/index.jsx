'use client'
import { useState, useRef, useEffect } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useSearch } from '../../lib/hooks.js'
import styles from './Nav.module.css'

export default function Nav() {
  const [query, setQuery] = useState('')
  const ref = useRef(null)
  const router = useRouter()

  const { data } = useSearch(query)
  const results = [
    ...(data?.players ?? []).map((p) => ({ type: 'player', id: p.id, label: p.name, sub: p.role })),
    ...(data?.teams ?? []).map((t) => ({ type: 'team', id: t.id, label: t.name, sub: t.abbreviation })),
  ].slice(0, 6)

  const isOpen = query.trim().length > 1 && results.length > 0

  useEffect(() => {
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setQuery('')
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  function select(result) {
    setQuery('')
    router.push(result.type === 'team' ? `/teams/${result.id}` : `/players/${result.id}`)
  }

  return (
    <nav className={styles.nav}>
      <div className={styles.inner}>
        <Link href="/" className={styles.logo}>Scout.gg</Link>
        <div className={styles.search} ref={ref}>
          <input
            className={styles.input}
            type="text"
            placeholder="Search players or teams..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          {isOpen && (
            <div className={styles.dropdown}>
              {results.map((r) => (
                <button type="button" key={`${r.type}-${r.id}`} className={styles.result} onMouseDown={() => select(r)}>
                  <span>{r.label}</span>
                  <span className={styles.resultSub}>{r.sub}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </nav>
  )
}
