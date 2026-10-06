import { pageWindow } from '../lib/pageWindow'

interface Props {
  page: number
  totalPages: number
  onPage: (p: number) => void
}

export function Pagination({ page, totalPages, onPage }: Props) {
  if (totalPages <= 1) return null
  return (
    <nav className="pagination" aria-label="Pagination">
      <button disabled={page === 0} onClick={() => onPage(page - 1)}>‹ Prev</button>
      {pageWindow(page, totalPages).map((p, i) =>
        p === '…'
          ? <span key={`e${i}`} className="ellipsis">…</span>
          : <button key={p} aria-current={p === page ? 'page' : undefined} onClick={() => onPage(p)}>{p + 1}</button>,
      )}
      <button disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>Next ›</button>
    </nav>
  )
}
