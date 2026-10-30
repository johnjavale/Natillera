import { NextResponse } from 'next/server';
import { backendUrl, escribirCookies, mismoOrigen, problema, type SesionBackend } from '@/lib/bff';
import { tokenVigente } from '@/lib/proxy';
import { inicioPara } from '@/lib/rutas';

export async function POST(req: Request) {
  if (!mismoOrigen(req)) return problema(403, 'Origen no permitido', 'Solicitud de otro origen');
  const token = await tokenVigente();
  if (!token.access) return problema(401, 'No autenticado', 'Su sesión expiró, ingrese de nuevo');
  const res = await fetch(backendUrl('/api/v1/auth/cambiar-password'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token.access}` },
    body: await req.text(),
    cache: 'no-store',
  });
  if (!res.ok) {
    return new NextResponse(await res.text(), {
      status: res.status,
      headers: { 'Content-Type': res.headers.get('content-type') ?? 'application/problem+json' },
    });
  }
  const sesion = (await res.json()) as SesionBackend;
  const respuesta = NextResponse.json({ destino: inicioPara(sesion.usuario.roles) });
  await escribirCookies(respuesta, sesion);
  return respuesta;
}
