import { http, HttpResponse } from 'msw'
import teams from '../../fixtures/teams.json'
import players from '../../fixtures/players.json'

export const teamHandlers = [
  http.get('/api/teams', () => {
    return HttpResponse.json(teams)
  }),

  http.get('/api/teams/:id', ({ params }) => {
    const team = teams.find((t) => t.id === params.id)
    if (!team) return new HttpResponse(null, { status: 404 })

    const roster = team.roster.map((member) => players.find((p) => p.id === member.id) ?? member)
    const unsungPlayer = players.find((p) => p.id === team.unsungHero?.playerId)

    return HttpResponse.json({
      ...team,
      roster,
      unsungHero: unsungPlayer ? { ...unsungPlayer, reason: team.unsungHero.reason } : null,
    })
  }),
]
