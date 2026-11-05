import { Suspense } from 'react';
import { FormularioRestablecer } from './formulario-restablecer';

export default function RestablecerPassword() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-xl font-bold">Cree una nueva contraseña</h1>
        <p className="mb-4 text-sm text-stone-600">Use al menos 10 caracteres, con letras y números.</p>
        <Suspense>
          <FormularioRestablecer />
        </Suspense>
      </div>
    </main>
  );
}
