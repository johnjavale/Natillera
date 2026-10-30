import { cookies } from 'next/headers';
import { backendUrl, type SesionBackend } from './bff';
import { COOKIE_ACCESS, COOKIE_REFRESH } from './sesion';

export interface TokenVigente {
  access: string | null;
  /** Sesión nueva si hubo que refrescar (el route handler debe escribir las cookies). */
  renovada: SesionBackend | null;
}

/** Refresca el access token con el refresh token (rotación en el backend). */
export async function refrescar(refresh: string): Promise<SesionBackend | null> {
  const res = await fetch(backendUrl('/api/v1/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: refresh }),
    cache: 'no-store',
  });
  return res.ok ? ((await res.json()) as SesionBackend) : null;
}

/** Access token de las cookies; si ya expiró (cookie vencida), lo renueva con el refresh token. */
export async function tokenVigente(): Promise<TokenVigente> {
  const jar = await cookies();
  const access = jar.get(COOKIE_ACCESS)?.value ?? null;
  if (access) return { access, renovada: null };
  const refresh = jar.get(COOKIE_REFRESH)?.value;
  if (!refresh) return { access: null, renovada: null };
  const renovada = await refrescar(refresh);
  return { access: renovada?.accessToken ?? null, renovada };
}
