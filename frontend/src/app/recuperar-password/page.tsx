'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Tarjeta } from '@/components/ui';
import { recuperarPasswordSchema } from '@/lib/esquemas';

type Datos = z.infer<typeof recuperarPasswordSchema>;

export default function RecuperarPassword() {
  const [enviado, setEnviado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<Datos>({ resolver: zodResolver(recuperarPasswordSchema) });

  const enviar = async (datos: Datos) => {
    setError(null);
    const res = await fetch('/api/auth/recuperar-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(datos),
    });
    if (!res.ok) {
      const cuerpo = await res.json().catch(() => ({}));
      setError(cuerpo.detail ?? 'No fue posible enviar la solicitud');
      return;
    }
    setEnviado(true);
  };

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-xl font-bold">¿Olvidó su contraseña?</h1>
        <p className="mb-4 text-sm text-stone-600">
          Escriba el correo con el que ingresa y le enviaremos un enlace para crear una nueva.
        </p>
        <Tarjeta>
          {enviado ? (
            <p role="status" className="rounded-lg bg-green-50 p-3 text-sm text-green-800">
              Si el correo está registrado, en unos minutos le llegará un enlace. Vale 30 minutos. Revise también la
              carpeta de spam.
            </p>
          ) : (
            <form method="post" onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
              <Campo etiqueta="Correo" type="email" autoComplete="email" {...register('correo')} error={formState.errors.correo?.message} />
              {error && <p role="alert" className="rounded-lg bg-red-50 p-2 text-sm text-red-800">{error}</p>}
              <Boton type="submit" className="w-full" disabled={formState.isSubmitting}>
                {formState.isSubmitting ? 'Enviando…' : 'Enviar enlace'}
              </Boton>
            </form>
          )}
        </Tarjeta>
        <p className="mt-4 text-center text-sm">
          <Link href="/login" className="text-marca-700 underline">Volver a ingresar</Link>
        </p>
        <p className="mt-2 text-center text-xs text-stone-500">
          Si ingresa con su celular, pida al administrador que restablezca su contraseña.
        </p>
      </div>
    </main>
  );
}
