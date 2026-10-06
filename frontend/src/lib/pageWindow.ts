/** Shows first, last, and a window around the current page. */
export function pageWindow(page: number, totalPages: number): (number | '…')[] {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i)
  const out: (number | '…')[] = [0]
  const start = Math.max(1, page - 1)
  const end = Math.min(totalPages - 2, page + 1)
  if (start > 1) out.push('…')
  for (let i = start; i <= end; i++) out.push(i)
  if (end < totalPages - 2) out.push('…')
  out.push(totalPages - 1)
  return out
}
