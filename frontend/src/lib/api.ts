/** Cliente del navegador: todas las llamadas pasan por el proxy BFF /api/backend (cookies httpOnly). */

export interface ProblemDetail {
  status: number;
  title?: string;
  detail?: string;
  type?: string;
  errores?: Record<string, string>;
}

export class ApiError extends Error {
  constructor(public readonly problema: ProblemDetail) {
    super(problema.detail ?? problema.title ?? 'Error inesperado');
  }

  get status(): number {
    return this.problema.status;
  }
}

async function leerError(res: Response): Promise<ApiError> {
  try {
    const cuerpo = (await res.json()) as ProblemDetail;
    return new ApiError({ ...cuerpo, status: res.status });
  } catch {
    return new ApiError({ status: res.status, detail: 'No se pudo completar la solicitud' });
  }
}

async function solicitar<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<T> {
  const res = await fetch(`/api/backend${ruta}`, {
    method: metodo,
    headers: cuerpo === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    credentials: 'same-origin',
  });
  if (res.status === 401 && typeof window !== 'undefined') {
    window.location.href = '/login?expirada=1';
  }
  if (res.status === 403) {
    const err = await leerError(res);
    if (err.problema.type?.endsWith('cambio-password-obligatorio') && typeof window !== 'undefined') {
      window.location.href = '/cambiar-password';
    }
    throw err;
  }
  if (!res.ok) throw await leerError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  get: <T>(ruta: string) => solicitar<T>('GET', ruta),
  post: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('POST', ruta, cuerpo ?? {}),
  put: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('PUT', ruta, cuerpo ?? {}),
};

/** URL de descarga de un reporte exportado (el proxy agrega el token). */
export function urlDescarga(ruta: string, formato: 'xlsx' | 'csv'): string {
  const sep = ruta.includes('?') ? '&' : '?';
  return `/api/backend${ruta}${sep}formato=${formato}`;
}
