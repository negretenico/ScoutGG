async function get(path) {
  const res = await fetch(`/api${path}`)
  if (!res.ok) throw new Error(`${res.status} ${path}`)
  return res.json()
}

export const api = {
  teams: () => get('/teams'),
  team: (id) => get(`/teams/${id}`),
  featuredPlayers: () => get('/players/featured'),
  player: (id) => get(`/players/${id}`),
  search: (q) => get(`/search?q=${encodeURIComponent(q)}`),
}
