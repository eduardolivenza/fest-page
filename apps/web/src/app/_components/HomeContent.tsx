'use client';

import Image from 'next/image';
import Link from 'next/link';
import { ProductCard } from '@festpage/ui';
import type { Product } from '@festpage/types';
import { useT } from '@/i18n';

export function HomeContent({ featured }: { featured: Product[] }) {
  const { t } = useT();
  const h = t.home;

  const categories = [
    { ...h.categories.beds, href: '/products?category=BED', img: '/store/interior-7.jpg' },
    { ...h.categories.mattresses, href: '/products?category=MATTRESS', img: '/store/interior-5.jpg' },
    { ...h.categories.pillows, href: '/products?category=PILLOW', img: '/store/interior-2.jpg' },
  ];

  return (
    <>
      {/* ── HERO ─────────────────────────────────────────────── */}
      <section className="relative min-h-[90vh] overflow-hidden bg-brand-blue-700">
        <Image
          src="/store/interior-1.jpg"
          alt={h.hero.altShowroom}
          fill
          className="object-cover opacity-30"
          priority
        />
        <div className="absolute inset-0 bg-gradient-to-r from-brand-blue-900/90 via-brand-blue-800/70 to-transparent" />

        <div className="relative mx-auto flex min-h-[90vh] max-w-6xl items-center px-4 py-24">
          <div className="max-w-2xl">
            <span className="inline-block rounded-full border border-brand-green-500/40 bg-brand-green-500/10 px-4 py-1 text-sm font-semibold tracking-widest text-brand-green-400 uppercase">
              {h.hero.badge}
            </span>
            <h1 className="mt-6 font-display text-5xl font-black leading-tight text-white md:text-6xl lg:text-7xl">
              {h.hero.headline1}{' '}
              <span className="text-brand-green-500">{h.hero.headline2}</span>
            </h1>
            <p className="mt-6 max-w-lg text-lg leading-relaxed text-blue-100">
              {h.hero.description}
            </p>
            <div className="mt-10 flex flex-wrap gap-4">
              <Link
                href="/products"
                className="rounded-lg bg-brand-green-500 px-8 py-4 font-display font-bold text-white shadow-lg transition-all hover:bg-brand-green-400 hover:shadow-xl"
              >
                {h.hero.cta}
              </Link>
              <Link
                href="/contact"
                className="rounded-lg border border-white/30 px-8 py-4 font-display font-bold text-white backdrop-blur-sm transition-all hover:border-white/60 hover:bg-white/10"
              >
                {h.hero.visitStore}
              </Link>
            </div>
          </div>
        </div>

        <div className="absolute bottom-0 left-0 right-0 h-24 bg-gradient-to-t from-white to-transparent" />
      </section>

      {/* ── CATEGORIES ───────────────────────────────────────── */}
      <section className="mx-auto max-w-6xl px-4 py-20">
        <div className="text-center">
          <h2 className="font-display text-4xl font-black text-brand-blue-700">
            {h.categories.title}
          </h2>
          <div className="mx-auto mt-2 h-1 w-16 rounded-full bg-brand-green-500" />
        </div>

        <div className="mt-12 grid gap-6 sm:grid-cols-3">
          {categories.map((cat) => (
            <Link
              key={cat.label}
              href={cat.href}
              className="group relative overflow-hidden rounded-2xl shadow-md transition-all duration-300 hover:-translate-y-1 hover:shadow-xl"
            >
              <div className="relative aspect-[3/4]">
                <Image
                  src={cat.img}
                  alt={cat.label}
                  fill
                  className="object-cover transition-transform duration-500 group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-brand-blue-900/90 via-brand-blue-800/30 to-transparent" />
              </div>
              <div className="absolute bottom-0 left-0 right-0 p-6">
                <div className="mb-1 h-0.5 w-8 rounded-full bg-brand-green-500 transition-all duration-300 group-hover:w-16" />
                <h3 className="font-display text-2xl font-black text-white">{cat.label}</h3>
                <p className="mt-1 text-sm text-blue-200">{cat.sub}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* ── FEATURED PRODUCTS ────────────────────────────────── */}
      {featured.length > 0 && (
        <section className="bg-gray-50 py-20">
          <div className="mx-auto max-w-6xl px-4">
            <div className="flex items-end justify-between">
              <div>
                <h2 className="font-display text-4xl font-black text-brand-blue-700">{h.featured.title}</h2>
                <div className="mt-2 h-1 w-16 rounded-full bg-brand-green-500" />
              </div>
              <Link
                href="/products"
                className="text-sm font-semibold text-brand-green-500 transition-colors hover:text-brand-green-600"
              >
                {h.featured.seeAll}
              </Link>
            </div>
            <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {featured.map((product) => (
                <ProductCard key={product.id} product={product} href={`/products/${product.slug}`} />
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ── STORE GALLERY ────────────────────────────────────── */}
      <section className="mx-auto max-w-6xl px-4 py-20">
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="flex flex-col justify-center">
            <span className="text-sm font-semibold tracking-widest text-brand-green-500 uppercase">
              {h.gallery.badge}
            </span>
            <h2 className="mt-3 font-display text-4xl font-black leading-tight text-brand-blue-700">
              {h.gallery.title}<br />{h.gallery.titleLine2}
            </h2>
            <p className="mt-5 text-lg leading-relaxed text-gray-600">
              {h.gallery.description}
            </p>
            <ul className="mt-6 space-y-3">
              {[h.gallery.feature1, h.gallery.feature2].map((item) => (
                <li key={item} className="flex items-start gap-3">
                  <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-green-500">
                    <svg className="h-3 w-3 text-white" fill="currentColor" viewBox="0 0 12 12">
                      <path d="M10 3L5 8.5 2 5.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
                    </svg>
                  </span>
                  <span className="text-gray-700">{item}</span>
                </li>
              ))}
            </ul>
            <Link
              href="/contact"
              className="mt-8 inline-flex w-fit items-center gap-2 rounded-lg bg-brand-blue-700 px-6 py-3 font-display font-bold text-white transition-colors hover:bg-brand-blue-800"
            >
              {h.gallery.bookVisit}
            </Link>
          </div>

          <div className="grid grid-cols-2 gap-3">
            {[
              { src: '/store/facade-1.jpg', alt: h.gallery.altFacade, className: 'col-span-2 aspect-[16/7]' },
              { src: '/store/interior-4.jpg', alt: h.gallery.altInterior, className: 'aspect-square' },
              { src: '/store/interior-6.jpg', alt: h.gallery.altExhibition, className: 'aspect-square' },
            ].map((img) => (
              <div key={img.src} className={`relative overflow-hidden rounded-xl ${img.className}`}>
                <Image src={img.src} alt={img.alt} fill className="object-cover" />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── BRAND STRIP ──────────────────────────────────────── */}
      <section className="relative overflow-hidden bg-brand-blue-700 py-20">
        <Image
          src="/store/interior-3.jpg"
          alt={h.brandStrip.altShowroom}
          fill
          className="object-cover opacity-15"
        />
        <div className="relative mx-auto max-w-4xl px-4 text-center">
          <p className="font-display text-5xl font-black text-white md:text-6xl">
            {h.brandStrip.line1}
          </p>
          <p className="mt-2 font-display text-5xl font-black text-brand-green-500 md:text-6xl">
            {h.brandStrip.line2}
          </p>
          <p className="mx-auto mt-6 max-w-lg text-lg text-blue-200">
            {h.brandStrip.address}
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-6 text-sm font-semibold text-blue-100">
            <a href="tel:+34977608088" className="flex items-center gap-2 hover:text-white">
              <span>📞</span> +34 977 608 088
            </a>
            <a href="mailto:info@fest.cat" className="flex items-center gap-2 hover:text-white">
              <span>✉️</span> info@fest.cat
            </a>
            <Link href="/contact" className="flex items-center gap-2 hover:text-white">
              <span>📍</span> {h.brandStrip.directions}
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
