import { NextResponse } from 'next/server';
import { backendUrl, escribirCookies, ipCliente, mismoOrigen, problema, type SesionBackend } from '@/lib/bff';
import { inicioPara } from '@/lib/rutas';

export async function POST(req: Request) {
  if (!mismoOrigen(req)) return problema(403, 'Origen no permitido', 'Solicitud de otro origen');
  const cuerpo = await req.text();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const ip = ipCliente(req);
  if (ip) headers['X-Forwarded-For'] = ip;

  let res: Response;
  try {
    res = await fetch(backendUrl('/api/v1/auth/login'), { method: 'POST', headers, body: cuerpo, cache: 'no-store' });
  } catch {
    return problema(503, 'Servicio no disponible', 'No fue posible contactar el servidor. Intente de nuevo.');
  }
  if (!res.ok) {
    return new NextResponse(await res.text(), {
      status: res.status,
      headers: { 'Content-Type': res.headers.get('content-type') ?? 'application/problem+json' },
    });
  }
  const sesion = (await res.json()) as SesionBackend;
  const destino = sesion.usuario.debeCambiarPassword ? '/cambiar-password' : inicioPara(sesion.usuario.roles);
  const respuesta = NextResponse.json({ usuario: sesion.usuario, destino });
  await escribirCookies(respuesta, sesion);
  return respuesta;
}
