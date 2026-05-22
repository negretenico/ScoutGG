import { vi } from 'vitest'

export const useRouter = vi.fn(() => ({
  push: vi.fn(),
  back: vi.fn(),
  replace: vi.fn(),
}))

export const useParams = vi.fn(() => ({}))
export const usePathname = vi.fn(() => '/')
export const useSearchParams = vi.fn(() => new URLSearchParams())
