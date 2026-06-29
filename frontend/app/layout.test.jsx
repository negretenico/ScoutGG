import { metadata } from './layout.jsx'

describe('metadata', () => {
  it('should_include_site_title', () => {
    expect(metadata.title).toBe('Scout.gg')
  })

  it('should_include_description', () => {
    expect(metadata.description).toBeTruthy()
  })

  it('should_include_author', () => {
    expect(metadata.authors).toEqual(
      expect.arrayContaining([expect.objectContaining({ name: 'negretenico' })])
    )
  })

  it('should_include_open_graph_metadata', () => {
    expect(metadata.openGraph).toMatchObject({
      title: 'Scout.gg',
      type: 'website',
    })
  })

  it('should_include_repository_link', () => {
    expect(metadata.repository).toContain('github.com/negretenico/ScoutGG')
  })
})
