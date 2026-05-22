import dynamic from 'next/dynamic'

const PlayerProfile = dynamic(() => import('../../../components/PlayerProfile/index.jsx'), { ssr: false })

export default function Page({ params }) {
  return <PlayerProfile id={params.id} />
}
