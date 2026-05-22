export default function Link({ href, children, className, onClick }) {
  return (
    <a href={href} className={className} onClick={onClick}>
      {children}
    </a>
  )
}
