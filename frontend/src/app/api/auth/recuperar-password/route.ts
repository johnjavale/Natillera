import { reenviarPublico } from '@/lib/bff';

/** "¿Olvidó su contraseña?": el backend responde 202 exista o no el correo. */
export async function POST(req: Request) {
  return reenviarPublico(req, '/api/v1/auth/recuperar-password');
}
