import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { decodeEntities, renderHighlight } from './highlight'

describe('renderHighlight', () => {
  it('wraps matches in <mark> and decodes entities', () => {
    const { container } = render(<p>{renderHighlight('Pro &amp; <mark>Wireless</mark> &#39;Max&#39;')}</p>)
    expect(container.querySelector('mark')?.textContent).toBe('Wireless')
    expect(container.textContent).toBe("Pro & Wireless 'Max'")
  })

  it('never turns product text into markup', () => {
    const { container } = render(<p>{renderHighlight('&lt;img src=x onerror=alert(1)&gt; <mark>tv</mark>')}</p>)
    expect(container.querySelector('img')).toBeNull()
    expect(container.textContent).toContain('<img src=x onerror=alert(1)>')
  })

  it('decodes hex and named entities', () => {
    expect(decodeEntities('a&#x2F;b &quot;c&quot; &unknown;')).toBe('a/b "c" &unknown;')
  })
})
