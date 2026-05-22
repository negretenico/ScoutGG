import { teamHandlers } from './handlers/teams/index.js'
import { playerHandlers } from './handlers/players/index.js'
import { searchHandlers } from './handlers/search/index.js'

export const handlers = [
  ...teamHandlers,
  ...playerHandlers,
  ...searchHandlers,
]
