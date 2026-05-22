import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.js'],
    css: {
      modules: { classNameStrategy: 'non-scoped' },
    },
  },
  resolve: {
    alias: {
      'next/navigation': path.resolve('./tests/mocks/next-navigation.js'),
      'next/link': path.resolve('./tests/mocks/next-link.jsx'),
    },
  },
})
