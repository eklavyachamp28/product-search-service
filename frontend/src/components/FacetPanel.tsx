import type { Dispatch } from 'react'
import { PRICE_BANDS, type Action, type SearchState } from '../lib/searchState'
import type { FacetBucket, SearchResult } from '../lib/types'

interface Props {
  facets: SearchResult['facets'] | undefined
  state: SearchState
  dispatch: Dispatch<Action>
}

function CheckboxGroup({ title, buckets, onToggle }: { title: string; buckets: FacetBucket[]; onToggle: (k: string) => void }) {
  if (buckets.length === 0) return null
  return (
    <fieldset className="facet">
      <legend>{title}</legend>
      {buckets.map(b => (
        <label key={b.key} className={b.count === 0 && !b.selected ? 'empty' : undefined}>
          <input type="checkbox" checked={b.selected} disabled={b.count === 0 && !b.selected} onChange={() => onToggle(b.key)} />
          <span className="facet-label">{b.key}</span>
          <span className="count">{b.count}</span>
        </label>
      ))}
    </fieldset>
  )
}

export function FacetPanel({ facets, state, dispatch }: Props) {
  if (!facets) return <aside className="facets" aria-busy="true" />
  const inStock = facets.availability.find(b => b.key === 'in-stock')
  return (
    <aside className="facets" aria-label="Filters">
      <CheckboxGroup title="Category" buckets={facets.category} onToggle={v => dispatch({ type: 'toggleCategory', value: v })} />
      <CheckboxGroup title="Brand" buckets={facets.brand} onToggle={v => dispatch({ type: 'toggleBrand', value: v })} />
      <fieldset className="facet">
        <legend>Price</legend>
        {facets.price.map(b => (
          <label key={b.key} className={b.count === 0 && !b.selected ? 'empty' : undefined}>
            <input
              type="radio"
              name="price"
              checked={state.priceBand === b.key}
              disabled={b.count === 0 && state.priceBand !== b.key}
              onClick={() => dispatch({ type: 'priceBand', value: b.key })}
              onChange={() => { /* handled on click so a second click clears it */ }}
            />
            <span className="facet-label">{PRICE_BANDS[b.key]?.label ?? b.key}</span>
            <span className="count">{b.count}</span>
          </label>
        ))}
      </fieldset>
      <fieldset className="facet">
        <legend>Availability</legend>
        <label>
          <input type="checkbox" checked={state.inStock === true}
                 onChange={e => dispatch({ type: 'inStock', value: e.target.checked ? true : null })} />
          <span className="facet-label">In stock only</span>
          <span className="count">{inStock?.count ?? 0}</span>
        </label>
      </fieldset>
    </aside>
  )
}
