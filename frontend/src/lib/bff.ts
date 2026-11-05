import { NextResponse } from 'next/server';
import {
  COOKIE_ACCESS,
  COOKIE_REFRESH,
  COOKIE_SESION,
  DURACION_ACCESS_S,
  DURACION_REFRESH_S,
  firmarSesion,
  opcionesCookie,
  type DatosSesion,
} from './sesion';
import type { Rol } from './rutas';

/** Respuesta de sesión del backend (/api/v1/auth/login, /refresh, /cambiar-password). */
export interface SesionBackend {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  usuario: {
    id: number;
    login: string;
    nombre: string;
    roles: Rol[];
    integranteId: number | null;
    tesoreroId: number | null;
    debeCambiarPassword: boolean;
  };
}

export function backendUrl(path: string): string {
  const base = process.env.BACKEND_URL ?? 'http://localhost:8080';
  return base.replace(/\/$/, '') + path;
}

export function datosSesion(s: SesionBackend): DatosSesion {
  const u = s.usuario;
  return {
    id: u.id,
    nombre: u.nombre,
    roles: u.roles,
    integranteId: u.integranteId,
    tesoreroId: u.tesoreroId,
    debeCambiarPassword: u.debeCambiarPassword,
  };
}

/** Guarda access, refresh y la sesión firmada en cookies httpOnly, Secure, SameSite=Lax. */
export async function escribirCookies(res: NextResponse, s: SesionBackend): Promise<void> {
  res.cookies.set(COOKIE_ACCESS, s.accessToken, opcionesCookie(Math.min(s.expiresIn, DURACION_ACCESS_S)));
  res.cookies.set(COOKIE_REFRESH, s.refreshToken, opcionesCookie(DURACION_REFRESH_S, '/api'));
  res.cookies.set(COOKIE_SESION, await firmarSesion(datosSesion(s)), opcionesCookie(DURACION_REFRESH_S));
}

export function borrarCookies(res: NextResponse): void {
  res.cookies.set(COOKIE_ACCESS, '', opcionesCookie(0));
  res.cookies.set(COOKIE_REFRESH, '', opcionesCookie(0, '/api'));
  res.cookies.set(COOKIE_SESION, '', opcionesCookie(0));
}

/** Protección CSRF adicional a SameSite=Lax: las mutaciones deben venir del mismo origen. */
export function mismoOrigen(req: Request): boolean {
  const origen = req.headers.get('origin');
  if (!origen) return true;
  const host = req.headers.get('x-forwarded-host') ?? req.headers.get('host');
  try {
    return new URL(origen).host === host;
  } catch {
    return false;
  }
}

export function ipCliente(req: Request): string | null {
  return req.headers.get('x-forwarded-for')?.split(',')[0]?.trim() ?? req.headers.get('x-real-ip');
}

export function problema(status: number, titulo: string, detalle: string): NextResponse {
  return NextResponse.json({ status, title: titulo, detail: detalle }, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  });
}

/** Reenvía al backend una petición pública (sin sesión), con la IP del cliente para el rate limit. */
export async function reenviarPublico(req: Request, ruta: string): Promise<NextResponse> {
  if (!mismoOrigen(req)) return problema(403, 'Origen no permitido', 'Solicitud de otro origen');
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const ip = ipCliente(req);
  if (ip) headers['X-Forwarded-For'] = ip;
  let res: Response;
  try {
    res = await fetch(backendUrl(ruta), { method: 'POST', headers, body: await req.text(), cache: 'no-store' });
  } catch {
    return problema(503, 'Servicio no disponible', 'No fue posible contactar el servidor. Intente de nuevo.');
  }
  if (res.status === 202 || res.status === 204) return new NextResponse(null, { status: res.status });
  return new NextResponse(await res.text(), {
    status: res.status,
    headers: { 'Content-Type': res.headers.get('content-type') ?? 'application/problem+json' },
  });
}
