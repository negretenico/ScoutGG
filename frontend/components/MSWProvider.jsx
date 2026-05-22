'use client'
import { useEffect } from 'react'

let workerStarted = false

export function MSWProvider({ children }) {
  useEffect(() => {
    if (workerStarted || process.env.NODE_ENV !== 'development') return
    workerStarted = true
    import('../mocks/browser.js').then(({ worker }) =>
      worker.start({ onUnhandledRequest: 'bypass' })
    )
  }, [])

  return children
}
