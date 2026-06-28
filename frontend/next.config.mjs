/** @type {import('next').NextConfig} */
const isStaticExport = process.env.NEXT_PUBLIC_STATIC_EXPORT === 'true'
const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ''

const nextConfig = {
  ...(isStaticExport && { output: 'export' }),
  basePath,
  assetPrefix: basePath || undefined,

  ...(!isStaticExport && {
    async rewrites() {
      return [
        {
          source: '/api/:path*',
          destination: 'http://localhost:8082/:path*',
        },
      ]
    },
  }),
}

export default nextConfig
