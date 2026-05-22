import { http, HttpResponse } from 'msw'
import players from '../../fixtures/players.json'

const FEATURED_IDS = ['inspired', 'blaber', 'doublelift']

export const playerHandlers = [
  http.get('/api/players/featured', () => {
    const featured = FEATURED_IDS.map((id) => players.find((p) => p.id === id)).filter(Boolean)
    return HttpResponse.json(featured)
  }),

  http.get('/api/players/:id', ({ params }) => {
    const player = players.find((p) => p.id === params.id)
    if (!player) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(player)
  }),
]
