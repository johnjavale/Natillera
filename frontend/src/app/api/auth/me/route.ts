import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { COOKIE_SESION, leerSesion } from '@/lib/sesion';

/** Datos no sensibles de la sesión para la interfaz (nombre y roles). */
export async function GET() {
  const sesion = await leerSesion((await cookies()).get(COOKIE_SESION)?.value);
  if (!sesion) return NextResponse.json({ status: 401, detail: 'No autenticado' }, { status: 401 });
  return NextResponse.json(sesion);
}
