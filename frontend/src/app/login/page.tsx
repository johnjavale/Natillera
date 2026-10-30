import Image from 'next/image';
import { Suspense } from 'react';
import { FormularioLogin } from './formulario-login';

export default function PaginaLogin() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <Image src="/natillera_familiar.svg" alt="" width={180} height={120} className="mx-auto" priority />
          <h1 className="mt-3 text-2xl font-bold text-stone-900">Natillera Familiar</h1>
          <p className="text-sm text-marca-700">Ahorramos juntos, crecemos juntos</p>
        </div>
        <Suspense>
          <FormularioLogin />
        </Suspense>
      </div>
    </main>
  );
}
