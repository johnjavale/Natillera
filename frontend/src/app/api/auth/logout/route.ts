import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { backendUrl, borrarCookies, mismoOrigen, problema } from '@/lib/bff';
import { COOKIE_REFRESH } from '@/lib/sesion';

export async function POST(req: Request) {
  if (!mismoOrigen(req)) return problema(403, 'Origen no permitido', 'Solicitud de otro origen');
  const refresh = (await cookies()).get(COOKIE_REFRESH)?.value;
  if (refresh) {
    // Revoca el refresh token en el backend; si falla, igual se cierran las cookies locales
    await fetch(backendUrl('/api/v1/auth/logout'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: refresh }),
      cache: 'no-store',
    }).catch(() => undefined);
  }
  const res = new NextResponse(null, { status: 204 });
  borrarCookies(res);
  return res;
}
