import { render, screen } from '@testing-library/react'
import { Card } from './index.jsx'

describe('Card', () => {
  it('renders as an anchor with the correct href', () => {
    render(<Card href="/teams/c9">Cloud9</Card>)
    const link = screen.getByRole('link', { name: 'Cloud9' })
    expect(link).toHaveAttribute('href', '/teams/c9')
  })

  it('renders children', () => {
    render(<Card href="/players/blaber"><span>Blaber</span></Card>)
    expect(screen.getByText('Blaber')).toBeInTheDocument()
  })

  it('applies additional className alongside the base style', () => {
    render(<Card href="/" className="extra">content</Card>)
    const link = screen.getByRole('link')
    expect(link.classList).toContain('extra')
  })
})
