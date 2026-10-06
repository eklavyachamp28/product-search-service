import { describe, expect, it } from 'vitest'
import { activeFilterCount, fromUrl, initialState, searchReducer, toApiParams, toUrl, type SearchState } from './searchState'

describe('searchReducer', () => {
  it('toggles multi-select facets and resets paging', () => {
    let s: SearchState = { ...initialState, page: 3 }
    s = searchReducer(s, { type: 'toggleCategory', value: 'Audio' })
    expect(s.categories).toEqual(['Audio'])
    expect(s.page).toBe(0)
    s = searchReducer(s, { type: 'toggleCategory', value: 'Laptops' })
    s = searchReducer(s, { type: 'toggleCategory', value: 'Audio' })
    expect(s.categories).toEqual(['Laptops'])
  })

  it('clicking the selected price band again clears it', () => {
    let s = searchReducer(initialState, { type: 'priceBand', value: '50-100' })
    expect(s.priceBand).toBe('50-100')
    s = searchReducer(s, { type: 'priceBand', value: '50-100' })
    expect(s.priceBand).toBeNull()
  })

  it('clearFilters keeps the query and sort', () => {
    const s = searchReducer(
      { ...initialState, q: 'tv', sort: 'price_asc', brands: ['Vantage'], inStock: true, page: 2 },
      { type: 'clearFilters' },
    )
    expect(s).toEqual({ ...initialState, q: 'tv', sort: 'price_asc' })
    expect(activeFilterCount(s)).toBe(0)
  })
})

describe('URL and API params', () => {
  const state: SearchState = {
    q: 'wireless headphones', categories: ['Audio'], brands: ['Auralis', 'Sonora'],
    priceBand: '100-250', inStock: true, sort: 'price_desc', page: 1,
  }

  it('round-trips through the browser URL', () => {
    expect(fromUrl(toUrl(state))).toEqual(state)
    expect(toUrl(initialState)).toBe('')
  })

  it('maps price bands to min/max and repeats multi-valued params', () => {
    const p = toApiParams(state, 12)
    expect(p.getAll('brand')).toEqual(['Auralis', 'Sonora'])
    expect(p.get('minPrice')).toBe('100')
    expect(p.get('maxPrice')).toBe('250')
    expect(p.get('sort')).toBe('price_desc')
    expect(p.get('page')).toBe('1')
  })

  it('ignores junk in the URL', () => {
    const s = fromUrl('?sort=cheapest&price=free&page=-4&inStock=maybe')
    expect(s.sort).toBe('relevance')
    expect(s.priceBand).toBeNull()
    expect(s.page).toBe(0)
    expect(s.inStock).toBeNull()
  })
})
