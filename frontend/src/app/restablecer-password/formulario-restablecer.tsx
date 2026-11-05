'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Tarjeta } from '@/components/ui';
import { restablecerPasswordSchema } from '@/lib/esquemas';

type Datos = z.infer<typeof restablecerPasswordSchema>;

export function FormularioRestablecer() {
  const router = useRouter();
  const token = useSearchParams().get('token');
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<Datos>({ resolver: zodResolver(restablecerPasswordSchema) });

  if (!token) {
    return (
      <Tarjeta>
        <p role="alert" className="text-sm text-red-800">
          El enlace está incompleto. Ábralo de nuevo desde el correo o solicite uno nuevo.
        </p>
        <p className="mt-3 text-sm">
          <Link href="/recuperar-password" className="text-marca-700 underline">Solicitar un enlace nuevo</Link>
        </p>
      </Tarjeta>
    );
  }

  const enviar = async ({ nueva }: Datos) => {
    setError(null);
    const res = await fetch('/api/auth/restablecer-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, nueva }),
    });
    if (!res.ok) {
      const cuerpo = await res.json().catch(() => ({}));
      setError(cuerpo.detail ?? 'No fue posible cambiar la contraseña');
      return;
    }
    router.replace('/login?restablecida=1');
  };

  return (
    <Tarjeta>
      <form method="post" onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <Campo etiqueta="Nueva contraseña" type="password" autoComplete="new-password" {...register('nueva')} error={formState.errors.nueva?.message} />
        <Campo etiqueta="Repita la nueva contraseña" type="password" autoComplete="new-password" {...register('confirmacion')} error={formState.errors.confirmacion?.message} />
        {error && (
          <div role="alert" className="rounded-lg bg-red-50 p-2 text-sm text-red-800">
            {error} <Link href="/recuperar-password" className="underline">Solicitar otro enlace</Link>
          </div>
        )}
        <Boton type="submit" className="w-full" disabled={formState.isSubmitting}>Guardar contraseña</Boton>
      </form>
    </Tarjeta>
  );
}
