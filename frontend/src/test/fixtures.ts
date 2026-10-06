import type { SearchResult } from '../lib/types'

export const result: SearchResult = {
  total: 2, page: 0, size: 12, totalPages: 1, tookMs: 7,
  hits: [
    {
      product: { id: 'P1', name: 'Auralis Wireless Headphones', description: 'Deep bass.', brand: 'Auralis',
        category: 'Audio', price: 129.99, rating: 4.5, reviewCount: 210, inStock: true },
      highlights: { name: ['Auralis <mark>Wireless</mark> Headphones'] },
    },
    {
      product: { id: 'P2', name: 'Sonora Earbuds', description: 'Small and light.', brand: 'Sonora',
        category: 'Audio', price: 59.0, rating: 3.9, reviewCount: 12, inStock: false },
    },
  ],
  facets: {
    category: [{ key: 'Audio', count: 2, selected: false }, { key: 'Laptops', count: 5, selected: false }],
    brand: [{ key: 'Auralis', count: 1, selected: false }, { key: 'Sonora', count: 1, selected: false }],
    price: [
      { key: 'under-50', count: 0, selected: false }, { key: '50-100', count: 1, selected: false },
      { key: '100-250', count: 1, selected: false }, { key: '250-500', count: 0, selected: false },
      { key: '500-1000', count: 0, selected: false }, { key: '1000-plus', count: 0, selected: false },
    ],
    availability: [{ key: 'in-stock', count: 1, selected: false }, { key: 'out-of-stock', count: 1, selected: false }],
  },
}
