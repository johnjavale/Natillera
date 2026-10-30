import { NextResponse, type NextRequest } from 'next/server';
import { COOKIE_SESION, leerSesion } from '@/lib/sesion';
import { inicioPara, rutaPermitida, RUTAS_PUBLICAS } from '@/lib/rutas';

/**
 * Protege las páginas por rol leyendo la cookie de sesión firmada (claim no sensible).
 * Es solo experiencia de usuario: la autorización real la hace el backend en cada endpoint.
 */
export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const sesion = await leerSesion(req.cookies.get(COOKIE_SESION)?.value);

  if (RUTAS_PUBLICAS.includes(pathname)) {
    if (sesion && !sesion.debeCambiarPassword) {
      return NextResponse.redirect(new URL(inicioPara(sesion.roles), req.url));
    }
    return NextResponse.next();
  }
  if (!sesion) {
    const login = new URL('/login', req.url);
    if (pathname !== '/') login.searchParams.set('volver', pathname);
    return NextResponse.redirect(login);
  }
  if (sesion.debeCambiarPassword && pathname !== '/cambiar-password') {
    return NextResponse.redirect(new URL('/cambiar-password', req.url));
  }
  if (pathname === '/' || !rutaPermitida(pathname, sesion.roles)) {
    return NextResponse.redirect(new URL(inicioPara(sesion.roles), req.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico|natillera_familiar.svg|.*\\.(?:svg|png|ico)$).*)'],
};
