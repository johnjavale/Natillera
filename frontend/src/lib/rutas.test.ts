import { inicioPara, menuPara, rutaPermitida } from './rutas';

describe('guardas de rutas', () => {
  it('el integrante solo ve Mi cuenta y las rifas', () => {
    expect(rutaPermitida('/mi-cuenta', ['INTEGRANTE'])).toBe(true);
    expect(rutaPermitida('/actividades', ['INTEGRANTE'])).toBe(true);
    expect(rutaPermitida('/admin', ['INTEGRANTE'])).toBe(false);
    expect(rutaPermitida('/tesoreria', ['INTEGRANTE'])).toBe(false);
    expect(rutaPermitida('/prestamos/1', ['INTEGRANTE'])).toBe(false);
    expect(rutaPermitida('/reportes', ['INTEGRANTE'])).toBe(false);
  });

  it('el tesorero no entra a administración', () => {
    expect(rutaPermitida('/tesoreria/aportes', ['TESORERO', 'INTEGRANTE'])).toBe(true);
    expect(rutaPermitida('/prestamos/nuevo', ['TESORERO'])).toBe(true);
    expect(rutaPermitida('/admin/usuarios', ['TESORERO'])).toBe(false);
  });

  it('el admin entra a todo', () => {
    for (const ruta of ['/admin', '/admin/ciclo', '/tesoreria', '/deudores', '/reparticion', '/mi-cuenta']) {
      expect(rutaPermitida(ruta, ['ADMIN'])).toBe(true);
    }
  });

  it('no confunde prefijos parecidos', () => {
    expect(rutaPermitida('/administracion', ['TESORERO'])).toBe(false);
    expect(rutaPermitida('/desconocida', ['ADMIN'])).toBe(false);
  });

  it('página de inicio y menú por rol', () => {
    expect(inicioPara(['INTEGRANTE'])).toBe('/mi-cuenta');
    expect(inicioPara(['TESORERO', 'INTEGRANTE'])).toBe('/tesoreria');
    expect(inicioPara(['ADMIN', 'INTEGRANTE'])).toBe('/admin');
    expect(menuPara(['INTEGRANTE'], true).map((m) => m.href)).toEqual(['/mi-cuenta', '/actividades']);
    expect(menuPara(['ADMIN'], false).map((m) => m.href)).not.toContain('/mi-cuenta');
  });
});
