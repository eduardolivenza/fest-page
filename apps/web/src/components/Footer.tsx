'use client';

import Image from 'next/image';
import { useT } from '@/i18n';

export function Footer() {
  const { t } = useT();
  const year = new Date().getFullYear();

  return (
    <footer className="bg-brand-blue-900 pt-14 pb-8 text-blue-200">
      <div className="mx-auto max-w-6xl px-4">
        <div className="grid gap-10 sm:grid-cols-3">
          <div>
            <Image src="/LOGO_fest.png" alt="Fest Descans" width={160} height={54} />
            <p className="mt-4 text-sm leading-relaxed text-blue-300">
              {t.footer.tagline}<br />
              {t.footer.since}
            </p>
            <div className="mt-4 space-y-1 text-sm">
              <p><a href="tel:+34977608088" className="hover:text-white">📞 +34 977 608 088</a></p>
              <p><a href="mailto:info@fest.cat" className="hover:text-white">✉️ info@fest.cat</a></p>
            </div>
          </div>

          <div>
            <h4 className="font-display font-bold uppercase tracking-wider text-white">{t.footer.products}</h4>
            <ul className="mt-3 space-y-2 text-sm">
              <li><a href="/products?category=BED" className="hover:text-white">{t.footer.bedsAndCanapes}</a></li>
              <li><a href="/products?category=MATTRESS" className="hover:text-white">{t.footer.mattresses}</a></li>
              <li><a href="/products?category=PILLOW" className="hover:text-white">{t.footer.pillows}</a></li>
              <li><a href="/products?category=ACCESSORY" className="hover:text-white">{t.footer.accessories}</a></li>
            </ul>
          </div>

          <div>
            <h4 className="font-display font-bold uppercase tracking-wider text-white">{t.footer.store}</h4>
            <ul className="mt-3 space-y-2 text-sm">
              <li>Carrer Lluís Homs, 3</li>
              <li>43800 Valls, Tarragona</li>
            </ul>
            <ul className="mt-4 space-y-2 text-sm">
              <li className="font-semibold text-blue-100">{t.footer.weekdays}</li>
              <li>10:00 – 14:00 / 16:30 – 20:00</li>
              <li className="font-semibold text-blue-100">{t.footer.saturday}</li>
              <li>10:00 – 14:00</li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-col items-center gap-2 border-t border-brand-blue-700 pt-6 text-xs text-blue-400 sm:flex-row sm:justify-between">
          <p>© {year} Fest Descans. {t.footer.rights}</p>
          <p>Carrer Lluís Homs, 3 · 43800 Valls · Tarragona</p>
        </div>
      </div>
    </footer>
  );
}
