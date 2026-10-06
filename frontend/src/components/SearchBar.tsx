import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { suggestProducts } from '../lib/api'
import { useDebounced } from '../lib/useDebounced'
import type { Suggestion } from '../lib/types'

interface Props {
  value: string
  onSearch: (q: string) => void
}

/**
 * Search box with type-ahead suggestions. Follows the WAI-ARIA combobox pattern:
 * arrow keys move through suggestions, Enter picks one (or searches the typed text), Escape closes.
 */
export function SearchBar({ value, onSearch }: Props) {
  const [text, setText] = useState(value)
  const [suggestions, setSuggestions] = useState<Suggestion[]>([])
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const listId = useId()
  const typed = useRef(false)
  const debounced = useDebounced(text, 150)

  useEffect(() => setText(value), [value])

  useEffect(() => {
    if (!typed.current || debounced.trim().length < 2) {
      setSuggestions([])
      return
    }
    const ctrl = new AbortController()
    suggestProducts(debounced.trim(), ctrl.signal)
      .then(s => { setSuggestions(s); setOpen(s.length > 0); setActive(-1) })
      .catch(() => { if (!ctrl.signal.aborted) setSuggestions([]) })
    return () => ctrl.abort()
  }, [debounced])

  const submit = (q: string) => {
    typed.current = false
    setOpen(false)
    setActive(-1)
    setText(q)
    onSearch(q)
  }

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (!open || suggestions.length === 0) return
    if (e.key === 'ArrowDown') { e.preventDefault(); setActive(a => (a + 1) % suggestions.length) }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setActive(a => (a <= 0 ? suggestions.length - 1 : a - 1)) }
    else if (e.key === 'Escape') { setOpen(false); setActive(-1) }
    else if (e.key === 'Enter' && active >= 0) { e.preventDefault(); submit(suggestions[active].name) }
  }

  return (
    <form className="searchbar" role="search" onSubmit={(e: FormEvent) => { e.preventDefault(); submit(text) }}>
      <input
        type="search"
        aria-label="Search products"
        placeholder="Search products, brands, categories…"
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={open}
        aria-controls={listId}
        aria-activedescendant={active >= 0 ? `${listId}-${active}` : undefined}
        value={text}
        onChange={e => { typed.current = true; setText(e.target.value) }}
        onKeyDown={onKeyDown}
        onBlur={() => setTimeout(() => setOpen(false), 120)}
        onFocus={() => suggestions.length > 0 && setOpen(true)}
        autoComplete="off"
      />
      <button type="submit">Search</button>
      {open && (
        <ul className="suggestions" role="listbox" id={listId}>
          {suggestions.map((s, i) => (
            <li
              key={s.id}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              className={i === active ? 'active' : undefined}
              onMouseDown={e => { e.preventDefault(); submit(s.name) }}
            >
              <span>{s.name}</span>
              <small>{s.category}</small>
            </li>
          ))}
        </ul>
      )}
    </form>
  )
}
