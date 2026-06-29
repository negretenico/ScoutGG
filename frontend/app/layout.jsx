import './globals.css'
import { MSWProvider } from '../components/MSWProvider.jsx'
import { QueryProvider } from '../components/QueryProvider.jsx'
import Nav from '../components/Nav/index.jsx'

export const metadata = {
  title: 'Scout.gg',
  description: 'AI-generated narratives for LCS players and teams. The human story behind the standings.',
  authors: [{ name: 'negretenico', url: 'https://github.com/negretenico' }],
  creator: 'negretenico',
  metadataBase: new URL('https://negretenico.github.io/ScoutGG'),
  openGraph: {
    title: 'Scout.gg',
    description: 'AI-generated narratives for LCS players and teams.',
    url: 'https://negretenico.github.io/ScoutGG',
    siteName: 'Scout.gg',
    type: 'website',
  },
  repository: 'https://github.com/negretenico/ScoutGG',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <MSWProvider>
          <QueryProvider>
            <Nav />
            <main className="main">{children}</main>
          </QueryProvider>
        </MSWProvider>
      </body>
    </html>
  )
}
