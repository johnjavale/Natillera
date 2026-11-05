'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Tarjeta } from '@/components/ui';
import { loginSchema } from '@/lib/esquemas';

type Datos = z.infer<typeof loginSchema>;

export function FormularioLogin() {
  const router = useRouter();
  const params = useSearchParams();
  const [error, setError] = useState<string | null>(params.get('expirada') ? 'Su sesión expiró, ingrese de nuevo.' : null);
  const restablecida = params.get('restablecida') === '1';
  const { register, handleSubmit, formState } = useForm<Datos>({ resolver: zodResolver(loginSchema) });

  const enviar = async (datos: Datos) => {
    setError(null);
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(datos),
    });
    const cuerpo = await res.json().catch(() => ({}));
    if (!res.ok) {
      setError(cuerpo.detail ?? 'No fue posible ingresar');
      return;
    }
    // Solo rutas internas (evita redirecciones abiertas a otros sitios)
    const volver = params.get('volver');
    const seguro = volver && volver.startsWith('/') && !volver.startsWith('//') ? volver : null;
    router.replace(cuerpo.destino === '/cambiar-password' || !seguro ? cuerpo.destino : seguro);
    router.refresh();
  };

  return (
    <Tarjeta>
      <form method="post" onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <Campo etiqueta="Correo o celular" autoComplete="username" {...register('login')} error={formState.errors.login?.message} />
        <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" {...register('password')} error={formState.errors.password?.message} />
        {restablecida && !error && (
          <p role="status" className="rounded-lg bg-green-50 p-2 text-sm text-green-800">Su contraseña se cambió. Ingrese con la nueva.</p>
        )}
        {error && <p role="alert" className="rounded-lg bg-red-50 p-2 text-sm text-red-800">{error}</p>}
        <Boton type="submit" className="w-full" disabled={formState.isSubmitting}>
          {formState.isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </Boton>
        <p className="text-center text-sm">
          <Link href="/recuperar-password" className="text-marca-700 underline">¿Olvidó su contraseña?</Link>
        </p>
      </form>
    </Tarjeta>
  );
}
