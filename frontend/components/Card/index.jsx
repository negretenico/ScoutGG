import Link from 'next/link'
import clsx from 'clsx'
import styles from './Card.module.css'

export function Card({ className, children, href }) {
  return (
    <Link href={href} className={clsx(styles.card, className)}>
      {children}
    </Link>
  )
}
