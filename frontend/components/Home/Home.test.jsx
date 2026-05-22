import { render, screen } from '@testing-library/react'
import { vi, beforeEach } from 'vitest'
import Home from './index.jsx'

vi.mock('../../lib/hooks.js', () => ({
  useTeams: vi.fn(),
  useFeaturedPlayers: vi.fn(),
}))

import { useTeams, useFeaturedPlayers } from '../../lib/hooks.js'

const mockTeams = [
  { id: 'fly', name: 'FlyQuest', abbreviation: 'FLY', record: { wins: 11, losses: 3 }, narrativeHook: 'Top team.' },
  { id: 'c9', name: 'Cloud9', abbreviation: 'C9', record: { wins: 10, losses: 4 }, narrativeHook: 'Contenders.' },
  { id: 'dig', name: 'Dignitas', abbreviation: 'DIG', record: { wins: 5, losses: 9 }, narrativeHook: 'Rebuilding.' },
]

const mockFeatured = [
  { id: 'inspired', name: 'Inspired', role: 'Jungle', team: 'FlyQuest', narrative: 'Best jungler.' },
]

beforeEach(() => {
  useTeams.mockReturnValue({ data: mockTeams })
  useFeaturedPlayers.mockReturnValue({ data: mockFeatured })
})

describe('Home', () => {
  it('renders featured players', () => {
    render(<Home />)
    expect(screen.getByText('Inspired')).toBeInTheDocument()
    expect(screen.getByText('Best jungler.')).toBeInTheDocument()
  })

  it('renders all teams', () => {
    render(<Home />)
    expect(screen.getAllByText('FlyQuest').length).toBeGreaterThan(0)
    expect(screen.getByText('Cloud9')).toBeInTheDocument()
    expect(screen.getByText('Dignitas')).toBeInTheDocument()
  })

  it('renders teams sorted by win rate descending', () => {
    render(<Home />)
    const abbrs = screen.getAllByText(/^(FLY|C9|DIG)$/).map((el) => el.textContent)
    expect(abbrs).toEqual(['FLY', 'C9', 'DIG'])
  })

  it('renders section labels', () => {
    render(<Home />)
    expect(screen.getByText(/worth watching/i)).toBeInTheDocument()
    expect(screen.getByText(/lcs standings/i)).toBeInTheDocument()
  })
})
