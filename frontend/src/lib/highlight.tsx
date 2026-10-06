import { Fragment, type ReactNode } from 'react'

const NAMED: Record<string, string> = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" }

/** Decodes the entities Elasticsearch's html highlight encoder produces. */
export function decodeEntities(s: string): string {
  return s.replace(/&(#x[0-9a-f]+|#\d+|[a-z]+);/gi, (m, code: string) => {
    if (code[0] === '#') {
      const n = code[1].toLowerCase() === 'x' ? parseInt(code.slice(2), 16) : parseInt(code.slice(1), 10)
      return Number.isFinite(n) ? String.fromCodePoint(n) : m
    }
    return NAMED[code.toLowerCase()] ?? m
  })
}

/**
 * Turns an HTML-encoded highlight fragment like "Auralis &lt;Pro&gt; <mark>Headphones</mark>" into React nodes.
 * Only <mark> is treated as markup; everything else becomes text, so there is no innerHTML and no XSS surface.
 */
export function renderHighlight(fragment: string): ReactNode {
  const parts = fragment.split(/(<mark>.*?<\/mark>)/g).filter(Boolean)
  return parts.map((part, i) => {
    const m = /^<mark>(.*?)<\/mark>$/.exec(part)
    return m
      ? <mark key={i}>{decodeEntities(m[1])}</mark>
      : <Fragment key={i}>{decodeEntities(part)}</Fragment>
  })
}
