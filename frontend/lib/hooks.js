import { useSuspenseQuery, useQuery } from '@tanstack/react-query'
import { api } from './api.js'

export function useTeams() {
  return useSuspenseQuery({ queryKey: ['teams'], queryFn: api.teams })
}

export function useTeam(id) {
  return useSuspenseQuery({ queryKey: ['teams', id], queryFn: () => api.team(id) })
}

export function useFeaturedPlayers() {
  return useSuspenseQuery({ queryKey: ['players', 'featured'], queryFn: api.featuredPlayers })
}

export function usePlayer(id) {
  return useSuspenseQuery({ queryKey: ['players', id], queryFn: () => api.player(id) })
}

// useQuery (not suspense) — conditional, only fires when query is long enough
export function useSearch(q) {
  return useQuery({
    queryKey: ['search', q],
    queryFn: () => api.search(q),
    enabled: q.trim().length > 1,
    staleTime: 30_000,
  })
}
