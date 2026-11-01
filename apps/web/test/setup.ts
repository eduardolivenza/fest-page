// Node 25+ ships its own global localStorage/sessionStorage (undefined without
// --localstorage-file), which shadows jsdom's. Point them back at jsdom.
const jsdomWindow = (globalThis as unknown as { jsdom: { window: Window } }).jsdom.window;
for (const key of ['localStorage', 'sessionStorage'] as const) {
  Object.defineProperty(globalThis, key, { value: jsdomWindow[key], configurable: true, writable: true });
}

export {};
