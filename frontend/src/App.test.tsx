import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { result } from './test/fixtures'

function mockFetch() {
  return vi.fn(async (url: string) => {
    const body = url.startsWith('/api/products/suggest')
      ? [{ id: 'P1', name: 'Auralis Wireless Headphones', category: 'Audio' }]
      : result
    return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
  })
}

describe('App', () => {
  let fetchMock: ReturnType<typeof mockFetch>

  beforeEach(() => {
    window.history.replaceState(null, '', '/')
    fetchMock = mockFetch()
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  const searchCalls = () => fetchMock.mock.calls.map(c => String(c[0])).filter(u => u.includes('/search'))

  it('renders results with highlights and facet counts', async () => {
    render(<App />)
    expect(await screen.findByText('Wireless', { selector: 'mark' })).toBeInTheDocument()
    expect(screen.getByText('Out of stock')).toBeInTheDocument()
    const category = screen.getByRole('group', { name: 'Category' })
    expect(within(category).getByText('Laptops').nextSibling).toHaveTextContent('5')
    expect(screen.getByText(/results/)).toHaveTextContent('2 results')
  })

  it('selecting a facet re-queries and updates the URL', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Wireless', { selector: 'mark' })
    await user.click(within(screen.getByRole('group', { name: 'Brand' })).getByLabelText(/Auralis/))
    await waitFor(() => expect(searchCalls().at(-1)).toContain('brand=Auralis'))
    expect(window.location.search).toBe('?brand=Auralis')
    expect(screen.getByRole('button', { name: 'Remove filter Auralis' })).toBeInTheDocument()
  })

  it('price radio maps to min/max price', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByText('Wireless', { selector: 'mark' })
    await user.click(screen.getByLabelText(/\$100 – \$250/))
    await waitFor(() => expect(searchCalls().at(-1)).toMatch(/minPrice=100&maxPrice=250/))
  })

  it('autocomplete suggestions can be picked with the keyboard', async () => {
    const user = userEvent.setup()
    render(<App />)
    const box = screen.getByRole('combobox', { name: 'Search products' })
    await user.type(box, 'aur')
    expect(await screen.findByRole('option', { name: /Auralis Wireless Headphones/ })).toBeInTheDocument()
    await user.keyboard('{ArrowDown}{Enter}')
    await waitFor(() => expect(searchCalls().at(-1)).toContain('q=Auralis+Wireless+Headphones'))
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('shows the API error message', async () => {
    fetchMock.mockImplementation(async () =>
      new Response(JSON.stringify({ detail: 'Search backend is unavailable' }), { status: 503 }))
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Search backend is unavailable')
  })
})
