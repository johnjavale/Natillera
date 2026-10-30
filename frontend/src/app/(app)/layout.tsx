import { cookies } from 'next/headers';
import Image from 'next/image';
import { redirect } from 'next/navigation';
import type { ReactNode } from 'react';
import { menuPara } from '@/lib/rutas';
import { COOKIE_SESION, leerSesion } from '@/lib/sesion';
import { Navegacion } from './navegacion';

export default async function LayoutApp({ children }: { children: ReactNode }) {
  const sesion = await leerSesion((await cookies()).get(COOKIE_SESION)?.value);
  if (!sesion) redirect('/login');
  const menu = menuPara(sesion.roles, sesion.integranteId !== null);

  return (
    <div className="min-h-screen">
      <header className="border-b border-marca-100 bg-white">
        <div className="mx-auto flex max-w-6xl items-center gap-3 px-4 py-2">
          <Image src="/natillera_familiar.svg" alt="" width={48} height={32} />
          <div className="min-w-0 flex-1">
            <p className="truncate font-bold leading-tight text-stone-900">Natillera Familiar</p>
            <p className="truncate text-xs text-marca-700">Ahorramos juntos, crecemos juntos</p>
          </div>
          <p className="hidden text-sm text-stone-600 sm:block" data-testid="usuario-actual">{sesion.nombre}</p>
        </div>
        <Navegacion items={menu} />
      </header>
      <main className="mx-auto max-w-6xl px-4 py-5">{children}</main>
    </div>
  );
}
