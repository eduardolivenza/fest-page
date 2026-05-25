'use client';

import { useState } from 'react';

export interface SizeRow {
  width: number;
  length: number;
  price: number;
}

interface SizesEditorProps {
  initial?: SizeRow[];
  name?: string;
}

const SIZED_CATEGORIES = ['BED', 'MATTRESS', 'PILLOW'];

export function SizesEditor({ initial = [], name = 'sizes' }: SizesEditorProps) {
  const [rows, setRows] = useState<SizeRow[]>(initial);

  function addRow() {
    setRows((prev) => [...prev, { width: 90, length: 190, price: 0 }]);
  }

  function removeRow(index: number) {
    setRows((prev) => prev.filter((_, i) => i !== index));
  }

  function updateRow(index: number, field: keyof SizeRow, value: number) {
    setRows((prev) => prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)));
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">Medidas disponibles</span>
        <button
          type="button"
          onClick={addRow}
          className="rounded-lg border border-brand-blue-200 px-3 py-1 text-xs font-medium text-brand-blue-700 hover:bg-brand-blue-50"
        >
          + Añadir medida
        </button>
      </div>

      {rows.length === 0 && (
        <p className="rounded-lg bg-gray-50 px-4 py-3 text-sm text-gray-400">
          Sin medidas. Añade una para ofrecer tallas específicas.
        </p>
      )}

      {rows.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-gray-100">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-xs font-semibold uppercase tracking-wider text-gray-500">
              <tr>
                <th className="px-3 py-2 text-left">Ancho (cm)</th>
                <th className="px-3 py-2 text-left">Largo (cm)</th>
                <th className="px-3 py-2 text-left">Precio (€)</th>
                <th className="px-3 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50 bg-white">
              {rows.map((row, i) => (
                <tr key={i}>
                  <td className="px-3 py-2">
                    <input
                      type="number" min="1" required
                      value={row.width}
                      onChange={(e) => updateRow(i, 'width', parseInt(e.target.value, 10))}
                      className="w-24 rounded border border-gray-200 px-2 py-1 outline-none focus:border-brand-blue-400"
                    />
                  </td>
                  <td className="px-3 py-2">
                    <input
                      type="number" min="1" required
                      value={row.length}
                      onChange={(e) => updateRow(i, 'length', parseInt(e.target.value, 10))}
                      className="w-24 rounded border border-gray-200 px-2 py-1 outline-none focus:border-brand-blue-400"
                    />
                  </td>
                  <td className="px-3 py-2">
                    <input
                      type="number" min="0" step="0.01" required
                      value={row.price}
                      onChange={(e) => updateRow(i, 'price', parseFloat(e.target.value))}
                      className="w-28 rounded border border-gray-200 px-2 py-1 outline-none focus:border-brand-blue-400"
                    />
                  </td>
                  <td className="px-3 py-2">
                    <button
                      type="button"
                      onClick={() => removeRow(i)}
                      className="text-red-400 hover:text-red-600"
                      aria-label="Eliminar medida"
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Serialized value read by the form handler */}
      <input type="hidden" name={name} value={JSON.stringify(rows)} />
    </div>
  );
}

export { SIZED_CATEGORIES };
