// In local dev: NEXT_PUBLIC_API_URL is unset, Next.js rewrites /api/* → localhost:8082/*
// In static export (GitHub Pages): NEXT_PUBLIC_API_URL points to the deployed server module
const BASE = process.env.NEXT_PUBLIC_API_URL ?? '/api'

async function get(path) {
  const res = await fetch(`${BASE}${path}`)
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
