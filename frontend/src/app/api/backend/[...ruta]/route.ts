import { cookies } from 'next/headers';
import { NextResponse, type NextRequest } from 'next/server';
import { backendUrl, borrarCookies, escribirCookies, mismoOrigen, problema, type SesionBackend } from '@/lib/bff';
import { refrescar, tokenVigente } from '@/lib/proxy';
import { COOKIE_REFRESH } from '@/lib/sesion';

/**
 * Proxy BFF: /api/backend/<ruta> → BACKEND_URL/api/v1/<ruta> con Authorization: Bearer (desde la cookie httpOnly).
 * Si el access token expiró, refresca una vez (rotación) y reintenta.
 */
async function proxy(req: NextRequest, ctx: { params: Promise<{ ruta: string[] }> }) {
  const metodo = req.method.toUpperCase();
  if (metodo !== 'GET' && !mismoOrigen(req)) return problema(403, 'Origen no permitido', 'Solicitud de otro origen');

  const { ruta } = await ctx.params;
  const destino = backendUrl(`/api/v1/${ruta.map(encodeURIComponent).join('/')}${req.nextUrl.search}`);
  const cuerpo = metodo === 'GET' || metodo === 'HEAD' ? undefined : await req.arrayBuffer();

  const vigente = await tokenVigente();
  const access = vigente.access;
  let renovada = vigente.renovada;
  if (!access) {
    const res = problema(401, 'No autenticado', 'Su sesión expiró, ingrese de nuevo');
    borrarCookies(res);
    return res;
  }

  const llamar = (token: string) =>
    fetch(destino, {
      method: metodo,
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: req.headers.get('accept') ?? 'application/json',
        ...(cuerpo ? { 'Content-Type': req.headers.get('content-type') ?? 'application/json' } : {}),
      },
      body: cuerpo,
      cache: 'no-store',
    });

  let res: Response;
  try {
    res = await llamar(access);
    if (res.status === 401 && !renovada) {
      const refresh = (await cookies()).get(COOKIE_REFRESH)?.value;
      const nueva: SesionBackend | null = refresh ? await refrescar(refresh) : null;
      if (nueva) {
        renovada = nueva;
        res = await llamar(nueva.accessToken);
      }
    }
  } catch {
    return problema(503, 'Servicio no disponible', 'No fue posible contactar el servidor. Intente de nuevo.');
  }

  const headers = new Headers();
  for (const h of ['content-type', 'content-disposition', 'retry-after']) {
    const v = res.headers.get(h);
    if (v) headers.set(h, v);
  }
  headers.set('Cache-Control', 'no-store');
  const respuesta = new NextResponse(res.status === 204 ? null : res.body, { status: res.status, headers });
  if (renovada) await escribirCookies(respuesta, renovada);
  if (res.status === 401) borrarCookies(respuesta);
  return respuesta;
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
