export type Rol = 'ADMIN' | 'TESORERO' | 'INTEGRANTE';

export interface ItemMenu {
  href: string;
  etiqueta: string;
  roles: Rol[];
}

/** Menú visible por rol. La autorización real está siempre en el backend. */
export const MENU: ItemMenu[] = [
  { href: '/admin', etiqueta: 'Resumen', roles: ['ADMIN'] },
  { href: '/mi-cuenta', etiqueta: 'Mi cuenta', roles: ['ADMIN', 'TESORERO', 'INTEGRANTE'] },
  { href: '/tesoreria', etiqueta: 'Mis cajitas', roles: ['TESORERO', 'ADMIN'] },
  { href: '/tesoreria/aportes', etiqueta: 'Aportes del mes', roles: ['TESORERO', 'ADMIN'] },
  { href: '/tesoreria/movimientos', etiqueta: 'Movimientos', roles: ['TESORERO', 'ADMIN'] },
  { href: '/aportes', etiqueta: 'Ahorros mensuales', roles: ['TESORERO', 'ADMIN'] },
  { href: '/prestamos', etiqueta: 'Préstamos', roles: ['TESORERO', 'ADMIN'] },
  { href: '/deudores', etiqueta: 'Deudores', roles: ['TESORERO', 'ADMIN'] },
  { href: '/actividades', etiqueta: 'Rifas', roles: ['ADMIN', 'TESORERO', 'INTEGRANTE'] },
  { href: '/reparticion', etiqueta: 'Repartición', roles: ['ADMIN', 'TESORERO'] },
  { href: '/reportes', etiqueta: 'Reportes', roles: ['TESORERO', 'ADMIN'] },
  { href: '/admin/integrantes', etiqueta: 'Integrantes', roles: ['ADMIN'] },
  { href: '/admin/usuarios', etiqueta: 'Usuarios', roles: ['ADMIN'] },
  { href: '/admin/ciclo', etiqueta: 'Configuración', roles: ['ADMIN'] },
  { href: '/admin/auditoria', etiqueta: 'Auditoría', roles: ['ADMIN'] },
];

/** Prefijos de ruta y roles que pueden abrirlos (el más específico gana). */
const REGLAS: Array<{ prefijo: string; roles: Rol[] }> = [
  { prefijo: '/admin', roles: ['ADMIN'] },
  { prefijo: '/tesoreria', roles: ['TESORERO', 'ADMIN'] },
  { prefijo: '/aportes', roles: ['TESORERO', 'ADMIN'] },
  { prefijo: '/prestamos', roles: ['TESORERO', 'ADMIN'] },
  { prefijo: '/deudores', roles: ['TESORERO', 'ADMIN'] },
  { prefijo: '/reportes', roles: ['TESORERO', 'ADMIN'] },
  { prefijo: '/reparticion', roles: ['TESORERO', 'ADMIN'] },
  { prefijo: '/actividades', roles: ['ADMIN', 'TESORERO', 'INTEGRANTE'] },
  { prefijo: '/mi-cuenta', roles: ['ADMIN', 'TESORERO', 'INTEGRANTE'] },
];

export const RUTAS_PUBLICAS = ['/login'];

/** Guarda de rutas usada por middleware.ts. */
export function rutaPermitida(ruta: string, roles: readonly Rol[]): boolean {
  const regla = REGLAS.filter((r) => ruta === r.prefijo || ruta.startsWith(r.prefijo + '/'))
    .sort((a, b) => b.prefijo.length - a.prefijo.length)[0];
  if (!regla) return ruta === '/' || ruta === '/cambiar-password';
  return regla.roles.some((r) => roles.includes(r));
}

/** Página de inicio según el rol principal. */
export function inicioPara(roles: readonly Rol[]): string {
  if (roles.includes('ADMIN')) return '/admin';
  if (roles.includes('TESORERO')) return '/tesoreria';
  return '/mi-cuenta';
}

export function menuPara(roles: readonly Rol[], tieneIntegrante: boolean): ItemMenu[] {
  return MENU.filter((m) => m.roles.some((r) => roles.includes(r)))
    .filter((m) => m.href !== '/mi-cuenta' || tieneIntegrante);
}
