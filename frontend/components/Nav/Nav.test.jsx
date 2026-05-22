import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, beforeEach } from 'vitest'
import { useRouter } from 'next/navigation'
import Nav from './index.jsx'

vi.mock('../../lib/hooks.js', () => ({
  useSearch: vi.fn(),
}))

import { useSearch } from '../../lib/hooks.js'

const mockResults = {
  players: [{ id: 'blaber', name: 'Blaber', role: 'Jungle' }],
  teams: [{ id: 'c9', name: 'Cloud9', abbreviation: 'C9' }],
}

beforeEach(() => {
  vi.clearAllMocks()
  useSearch.mockReturnValue({ data: null })
})

describe('Nav', () => {
  it('renders the logo', () => {
    render(<Nav />)
    expect(screen.getByText('Scout.gg')).toBeInTheDocument()
  })

  it('does not show dropdown when query is empty', () => {
    useSearch.mockReturnValue({ data: mockResults })
    render(<Nav />)
    expect(screen.queryByRole('button', { name: /Blaber/i })).not.toBeInTheDocument()
  })

  it('shows dropdown results when query length > 1', async () => {
    const user = userEvent.setup()
    useSearch.mockReturnValue({ data: mockResults })
    render(<Nav />)

    await user.type(screen.getByPlaceholderText(/search/i), 'bl')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Blaber/i })).toBeInTheDocument()
    })
  })

  it('navigates and clears query when a result is selected', async () => {
    const push = vi.fn()
    useRouter.mockReturnValue({ push, back: vi.fn() })
    const user = userEvent.setup()
    useSearch.mockReturnValue({ data: mockResults })
    render(<Nav />)

    const input = screen.getByPlaceholderText(/search/i)
    await user.type(input, 'bl')

    await waitFor(() => screen.getByRole('button', { name: /Blaber/i }))
    await user.click(screen.getByRole('button', { name: /Blaber/i }))

    expect(push).toHaveBeenCalledWith('/players/blaber')
    expect(input).toHaveValue('')
  })
})
