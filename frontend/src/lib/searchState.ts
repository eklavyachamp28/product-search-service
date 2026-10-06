import type { SortOption } from './types'

/** Everything that defines the current search. Kept in the URL so results are shareable and survive refresh. */
export interface SearchState {
  q: string
  categories: string[]
  brands: string[]
  priceBand: string | null
  inStock: boolean | null
  sort: SortOption
  page: number
}

export const initialState: SearchState = {
  q: '', categories: [], brands: [], priceBand: null, inStock: null, sort: 'relevance', page: 0,
}

export type Action =
  | { type: 'query'; q: string }
  | { type: 'toggleCategory'; value: string }
  | { type: 'toggleBrand'; value: string }
  | { type: 'priceBand'; value: string | null }
  | { type: 'inStock'; value: boolean | null }
  | { type: 'sort'; value: SortOption }
  | { type: 'page'; value: number }
  | { type: 'clearFilters' }
  | { type: 'replace'; state: SearchState }

const toggle = (list: string[], v: string) => (list.includes(v) ? list.filter(x => x !== v) : [...list, v])

/** Any change except paging sends the user back to page 0, so they never land on an empty page. */
export function searchReducer(state: SearchState, action: Action): SearchState {
  switch (action.type) {
    case 'query': return { ...state, q: action.q, page: 0 }
    case 'toggleCategory': return { ...state, categories: toggle(state.categories, action.value), page: 0 }
    case 'toggleBrand': return { ...state, brands: toggle(state.brands, action.value), page: 0 }
    case 'priceBand': return { ...state, priceBand: action.value === state.priceBand ? null : action.value, page: 0 }
    case 'inStock': return { ...state, inStock: action.value, page: 0 }
    case 'sort': return { ...state, sort: action.value, page: 0 }
    case 'page': return { ...state, page: Math.max(0, action.value) }
    case 'clearFilters': return { ...initialState, q: state.q, sort: state.sort }
    case 'replace': return action.state
  }
}

export const PRICE_BANDS: Record<string, { label: string; min?: number; max?: number }> = {
  'under-50': { label: 'Under $50', max: 50 },
  '50-100': { label: '$50 – $100', min: 50, max: 100 },
  '100-250': { label: '$100 – $250', min: 100, max: 250 },
  '250-500': { label: '$250 – $500', min: 250, max: 500 },
  '500-1000': { label: '$500 – $1,000', min: 500, max: 1000 },
  '1000-plus': { label: '$1,000 and up', min: 1000 },
}

const SORTS: SortOption[] = ['relevance', 'price_asc', 'price_desc', 'rating', 'newest']

/** Parameters for GET /api/products/search. */
export function toApiParams(s: SearchState, size = 12): URLSearchParams {
  const p = new URLSearchParams()
  if (s.q.trim()) p.set('q', s.q.trim())
  s.categories.forEach(c => p.append('category', c))
  s.brands.forEach(b => p.append('brand', b))
  const band = s.priceBand ? PRICE_BANDS[s.priceBand] : undefined
  if (band?.min !== undefined) p.set('minPrice', String(band.min))
  if (band?.max !== undefined) p.set('maxPrice', String(band.max))
  if (s.inStock !== null) p.set('inStock', String(s.inStock))
  if (s.sort !== 'relevance') p.set('sort', s.sort)
  if (s.page > 0) p.set('page', String(s.page))
  p.set('size', String(size))
  return p
}

/** Browser URL <-> state. Uses the same names as the API where it can. */
export function toUrl(s: SearchState): string {
  const p = new URLSearchParams()
  if (s.q.trim()) p.set('q', s.q.trim())
  s.categories.forEach(c => p.append('category', c))
  s.brands.forEach(b => p.append('brand', b))
  if (s.priceBand) p.set('price', s.priceBand)
  if (s.inStock !== null) p.set('inStock', String(s.inStock))
  if (s.sort !== 'relevance') p.set('sort', s.sort)
  if (s.page > 0) p.set('page', String(s.page + 1))
  const qs = p.toString()
  return qs ? `?${qs}` : ''
}

export function fromUrl(search: string): SearchState {
  const p = new URLSearchParams(search)
  const sort = p.get('sort') as SortOption | null
  const price = p.get('price')
  const page = Number(p.get('page') ?? '1')
  const inStock = p.get('inStock')
  return {
    q: p.get('q') ?? '',
    categories: p.getAll('category'),
    brands: p.getAll('brand'),
    priceBand: price && price in PRICE_BANDS ? price : null,
    inStock: inStock === 'true' ? true : inStock === 'false' ? false : null,
    sort: sort && SORTS.includes(sort) ? sort : 'relevance',
    page: Number.isFinite(page) && page >= 1 ? Math.floor(page) - 1 : 0,
  }
}

export function activeFilterCount(s: SearchState): number {
  return s.categories.length + s.brands.length + (s.priceBand ? 1 : 0) + (s.inStock !== null ? 1 : 0)
}
