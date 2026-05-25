import type { Product } from '@festpage/types';
import { Badge } from './Badge';

interface ProductCardProps {
  product: Product;
  onAddToCart?: (product: Product) => void;
  href?: string;
}

const categoryLabels: Record<Product['category'], string> = {
  BED: 'Cama',
  MATTRESS: 'Colchón',
  PILLOW: 'Almohada',
  ACCESSORY: 'Accesorio',
};

export function ProductCard({ product, onAddToCart, href }: ProductCardProps) {
  const firstImage = product.images[0];

  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm transition-shadow hover:shadow-md">
      <a href={href ?? `/products/${product.id}`} className="block overflow-hidden">
        <div className="relative aspect-[4/3] bg-gray-50">
          {firstImage ? (
            <img
              src={firstImage.url}
              alt={firstImage.alt ?? product.name}
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-gray-300">
              <svg className="h-16 w-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          )}
          {product.featured && (
            <div className="absolute left-3 top-3">
              <Badge variant="green">Destacado</Badge>
            </div>
          )}
        </div>
      </a>

      <div className="flex flex-1 flex-col p-4">
        <div className="mb-1">
          <Badge variant="blue">{categoryLabels[product.category]}</Badge>
        </div>
        <h3 className="mt-1 font-display text-lg font-semibold text-brand-blue-900 line-clamp-2">
          <a href={href ?? `/products/${product.id}`}>{product.name}</a>
        </h3>
        {product.description && (
          <p className="mt-1 text-sm text-gray-500 line-clamp-2">{product.description}</p>
        )}
        <div className="mt-auto flex items-center justify-between pt-4">
          <span className="text-2xl font-bold text-brand-blue-800">
            {product.price.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })}
          </span>
          {onAddToCart && (
            <button
              onClick={() => onAddToCart(product)}
              className="rounded-lg bg-brand-blue-700 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-brand-blue-800"
            >
              Añadir
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
