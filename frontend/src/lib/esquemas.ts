import { z } from 'zod';

/** Esquemas Zod de los formularios. El backend vuelve a validar todo (Bean Validation + reglas de dominio). */

const monto = (mensaje = 'Ingrese un valor válido') =>
  z.coerce.number({ error: mensaje }).finite(mensaje);

/** Select o campo opcional: "" → undefined (z.coerce convertiría "" en 0). */
const vacioAIndefinido = (v: unknown) => (v === '' || v === null || v === undefined ? undefined : v);
const idOpcional = z.preprocess(vacioAIndefinido, z.coerce.number().int().positive().optional());

const fecha = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Fecha inválida');

export const loginSchema = z.object({
  login: z.string().trim().min(1, 'Ingrese su correo o celular').max(160),
  password: z.string().min(1, 'Ingrese su contraseña').max(72),
});

export const cambiarPasswordSchema = z
  .object({
    actual: z.string().min(1, 'Ingrese la contraseña actual'),
    nueva: z
      .string()
      .min(10, 'Mínimo 10 caracteres')
      .max(72, 'Máximo 72 caracteres')
      .regex(/[A-Za-zÁÉÍÓÚáéíóúÑñ]/, 'Debe incluir letras')
      .regex(/\d/, 'Debe incluir números'),
    confirmacion: z.string(),
  })
  .refine((d) => d.nueva === d.confirmacion, { path: ['confirmacion'], message: 'Las contraseñas no coinciden' })
  .refine((d) => d.nueva !== d.actual, { path: ['nueva'], message: 'Debe ser diferente a la actual' });

export const TIPOS_MANUALES = [
  { valor: 'AHORRO', etiqueta: 'Ahorro' },
  { valor: 'RENDIMIENTO_NU', etiqueta: 'Rendimiento Nu' },
  { valor: 'OTRO_INGRESO', etiqueta: 'Otro ingreso' },
  { valor: 'COMPRA_PREMIO', etiqueta: 'Compra premio' },
  { valor: 'GASTO', etiqueta: 'Gasto' },
  { valor: 'RETIRO_ENTREGA_FINAL', etiqueta: 'Retiro / entrega final' },
] as const;

export const movimientoSchema = z.object({
  cajitaId: z.coerce.number().int().positive('Seleccione la cajita'),
  fecha,
  tipo: z.enum(['AHORRO', 'RENDIMIENTO_NU', 'OTRO_INGRESO', 'COMPRA_PREMIO', 'GASTO', 'RETIRO_ENTREGA_FINAL']),
  descripcion: z.string().max(300).optional(),
  valor: monto().positive('El valor debe ser mayor que cero'),
});

export const aportesLoteSchema = z.object({
  fecha,
  descripcion: z.string().max(300).optional(),
  aportes: z
    .array(z.object({ integranteId: z.number().int(), monto: monto().min(0, 'No puede ser negativo') }))
    .refine((a) => a.some((x) => x.monto > 0), 'Ingrese al menos un aporte mayor que cero'),
});

export function prestamoSchema(maxCuotas: number) {
  return z.object({
    integranteId: z.coerce.number().int().positive('Seleccione el integrante'),
    cajitaOrigenId: z.coerce.number().int().positive('Seleccione la cajita de origen'),
    fechaDesembolso: fecha,
    monto: monto().positive('El monto debe ser mayor que cero'),
    numeroCuotas: z.coerce
      .number()
      .int()
      .min(1, 'Mínimo 1 cuota')
      .max(maxCuotas, `Los préstamos no pueden ser a más de ${maxCuotas} cuotas (un año)`),
    tasaMensual: z.preprocess(vacioAIndefinido, monto().min(0).max(1).optional()),
  });
}

export const pagoCuotaSchema = z.object({
  fecha,
  cajitaDestinoId: idOpcional,
});

export const actividadSchema = z.object({
  nombre: z.string().trim().min(1, 'El nombre es obligatorio').max(120),
  fechaSorteo: fecha,
  valorBoleta: monto().min(0),
  boletasPorIntegrante: z.coerce.number().int().min(0).max(1000),
  costoPremio: monto().min(0),
  otrosGastos: monto().min(0),
  ganador: z.string().max(160).optional(),
});

export const integranteSchema = z.object({
  nombre: z.string().trim().min(1, 'El nombre es obligatorio').max(120),
  telefono: z.string().regex(/^$|^[0-9+ ]{7,20}$/, 'Teléfono inválido').optional(),
  cuotaMensual: monto().min(0, 'La cuota no puede ser negativa'),
  tesoreroId: idOpcional,
  nombreCajitaNu: z.string().max(160).optional(),
  activo: z.boolean().default(true),
});

export const cicloSchema = z.object({
  nombre: z.string().trim().min(1).max(120),
  fechaInicio: fecha,
  numeroMeses: z.coerce.number().int().min(1).max(12),
  tasaInteresMensualPrestamos: monto().min(0).max(1),
  gastosAdministrativos: monto().min(0),
  maxCuotasPrestamo: z.coerce.number().int().min(1).max(12),
  tasaEACajitasNu: monto().min(0).max(1),
  maxActividades: z.coerce.number().int().min(0).max(12),
  permitirPrestamoDespuesDelCierre: z.boolean(),
});

export const usuarioSchema = z
  .object({
    nombre: z.string().trim().min(1, 'El nombre es obligatorio').max(120),
    correo: z.string().trim().email('Correo inválido').optional().or(z.literal('')),
    celular: z.string().regex(/^$|^[0-9+ ]{7,20}$/, 'Celular inválido').optional(),
    roles: z.array(z.enum(['ADMIN', 'TESORERO', 'INTEGRANTE'])).min(1, 'Asigne al menos un rol'),
    contrasenaTemporal: z.string().min(10, 'Mínimo 10 caracteres').regex(/\d/, 'Debe incluir números'),
    integranteId: idOpcional,
  })
  .refine((d) => Boolean(d.correo) || Boolean(d.celular), { path: ['correo'], message: 'Indique correo o celular' });

export const saldoReportadoSchema = z.object({
  fecha,
  saldoApp: monto().min(0, 'No puede ser negativo'),
});
