import type { Rol } from './rutas';

/** Montos llegan del backend como número con 2 decimales (BigDecimal serializado). */
export type Monto = number;

export interface Ciclo {
  id: number;
  nombre: string;
  fechaInicio: string;
  numeroMeses: number;
  tasaInteresMensualPrestamos: number;
  gastosAdministrativos: Monto;
  maxCuotasPrestamo: number;
  tasaEACajitasNu: number;
  tasaMensualNu: number;
  maxActividades: number;
  permitirPrestamoDespuesDelCierre: boolean;
  estado: 'INSCRIPCION' | 'ACTIVO' | 'CERRADO';
  fechaCierre: string;
  numeroIntegrantes: number;
  mesesTranscurridos: number;
  hoy: string;
}

export interface UsuarioSesion {
  id: number;
  login: string;
  nombre: string;
  roles: Rol[];
  integranteId: number | null;
  tesoreroId: number | null;
  debeCambiarPassword: boolean;
}

export interface Integrante {
  id: number;
  usuarioId: number | null;
  nombre: string;
  telefono: string | null;
  cuotaMensual: Monto;
  tesoreroId: number | null;
  nombreTesorero: string | null;
  cajitaId: number | null;
  nombreCajitaNu: string;
  activo: boolean;
  ahorroEsperado: Monto;
  porcentajeDelTotal: number;
  rendimientoProyectadoCajita: Monto;
  saldoCajita: Monto;
}

export interface EstadoAportes {
  integranteId: number;
  nombre: string;
  cuotaMensual: Monto;
  aportesPorMes: Monto[];
  totalAportado: Monto;
  ahorroEsperado: Monto;
  diferencia: Monto;
  cumplimiento: number;
  esperadoALaFecha: Monto;
  atraso: Monto;
  enAtraso: boolean;
}

export interface MatrizAportes {
  numeroMeses: number;
  mesesTranscurridos: number;
  filas: EstadoAportes[];
  totales: Array<{ mes: number; totalReal: Monto; totalEsperado: Monto; acumuladoReal: Monto; acumuladoEsperado: Monto }>;
}

export interface Cajita {
  cajitaId: number;
  nombreCajita: string;
  integranteId: number;
  nombreIntegrante: string;
  tesoreroId: number | null;
  nombreTesorero: string | null;
  entradas: Monto;
  salidas: Monto;
  saldo: Monto;
  saldoApp: Monto | null;
  fechaSaldoApp: string | null;
  diferencia: Monto;
  cuadra: boolean;
}

export type TipoMovimiento =
  | 'AHORRO'
  | 'PAGO_CUOTA_PRESTAMO'
  | 'PAGO_BOLETAS_RIFA'
  | 'RENDIMIENTO_NU'
  | 'OTRO_INGRESO'
  | 'DESEMBOLSO_PRESTAMO'
  | 'COMPRA_PREMIO'
  | 'GASTO'
  | 'RETIRO_ENTREGA_FINAL';

export interface Movimiento {
  id: number;
  fecha: string;
  cajitaId: number;
  nombreCajita: string;
  integranteId: number;
  nombreIntegrante: string;
  tesoreroId: number | null;
  nombreTesorero: string | null;
  tipo: TipoMovimiento;
  tipoEtiqueta: string;
  descripcion: string | null;
  entrada: Monto;
  salida: Monto;
  prestamoId: number | null;
  actividadId: number | null;
  anulaA: number | null;
  anuladoPor: number | null;
  creadoPor: string | null;
}

export interface TesoreroSaldo {
  tesoreroId: number;
  nombre: string;
  numeroCajitas: number;
  saldoRegistro: Monto;
  saldoApp: Monto;
  diferencia: Monto;
}

export interface ResumenTesoreria {
  porTipo: Array<{ tipo: TipoMovimiento; etiqueta: string; entradas: Monto; salidas: Monto }>;
  porMes: Array<{ mes: number; entradas: Monto; salidas: Monto; saldoAcumulado: Monto; saldoAcumuladoPorTesorero: Record<string, Monto> }>;
  tesoreros: Array<{ id: number; nombre: string }>;
  porTesorero: TesoreroSaldo[];
}

export interface Cuota {
  numero: number;
  mesVencimiento: number;
  capital: Monto;
  interes: Monto;
  valor: Monto;
  pagada: boolean;
  vencida: boolean;
}

export interface Prestamo {
  id: number | null;
  integranteId: number;
  nombreIntegrante: string | null;
  cajitaOrigenId: number;
  fechaDesembolso: string;
  monto: Monto;
  mesDelPrestamo: number;
  numeroCuotas: number;
  tasaMensual: number;
  interesPorCuota: Monto;
  abonoCapitalPorCuota: Monto;
  valorCuota: Monto;
  totalIntereses: Monto;
  totalAPagar: Monto;
  cuotasPagadas: number;
  interesesRecibidos: Monto;
  saldoPendiente: Monto;
  cuotasVencidas: number;
  saldoVencido: Monto;
  estado: 'VIGENTE' | 'EN_MORA' | 'PAGADO';
  plan: Cuota[];
  pagos: Array<{ id: number; numeroCuota: number; fecha: string; monto: Monto; cajitaDestinoId: number; anulado: boolean; motivoAnulacion: string | null }>;
}

export interface ProyeccionIntereses {
  filas: Array<{ prestamoId: number; nombreIntegrante: string; interesesPorMes: Monto[] }>;
  totalesPorMes: Monto[];
  total: Monto;
}

export interface Actividad {
  id: number;
  nombre: string;
  fechaSorteo: string;
  valorBoleta: Monto;
  boletasPorIntegrante: number;
  costoPremio: Monto;
  otrosGastos: Monto;
  ganador: string | null;
  mesDeLaActividad: number;
  recaudoEsperado: Monto;
  recaudoReal: Monto;
  costos: Monto;
  utilidadEsperada: Monto;
  utilidadReal: Monto;
  utilidadEsperadaPorIntegrante: Monto;
  utilidadRealPorIntegrante: Monto;
  boletas: Array<{ integranteId: number; nombre: string; boletas: number; valorAPagar: Monto; pagado: Monto; pendiente: Monto }>;
}

export interface ConceptosReparticion {
  interesesPrestamos: Monto;
  gastosAdministrativos: Monto;
  interesesNetos: Monto;
  utilidadActividades: Monto;
  utilidadActividadesPorIntegrante: Monto;
}

export interface FilaReparticion {
  integranteId: number;
  nombre: string;
  ahorroEsperado: Monto;
  participacionProyectada: number;
  interesesProyectados: Monto;
  actividadesProyectadas: Monto;
  rendimientoNuProyectado: Monto;
  totalProyectado: Monto;
  ahorroReal: Monto;
  participacionReal: number;
  interesesReales: Monto;
  actividadesReales: Monto;
  rendimientoNuReal: Monto;
  totalReal: Monto;
  puesto: number;
  puestoReal: number;
}

export interface Reparticion {
  proyectado: ConceptosReparticion | null;
  real: ConceptosReparticion | null;
  filas: FilaReparticion[];
  totalProyectado: Monto | null;
  totalReal: Monto | null;
  regla: string;
}

export interface MesResumen {
  mes: number;
  fechaMes: string;
  ahorroEsperado: Monto;
  ahorroReal: Monto;
  prestamosEntregados: Monto;
  capitalRecuperado: Monto;
  capitalRecuperadoReal: Monto;
  intereses: Monto;
  utilidadActividades: Monto;
  flujoNeto: Monto;
  cajaDisponibleAcumulada: Monto;
  fondoTotalAcumulado: Monto;
  alertaCajaNegativa: boolean;
}

export interface Resumen {
  mesesTranscurridos: number;
  meses: MesResumen[];
  kpis: {
    ahorroEsperadoAnual: Monto;
    ahorroRealALaFecha: Monto;
    totalPrestado: Monto;
    interesesProyectados: Monto;
    interesesRecibidos: Monto;
    saldoPorCobrar: Monto;
    utilidadEsperadaActividades: Monto;
    rendimientoProyectadoNu: Monto;
    totalARepartirProyectado: Monto;
    rentabilidadProyectada: number;
    saldoTotalCajitas: Monto;
    diferenciaNu: Monto;
  };
  distribucion: { ahorro: Monto; intereses: Monto; actividades: Monto; rendimientoNu: Monto };
  alertaCajaNegativa: boolean;
}

export interface Deudor {
  integranteId: number;
  nombre: string;
  tesoreroId: number | null;
  nombreTesorero: string | null;
  atrasoAportes: Monto;
  saldoPrestamos: Monto;
  vencidoPrestamos: Monto;
  boletasPendientes: Monto;
  total: Monto;
  enMora: boolean;
}

export interface EstadoCuenta {
  resumen: {
    integranteId: number;
    nombre: string;
    nombreTesorero: string | null;
    cuotaMensual: Monto;
    ahorroEsperado: Monto;
    totalAportado: Monto;
    esperadoALaFecha: Monto;
    atraso: Monto;
    cumplimiento: number;
    saldoCajita: Monto;
    prestamosVigentes: number;
    saldoPrestamos: Monto;
    cuotasVencidas: number;
    boletasPendientes: Monto;
    interesesProyectados: Monto;
    totalProyectado: Monto;
    totalReal: Monto;
  };
  aportesPorMes: Monto[];
  movimientos: Movimiento[];
  prestamos: Prestamo[];
  boletas: Array<{ actividadId: number; actividad: string; boletas: number; valorAPagar: Monto; pagado: Monto; pendiente: Monto }>;
  reparto: {
    ahorroEsperado: Monto;
    interesesProyectados: Monto;
    actividadesProyectadas: Monto;
    rendimientoNuProyectado: Monto;
    totalProyectado: Monto;
    ahorroReal: Monto;
    interesesReales: Monto;
    actividadesReales: Monto;
    rendimientoNuReal: Monto;
    totalReal: Monto;
    participacionProyectada: number;
    puesto: number;
  } | null;
}

export interface Usuario {
  id: number;
  nombre: string;
  correo: string | null;
  celular: string | null;
  roles: Rol[];
  activo: boolean;
  debeCambiarPassword: boolean;
  integranteId: number | null;
  tesoreroId: number | null;
}

export interface RegistroAuditoria {
  id: number;
  entidad: string;
  entidadId: number | null;
  accion: string;
  detalle: string | null;
  usuario: string | null;
  fecha: string;
}
