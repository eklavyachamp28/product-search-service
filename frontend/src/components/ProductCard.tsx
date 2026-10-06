import { renderHighlight } from '../lib/highlight'
import type { Hit } from '../lib/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

function Stars({ rating }: { rating: number }) {
  return (
    <span className="stars" aria-label={`Rated ${rating} out of 5`}>
      {'★'.repeat(Math.round(rating))}<span className="dim">{'★'.repeat(5 - Math.round(rating))}</span>
    </span>
  )
}

export function ProductCard({ hit }: { hit: Hit }) {
  const p = hit.product
  const name = hit.highlights?.name?.[0]
  const desc = hit.highlights?.description?.[0]
  return (
    <article className="card">
      <div className="card-top">
        <span className="category">{p.category}</span>
        {!p.inStock && <span className="oos">Out of stock</span>}
      </div>
      <h3>{name ? renderHighlight(name) : p.name}</h3>
      <p className="brand">{p.brand}</p>
      <p className="desc">{desc ? renderHighlight(desc) : p.description}</p>
      <div className="card-bottom">
        <strong className="price">{money.format(p.price)}</strong>
        {p.rating !== undefined && (
          <span className="rating"><Stars rating={p.rating} /> {p.rating.toFixed(1)} <small>({p.reviewCount ?? 0})</small></span>
        )}
      </div>
    </article>
  )
}
