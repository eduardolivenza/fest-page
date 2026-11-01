import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { messages } from '@/i18n/messages';

const t = messages.ca.contact.form;

async function renderContactPage() {
  // Fresh modules so the Turnstile site key (read at import time) follows the stubbed env.
  vi.resetModules();
  const { LanguageProvider } = await import('@/i18n');
  const { default: ContactPage } = await import('@/app/contact/page');
  render(<LanguageProvider><ContactPage /></LanguageProvider>);
}

function fillForm() {
  fireEvent.change(screen.getByLabelText(t.name), { target: { value: 'Maria' } });
  fireEvent.change(screen.getByLabelText(t.email), { target: { value: 'maria@example.com' } });
  fireEvent.change(screen.getByLabelText(t.message), { target: { value: 'Voldria informació.' } });
}

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  vi.stubEnv('NEXT_PUBLIC_API_URL', 'https://api.test');
  fetchMock = vi.fn().mockResolvedValue(new Response('{}', { status: 201 }));
  vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  delete window.turnstile;
});

describe('contact page', () => {
  it('sends the form to the API and shows the success message', async () => {
    vi.stubEnv('NEXT_PUBLIC_TURNSTILE_SITE_KEY', '');
    await renderContactPage();

    fillForm();
    fireEvent.click(screen.getByRole('button', { name: t.send }));

    await screen.findByText(t.successTitle);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toBe('https://api.test/api/contacts');
    expect(JSON.parse(init.body)).toMatchObject({ name: 'Maria', email: 'maria@example.com', message: 'Voldria informació.' });
  });

  it('shows an error when the API rejects the message', async () => {
    vi.stubEnv('NEXT_PUBLIC_TURNSTILE_SITE_KEY', '');
    fetchMock.mockResolvedValue(new Response('{}', { status: 500 }));
    await renderContactPage();

    fillForm();
    fireEvent.click(screen.getByRole('button', { name: t.send }));

    await screen.findByText(t.error);
  });

  it('with Turnstile: waits for the token and sends it with the message', async () => {
    vi.stubEnv('NEXT_PUBLIC_TURNSTILE_SITE_KEY', 'site-key');
    let issueToken: (token: string) => void = () => {};
    window.turnstile = {
      render: vi.fn((_el: HTMLElement, opts: Record<string, unknown>) => {
        expect(opts['sitekey']).toBe('site-key');
        issueToken = opts['callback'] as (token: string) => void;
        return 'widget-1';
      }),
      reset: vi.fn(),
      remove: vi.fn(),
    };
    await renderContactPage();
    fillForm();

    const send = screen.getByRole('button', { name: t.send });
    expect(send).toHaveProperty('disabled', true);

    await waitFor(() => expect(window.turnstile!.render).toHaveBeenCalled());
    issueToken('tok-123');
    await waitFor(() => expect(send).toHaveProperty('disabled', false));

    fireEvent.click(send);
    await screen.findByText(t.successTitle);
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body).turnstileToken).toBe('tok-123');
  });
});
