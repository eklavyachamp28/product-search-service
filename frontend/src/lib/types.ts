export interface Product {
  id: string
  name: string
  description?: string
  brand: string
  category: string
  price: number
  rating?: number
  reviewCount?: number
  inStock: boolean
  tags?: string[]
  releasedOn?: string
}

export interface Hit {
  product: Product
  score?: number
  highlights?: Record<string, string[]>
}

export interface FacetBucket {
  key: string
  count: number
  selected: boolean
}

export interface SearchResult {
  total: number
  page: number
  size: number
  totalPages: number
  tookMs: number
  hits: Hit[]
  facets: Record<'category' | 'brand' | 'price' | 'availability', FacetBucket[]>
}

export interface Suggestion {
  id: string
  name: string
  category: string
}

export type SortOption = 'relevance' | 'price_asc' | 'price_desc' | 'rating' | 'newest'
