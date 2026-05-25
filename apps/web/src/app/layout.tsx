import type { Metadata } from 'next';
import './globals.css';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { LanguageProvider } from '@/i18n';

export const metadata: Metadata = {
  title: { default: 'Fest Descans — Llits, Matalassos i Coixins a Valls', template: '%s | Fest Descans' },
  description:
    'Centre Tècnic del Descans a Valls, Tarragona. Llits, matalassos i coixins de qualitat. Assessorament personalitzat a la botiga.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ca">
      <body className="flex min-h-screen flex-col">
        <LanguageProvider>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Footer />
        </LanguageProvider>
      </body>
    </html>
  );
}
