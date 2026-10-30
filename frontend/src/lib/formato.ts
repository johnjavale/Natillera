const cop = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

const porcentaje = new Intl.NumberFormat('es-CO', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/** $1.250.000 — sin decimales, redondeo al presentar. */
export function formatoCOP(valor: number | string | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') return '—';
  const n = typeof valor === 'string' ? Number(valor) : valor;
  if (Number.isNaN(n)) return '—';
  // Intl usa espacio duro entre el símbolo y el número; se normaliza a "$1.250.000"
  return cop.format(Math.round(n)).replace(/\s/g, '');
}

/** 0.08 → "8,0 %" */
export function formatoPorcentaje(valor: number | string | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') return '—';
  const n = typeof valor === 'string' ? Number(valor) : valor;
  // ICU varía entre "8,0 %" (espacio duro) y "8,0%" según el entorno: se normaliza
  return porcentaje.format(n).replace(/\s/g, '').replace('%', ' %');
}

/** "2027-03-20" → "20 mar 2027" (sin conversión de zona horaria: es una fecha de negocio). */
export function formatoFecha(iso: string | null | undefined): string {
  if (!iso) return '—';
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  if (!a || !m || !d) return iso;
  return `${d} ${MESES[m - 1]} ${a}`;
}

/** Nombre corto del mes m del ciclo dada la fecha de inicio ("2027-01-01", 3) → "mar 2027". */
export function nombreMesCiclo(fechaInicio: string, mes: number): string {
  const [a, m] = fechaInicio.split('-').map(Number);
  const indice = (m ?? 1) - 1 + (mes - 1);
  const anio = (a ?? 0) + Math.floor(indice / 12);
  return `${MESES[indice % 12]} ${anio}`;
}

export function hoyISO(): string {
  const ahora = new Date();
  const local = new Date(ahora.toLocaleString('en-US', { timeZone: 'America/Bogota' }));
  const mm = String(local.getMonth() + 1).padStart(2, '0');
  const dd = String(local.getDate()).padStart(2, '0');
  return `${local.getFullYear()}-${mm}-${dd}`;
}

export function aNumero(valor: number | string | null | undefined): number {
  if (valor === null || valor === undefined || valor === '') return 0;
  return typeof valor === 'string' ? Number(valor) : valor;
}
