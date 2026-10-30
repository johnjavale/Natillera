'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import type { ItemMenu } from '@/lib/rutas';

export function Navegacion({ items }: { items: ItemMenu[] }) {
  const ruta = usePathname();
  const router = useRouter();
  const activo = (href: string) =>
    ruta === href || (ruta.startsWith(href + '/') && !items.some((i) => i.href !== href && i.href.startsWith(href) && ruta.startsWith(i.href)));

  const salir = async () => {
    await fetch('/api/auth/logout', { method: 'POST' });
    router.replace('/login');
    router.refresh();
  };

  return (
    <nav aria-label="Principal" className="mx-auto max-w-6xl overflow-x-auto px-2">
      <ul className="flex gap-1 pb-2">
        {items.map((i) => (
          <li key={i.href}>
            <Link
              href={i.href}
              aria-current={activo(i.href) ? 'page' : undefined}
              className="block whitespace-nowrap rounded-lg px-3 py-2 text-sm text-stone-700 hover:bg-marca-50 aria-[current=page]:bg-marca-100 aria-[current=page]:font-semibold aria-[current=page]:text-marca-900"
            >
              {i.etiqueta}
            </Link>
          </li>
        ))}
        <li>
          <button onClick={salir} className="whitespace-nowrap rounded-lg px-3 py-2 text-sm text-stone-600 hover:bg-stone-100">
            Salir
          </button>
        </li>
      </ul>
    </nav>
  );
}
