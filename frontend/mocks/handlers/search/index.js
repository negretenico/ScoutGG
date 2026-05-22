import { http, HttpResponse } from 'msw'
import teams from '../../fixtures/teams.json'
import players from '../../fixtures/players.json'

export const searchHandlers = [
  http.get('/api/search', ({ request }) => {
    const q = new URL(request.url).searchParams.get('q')?.toLowerCase() ?? ''
    if (!q) return HttpResponse.json({ players: [], teams: [] })

    return HttpResponse.json({
      players: players.filter((p) => p.name.toLowerCase().includes(q)),
      teams: teams.filter((t) => t.name.toLowerCase().includes(q) || t.abbreviation.toLowerCase().includes(q)),
    })
  }),
]
