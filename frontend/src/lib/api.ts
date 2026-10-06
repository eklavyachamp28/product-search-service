import type { SearchResult, Suggestion } from './types'
import { toApiParams, type SearchState } from './searchState'

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message)
  }
}

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(url, { signal, headers: { Accept: 'application/json' } })
  if (!res.ok) {
    let detail = `Request failed (${res.status})`
    try {
      const body = await res.json()
      if (body?.detail) detail = body.detail
    } catch { /* not JSON */ }
    throw new ApiError(res.status, detail)
  }
  return res.json() as Promise<T>
}

export const PAGE_SIZE = 12

export function searchProducts(state: SearchState, signal?: AbortSignal): Promise<SearchResult> {
  return getJson<SearchResult>(`/api/products/search?${toApiParams(state, PAGE_SIZE)}`, signal)
}

export function suggestProducts(prefix: string, signal?: AbortSignal): Promise<Suggestion[]> {
  const p = new URLSearchParams({ prefix, size: '6' })
  return getJson<Suggestion[]>(`/api/products/suggest?${p}`, signal)
}
