'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Tarjeta } from '@/components/ui';
import { cambiarPasswordSchema } from '@/lib/esquemas';

type Datos = z.infer<typeof cambiarPasswordSchema>;

export default function CambiarPassword() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<Datos>({ resolver: zodResolver(cambiarPasswordSchema) });

  const enviar = async ({ actual, nueva }: Datos) => {
    setError(null);
    const res = await fetch('/api/auth/cambiar-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ actual, nueva }),
    });
    const cuerpo = await res.json().catch(() => ({}));
    if (!res.ok) {
      setError(cuerpo.detail ?? 'No fue posible cambiar la contraseña');
      return;
    }
    router.replace(cuerpo.destino);
    router.refresh();
  };

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-xl font-bold">Cambie su contraseña</h1>
        <p className="mb-4 text-sm text-stone-600">Use al menos 10 caracteres, con letras y números.</p>
        <Tarjeta>
          <form method="post" onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
            <Campo etiqueta="Contraseña actual" type="password" autoComplete="current-password" {...register('actual')} error={formState.errors.actual?.message} />
            <Campo etiqueta="Nueva contraseña" type="password" autoComplete="new-password" {...register('nueva')} error={formState.errors.nueva?.message} />
            <Campo etiqueta="Repita la nueva contraseña" type="password" autoComplete="new-password" {...register('confirmacion')} error={formState.errors.confirmacion?.message} />
            {error && <p role="alert" className="rounded-lg bg-red-50 p-2 text-sm text-red-800">{error}</p>}
            <Boton type="submit" className="w-full" disabled={formState.isSubmitting}>Guardar</Boton>
          </form>
        </Tarjeta>
      </div>
    </main>
  );
}
