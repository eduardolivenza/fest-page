import type { ReactNode } from 'react';

type Variant = 'blue' | 'green' | 'gray' | 'red';

interface BadgeProps {
  variant?: Variant;
  children: ReactNode;
}

const variantClasses: Record<Variant, string> = {
  blue: 'bg-brand-blue-100 text-brand-blue-800',
  green: 'bg-brand-green-100 text-green-800',
  gray: 'bg-gray-100 text-gray-700',
  red: 'bg-red-100 text-red-700',
};

export function Badge({ variant = 'blue', children }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${variantClasses[variant]}`}
    >
      {children}
    </span>
  );
}
