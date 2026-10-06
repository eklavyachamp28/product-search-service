import { useEffect, useReducer } from 'react'
import { FacetPanel } from './components/FacetPanel'
import { Pagination } from './components/Pagination'
import { ProductCard } from './components/ProductCard'
import { SearchBar } from './components/SearchBar'
import { PRICE_BANDS, activeFilterCount, fromUrl, searchReducer, toUrl } from './lib/searchState'
import type { SortOption } from './lib/types'
import { useSearch } from './lib/useSearch'

const SORT_LABELS: Record<SortOption, string> = {
  relevance: 'Best match', price_asc: 'Price: low to high', price_desc: 'Price: high to low',
  rating: 'Top rated', newest: 'Newest',
}

export default function App() {
  const [state, dispatch] = useReducer(searchReducer, undefined, () => fromUrl(window.location.search))
  const { result, loading, error } = useSearch(state)

  // keep the address bar in sync so a search can be bookmarked or shared
  useEffect(() => {
    const url = `${window.location.pathname}${toUrl(state)}`
    if (url !== `${window.location.pathname}${window.location.search}`) window.history.pushState(null, '', url)
  }, [state])

  useEffect(() => {
    const onPop = () => dispatch({ type: 'replace', state: fromUrl(window.location.search) })
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  const chips = [
    ...state.categories.map(v => ({ label: v, clear: () => dispatch({ type: 'toggleCategory', value: v }) })),
    ...state.brands.map(v => ({ label: v, clear: () => dispatch({ type: 'toggleBrand', value: v }) })),
    ...(state.priceBand ? [{ label: PRICE_BANDS[state.priceBand].label, clear: () => dispatch({ type: 'priceBand', value: null }) }] : []),
    ...(state.inStock ? [{ label: 'In stock', clear: () => dispatch({ type: 'inStock', value: null }) }] : []),
  ]

  return (
    <div className="app">
      <header className="topbar">
        <a className="logo" href="/">Catalog<span>Search</span></a>
        <SearchBar value={state.q} onSearch={q => dispatch({ type: 'query', q })} />
      </header>

      <div className="layout">
        <FacetPanel facets={result?.facets} state={state} dispatch={dispatch} />

        <main>
          <div className="results-head">
            <p className="summary" aria-live="polite">
              {result && (
                <>
                  <strong>{result.total.toLocaleString()}</strong> {result.total === 1 ? 'result' : 'results'}
                  {state.q && <> for “{state.q}”</>}
                  <span className="took"> · {result.tookMs} ms</span>
                </>
              )}
            </p>
            <label className="sort">
              Sort by{' '}
              <select value={state.sort} onChange={e => dispatch({ type: 'sort', value: e.target.value as SortOption })}>
                {Object.entries(SORT_LABELS).map(([k, label]) => <option key={k} value={k}>{label}</option>)}
              </select>
            </label>
          </div>

          {chips.length > 0 && (
            <div className="chips">
              {chips.map(c => (
                <button key={c.label} className="chip" onClick={c.clear} aria-label={`Remove filter ${c.label}`}>{c.label} ✕</button>
              ))}
              {activeFilterCount(state) > 1 && (
                <button className="chip clear" onClick={() => dispatch({ type: 'clearFilters' })}>Clear all</button>
              )}
            </div>
          )}

          {error && <p className="error" role="alert">{error}</p>}

          <section className={`grid${loading ? ' loading' : ''}`} aria-busy={loading}>
            {result?.hits.map(h => <ProductCard key={h.product.id} hit={h} />)}
          </section>

          {result && result.total === 0 && !loading && (
            <div className="empty-state">
              <p>No products match{state.q ? <> “{state.q}”</> : null}{activeFilterCount(state) > 0 ? ' with these filters' : ''}.</p>
              {activeFilterCount(state) > 0 && <button onClick={() => dispatch({ type: 'clearFilters' })}>Clear filters</button>}
            </div>
          )}

          {result && (
            <Pagination page={state.page} totalPages={result.totalPages}
                        onPage={p => { dispatch({ type: 'page', value: p }); window.scrollTo({ top: 0, behavior: 'smooth' }) }} />
          )}
        </main>
      </div>
    </div>
  )
}
