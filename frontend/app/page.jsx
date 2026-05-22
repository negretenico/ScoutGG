import dynamic from 'next/dynamic'

const Home = dynamic(() => import('../components/Home/index.jsx'), { ssr: false })

export default function Page() {
  return <Home />
}
