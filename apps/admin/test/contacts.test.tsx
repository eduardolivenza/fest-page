import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import ContactsPage from '@/app/dashboard/contacts/page';
import { apiFetch } from '@/lib/api';

vi.mock('@/lib/api', () => ({ apiFetch: vi.fn() }));

const api = vi.mocked(apiFetch);

const contact = (id: string, read: boolean) => ({
  id,
  name: `Persona ${id}`,
  email: `${id}@example.com`,
  phone: null,
  message: `Missatge ${id}`,
  read,
  createdAt: '2026-10-30T10:00:00.000Z',
});

beforeEach(() => {
  api.mockReset();
  api.mockImplementation(async (path: string) =>
    path.startsWith('/api/admin/contacts?')
      ? { data: [contact('c1', false), contact('c2', true)], total: 2, page: 1, pageSize: 50, totalPages: 1 }
      : undefined,
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('admin contacts page', () => {
  it('lists messages and flags unread ones', async () => {
    render(<ContactsPage />);

    await screen.findByText('Persona c1');
    expect(screen.getByText('Missatge c2')).toBeTruthy();
    expect(screen.getAllByText('Nuevo')).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'Marcar leído' })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'Eliminar' })).toHaveLength(2);
  });

  it('marks a message as read', async () => {
    render(<ContactsPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Marcar leído' }));

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/admin/contacts/c1/read', { method: 'PATCH' }));
  });

  it('deletes a message after confirmation and reloads the list', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    render(<ContactsPage />);
    await screen.findByText('Persona c2');

    fireEvent.click(screen.getAllByRole('button', { name: 'Eliminar' })[1]!);

    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/admin/contacts/c2', { method: 'DELETE' }));
    await waitFor(() => expect(api.mock.calls.filter(([p]) => p.startsWith('/api/admin/contacts?'))).toHaveLength(2));
  });

  it('does not delete when the confirmation is cancelled', async () => {
    vi.stubGlobal('confirm', vi.fn(() => false));
    render(<ContactsPage />);
    await screen.findByText('Persona c1');

    fireEvent.click(screen.getAllByRole('button', { name: 'Eliminar' })[0]!);

    expect(api).not.toHaveBeenCalledWith(expect.stringMatching(/^\/api\/admin\/contacts\/c1$/), expect.anything());
  });
});
