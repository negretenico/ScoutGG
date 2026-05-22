import './globals.css'
import { MSWProvider } from '../components/MSWProvider.jsx'
import { QueryProvider } from '../components/QueryProvider.jsx'
import Nav from '../components/Nav/index.jsx'

export const metadata = { title: 'Scout.gg' }

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
