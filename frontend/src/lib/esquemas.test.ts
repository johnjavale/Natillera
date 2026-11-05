import { aportesLoteSchema, cambiarPasswordSchema, integranteSchema, loginSchema, movimientoSchema, prestamoSchema, recuperarPasswordSchema, restablecerPasswordSchema, usuarioSchema } from './esquemas';

describe('esquemas Zod de formularios', () => {
  it('login exige usuario y contraseña', () => {
    expect(loginSchema.safeParse({ login: '', password: '' }).success).toBe(false);
    expect(loginSchema.safeParse({ login: 'admin@natillera.local', password: 'x' }).success).toBe(true);
  });

  it('contraseña nueva: longitud, letras y números, confirmación', () => {
    const base = { actual: 'Vieja12345' };
    expect(cambiarPasswordSchema.safeParse({ ...base, nueva: 'corta1', confirmacion: 'corta1' }).success).toBe(false);
    expect(cambiarPasswordSchema.safeParse({ ...base, nueva: 'sololetrasaqui', confirmacion: 'sololetrasaqui' }).success).toBe(false);
    expect(cambiarPasswordSchema.safeParse({ ...base, nueva: 'Familia2027x', confirmacion: 'Otra2027xxx' }).success).toBe(false);
    expect(cambiarPasswordSchema.safeParse({ ...base, nueva: 'Familia2027x', confirmacion: 'Familia2027x' }).success).toBe(true);
  });

  it('recuperar y restablecer contraseña: correo válido y misma regla de clave nueva', () => {
    expect(recuperarPasswordSchema.safeParse({ correo: '' }).success).toBe(false);
    expect(recuperarPasswordSchema.safeParse({ correo: '3000000003' }).success).toBe(false);
    expect(recuperarPasswordSchema.safeParse({ correo: ' samay@natillera.local ' }).success).toBe(true);
    expect(restablecerPasswordSchema.safeParse({ nueva: 'corta1', confirmacion: 'corta1' }).success).toBe(false);
    expect(restablecerPasswordSchema.safeParse({ nueva: 'Familia2027x', confirmacion: 'Familia2027y' }).success).toBe(false);
    expect(restablecerPasswordSchema.safeParse({ nueva: 'Familia2027x', confirmacion: 'Familia2027x' }).success).toBe(true);
  });

  it('préstamo: máximo de cuotas del ciclo y monto positivo', () => {
    const s = prestamoSchema(12);
    const ok = { integranteId: '3', cajitaOrigenId: '7', fechaDesembolso: '2027-02-15', monto: '1000000', numeroCuotas: '6', tasaMensual: '' };
    const r = s.safeParse(ok);
    expect(r.success).toBe(true);
    expect(r.success && r.data.tasaMensual).toBeUndefined();
    const trece = s.safeParse({ ...ok, numeroCuotas: '13' });
    expect(trece.success).toBe(false);
    expect(trece.error?.issues[0]?.message).toBe('Los préstamos no pueden ser a más de 12 cuotas (un año)');
    expect(s.safeParse({ ...ok, monto: '0' }).success).toBe(false);
  });

  it('movimiento: tipos derivados no se ofrecen y el valor debe ser positivo', () => {
    expect(movimientoSchema.safeParse({ cajitaId: 1, fecha: '2027-03-01', tipo: 'DESEMBOLSO_PRESTAMO', valor: 10 }).success).toBe(false);
    expect(movimientoSchema.safeParse({ cajitaId: 1, fecha: '2027-03-01', tipo: 'AHORRO', valor: -1 }).success).toBe(false);
    expect(movimientoSchema.safeParse({ cajitaId: 1, fecha: '2027-03-01', tipo: 'AHORRO', valor: '100000' }).success).toBe(true);
  });

  it('aportes en lote: al menos uno mayor que cero', () => {
    expect(aportesLoteSchema.safeParse({ fecha: '2027-03-05', aportes: [{ integranteId: 1, monto: '0' }] }).success).toBe(false);
    expect(aportesLoteSchema.safeParse({ fecha: '2027-03-05', aportes: [{ integranteId: 1, monto: '50000' }] }).success).toBe(true);
  });

  it('integrante con tesorero sin asignar ("") y usuario con correo o celular', () => {
    const r = integranteSchema.safeParse({ nombre: 'Ana', cuotaMensual: '100000', tesoreroId: '' });
    expect(r.success && r.data.tesoreroId).toBeUndefined();
    expect(usuarioSchema.safeParse({ nombre: 'Ana', correo: '', celular: '', roles: ['INTEGRANTE'], contrasenaTemporal: 'Temporal2027' }).success).toBe(false);
    expect(usuarioSchema.safeParse({ nombre: 'Ana', correo: 'ana@x.co', roles: ['INTEGRANTE'], contrasenaTemporal: 'Temporal2027' }).success).toBe(true);
  });
});
