import { describe, expect, it } from 'vitest';
import { messages } from '@/i18n/messages';

/** Flattens nested messages into dotted keys: { a: { b: 'x' } } → { 'a.b': 'x' }. */
function flatten(obj: object, prefix = ''): Record<string, unknown> {
  return Object.entries(obj).reduce<Record<string, unknown>>((acc, [key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) Object.assign(acc, flatten(value, path));
    else acc[path] = value;
    return acc;
  }, {});
}

describe('i18n messages', () => {
  const ca = flatten(messages.ca);
  const es = flatten(messages.es);

  it('Catalan and Spanish have exactly the same keys', () => {
    expect(Object.keys(es).sort()).toEqual(Object.keys(ca).sort());
  });

  it.each(Object.entries({ ca, es }))('%s has no empty texts', (_locale, flat) => {
    const empty = Object.entries(flat).filter(([, v]) => typeof v === 'string' && v.trim() === '');
    expect(empty).toEqual([]);
  });
});
