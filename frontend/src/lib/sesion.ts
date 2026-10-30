import { SignJWT, jwtVerify } from 'jose';
import type { Rol } from './rutas';

/** Cookies httpOnly del BFF. El navegador nunca recibe el JWT del backend en JavaScript. */
export const COOKIE_ACCESS = 'nat_at';
export const COOKIE_REFRESH = 'nat_rt';
export const COOKIE_SESION = 'nat_sesion';

export const DURACION_ACCESS_S = 15 * 60;
export const DURACION_REFRESH_S = 7 * 24 * 60 * 60;

/** Datos NO sensibles que lee middleware.ts para proteger rutas por rol. Firmados con SESSION_SECRET. */
export interface DatosSesion {
  id: number;
  nombre: string;
  roles: Rol[];
  integranteId: number | null;
  tesoreroId: number | null;
  debeCambiarPassword: boolean;
}

function clave(): Uint8Array {
  const secreto = process.env.SESSION_SECRET;
  if (!secreto || secreto.length < 32) {
    throw new Error('SESSION_SECRET debe tener al menos 32 caracteres');
  }
  return new TextEncoder().encode(secreto);
}

export async function firmarSesion(datos: DatosSesion): Promise<string> {
  return new SignJWT({ ...datos })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt()
    .setExpirationTime(`${DURACION_REFRESH_S}s`)
    .sign(clave());
}

export async function leerSesion(token: string | undefined): Promise<DatosSesion | null> {
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, clave(), { algorithms: ['HS256'] });
    return {
      id: Number(payload.id),
      nombre: String(payload.nombre ?? ''),
      roles: (payload.roles as Rol[]) ?? [],
      integranteId: (payload.integranteId as number | null) ?? null,
      tesoreroId: (payload.tesoreroId as number | null) ?? null,
      debeCambiarPassword: Boolean(payload.debeCambiarPassword),
    };
  } catch {
    return null;
  }
}

export function opcionesCookie(maxAge: number, path = '/') {
  return {
    httpOnly: true,
    secure: process.env.COOKIE_SECURE !== 'false' && process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path,
    maxAge,
  };
}
