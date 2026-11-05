import { reenviarPublico } from '@/lib/bff';

/** Fija la contraseña nueva con el token del enlace del correo (204, o 422 si el enlace ya no sirve). */
export async function POST(req: Request) {
  return reenviarPublico(req, '/api/v1/auth/restablecer-password');
}
