'use client';

import { useState } from 'react';
import { Button } from '@festpage/ui';
import { useT } from '@/i18n';

const STORE = {
  address: 'Carrer Lluís Homs, 3',
  city: '43800 Valls, Tarragona',
  phone: '+34 977 608 088',
  email: 'info@fest.cat',
  mapsEmbed:
    'https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d1508.7!2d1.2581613!3d41.2867953!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x12a4008e538c5fcb%3A0x76b3e9379db77652!2sFest+Descans!5e0!3m2!1ses!2ses!4v1',
  mapsUrl: 'https://maps.app.goo.gl/b5ztEJ13dGMU3Ge7A',
};

export default function ContactPage() {
  const { t } = useT();
  const c = t.contact;
  const [status, setStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');

  const hours = [
    { days: c.hours.weekdays, time: '10:00 – 14:00 / 16:30 – 20:00' },
    { days: c.hours.saturday, time: '10:00 – 14:00' },
    { days: c.hours.sunday, time: c.hours.closed },
  ];

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setStatus('sending');

    const formData = new FormData(e.currentTarget);
    const body = {
      name: formData.get('name'),
      email: formData.get('email'),
      phone: formData.get('phone') || undefined,
      message: formData.get('message'),
    };

    try {
      const res = await fetch(`${process.env['NEXT_PUBLIC_API_URL']}/api/contacts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (res.ok) {
        setStatus('success');
        (e.target as HTMLFormElement).reset();
      } else {
        setStatus('error');
      }
    } catch {
      setStatus('error');
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-16">
      <h1 className="font-display text-4xl font-bold text-brand-blue-900">{c.title}</h1>
      <p className="mt-3 text-gray-600">{c.subtitle}</p>

      <div className="mt-12 grid gap-12 lg:grid-cols-2">
        {/* Left — store info + form */}
        <div className="space-y-10">
          <div className="space-y-5">
            <InfoRow icon="📍" label={c.labels.address}>
              <a
                href={STORE.mapsUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-brand-blue-700 hover:underline"
              >
                {STORE.address}<br />{STORE.city}
              </a>
            </InfoRow>

            <InfoRow icon="📞" label={c.labels.phone}>
              <a href={`tel:${STORE.phone.replace(/\s/g, '')}`} className="hover:text-brand-blue-700 hover:underline">
                {STORE.phone}
              </a>
            </InfoRow>

            <InfoRow icon="✉️" label={c.labels.email}>
              <a href={`mailto:${STORE.email}`} className="hover:text-brand-blue-700 hover:underline">
                {STORE.email}
              </a>
            </InfoRow>

            <InfoRow icon="🕐" label={c.labels.hours}>
              <ul className="space-y-0.5">
                {hours.map((h) => (
                  <li key={h.days} className="flex gap-2">
                    <span className="w-36 shrink-0 text-gray-500">{h.days}</span>
                    <span>{h.time}</span>
                  </li>
                ))}
              </ul>
            </InfoRow>
          </div>

          {/* Contact form */}
          <div>
            <h2 className="font-display text-2xl font-bold text-brand-blue-900">{c.form.title}</h2>

            {status === 'success' ? (
              <div className="mt-6 rounded-2xl bg-brand-green-100 p-8 text-center">
                <p className="text-2xl">✅</p>
                <p className="mt-2 font-semibold text-green-800">{c.form.successTitle}</p>
                <p className="text-green-700">{c.form.successMsg}</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="mt-6 space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-gray-700" htmlFor="name">{c.form.name}</label>
                    <input
                      id="name" name="name" type="text" required
                      className="mt-1 w-full rounded-xl border border-gray-200 px-4 py-3 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700" htmlFor="phone">{c.form.phone}</label>
                    <input
                      id="phone" name="phone" type="tel"
                      className="mt-1 w-full rounded-xl border border-gray-200 px-4 py-3 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700" htmlFor="email">{c.form.email}</label>
                  <input
                    id="email" name="email" type="email" required
                    className="mt-1 w-full rounded-xl border border-gray-200 px-4 py-3 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700" htmlFor="message">{c.form.message}</label>
                  <textarea
                    id="message" name="message" required rows={5}
                    className="mt-1 w-full rounded-xl border border-gray-200 px-4 py-3 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
                  />
                </div>
                {status === 'error' && (
                  <p className="text-sm text-red-600">{c.form.error}</p>
                )}
                <Button type="submit" loading={status === 'sending'} className="w-full">
                  {c.form.send}
                </Button>
              </form>
            )}
          </div>
        </div>

        {/* Right — map */}
        <div className="flex flex-col gap-6">
          <div className="overflow-hidden rounded-2xl border border-gray-100 shadow-sm">
            <iframe
              src={STORE.mapsEmbed}
              width="100%"
              height="420"
              style={{ border: 0 }}
              allowFullScreen
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
              title={c.map.iframeTitle}
            />
          </div>
          <a
            href={STORE.mapsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand-blue-200 px-4 py-2.5 text-sm font-medium text-brand-blue-700 transition-colors hover:bg-brand-blue-50"
          >
            <span>📍</span> {c.map.openMaps}
          </a>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ icon, label, children }: { icon: string; label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-4">
      <span className="mt-0.5 text-xl">{icon}</span>
      <div>
        <p className="text-xs font-semibold uppercase tracking-wider text-gray-400">{label}</p>
        <div className="mt-0.5 text-gray-800">{children}</div>
      </div>
    </div>
  );
}
