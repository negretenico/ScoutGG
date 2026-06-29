import dynamic from 'next/dynamic'

export function generateStaticParams() { return [] }

const TeamPage = dynamic(() => import('../../../components/TeamPage/index.jsx'), { ssr: false })

export default function Page({ params }) {
  return <TeamPage id={params.id} />
}
