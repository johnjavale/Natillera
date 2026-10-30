// @vitest-environment node
import { firmarSesion, leerSesion, type DatosSesion } from './sesion';

const datos: DatosSesion = {
  id: 1, nombre: 'Admin', roles: ['ADMIN'], integranteId: 1, tesoreroId: null, debeCambiarPassword: false,
};

describe('cookie de sesión firmada', () => {
  beforeEach(() => {
    process.env.SESSION_SECRET = 'secreto-de-pruebas-de-sesion-0123456789abcdef';
  });

  it('firma y lee los claims no sensibles', async () => {
    const token = await firmarSesion(datos);
    expect(await leerSesion(token)).toEqual(datos);
  });

  it('rechaza una cookie alterada o firmada con otra clave', async () => {
    const token = await firmarSesion(datos);
    const [h, , s] = token.split('.');
    const payloadFalso = Buffer.from(JSON.stringify({ ...datos, roles: ['ADMIN', 'TESORERO'] })).toString('base64url');
    expect(await leerSesion(`${h}.${payloadFalso}.${s}`)).toBeNull();
    process.env.SESSION_SECRET = 'otro-secreto-completamente-distinto-0123456789';
    expect(await leerSesion(token)).toBeNull();
    expect(await leerSesion(undefined)).toBeNull();
  });
});
