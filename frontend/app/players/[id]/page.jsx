import dynamic from 'next/dynamic'

export function generateStaticParams() { return [{ id: '_' }] }

const PlayerProfile = dynamic(() => import('../../../components/PlayerProfile/index.jsx'), { ssr: false })

export default function Page({ params }) {
  return <PlayerProfile id={params.id} />
}
