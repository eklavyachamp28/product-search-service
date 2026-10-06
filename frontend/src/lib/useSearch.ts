import { useEffect, useState } from 'react'
import { searchProducts } from './api'
import type { SearchState } from './searchState'
import type { SearchResult } from './types'

interface SearchStatus {
  result: SearchResult | null
  loading: boolean
  error: string | null
}

/**
 * Runs a search whenever the state changes. Each run aborts the previous request, so a slow
 * response for an old query can never overwrite the results of a newer one.
 */
export function useSearch(state: SearchState): SearchStatus {
  const [status, setStatus] = useState<SearchStatus>({ result: null, loading: true, error: null })

  useEffect(() => {
    const ctrl = new AbortController()
    setStatus(s => ({ ...s, loading: true, error: null }))
    searchProducts(state, ctrl.signal)
      .then(result => setStatus({ result, loading: false, error: null }))
      .catch((e: unknown) => {
        if (ctrl.signal.aborted) return
        setStatus(s => ({ ...s, loading: false, error: e instanceof Error ? e.message : 'Search failed' }))
      })
    return () => ctrl.abort()
  }, [state])

  return status
}
